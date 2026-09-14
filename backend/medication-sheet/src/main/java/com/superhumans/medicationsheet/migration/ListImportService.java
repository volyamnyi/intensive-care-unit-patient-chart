package com.superhumans.medicationsheet.migration;

import com.superhumans.medicationsheet.entity.PrescriptionDayPart;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.medicationsheet.migration.ImportConverters.Converted;
import com.superhumans.medicationsheet.migration.OldMedicineJson.DoseCell;
import com.superhumans.medicationsheet.migration.OldMedicineJson.MedDay;
import com.superhumans.medicationsheet.migration.OldMedicineJson.MedElement;
import com.superhumans.medicationsheet.migration.OldDocs.OldListDoc;
import com.superhumans.medicationsheet.repository.ImportIdMapRepository;
import com.superhumans.medicationsheet.repository.ImportQuarantineRepository;
import com.superhumans.medicationsheet.repository.PrescriptionDayPartRepository;
import com.superhumans.medicationsheet.repository.PrescriptionItemDayRepository;
import com.superhumans.medicationsheet.repository.PrescriptionItemRepository;
import com.superhumans.medicationsheet.repository.PrescriptionListRepository;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Imports one prescription list with its items, days and dose cells in a
 * single med-database transaction (design #289 section 5).
 *
 * <p>Deliberately writes through repositories instead of the domain services:
 * those carry the chained transaction manager and the 21-day auto-grid side
 * effect, neither of which an importer wants.
 */
@Slf4j
@Service
@Profile("migration")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ListImportService {

    static final String KIND_LIST = "LIST";
    static final String KIND_ELEMENT = "MED_EL";
    static final String KIND_DAY = "MED_DAY";

    PrescriptionListRepository listRepository;
    PrescriptionItemRepository itemRepository;
    PrescriptionItemDayRepository dayRepository;
    PrescriptionDayPartRepository partRepository;
    ImportIdMapRepository mapRepository;
    ImportQuarantineRepository quarantineRepository;
    VitalMergeService vitalMergeService;

    public record ItemCounts(int items, int days, int parts, int quarantinedElements,
            int nameReports) {
    }

    public record ListImportResult(UUID newListId, ItemCounts items,
            VitalMergeService.MergeCounts vital, boolean skipped) {
    }

    /**
     * Imports one list subtree. Throws on unexpected failures (the caller
     * quarantines the whole list outside this transaction); returns a
     * skipped result when the list is already mapped.
     */
    @Transactional("medTransactionManager")
    public ListImportResult importList(OldListDoc list, String medicineDetailsJson,
            String embeddedVitalJson, UUID runId) {
        if (mapRepository.existsByOldKindAndOldId(KIND_LIST, list.oldId())) {
            return new ListImportResult(null,
                    new ItemCounts(0, 0, 0, 0, 0), VitalMergeService.MergeCounts.zero(), true);
        }
        Converted<LocalDateTime> created = ImportConverters.parseCreationTimestamp(
                list.creationDate());
        long patientRef;
        try {
            patientRef = Long.parseLong(list.patientRef().strip());
        } catch (NumberFormatException | NullPointerException e) {
            quarantine(runId, list.oldId(), KIND_LIST, "PATIENT_REF", list.patientRef());
            return new ListImportResult(null,
                    new ItemCounts(0, 0, 0, 0, 0), VitalMergeService.MergeCounts.zero(), false);
        }
        if (!created.present()) {
            quarantine(runId, list.oldId(), KIND_LIST, "CREATED_AT", list.creationDate());
            return new ListImportResult(null,
                    new ItemCounts(0, 0, 0, 0, 0), VitalMergeService.MergeCounts.zero(), false);
        }
        PrescriptionList entity = PrescriptionList.builder()
                .patientId(patientRef)
                .documentName(ImportConverters.LIST_DOCUMENT_NAME)
                .status("Saved")
                .build();
        stamp(entity, created.value());
        entity = listRepository.save(entity);
        map(runId, KIND_LIST, list.oldId(), entity.getId());

        List<MedElement> elements;
        try {
            elements = OldMedicineJson.parseMedicineElements(medicineDetailsJson);
        } catch (ImportParseException e) {
            throw new ImportParseException("List " + list.oldId() + ": " + e.getMessage(), e);
        }
        int sortOrder = 0;
        int dayCount = 0;
        int partCount = 0;
        int quarantinedElements = 0;
        int nameReports = 0;
        for (int index = 0; index < elements.size(); index++) {
            MedElement element = elements.get(index);
            if (element.name() == null || element.name().isBlank()) {
                quarantine(runId, list.oldId(), KIND_ELEMENT, "EMPTY_NAME", "index=" + index);
                quarantinedElements++;
                continue;
            }
            PrescriptionItem item = PrescriptionItem.builder()
                    .list(entity)
                    .medicineName(element.name())
                    .medicineMethod(blankToNull(element.method()))
                    .regime(ImportConverters.stripRegime(element.regime()))
                    .status("Active")
                    .sortOrder(sortOrder++)
                    .build();
            stamp(item, created.value());
            item = itemRepository.save(item);
            map(runId, KIND_ELEMENT, elementKey(list.oldId(), element, index), item.getId());
            for (MedDay day : element.days()) {
                Converted<LocalDate> dayDate = ImportConverters.parseDayDate(day.date());
                if (!dayDate.present()) {
                    quarantine(runId, list.oldId(), KIND_DAY, "BAD_DATE",
                            "element=" + index + " date=" + day.date());
                    continue;
                }
                PrescriptionItemDay dayEntity = PrescriptionItemDay.builder()
                        .item(item)
                        .dayDate(dayDate.value())
                        .build();
                stamp(dayEntity, created.value());
                dayEntity = dayRepository.save(dayEntity);
                map(runId, KIND_DAY, dayKey(day, list.oldId(), index), dayEntity.getId());
                dayCount++;
                for (String period : OldMedicineJson.PERIODS) {
                    PartSave saved = savePart(dayEntity, period, day.cells().get(period),
                            created.value(), runId, list.oldId());
                    partCount += saved.rows();
                    nameReports += saved.nameReports();
                }
            }
        }
        VitalMergeService.MergeCounts vital = vitalMergeService.mergeIntoList(
                entity.getId(), embeddedVitalJson, runId, list.oldId(), created.value());
        log.info("Imported list {} -> {} ({} items)", list.oldId(), entity.getId(), sortOrder);
        return new ListImportResult(entity.getId(),
                new ItemCounts(sortOrder, dayCount, partCount, quarantinedElements, nameReports),
                vital, false);
    }

    private record PartSave(int rows, int nameReports) {
    }

    private PartSave savePart(PrescriptionItemDay day, String period, DoseCell cell,
            LocalDateTime created, UUID runId, String oldListId) {
        Converted<String> doctor = cell == null
                ? Converted.empty() : ImportConverters.convertDoctorName(cell.doctor());
        Converted<String> nurse = cell == null
                ? Converted.empty() : ImportConverters.convertNurseName(cell.nurse());
        PrescriptionDayPart part = PrescriptionDayPart.builder()
                .day(day)
                .period(period)
                .dose(cell == null || cell.dose() == null || cell.dose().isBlank()
                        ? null : cell.dose())
                .isPlanned(cell != null && cell.planned())
                .isPlannedFinished(cell != null && cell.plannedFinished())
                .isCompleted(cell != null && cell.completed())
                .isCompletedFinished(cell != null && cell.completedFinished())
                .doctorName(doctor.present() ? doctor.value() : null)
                .nurseName(nurse.present() ? nurse.value() : null)
                .build();
        stamp(part, created);
        partRepository.save(part);
        int reports = 0;
        if (doctor.quarantined()) {
            quarantine(runId, oldListId, "MED_CELL", "NAME_GARBAGE", cell.doctor());
            reports++;
        }
        if (nurse.quarantined()) {
            quarantine(runId, oldListId, "MED_CELL", "NAME_GARBAGE", cell.nurse());
            reports++;
        }
        return new PartSave(1, reports);
    }

    /**
     * Element map keys are scoped by list: legacy element IDs repeat across
     * lists (344 duplicate IDs file-wide), so a bare element ID is not a key.
     */
    private String elementKey(String oldListId, MedElement element, int index) {
        return oldListId + ":"
                + (element.id() != null ? element.id() : "noid:" + index);
    }

    /**
     * Day map keys are scoped by list, like elements: legacy schedule IDs
     * repeat across lists (3297 cross-list duplicates file-wide).
     */
    private String dayKey(MedDay day, String oldListId, int index) {
        return oldListId + ":"
                + (day.id() != null ? day.id() : "noid:" + index + ":" + day.date());
    }

    private String blankToNull(String value) {
        return value == null || value.isBlank() ? null : value;
    }

    private void stamp(com.superhumans.entity.base.BaseEntity entity, LocalDateTime created) {
        entity.setCreatedAt(created);
        entity.setUpdatedAt(created);
        entity.setCreatedBy(0L);
        entity.setUpdatedBy(0L);
    }

    private void map(UUID runId, String kind, String oldId, UUID newId) {
        mapRepository.save(com.superhumans.medicationsheet.entity.ImportIdMap.builder()
                .oldKind(kind)
                .oldId(oldId)
                .newId(newId)
                .runId(runId)
                .build());
    }

    private void quarantine(UUID runId, String oldListId, String kind, String reason,
            String payload) {
        quarantineRepository.save(com.superhumans.medicationsheet.entity.ImportQuarantine.builder()
                .id(UUID.randomUUID())
                .runId(runId)
                .oldListId(oldListId)
                .oldKind(kind)
                .reason(reason)
                .payload(payload)
                .build());
    }
}
