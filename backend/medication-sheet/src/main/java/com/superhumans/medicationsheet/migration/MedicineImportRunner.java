package com.superhumans.medicationsheet.migration;

import com.superhumans.medicationsheet.entity.ImportRun;
import com.superhumans.medicationsheet.migration.ListImportService.ListImportResult;
import com.superhumans.medicationsheet.migration.OldDocs.OldItemDoc;
import com.superhumans.medicationsheet.migration.OldDocs.OldListDoc;
import com.superhumans.medicationsheet.migration.VitalMergeService.VitalMergeResult;
import com.superhumans.medicationsheet.repository.ImportQuarantineRepository;
import com.superhumans.medicationsheet.repository.ImportRunRepository;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.PatientDTO;
import com.superhumans.service.AuditService;
import java.io.BufferedWriter;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.security.MessageDigest;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * One-shot legacy import driver (design #289). Active only under the
 * {@code migration} profile, e.g.
 * {@code java -jar app.jar --spring.profiles.active=migration
 * --app.import.csv-dir=C:\import --app.seed-data.enabled=false}.
 *
 * <p>The driver loop itself is not transactional: every list merges in its
 * own transaction inside the workers, so one bad list quarantines instead
 * of failing the run. IMPORT audit rows are written after each successful
 * commit (the audit log lives in the core database).
 */
@Slf4j
@Component
@Profile("migration")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class MedicineImportRunner implements CommandLineRunner {

    static final String LIST_FILE = "MedicineList.csv";
    static final String ITEM_FILE = "MedicineListItem.csv";

    ImportProperties props;
    ImportRunRepository runRepository;
    ImportQuarantineRepository quarantineRepository;
    ListImportService listService;
    VitalMergeService vitalService;
    ImportRollbackService rollbackService;
    MisService misService;
    AuditService auditService;
    ImportValidationService validationService;
    ConfigurableApplicationContext context;

    @Override
    public void run(String... args) throws Exception {
        try {
            if ("rollback".equalsIgnoreCase(props.getMode())) {
                doRollback();
                return;
            }
            if ("validate".equalsIgnoreCase(props.getMode())) {
                doValidate();
                return;
            }
            if ("recon".equalsIgnoreCase(props.getMode())) {
                doRecon();
                return;
            }
            doImport();
        } finally {
            context.close();
        }
    }

    void doRollback() {
        UUID runId = props.getRollbackRunId();
        if (runId == null) {
            throw new IllegalStateException("Rollback mode requires app.import.rollback-run-id");
        }
        log.info("Rolling back import run {}", runId);
        log.info("Rollback result: {}", rollbackService.rollback(runId));
    }

    /**
     * Read-only MIS census over the legacy patient refs (no database writes,
     * no import). Reports existence + department per ref for the visibility
     * gate: only departments 19/37 reach the medication roster.
     */
    void doRecon() throws Exception {
        if (props.getCsvDir() == null) {
            throw new IllegalStateException("Recon mode requires app.import.csv-dir");
        }
        if (props.getReconOut() == null) {
            throw new IllegalStateException("Recon mode requires app.import.recon-out");
        }
        Path listFile = Path.of(props.getCsvDir()).resolve(LIST_FILE);
        if (!Files.isRegularFile(listFile)) {
            throw new IllegalStateException("Missing " + LIST_FILE + " in " + props.getCsvDir());
        }
        Map<String, Integer> listsPerPatient = new LinkedHashMap<>();
        MedicineCsvReader.streamRows(listFile, row -> {
            if (row.length < 2) {
                return;
            }
            listsPerPatient.merge(row[1].strip(), 1, Integer::sum);
        });
        Map<Long, Integer> byDepartment = new TreeMap<>();
        int missing = 0;
        try (BufferedWriter out = Files.newBufferedWriter(
                Path.of(props.getReconOut()), StandardCharsets.UTF_8,
                StandardOpenOption.CREATE,
                StandardOpenOption.TRUNCATE_EXISTING)) {
            out.write("patientRef;exists;departmentId;fullName;lists\n");
            for (Map.Entry<String, Integer> entry : listsPerPatient.entrySet()) {
                long patientId;
                try {
                    patientId = Long.parseLong(entry.getKey());
                } catch (NumberFormatException e) {
                    out.write(entry.getKey() + ";false;;;" + entry.getValue() + "\n");
                    missing++;
                    continue;
                }
                Optional<PatientDTO> patient;
                try {
                    patient = misService.getPatient(patientId);
                } catch (RuntimeException e) {
                    throw new IllegalStateException(
                            "MIS census failed at ref " + entry.getKey() + ": " + message(e), e);
                }
                if (patient.isEmpty()) {
                    out.write(entry.getKey() + ";false;;;" + entry.getValue() + "\n");
                    missing++;
                } else {
                    Long department = patient.get().getDepartmentId();
                    if (department != null) {
                        byDepartment.merge(department, 1, Integer::sum);
                    }
                    String name = patient.get().getFullName() == null ? ""
                            : patient.get().getFullName().replace(";", ",");
                    out.write(entry.getKey() + ";true;"
                            + (department == null ? "" : department) + ";" + name + ";"
                            + entry.getValue() + "\n");
                }
            }
        }
        long inScope = byDepartment.getOrDefault(19L, 0) + byDepartment.getOrDefault(37L, 0);
        log.info("Recon: {} refs, {} exist, {} missing, {} in departments 19/37: {}",
                listsPerPatient.size(), listsPerPatient.size() - missing, missing, inScope,
                byDepartment);
    }

    void doValidate() {        UUID runId = props.getValidateRunId();
        if (runId == null) {
            throw new IllegalStateException("Validate mode requires app.import.validate-run-id");
        }
        if (props.getCsvDir() == null) {
            throw new IllegalStateException("Validate mode requires app.import.csv-dir");
        }
        ImportValidationService.ValidationReport report =
                validationService.validate(runId, Path.of(props.getCsvDir()));
        for (ImportValidationService.CheckResult check : report.checks()) {
            log.info("CHECK {} expected=[{}] actual=[{}] {}", check.name(), check.expected(),
                    check.actual(), check.passed() ? "PASS" : "FAIL");
        }
        if (!report.passed()) {
            throw new IllegalStateException(
                    "Post-import validation FAILED for run " + runId);
        }
        log.info("Post-import validation PASSED for run {}", runId);
    }

    void doImport() throws Exception {
        Path dir = Path.of(props.getCsvDir());
        Path listFile = dir.resolve(LIST_FILE);
        Path itemFile = dir.resolve(ITEM_FILE);
        if (!Files.isRegularFile(listFile) || !Files.isRegularFile(itemFile)) {
            throw new IllegalStateException("CSV directory must hold " + LIST_FILE + " and "
                    + ITEM_FILE + ": " + dir);
        }
        if (!runRepository.findByStatus("RUNNING").isEmpty()) {
            throw new IllegalStateException(
                    "A RUNNING import run exists (crashed attempt?). Roll it back or delete "
                            + "its import_run row before starting a new run.");
        }
        String sourceHash = sha256(listFile) + sha256(itemFile);
        Map<String, OldListDoc> lists = readLists(listFile);
        log.info("Pre-report: {} lists ({} prescription, {} vital), {} patients",
                lists.size(), countPrescription(lists), countVital(lists),
                distinctPatients(lists).size());

        Set<String> missingPatients = props.isSkipPatientCheck() ? Set.of()
                : checkPatients(distinctPatients(lists));
        if (!missingPatients.isEmpty() && !props.isAllowMissingPatients()) {
            throw new IllegalStateException("Missing MIS patients " + missingPatients.size()
                    + " (e.g. " + missingPatients.stream().sorted().limit(5).toList()
                    + "). Set app.import.allow-missing-patients=true to quarantine them.");
        }

        UUID runId = UUID.randomUUID();
        runRepository.save(ImportRun.builder()
                .runId(runId)
                .sourceHash(sourceHash)
                .startedAt(LocalDateTime.now())
                .status("RUNNING")
                .build());
        ImportCounters counters = new ImportCounters();
        counters.getMissingPatients().addAll(missingPatients);

        // Two passes, in file order each: prescription lists first so every
        // vital list resolves against complete parents (no spurious shells).
        MedicineCsvReader.streamRows(itemFile,
                row -> processRow(row, lists, missingPatients, runId, counters, true));
        log.info("Prescription pass done: {} lists", counters.getListsCreated());
        MedicineCsvReader.streamRows(itemFile,
                row -> processRow(row, lists, missingPatients, runId, counters, false));

        ImportRun run = runRepository.findById(runId).orElseThrow();
        run.setStatus("FINISHED");
        run.setFinishedAt(LocalDateTime.now());
        run.setCounts(counters.toJson());
        runRepository.save(run);
        log.info("Post-report run {}: {}", runId, counters.toJson());
    }

    private void processRow(String[] row, Map<String, OldListDoc> lists,
            Set<String> missingPatients, UUID runId, ImportCounters counters,
            boolean prescriptionPass) {
        if (row.length < 6) {
            log.warn("Skipping malformed item row ({} columns)", row.length);
            return;
        }
        OldItemDoc item = new OldItemDoc(row[0], row[1], row[2], row[3], row[4], row[5]);
        OldListDoc list = lists.get(item.listRef());
        if (list == null) {
            if (prescriptionPass) {
                quarantine(runId, item.listRef(), "ITEM", "ITEM_ORPHAN", item.oldId());
                counters.listQuarantined();
            }
            return;
        }
        if (missingPatients.contains(list.patientRef())) {
            if (prescriptionPass) {
                quarantine(runId, list.oldId(), "LIST", "MISSING_PATIENT", list.patientRef());
                counters.listQuarantined();
            }
            return;
        }
        if (list.prescription() != prescriptionPass) {
            return;
        }
        try {
            if (list.prescription()) {
                ListImportResult result = listService.importList(
                        list, item.medicineDetails(), item.vitalList(), runId);
                if (result.skipped()) {
                    counters.listSkippedMapped();
                } else {
                    counters.listCreated();
                    counters.addItems(result.items().items(), result.items().days(),
                            result.items().parts(), result.items().quarantinedElements(),
                            result.items().nameReports());
                    if (!ImportConverters.isEmptyJsonValue(item.vitalList())) {
                        counters.addVitalMerge(result.vital().inserted(),
                                result.vital().skipped(), result.vital().quarantined());
                    }
                    audit("PrescriptionList", result.newListId(),
                            "MedicineList:" + list.oldId(),
                            "patient=" + list.patientRef() + " author=" + list.creationUser()
                                    + " created=" + list.creationDate(),
                            runId, counters);
                }
            } else {
                VitalMergeResult result = vitalService.mergeVitalList(
                        list, item.vitalList(), runId);
                if (result.skipped()) {
                    counters.listSkippedMapped();
                } else {
                    if (VitalMergeService.SHELL_PARENT.equals(result.parentRef())) {
                        counters.listCreated();
                    }
                    counters.addVitalMerge(result.counts().inserted(),
                            result.counts().skipped(), result.counts().quarantined());
                    if (result.shellListId() != null) {
                        audit("PrescriptionList", result.shellListId(),
                                "MedicineList:" + list.oldId(),
                                "shell patient=" + list.patientRef()
                                        + " created=" + list.creationDate(),
                                runId, counters);
                    }
                    audit("VitalSignList", result.vitalListId(),
                            "MedicineList:" + list.oldId() + "->parent:" + result.parentRef(),
                            "inserted=" + result.counts().inserted()
                                    + " skipped=" + result.counts().skipped()
                                    + " quarantined=" + result.counts().quarantined(),
                            runId, counters);
                }
            }
        } catch (RuntimeException e) {
            quarantine(runId, list.oldId(), "LIST", "IMPORT_ERROR", message(e));
            counters.listQuarantined();
            log.warn("Quarantined list {}: {}", list.oldId(), message(e));
        }
    }

    private void audit(String entity, UUID entityId, String oldValue, String newValue,
            UUID runId, ImportCounters counters) {
        if (!props.isAuditEnabled() || entityId == null) {
            return;
        }
        try {
            auditService.logEvent(entity, entityId, "IMPORT", 0L, oldValue, newValue,
                    runId.toString());
        } catch (RuntimeException e) {
            counters.auditFailure();
            log.warn("IMPORT audit failed for {}: {}", entityId, message(e));
        }
    }

    private void quarantine(UUID runId, String oldListId, String kind, String reason,
            String payload) {
        quarantineRepository.save(com.superhumans.medicationsheet.entity.ImportQuarantine
                .builder()
                .id(UUID.randomUUID())
                .runId(runId)
                .oldListId(oldListId)
                .oldKind(kind)
                .reason(reason)
                .payload(payload)
                .build());
    }

    private Map<String, OldListDoc> readLists(Path listFile) throws Exception {
        Map<String, OldListDoc> lists = new LinkedHashMap<>();
        MedicineCsvReader.streamRows(listFile, row -> {
            if (row.length < 8) {
                log.warn("Skipping malformed list row ({} columns)", row.length);
                return;
            }
            lists.put(row[0], new OldListDoc(row[0], row[1], row[2], row[3], row[4]));
        });
        return lists;
    }

    private Set<String> checkPatients(Set<String> refs) {
        Set<String> missing = new HashSet<>();
        for (String ref : refs) {
            long patientId;
            try {
                patientId = Long.parseLong(ref.strip());
            } catch (NumberFormatException e) {
                missing.add(ref);
                continue;
            }
            try {
                if (misService.getPatient(patientId).isEmpty()) {
                    missing.add(ref);
                }
            } catch (RuntimeException e) {
                throw new IllegalStateException(
                        "MIS pre-check failed for patient " + ref + ": " + message(e), e);
            }
        }
        log.info("MIS pre-check: {} patients, {} missing", refs.size(), missing.size());
        return missing;
    }

    private long countPrescription(Map<String, OldListDoc> lists) {
        return lists.values().stream().filter(OldListDoc::prescription).count();
    }

    private long countVital(Map<String, OldListDoc> lists) {
        return lists.values().stream().filter(doc -> !doc.prescription()).count();
    }

    private Set<String> distinctPatients(Map<String, OldListDoc> lists) {
        Set<String> refs = new HashSet<>();
        for (OldListDoc doc : lists.values()) {
            refs.add(doc.patientRef());
        }
        return refs;
    }

    private String sha256(Path file) throws Exception {
        MessageDigest digest = MessageDigest.getInstance("SHA-256");
        digest.update(java.nio.file.Files.readAllBytes(file));
        StringBuilder hex = new StringBuilder();
        for (byte part : digest.digest()) {
            hex.append(String.format("%02x", part));
        }
        return hex.toString();
    }

    private String message(Exception e) {
        return e.getMessage() == null ? e.toString() : e.getMessage();
    }
}
