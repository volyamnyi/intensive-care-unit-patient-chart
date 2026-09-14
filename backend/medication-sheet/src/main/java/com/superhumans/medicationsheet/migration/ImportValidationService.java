package com.superhumans.medicationsheet.migration;

import com.superhumans.entity.core.AuditLog;
import com.superhumans.medicationsheet.entity.PrescriptionItem;
import com.superhumans.medicationsheet.entity.ImportIdMap;
import com.superhumans.medicationsheet.entity.PrescriptionItemDay;
import com.superhumans.medicationsheet.entity.PrescriptionList;
import com.superhumans.medicationsheet.entity.VitalSignDay;
import com.superhumans.medicationsheet.entity.VitalSignList;
import com.superhumans.medicationsheet.migration.OldDocs.OldListDoc;
import com.superhumans.medicationsheet.migration.OldMedicineJson.MedElement;
import com.superhumans.medicationsheet.migration.OldMedicineJson.VitalEntry;
import com.superhumans.medicationsheet.repository.ImportIdMapRepository;
import com.superhumans.medicationsheet.repository.ImportQuarantineRepository;
import com.superhumans.medicationsheet.repository.ImportRunRepository;
import com.superhumans.medicationsheet.repository.MigrationCheckRepository;
import com.superhumans.medicationsheet.repository.PrescriptionDayPartRepository;
import com.superhumans.medicationsheet.repository.PrescriptionItemDayRepository;
import com.superhumans.medicationsheet.repository.PrescriptionItemRepository;
import com.superhumans.medicationsheet.repository.PrescriptionListRepository;
import com.superhumans.medicationsheet.repository.VitalSignDayRepository;
import com.superhumans.medicationsheet.repository.VitalSignListRepository;
import com.superhumans.repository.core.AuditLogRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

/**
 * Repeatable post-import validation (design #291). Recomputes expectations
 * from the frozen CSV source and compares them against the database, one
 * deterministic check at a time. Read-only: never writes.
 *
 * <p>Global row counts assume a dry-run database holding only imported rows;
 * run-scoped checks (statuses, map coverage, audit, quarantine) are exact on
 * any database.
 */
