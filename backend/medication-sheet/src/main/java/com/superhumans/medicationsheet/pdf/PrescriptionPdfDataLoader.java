package com.superhumans.medicationsheet.pdf;

import com.superhumans.exception.NotFoundException;
import com.superhumans.medicationsheet.entity.PrescriptionExecution;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.medicationsheet.repository.PrescriptionExecutionRepository;
import com.superhumans.medicationsheet.repository.PrescriptionItemRepository;
import com.superhumans.medicationsheet.repository.PrescriptionListRepository;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.repository.core.SystemSettingsRepository;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Loads one consistent snapshot for Phase 17 PDF generation: the list with
 * all non-deleted items/days/parts plus executions, the MIS patient, and the
 * institution settings.
 *
 * <h2>Transaction shape (#383)</h2>
 *
 * The snapshot spans two databases (medication data + core institution
 * settings) and one external system (MIS). A single {@code @Transactional}
 * method bound to the {@code @Primary} chained manager would hold one
 * connection in <em>every</em> pool for the whole call — including the
 * seconds-long MIS HTTP round trip — which is how a slow MIS exhausts
 * {@code APP_DATASOURCE_POOL_MAX} and cascades timeouts to unrelated
 * requests. Instead each database is opened only for the time its own query
 * runs, and the MIS call happens with no transaction open at all:
 *
 * <ol>
 *   <li>{@code medTransactionManager} — list/items/days/parts/executions
 *       (lazy collections must be initialized here)</li>
 *   <li>{@code coreTransactionManager} — the two institution settings</li>
 *   <li>no transaction — {@code misService.getPatient}</li>
 * </ol>
 */
@Service
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class PrescriptionPdfDataLoader {

    PrescriptionListRepository listRepository;
    PrescriptionItemRepository itemRepository;
    PrescriptionExecutionRepository executionRepository;
    MisService misService;
    SystemSettingsRepository systemSettingsRepository;
    TransactionTemplate medTransactionTemplate;
    TransactionTemplate coreTransactionTemplate;

    public PrescriptionPdfDataLoader(
            PrescriptionListRepository listRepository,
            PrescriptionItemRepository itemRepository,
            PrescriptionExecutionRepository executionRepository,
            MisService misService,
            SystemSettingsRepository systemSettingsRepository,
            @Qualifier("medTransactionManager") PlatformTransactionManager medTransactionManager,
            @Qualifier("coreTransactionManager") PlatformTransactionManager coreTransactionManager) {
        this.listRepository = listRepository;
        this.itemRepository = itemRepository;
        this.executionRepository = executionRepository;
        this.misService = misService;
        this.systemSettingsRepository = systemSettingsRepository;
        this.medTransactionTemplate = new TransactionTemplate(medTransactionManager);
        this.medTransactionTemplate.setReadOnly(true);
        this.coreTransactionTemplate = new TransactionTemplate(coreTransactionManager);
        this.coreTransactionTemplate.setReadOnly(true);
    }

    /** Snapshot rows plus the patient id the MIS lookup needs. */
    private record PrescriptionData(PrescriptionPdfSnapshot snapshot, Long patientId) {
    }

    /** The two core-database settings, both fail-soft to an empty string. */
    private record InstitutionSettings(String name, String edrpou) {
    }

    public LoadedSnapshot load(UUID listId) {
        PrescriptionData data = medTransactionTemplate.execute(
                status -> readPrescriptionData(listId));

        InstitutionSettings settings = coreTransactionTemplate.execute(
                status -> new InstitutionSettings(loadSetting("institution_name"),
                        loadSetting("institution_edrpou")));

        // External call, deliberately outside every transaction (#383).
        Optional<PatientDTO> patient = data.patientId() == null
                ? Optional.empty()
                : misService.getPatient(data.patientId());

        return new LoadedSnapshot(data.snapshot(), patient, settings.name(), settings.edrpou());
    }

    /** Runs inside the medication-database transaction; see {@link #load}. */
    private PrescriptionData readPrescriptionData(UUID listId) {
        PrescriptionList list = listRepository.findById(listId)
                .filter(row -> !Boolean.TRUE.equals(row.getDeleted()))
                .orElseThrow(() -> new NotFoundException("Prescription list not found: " + listId));

        List<PrescriptionItem> items =
                itemRepository.findByListIdAndDeletedFalseOrderBySortOrderAscIdAsc(listId);

        List<UUID> partIds = new ArrayList<>();
        List<PrescriptionPdfSnapshot.Item> snapItems = new ArrayList<>();
        for (PrescriptionItem item : items) {
            List<PrescriptionPdfSnapshot.Day> days = new ArrayList<>();
            if (item.getDays() != null) {
                List<PrescriptionItemDay> ordered = item.getDays().stream()
                        .filter(day -> !Boolean.TRUE.equals(day.getDeleted()))
                        .sorted(Comparator.comparing(PrescriptionItemDay::getDayDate))
                        .toList();
                for (PrescriptionItemDay day : ordered) {
                    List<PrescriptionPdfSnapshot.Part> parts = new ArrayList<>();
                    if (day.getDayParts() != null) {
                        List<PrescriptionPdfSnapshot.Part> orderedParts = day.getDayParts().stream()
                                .filter(part -> !Boolean.TRUE.equals(part.getDeleted()))
                                .sorted(Comparator.comparingInt(part -> PrescriptionPdfPeriod.ORDER
                                        .indexOf(part.getPeriod())))
                                .map(part -> {
                                    partIds.add(part.getId());
                                    return new PrescriptionPdfSnapshot.Part(
                                            part.getId(),
                                            part.getPeriod(),
                                            part.getDose(),
                                            Boolean.TRUE.equals(part.getIsPlanned()),
                                            Boolean.TRUE.equals(part.getIsPlannedFinished()),
                                            Boolean.TRUE.equals(part.getIsCompleted()),
                                            Boolean.TRUE.equals(part.getIsCompletedFinished()),
                                            part.getDoctorName(),
                                            part.getNurseName());
                                })
                                .toList();
                        parts.addAll(orderedParts);
                    }
                    days.add(new PrescriptionPdfSnapshot.Day(day.getId(), day.getDayDate(), parts));
                }
            }
            snapItems.add(new PrescriptionPdfSnapshot.Item(
                    item.getId(),
                    item.getMedicineName(),
                    item.getMedicineMethod(),
                    item.getRegime(),
                    item.getSortOrder() == null ? 0 : item.getSortOrder(),
                    days));
        }

        Map<UUID, List<PrescriptionPdfSnapshot.Execution>> executions = new LinkedHashMap<>();
        if (!partIds.isEmpty()) {
            Map<UUID, List<PrescriptionExecution>> grouped = executionRepository
                    .findByDayPartIdIn(partIds).stream()
                    .filter(exec -> !Boolean.TRUE.equals(exec.getDeleted()))
                    .collect(Collectors.groupingBy(
                            exec -> exec.getDayPart().getId(), LinkedHashMap::new, Collectors.toList()));
            for (Map.Entry<UUID, List<PrescriptionExecution>> entry : grouped.entrySet()) {
                List<PrescriptionPdfSnapshot.Execution> converted = entry.getValue().stream()
                        .map(exec -> new PrescriptionPdfSnapshot.Execution(
                                exec.getExecutedBy(),
                                exec.getExecutedAt(),
                                exec.getActualDose(),
                                exec.getStatus(),
                                exec.getSecondPersonId(),
                                exec.getComment()))
                        .toList();
                executions.put(entry.getKey(), converted);
            }
        }

        PrescriptionPdfSnapshot snapshot = new PrescriptionPdfSnapshot(
                list.getId(),
                list.getPatientId(),
                list.getHospitalizationId(),
                list.getDepartmentId(),
                list.getDocumentName(),
                list.getStatus(),
                snapItems,
                executions);

        return new PrescriptionData(snapshot, list.getPatientId());
    }

    private String loadSetting(String key) {
        try {
            return systemSettingsRepository.findByKey(key)
                    .map(setting -> setting.getValue())
                    .orElse("");
        } catch (Exception e) {
            return "";
        }
    }

    /** Snapshot plus read-only external data needed for the header. */
    public record LoadedSnapshot(
            PrescriptionPdfSnapshot snapshot,
            Optional<PatientDTO> patient,
            String institutionName,
            String edrpou) {
    }
}
