package com.superhumans.medicationsheet.migration;

import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.medicationsheet.entity.VitalSignDay;
import com.superhumans.medicationsheet.entity.VitalSignEntry;
import com.superhumans.medicationsheet.entity.VitalSignList;
import com.superhumans.medicationsheet.migration.ImportConverters.BloodPressure;
import com.superhumans.medicationsheet.migration.ImportConverters.Converted;
import com.superhumans.medicationsheet.migration.ImportConverters.ParentCandidate;
import com.superhumans.medicationsheet.migration.ImportConverters.ParentRef;
import com.superhumans.medicationsheet.migration.OldDocs.OldListDoc;
import com.superhumans.medicationsheet.migration.OldMedicineJson.VitalCell;
import com.superhumans.medicationsheet.migration.OldMedicineJson.VitalEntry;
import com.superhumans.medicationsheet.repository.ImportIdMapRepository;
import com.superhumans.medicationsheet.repository.ImportQuarantineRepository;
import com.superhumans.medicationsheet.repository.PrescriptionListRepository;
import com.superhumans.medicationsheet.repository.VitalSignDayRepository;
import com.superhumans.medicationsheet.repository.VitalSignEntryRepository;
import com.superhumans.medicationsheet.repository.VitalSignListRepository;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Merges legacy vital-sign JSON into vital-sign chains (design #289
 * sections 3-4). Standalone V-lists resolve a parent prescription list via
 * the chronological rule (D0.1); vital-only patients get a shell list (D0.3).
 * Fill-empty-only: an occupied {@code (day, period)} cell is never
 * overwritten. Day/night slots stay absent (the source has none).
 */