@Slf4j
@Service
@Profile("migration")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ImportValidationService {

    static final Set<String> KNOWN_QUARANTINE_REASONS =
            Set.of("EMPTY_NAME", "VIT_VALUE", "NAME_GARBAGE");
    static final Pattern MERGE_TRIPLE =
            Pattern.compile("inserted=(\\d+) skipped=(\\d+) quarantined=(\\d+)");
    static final ObjectMapper JSON = new ObjectMapper();

    PrescriptionListRepository listRepository;
    PrescriptionItemRepository itemRepository;
    PrescriptionItemDayRepository dayRepository;
    PrescriptionDayPartRepository partRepository;
    VitalSignListRepository vitalListRepository;
    VitalSignDayRepository vitalDayRepository;
    ImportIdMapRepository mapRepository;
    ImportQuarantineRepository quarantineRepository;
    ImportRunRepository runRepository;
    MigrationCheckRepository checkRepository;
    AuditLogRepository auditLogRepository;

    public record CheckResult(String name, String expected, String actual, boolean passed) {
    }

    public record ValidationReport(UUID runId, List<CheckResult> checks) {
        public boolean passed() {
            return checks.stream().allMatch(CheckResult::passed);
        }
    }

    record SourceCell(String listRef, int elementIndex, String date, String period,
            String first, String second) {
    }

    record SourceProfile(Map<String, OldListDoc> lists, int prescriptionLists,
            int vitalLists, Set<String> vitalOnlyPatients, int elements, int emptyElements,
            int schedDays, int vitalEntries, Map<String, Integer> vitalEntriesByRef,
            Map<String, String> spotMedicine, Map<String, String> spotVital,
            SourceCell firstDoctor, SourceCell firstPair) {
    }

    /**
     * Validates one finished run against a fresh recomputation from source.
     */
    public ValidationReport validate(UUID runId, Path csvDir) {
        List<CheckResult> checks = new ArrayList<>();
        SourceProfile source;
        try {
            source = readSource(csvDir);
            checks.add(new CheckResult("source-readable", "CSVs parse",
                    source.lists().size() + " lists parsed", true));
        } catch (Exception e) {
            checks.add(new CheckResult("source-readable", "readable CSVs",
                    e.getMessage() == null ? e.toString() : e.getMessage(), false));
            return new ValidationReport(runId, checks);
        }
        var run = runRepository.findById(runId);
        boolean finished = run.isPresent() && "FINISHED".equals(run.get().getStatus());
        checks.add(check("run-finished", "FINISHED",
                run.map(r -> r.getStatus()).orElse("MISSING"), finished));
        if (run.isPresent() && run.get().getCounts() != null) {
            try {
                JSON.readTree(run.get().getCounts());
                checks.add(check("run-counts-json", "parseable", "parseable", true));
            } catch (Exception e) {
                checks.add(check("run-counts-json", "parseable", "broken", false));
            }
        }
        checks.add(checkLists(runId, source));
        checks.add(checkItems(runId, source));
        checks.add(checkDays(runId, source));
        checks.add(checkVitals(runId, source));
        checks.add(checkOrphans());
        checks.add(checkSpots(runId, source));
        checks.add(checkRanges());
        checks.add(checkStatuses(runId));
        checks.add(checkQuarantine(runId, source));
        checks.addAll(checkAudit(runId, source));
        return new ValidationReport(runId, checks);
    }

    private CheckResult check(String name, String expected, String actual, boolean passed) {
        return new CheckResult(name, expected, actual, passed);
    }

    private SourceProfile readSource(Path csvDir) throws Exception {
        Path listFile = csvDir.resolve("MedicineList.csv");
        Path itemFile = csvDir.resolve("MedicineListItem.csv");
        if (!Files.isRegularFile(listFile) || !Files.isRegularFile(itemFile)) {
            throw new IllegalStateException("Missing CSV files in " + csvDir);
        }
        Map<String, OldListDoc> lists = new HashMap<>();
        MedicineCsvReader.streamRows(listFile, row -> {
            if (row.length < 8) {
                throw new ImportParseException("Malformed list row");
            }
            lists.put(row[0], new OldListDoc(row[0], row[1], row[2], row[3], row[4]));
        });
        Set<String> prescriptionPatients = new HashSet<>();
        Set<String> vitalPatients = new HashSet<>();
        int prescriptionLists = 0;
        int vitalLists = 0;
        for (OldListDoc doc : lists.values()) {
            if (doc.prescription()) {
                prescriptionLists++;
                prescriptionPatients.add(doc.patientRef());
            } else {
                vitalLists++;
                vitalPatients.add(doc.patientRef());
            }
        }
        Set<String> vitalOnly = new HashSet<>(vitalPatients);
        vitalOnly.removeAll(prescriptionPatients);
        int[] elements = {0};
        int[] emptyElements = {0};
        int[] schedDays = {0};
        int[] vitalEntries = {0};
        Map<String, Integer> vitalEntriesByRef = new HashMap<>();
        Set<String> spotRefs = Set.of("288", "606", "1487");
        Map<String, String> spotMedicine = new HashMap<>();
        Map<String, String> spotVital = new HashMap<>();
        SourceCell[] firstDoctor = {null};
        SourceCell[] firstPair = {null};
        MedicineCsvReader.streamRows(itemFile, row -> {
            if (row.length < 6) {
                throw new ImportParseException("Malformed item row");
            }
            String ref = row[1];
            OldListDoc list = lists.get(ref);
            if (list == null) {
                throw new ImportParseException("Orphan item ref " + ref);
            }
            if (spotRefs.contains(ref)) {
                spotMedicine.put(ref, row[4]);
                spotVital.put(ref, row[5]);
            }
            if (list.prescription()) {
                List<MedElement> parsed = OldMedicineJson.parseMedicineElements(row[4]);
                for (int index = 0; index < parsed.size(); index++) {
                    MedElement element = parsed.get(index);
                    elements[0]++;
                    if (element.name() == null || element.name().isBlank()) {
                        emptyElements[0]++;
                        continue;
                    }
                    schedDays[0] += element.days().size();
                    if (firstDoctor[0] == null || firstPair[0] == null) {
                        for (var day : element.days()) {
                            for (var cell : day.cells().entrySet()) {
                                String doctor = cell.getValue().doctor();
                                String nurse = cell.getValue().nurse();
                                if (firstDoctor[0] == null && doctor != null
                                        && !doctor.isBlank() && !doctor.contains("\n")) {
                                    firstDoctor[0] = new SourceCell(ref, index,
                                            day.date(), cell.getKey(), doctor, null);
                                }
                                if (firstPair[0] == null && nurse != null
                                        && nurse.contains("\n")) {
                                    String[] pair = nurse.split("\n", -1);
                                    if (pair.length == 2) {
                                        firstPair[0] = new SourceCell(ref, index,
                                                day.date(), cell.getKey(), pair[0], pair[1]);
                                    }
                                }
                            }
                        }
                    }
                }
            } else {
                List<VitalEntry> parsed = OldMedicineJson.parseVitalEntries(row[5]);
                vitalEntries[0] += parsed.size();
                vitalEntriesByRef.put(ref, parsed.size());
            }
        });
        return new SourceProfile(lists, prescriptionLists, vitalLists, vitalOnly,
                elements[0], emptyElements[0], schedDays[0], vitalEntries[0], vitalEntriesByRef,
                spotMedicine, spotVital, firstDoctor[0], firstPair[0]);
    }

    private CheckResult checkLists(UUID runId, SourceProfile source) {
        List<ImportIdMap> listRows = mapRepository.findByRunIdAndOldKind(runId, "LIST");
        Set<UUID> mappedListIds = new HashSet<>();
        for (var row : listRows) {
            mappedListIds.add(row.getNewId());
        }
        long plain = 0;
        long shells = 0;
        for (var row : listRows) {
            Optional<PrescriptionList> list = listRepository.findById(row.getNewId());
            if (list.isEmpty()) {
                return check("lists", "all mapped resolvable", "missing " + row.getOldId(),
                        false);
            }
            String name = list.get().getDocumentName();
            if (ImportConverters.LIST_DOCUMENT_NAME.equals(name)) {
                plain++;
            } else if (name != null && name.startsWith(ImportConverters.LIST_DOCUMENT_NAME)) {
                shells++;
            } else {
                return check("lists", "canonical names", "odd name on " + row.getOldId(), false);
            }
            if (!"Saved".equals(list.get().getStatus())) {
                return check("lists", "all Saved", "status drift on " + row.getOldId(), false);
            }
        }
        long absorbed = 0;
        for (var row : mapRepository.findByRunIdAndOldKind(runId, "VIT_LIST")) {
            Optional<VitalSignList> vitalList = vitalListRepository.findById(row.getNewId());
            if (vitalList.isEmpty()) {
                return check("lists", "all vital chains resolvable",
                        "missing chain " + row.getOldId(), false);
            }
            UUID parentId = vitalList.get().getPrescriptionList().getId();
            if (!mappedListIds.contains(parentId)) {
                absorbed++;
            }
        }
        boolean passed = plain == source.prescriptionLists()
                && shells + absorbed == source.vitalOnlyPatients().size();
        return check("lists",
                "plain=" + source.prescriptionLists() + " shells+absorbed="
                        + source.vitalOnlyPatients().size(),
                "plain=" + plain + " shells=" + shells + " absorbed=" + absorbed, passed);
    }

    private CheckResult checkItems(UUID runId, SourceProfile source) {
        long expected = (long) source.elements() - source.emptyElements();
        long mapped = mapRepository.countByRunIdAndOldKind(runId, "MED_EL");
        long total = checkRepository.importedItemCount(runId);
        boolean passed = mapped == expected && total == expected;
        return check("items", "items=" + expected, "mapped=" + mapped + " total=" + total,
                passed);
    }

    private CheckResult checkDays(UUID runId, SourceProfile source) {
        long days = checkRepository.importedDayCount(runId);
        long parts = checkRepository.importedPartCount(runId);
        List<UUID> badGrid = checkRepository.findImportedDayIdsWithBadGrid(runId);
        boolean passed = days == source.schedDays() && parts == 4L * days && badGrid.isEmpty();
        return check("days-grid", "days=" + source.schedDays() + " parts=4x bad=0",
                "days=" + days + " parts=" + parts + " bad=" + badGrid.size(), passed);
    }

    private CheckResult checkVitals(UUID runId, SourceProfile source) {
        long mapped = mapRepository.countByRunIdAndOldKind(runId, "VIT_LIST");
        if (mapped != source.vitalLists()) {
            return check("vitals", "VIT_LIST=" + source.vitalLists(), "VIT_LIST=" + mapped,
                    false);
        }
        return check("vitals", "VIT_LIST=" + source.vitalLists(), "VIT_LIST=" + mapped, true);
    }

    private CheckResult checkOrphans() {
        long total = checkRepository.orphanItemCount() + checkRepository.orphanDayCount()
                + checkRepository.orphanPartCount() + checkRepository.orphanVitalDayCount()
                + checkRepository.orphanVitalEntryCount()
                + checkRepository.orphanVitalListCount();
        return check("orphans", "0", String.valueOf(total), total == 0);
    }

    private CheckResult checkSpots(UUID runId, SourceProfile source) {
        Optional<String> list288 = findMapped(runId, "LIST", "288");
        if (list288.isEmpty()) {
            return check("spots", "288 mapped", "288 missing", false);
        }
        List<MedElement> elements;
        try {
            elements = OldMedicineJson.parseMedicineElements(
                    source.spotMedicine().get("288"));
        } catch (RuntimeException e) {
            return check("spots", "288 parseable", "parse error", false);
        }
        List<PrescriptionItem> items =
                itemRepository.findByListId(UUID.fromString(list288.get()));
        if (items.size() != elements.size()) {
            return check("spots", "288 items=" + elements.size(), "items=" + items.size(),
                    false);
        }
        Optional<String> list606 = findMapped(runId, "LIST", "606");
        if (list606.isEmpty()) {
            return check("spots", "606 mapped", "606 missing", false);
        }
        Optional<PrescriptionList> day606 =
                listRepository.findById(UUID.fromString(list606.get()));
        if (day606.isEmpty()
                || !itemRepository.findByListId(UUID.fromString(list606.get())).isEmpty()) {
            return check("spots", "606 empty", "606 has items", false);
        }
        Optional<String> vital1487 = findMapped(runId, "VIT_LIST", "1487");
        if (vital1487.isEmpty()) {
            return check("spots", "1487 merged", "1487 missing", false);
        }
        List<VitalSignDay> vitalDays = vitalDayRepository
                .findByVitalListIdOrderByDayDateAsc(UUID.fromString(vital1487.get()));
        if (vitalDays.isEmpty()) {
            return check("spots", "1487 has days", "1487 empty", false);
        }
        String doctorCheck = checkSourceCell(runId, source, source.firstDoctor(), false);
        if (doctorCheck != null) {
            return check("spots", "doctor cell converted", doctorCheck, false);
        }
        String pairCheck = checkSourceCell(runId, source, source.firstPair(), true);
        if (pairCheck != null) {
            return check("spots", "pair cell converted", pairCheck, false);
        }
        return check("spots", "288/606/1487/doctor/pair ok", "all ok", true);
    }

    private String checkSourceCell(UUID runId, SourceProfile source, SourceCell wanted,
            boolean pair) {
        if (wanted == null) {
            return "no source cell";
        }
        Optional<String> listId = findMapped(runId, "LIST", wanted.listRef());
        if (listId.isEmpty()) {
            return "parent list missing";
        }
        List<PrescriptionItem> items = itemRepository
                .findByListIdOrderBySortOrderAsc(UUID.fromString(listId.get()));
        if (wanted.elementIndex() >= items.size()) {
            return "item index missing";
        }
        PrescriptionItem item = items.get(wanted.elementIndex());
        Optional<PrescriptionItemDay> day = dayRepository.findByItemId(item.getId()).stream()
                .filter(d -> d.getDayDate() != null
                        && d.getDayDate().toString().equals(wanted.date()))
                .findFirst();
        if (day.isEmpty()) {
            return "day missing";
        }
        Optional<com.superhumans.medicationsheet.entity.PrescriptionDayPart> part =
                partRepository.findByDayId(day.get().getId()).stream()
                        .filter(p -> p.getPeriod().equals(wanted.period()))
                        .findFirst();
        if (part.isEmpty()) {
            return "part missing";
        }
        if (pair) {
            String expected = wanted.first() + "/2P:" + wanted.second();
            if (!expected.equals(part.get().getNurseName())) {
                return "pair mismatch";
            }
            return null;
        }
        String expected = ImportConverters.nameUuid(wanted.first()).toString();
        if (!expected.equals(part.get().getDoctorName())) {
            return "doctor mismatch";
        }
        return null;
    }

    private Optional<String> findMapped(UUID runId, String kind, String oldId) {
        return mapRepository.findByOldKindAndOldId(kind, oldId)
                .map(row -> row.getNewId().toString());
    }

    private CheckResult checkRanges() {
        long bad = checkRepository.outOfRangeTemperatureCount()
                + checkRepository.outOfRangePainCount()
                + checkRepository.outOfRangeSpo2Count()
                + checkRepository.outOfRangeSystolicCount()
                + checkRepository.outOfRangeDiastolicCount()
                + checkRepository.outOfRangePulseCount();
        return check("ranges", "0 violations", bad + " violations", bad == 0);
    }

    private CheckResult checkStatuses(UUID runId) {
        long badLists = checkRepository.nonSavedImportedListCount(runId);
        long badItems = checkRepository.nonActiveImportedItemCount(runId);
        return check("statuses", "Saved/Active", "badLists=" + badLists + " badItems=" + badItems,
                badLists == 0 && badItems == 0);
    }

    private CheckResult checkQuarantine(UUID runId, SourceProfile source) {
        var rows = quarantineRepository.findByRunId(runId);
        Map<String, Long> byReason = new HashMap<>();
        for (var row : rows) {
            if ("LIST".equals(row.getOldKind()) || "ITEM".equals(row.getOldKind())) {
                return check("quarantine", "no list/item rows",
                        row.getOldKind() + "/" + row.getReason(), false);
            }
            byReason.merge(row.getReason(), 1L, Long::sum);
        }
        for (String reason : byReason.keySet()) {
            if (!KNOWN_QUARANTINE_REASONS.contains(reason)) {
                return check("quarantine", "known reasons only", "unknown " + reason, false);
            }
        }
        long emptyNames = byReason.getOrDefault("EMPTY_NAME", 0L);
        if (emptyNames != source.emptyElements()) {
            return check("quarantine", "EMPTY_NAME=" + source.emptyElements(),
                    "EMPTY_NAME=" + emptyNames, false);
        }
        return check("quarantine", "reasons=" + byReason, "reasons=" + byReason, true);
    }

    private List<CheckResult> checkAudit(UUID runId, SourceProfile source) {
        long listAudits = 0;
        long mergeAudits = 0;
        long inserted = 0;
        long skipped = 0;
        long quarantined = 0;
        Map<String, Long> sourceTriple = new HashMap<>();
        int page = 0;
        while (true) {
            Page<AuditLog> batch = auditLogRepository.findByActionOrderByTimestampDesc(
                    "IMPORT", PageRequest.of(page, 2000));
            for (AuditLog row : batch.getContent()) {
                if (!runId.toString().equals(row.getCorrelationId())) {
                    continue;
                }
                if ("PrescriptionList".equals(row.getEntity())) {
                    listAudits++;
                } else if ("VitalSignList".equals(row.getEntity())) {
                    mergeAudits++;
                    Matcher triple = MERGE_TRIPLE.matcher(
                            row.getNewValue() == null ? "" : row.getNewValue());
                    if (!triple.find()) {
                        return List.of(
                                check("audit", "parseable triples", "unparseable row", false));
                    }
                    long tripleSum = Long.parseLong(triple.group(1))
                            + Long.parseLong(triple.group(2))
                            + Long.parseLong(triple.group(3));
                    inserted += Long.parseLong(triple.group(1));
                    skipped += Long.parseLong(triple.group(2));
                    quarantined += Long.parseLong(triple.group(3));
                    String oldValue = row.getOldValue() == null ? "" : row.getOldValue();
                    if (oldValue.startsWith("MedicineList:")) {
                        String sourceRef = oldValue.substring("MedicineList:".length())
                                .split("->", -1)[0];
                        sourceTriple.merge(sourceRef, tripleSum, Long::sum);
                    }
                }
            }
            if (!batch.hasNext()) {
                break;
            }
            page++;
        }
        long mapLists = mapRepository.countByRunIdAndOldKind(runId, "LIST");
        long mapVitals = mapRepository.countByRunIdAndOldKind(runId, "VIT_LIST");
        boolean countsOk = listAudits == mapLists && mergeAudits == mapVitals;
        boolean tripleOk = inserted + skipped + quarantined == source.vitalEntries();
        if (!countsOk || !tripleOk) {
            return List.of(check("audit",
                    "lists=" + mapLists + " merges=" + mapVitals
                            + " triple=" + source.vitalEntries(),
                    "lists=" + listAudits + " merges=" + mergeAudits
                            + " triple=" + (inserted + skipped + quarantined), false));
        }
        Integer expected1487 = source.vitalEntriesByRef().get("1487");
        Long actual1487 = sourceTriple.get("1487");
        boolean spotOk = expected1487 != null && actual1487 != null
                && actual1487 == expected1487.longValue();
        return List.of(
                check("audit",
                        "lists=" + mapLists + " merges=" + mapVitals
                                + " triple=" + source.vitalEntries(),
                        "lists=" + listAudits + " merges=" + mergeAudits
                                + " triple=" + (inserted + skipped + quarantined), true),
                check("audit-1487-triple",
                        "1487 triple=" + expected1487, "1487 triple=" + actual1487, spotOk));
    }
}