@Slf4j
@Service
@Profile("migration")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VitalMergeService {

    static final String KIND_VIT_LIST = "VIT_LIST";
    static final String KIND_VIT_DAY = "VIT_DAY";
    static final String KIND_LIST = "LIST";

    /** Parent marker used when a shell list is created (D0.3). */
    public static final String SHELL_PARENT = "shell";

    VitalSignListRepository vitalListRepository;
    VitalSignDayRepository vitalDayRepository;
    VitalSignEntryRepository vitalEntryRepository;
    PrescriptionListRepository listRepository;
    ImportIdMapRepository mapRepository;
    ImportQuarantineRepository quarantineRepository;

    /** Per-entry buckets: every source entry lands in exactly one bucket. */
    public record MergeCounts(int inserted, int skipped, int quarantined) {
        public static MergeCounts zero() {
            return new MergeCounts(0, 0, 0);
        }
    }

    public record VitalMergeResult(UUID vitalListId, MergeCounts counts, boolean skipped,
            boolean fallback, String parentRef, UUID shellListId) {
    }

    /**
     * Merges one standalone V-list in its own transaction. Returns a skipped
     * result when already mapped; throws on unexpected failures.
     */
    @Transactional("medTransactionManager")
    public VitalMergeResult mergeVitalList(OldListDoc vlist, String vitalJson, UUID runId) {
        if (mapRepository.existsByOldKindAndOldId(KIND_VIT_LIST, vlist.oldId())) {
            return new VitalMergeResult(null, MergeCounts.zero(), true, false, null, null);
        }
        Converted<LocalDateTime> created = ImportConverters.parseCreationTimestamp(
                vlist.creationDate());
        long patientRef;
        try {
            patientRef = Long.parseLong(vlist.patientRef().strip());
        } catch (NumberFormatException | NullPointerException e) {
            throw new ImportParseException("V-list " + vlist.oldId() + ": bad patientRef");
        }
        if (!created.present()) {
            throw new ImportParseException("V-list " + vlist.oldId() + ": bad creation date");
        }
        List<ParentCandidate> candidates = listRepository
                .findByPatientIdAndDeletedFalse(patientRef).stream()
                .filter(list -> list.getCreatedAt() != null)
                .map(list -> new ParentCandidate(list.getCreatedAt(), list.getId().toString()))
                .toList();
        ParentRef parent = ImportConverters.resolveParent(created.value(), candidates);
        UUID parentId;
        boolean fallback = false;
        String parentOldText;
        UUID shellListId = null;
        if (parent == null) {
            parentId = createShell(vlist, patientRef, created.value(), runId);
            parentOldText = SHELL_PARENT;
            shellListId = parentId;
        } else {
            parentId = UUID.fromString(parent.parentRef());
            fallback = parent.fallback();
            parentOldText = parentOldText(parentId);
        }
        VitalSignList vitalList = ensureVitalList(parentId, created.value());
        map(runId, KIND_VIT_LIST, vlist.oldId(), vitalList.getId());
        MergeCounts counts = mergeEntries(vitalList.getId(), vitalJson, runId, vlist.oldId(),
                created.value());
        log.info("Merged V-list {} -> parent {} (fallback={}, shell={}): {}",
                vlist.oldId(), parentId, fallback, parent == null, counts);
        return new VitalMergeResult(vitalList.getId(), counts, false, fallback, parentOldText,
                shellListId);
    }

    /**
     * Merges embedded vitals (133 PRIZN rows) into an existing list, joining
     * the caller's transaction. No {@code VIT_LIST} row: idempotency rides on
     * the parent LIST row of the same transaction.
     */
    public MergeCounts mergeIntoList(UUID ownerListId, String vitalJson, UUID runId,
            String sourceOldId, LocalDateTime rowCreation) {
        List<OldMedicineJson.VitalEntry> entries;
        try {
            entries = OldMedicineJson.parseVitalEntries(vitalJson);
        } catch (ImportParseException e) {
            throw new ImportParseException(
                    "List " + sourceOldId + " embedded vitals: " + e.getMessage(), e);
        }
        if (entries.isEmpty()) {
            return MergeCounts.zero();
        }
        VitalSignList vitalList = ensureVitalList(ownerListId, rowCreation);
        return mergeEntries(vitalList.getId(), entries, runId, sourceOldId, rowCreation);
    }

    private MergeCounts mergeEntries(UUID vitalListId, String vitalJson, UUID runId,
            String sourceOldId, LocalDateTime rowCreation) {
        List<VitalEntry> entries;
        try {
            entries = OldMedicineJson.parseVitalEntries(vitalJson);
        } catch (ImportParseException e) {
            throw new ImportParseException(
                    "V-list " + sourceOldId + ": " + e.getMessage(), e);
        }
        return mergeEntries(vitalListId, entries, runId, sourceOldId, rowCreation);
    }

    private MergeCounts mergeEntries(UUID vitalListId, List<VitalEntry> entries, UUID runId,
            String sourceOldId, LocalDateTime rowCreation) {
        Map<LocalDate, VitalSignDay> days = new HashMap<>();
        for (VitalSignDay day : vitalDayRepository.findByVitalListIdOrderByDayDateAsc(
                vitalListId)) {
            days.putIfAbsent(day.getDayDate(), day);
        }
        int inserted = 0;
        int skipped = 0;
        int quarantined = 0;
        for (VitalEntry entry : entries) {
            Converted<LocalDate> date = ImportConverters.parseDayDate(entry.date());
            if (!date.present()) {
                quarantine(runId, sourceOldId, "VIT_ENTRY", "BAD_DATE", entry.date());
                quarantined++;
                continue;
            }
            VitalSignDay day = days.get(date.value());
            if (day == null) {
                day = VitalSignDay.builder()
                        .vitalList(vitalListRepository.getReferenceById(vitalListId))
                        .dayDate(date.value())
                        .build();
                stamp(day, rowCreation);
                day = vitalDayRepository.save(day);
                days.put(date.value(), day);
                map(runId, KIND_VIT_DAY, dayKey(entry, sourceOldId), day.getId());
            }
            Map<String, VitalSignEntry> existing = new HashMap<>();
            for (VitalSignEntry row : vitalEntryRepository.findByDayId(day.getId())) {
                existing.putIfAbsent(row.getPeriod(), row);
            }
            boolean entryBad = false;
            boolean entryFilled = false;
            for (Map.Entry<String, VitalCell> cell : entry.cells().entrySet()) {
                VitalSignEntry row = existing.get(cell.getKey());
                if (row != null && occupied(row)) {
                    continue;
                }
                FillOutcome outcome = fillRow(row, day, cell.getKey(), cell.getValue(),
                        runId, sourceOldId, rowCreation);
                entryFilled |= outcome.filled();
                entryBad |= outcome.bad();
            }
            if (entryBad) {
                quarantined++;
            } else if (entryFilled) {
                inserted++;
            } else {
                skipped++;
            }
        }
        return new MergeCounts(inserted, skipped, quarantined);
    }

    private record FillOutcome(boolean filled, boolean bad) {
    }

    private FillOutcome fillRow(VitalSignEntry existing, VitalSignDay day, String period,
            VitalCell cell, UUID runId, String sourceOldId, LocalDateTime rowCreation) {
        Converted<Double> temperature = ImportConverters.parseTemperature(cell.temperature());
        Converted<BloodPressure> pressure =
                ImportConverters.splitBloodPressure(cell.bloodPressure());
        Converted<Integer> spo2 = ImportConverters.parseBoundedInt(cell.saturation(), 50, 100);
        Converted<Integer> pulse = ImportConverters.parseBoundedInt(cell.pulse(), 0, 300);
        Converted<Integer> pain = ImportConverters.parseBoundedInt(cell.pain(), 0, 10);
        String stool = cell.poop() == null || cell.poop().isEmpty() ? null : cell.poop();
        boolean bad = temperature.quarantined() || pressure.quarantined() || spo2.quarantined()
                || pulse.quarantined() || pain.quarantined()
                || (stool != null && stool.length() > 50);
        if (bad) {
            quarantine(runId, sourceOldId, "VIT_ENTRY", "VIT_VALUE",
                    "t=" + cell.temperature() + " bp=" + cell.bloodPressure()
                            + " spo2=" + cell.saturation() + " pulse=" + cell.pulse()
                            + " pain=" + cell.pain());
        }
        boolean filled = temperature.present() || pressure.present() || spo2.present()
                || pulse.present() || pain.present() || stool != null;
        if (!filled) {
            return new FillOutcome(false, bad);
        }
        VitalSignEntry row = existing;
        if (row == null) {
            row = VitalSignEntry.builder().day(day).period(period).build();
            stamp(row, rowCreation);
        }
        if (temperature.present()) {
            row.setTemperature(round1(temperature.value()));
        }
        if (pressure.present()) {
            row.setSystolicBp(pressure.value().systolic());
            row.setDiastolicBp(pressure.value().diastolic());
        }
        if (spo2.present()) {
            row.setSpo2(spo2.value());
        }
        if (pulse.present()) {
            row.setPulse(pulse.value());
        }
        if (pain.present()) {
            row.setPainScore(pain.value());
        }
        if (stool != null) {
            row.setStool(stool);
        }
        vitalEntryRepository.save(row);
        return new FillOutcome(true, bad);
    }

    private boolean occupied(VitalSignEntry row) {
        return row.getTemperature() != null || row.getSystolicBp() != null
                || row.getDiastolicBp() != null || row.getSpo2() != null || row.getPulse() != null
                || row.getStool() != null || row.getPainScore() != null;
    }

    private double round1(double value) {
        return BigDecimal.valueOf(value).setScale(1, RoundingMode.HALF_UP).doubleValue();
    }

    private VitalSignList ensureVitalList(UUID parentId, LocalDateTime created) {
        return vitalListRepository.findByPrescriptionListId(parentId).orElseGet(() -> {
            VitalSignList vitalList = VitalSignList.builder()
                    .prescriptionList(listRepository.getReferenceById(parentId))
                    .build();
            stamp(vitalList, created);
            return vitalListRepository.save(vitalList);
        });
    }

    private UUID createShell(OldListDoc vlist, long patientRef, LocalDateTime created,
            UUID runId) {
        PrescriptionList shell = PrescriptionList.builder()
                .patientId(patientRef)
                .documentName(ImportConverters.shellDocumentName(vlist.oldId()))
                .status("Saved")
                .build();
        stamp(shell, created);
        shell = listRepository.save(shell);
        map(runId, KIND_LIST, vlist.oldId(), shell.getId());
        log.info("Created shell list {} for vital-only V-list {}", shell.getId(), vlist.oldId());
        return shell.getId();
    }

    private String parentOldText(UUID parentId) {
        return mapRepository.findByNewId(parentId).stream()
                .filter(row -> KIND_LIST.equals(row.getOldKind()))
                .map(row -> "MedicineList:" + row.getOldId())
                .findFirst()
                .orElse("live:" + parentId);
    }

    /**
     * Vital day map keys are scoped by V-list: source entry IDs are not
     * trusted to be globally unique (same lesson as MED_EL/MED_DAY).
     */
    private String dayKey(VitalEntry entry, String sourceOldId) {
        return sourceOldId + ":"
                + (entry.id() != null ? entry.id() : "noid:" + entry.date());
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
