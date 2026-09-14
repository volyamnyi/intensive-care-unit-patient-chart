package com.superhumans.medicationsheet.migration;

import com.superhumans.medicationsheet.entity.ImportRun;
import com.superhumans.medicationsheet.migration.ListImportService.ListImportResult;
import com.superhumans.medicationsheet.migration.VitalMergeService.VitalMergeResult;
import com.superhumans.medicationsheet.repository.ImportQuarantineRepository;
import com.superhumans.medicationsheet.repository.ImportRunRepository;
import com.superhumans.mis.MisService;
import com.superhumans.service.AuditService;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.junit.jupiter.api.io.TempDir;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class MedicineImportRunnerTest {

    @Mock
    ImportRunRepository runRepository;
    @Mock
    ImportQuarantineRepository quarantineRepository;
    @Mock
    ListImportService listService;
    @Mock
    VitalMergeService vitalService;
    @Mock
    ImportRollbackService rollbackService;
    @Mock
    MisService misService;
    @Mock
    AuditService auditService;
    @Mock
    ImportValidationService validationService;
    @Mock
    org.springframework.context.ConfigurableApplicationContext context;

    @InjectMocks
    MedicineImportRunner runner;

    @TempDir
    Path temp;

    static final String PRIZN_DOC = "Sheet \u043F\u0440\u0438\u0437\u043D\u0430\u0447 test";

    ImportProperties props() {
        ImportProperties properties = new ImportProperties();
        properties.setCsvDir(temp.toString());
        properties.setSkipPatientCheck(true);
        properties.setAuditEnabled(false);
        return properties;
    }

    void writeLists(String... rows) throws Exception {
        StringBuilder content = new StringBuilder(
                "MedicineListID;PatientRef;DocumentName;MedicineListCreationUser;"
                        + "MedicineListCreationDate;MakeDEDocument;isAutomaticallyGenerated;"
                        + "ApprovedRowIndexes\n");
        for (String row : rows) {
            content.append(row).append('\n');
        }
        Files.writeString(temp.resolve("MedicineList.csv"), content.toString());
    }

    void writeItems(String... rows) throws Exception {
        StringBuilder content = new StringBuilder(
                "MedicineListItemID;MedicineListRef;MedicineListItemEditUser;"
                        + "MedicineListItemEditDate;MedicineDetails;VitalList\n");
        for (String row : rows) {
            content.append(row).append('\n');
        }
        Files.writeString(temp.resolve("MedicineListItem.csv"), content.toString());
    }

    String itemRow(String itemId, String ref, String detailsJson, String vital) {
        String details = "\"" + detailsJson.replace("\"", "\"\"") + "\"";
        return itemId + ";" + ref + ";author;2025-06-24 15:57:24.850;" + details + ";" + vital;
    }

    void echoRunRepo(AtomicReference<ImportRun> stored) {
        when(runRepository.save(any())).thenAnswer(inv -> {
            ImportRun run = inv.getArgument(0);
            stored.set(run);
            return run;
        });
        when(runRepository.findById(any())).thenAnswer(inv -> {
            ImportRun run = stored.get();
            return Optional.ofNullable(run)
                    .filter(candidate -> candidate.getRunId().equals(inv.getArgument(0)));
        });
    }

    void useProps(ImportProperties properties) {
        runner = new MedicineImportRunner(properties, runRepository, quarantineRepository,
                listService, vitalService, rollbackService, misService, auditService,
                validationService, context);
    }

    @Test
    void run_abortsWithoutCsvFiles() {
        useProps(props());
        assertThatThrownBy(() -> runner.run()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void run_refusesWithRunningAttempt() throws Exception {
        writeLists();
        writeItems();
        useProps(props());
        when(runRepository.findByStatus("RUNNING")).thenReturn(List.of(ImportRun.builder()
                .runId(UUID.randomUUID()).sourceHash("x").status("RUNNING").build()));
        assertThatThrownBy(() -> runner.run()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void run_rollbackRequiresRunId() {
        ImportProperties properties = props();
        properties.setMode("rollback");
        useProps(properties);
        assertThatThrownBy(() -> runner.run()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void run_rollbackDelegates() throws Exception {        ImportProperties properties = props();
        properties.setMode("rollback");
        UUID runId = UUID.randomUUID();
        properties.setRollbackRunId(runId);
        useProps(properties);
        runner.run();
        verify(rollbackService).rollback(runId);
    }

    @Test
    void run_happyPath() throws Exception {
        writeLists(
                "288;3330;" + PRIZN_DOC + ";author;2025-06-24 12:55:33.827;;0;NULL",
                "1487;4269;Vital doc;author;2026-02-10 10:00:00.000;;0;[ ]");
        writeItems(
                itemRow("287", "288", "[]", "NULL"),
                itemRow("1477", "1487", "[ ]", "null"));
        useProps(props());
        when(runRepository.findByStatus("RUNNING")).thenReturn(List.of());
        AtomicReference<ImportRun> stored = new AtomicReference<>();
        echoRunRepo(stored);
        UUID listId = UUID.randomUUID();
        UUID vitalId = UUID.randomUUID();
        when(listService.importList(any(), any(), any(), any())).thenReturn(
                new ListImportResult(listId,
                        new ListImportService.ItemCounts(0, 0, 0, 0, 0),
                        VitalMergeService.MergeCounts.zero(), false));
        when(vitalService.mergeVitalList(any(), any(), any())).thenReturn(
                new VitalMergeResult(vitalId, new VitalMergeService.MergeCounts(1, 0, 0),
                        false, false, "MedicineList:1411", null));

        runner.run();

        verify(listService).importList(any(), any(), any(), any());
        verify(vitalService).mergeVitalList(any(), any(), any());
        assertThat(stored.get().getStatus()).isEqualTo("FINISHED");
        assertThat(stored.get().getCounts()).contains("\"listsCreated\":1");
        assertThat(stored.get().getCounts()).contains("\"vitEntriesInserted\":1");
        verify(quarantineRepository, never()).save(any());
        verify(auditService, never()).logEvent(any(), any(), any(), any(), any(), any(), any());
        verify(context).close();
    }

    @Test
    void run_processesVitalListsAfterPrescriptionLists() throws Exception {
        writeLists(
                "288;3330;" + PRIZN_DOC + ";author;2025-06-24 12:55:33.827;;0;NULL",
                "1487;4269;Vital doc;author;2026-02-10 10:00:00.000;;0;[ ]");
        writeItems(
                itemRow("1477", "1487", "[ ]", "null"),
                itemRow("287", "288", "[]", "NULL"));
        useProps(props());
        when(runRepository.findByStatus("RUNNING")).thenReturn(List.of());
        AtomicReference<ImportRun> stored = new AtomicReference<>();
        echoRunRepo(stored);
        when(listService.importList(any(), any(), any(), any())).thenReturn(
                new ListImportResult(UUID.randomUUID(),
                        new ListImportService.ItemCounts(0, 0, 0, 0, 0),
                        VitalMergeService.MergeCounts.zero(), false));
        when(vitalService.mergeVitalList(any(), any(), any())).thenReturn(
                new VitalMergeResult(UUID.randomUUID(),
                        new VitalMergeService.MergeCounts(1, 0, 0),
                        false, false, "MedicineList:1411", null));

        runner.run();

        var order = org.mockito.Mockito.inOrder(listService, vitalService);
        order.verify(listService).importList(any(), any(), any(), any());
        order.verify(vitalService).mergeVitalList(any(), any(), any());
    }

    @Test
    void run_countsShellListsAsCreated() throws Exception {
        writeLists("1518;9715;Vital doc;author;2026-02-09 19:56:49.377;;0;[ ]");
        writeItems(itemRow("1517", "1518", "[ ]", "null"));
        ImportProperties shellProps = props();
        shellProps.setAuditEnabled(true);
        useProps(shellProps);
        when(runRepository.findByStatus("RUNNING")).thenReturn(List.of());
        AtomicReference<ImportRun> stored = new AtomicReference<>();
        echoRunRepo(stored);
        UUID shellId = UUID.randomUUID();
        when(vitalService.mergeVitalList(any(), any(), any())).thenReturn(
                new VitalMergeResult(UUID.randomUUID(),
                        new VitalMergeService.MergeCounts(1, 0, 0),
                        false, false, VitalMergeService.SHELL_PARENT, shellId));

        runner.run();

        assertThat(stored.get().getCounts()).contains("\"listsCreated\":1");
        assertThat(stored.get().getCounts()).contains("\"vitListsMerged\":1");
        verify(auditService).logEvent(eq("PrescriptionList"), eq(shellId), eq("IMPORT"),
                eq(0L), eq("MedicineList:1518"), any(), any());
        verify(auditService).logEvent(eq("VitalSignList"), any(), eq("IMPORT"),
                eq(0L), any(), any(), any());
    }

    @Test
    void run_validateRequiresRunId() {
        ImportProperties properties = props();
        properties.setMode("validate");
        useProps(properties);
        assertThatThrownBy(() -> runner.run()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void run_validateDelegatesAndFailsClosed() {
        ImportProperties properties = props();
        properties.setMode("validate");
        UUID runId = UUID.randomUUID();
        properties.setValidateRunId(runId);
        useProps(properties);
        var ok = new ImportValidationService.ValidationReport(runId, List.of(
                new ImportValidationService.CheckResult("c", "e", "a", true)));
        when(validationService.validate(eq(runId), any())).thenReturn(ok);
        org.junit.jupiter.api.Assertions.assertDoesNotThrow(() -> runner.run());

        var failed = new ImportValidationService.ValidationReport(runId, List.of(
                new ImportValidationService.CheckResult("c", "e", "x", false)));
        when(validationService.validate(eq(runId), any())).thenReturn(failed);
        assertThatThrownBy(() -> runner.run()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void run_reconRequiresOut() {
        ImportProperties properties = props();
        properties.setMode("recon");
        useProps(properties);
        assertThatThrownBy(() -> runner.run()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void run_reconWritesCensus() throws Exception {
        writeLists(
                "288;3330;" + PRIZN_DOC + ";author;2025-06-24 12:55:33.827;;0;NULL",
                "1487;4269;Vital doc;author;2026-02-10 10:00:00.000;;0;[ ]",
                "9999;4242;" + PRIZN_DOC + ";author;2025-06-24 12:55:33.827;;0;NULL");
        writeItems(itemRow("287", "288", "[]", "NULL"));
        ImportProperties properties = props();
        properties.setMode("recon");
        Path out = temp.resolve("recon.csv");
        properties.setReconOut(out.toString());
        useProps(properties);
        var present = org.mockito.Mockito.mock(com.superhumans.mis.dto.PatientDTO.class);
        when(present.getDepartmentId()).thenReturn(19L);
        when(present.getFullName()).thenReturn("Test Patient");
        when(misService.getPatient(3330L)).thenReturn(Optional.of(present));
        when(misService.getPatient(4269L)).thenReturn(Optional.of(present));
        when(misService.getPatient(4242L)).thenReturn(Optional.empty());

        runner.run();

        List<String> lines = Files.readAllLines(out);
        assertThat(lines.get(0)).isEqualTo("patientRef;exists;departmentId;fullName;lists");
        assertThat(lines).anySatisfy(line -> assertThat(line).startsWith("3330;true;19;"));
        assertThat(lines).anySatisfy(line -> assertThat(line).startsWith("4242;false;"));
        verify(runRepository, never()).save(any());
    }

    @Test
    void run_reconAbortsOnMisFailure() throws Exception {
        writeLists("288;3330;" + PRIZN_DOC + ";author;2025-06-24 12:55:33.827;;0;NULL");
        writeItems(itemRow("287", "288", "[]", "NULL"));
        ImportProperties properties = props();
        properties.setMode("recon");
        properties.setReconOut(temp.resolve("recon.csv").toString());
        useProps(properties);
        when(misService.getPatient(3330L)).thenThrow(new RuntimeException("MIS down"));

        assertThatThrownBy(() -> runner.run()).isInstanceOf(IllegalStateException.class);
    }

    @Test
    void run_quarantinesOrphanItems() throws Exception {
        writeLists("288;3330;" + PRIZN_DOC + ";author;2025-06-24 12:55:33.827;;0;NULL");
        writeItems(itemRow("999", "4242", "[]", "NULL"));
        useProps(props());
        when(runRepository.findByStatus("RUNNING")).thenReturn(List.of());
        AtomicReference<ImportRun> stored = new AtomicReference<>();
        echoRunRepo(stored);
        when(quarantineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        runner.run();

        verify(listService, never()).importList(any(), any(), any(), any());
        var quarantineCaptor = ArgumentCaptor.forClass(
                com.superhumans.medicationsheet.entity.ImportQuarantine.class);
        verify(quarantineRepository).save(quarantineCaptor.capture());
        assertThat(quarantineCaptor.getValue().getReason()).isEqualTo("ITEM_ORPHAN");
        assertThat(stored.get().getCounts()).contains("\"listsQuarantined\":1");
    }

    @Test
    void run_abortsOnMissingPatients() throws Exception {
        writeLists("288;3330;" + PRIZN_DOC + ";author;2025-06-24 12:55:33.827;;0;NULL");
        writeItems(itemRow("287", "288", "[]", "NULL"));
        ImportProperties properties = props();
        properties.setSkipPatientCheck(false);
        properties.setAllowMissingPatients(false);
        useProps(properties);
        when(runRepository.findByStatus("RUNNING")).thenReturn(List.of());
        when(misService.getPatient(3330L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> runner.run()).isInstanceOf(IllegalStateException.class);
        verify(listService, never()).importList(any(), any(), any(), any());
    }

    @Test
    void run_quarantinesMissingPatientsWhenAllowed() throws Exception {
        writeLists("288;3330;" + PRIZN_DOC + ";author;2025-06-24 12:55:33.827;;0;NULL");
        writeItems(itemRow("287", "288", "[]", "NULL"));
        ImportProperties properties = props();
        properties.setSkipPatientCheck(false);
        properties.setAllowMissingPatients(true);
        useProps(properties);
        when(runRepository.findByStatus("RUNNING")).thenReturn(List.of());
        AtomicReference<ImportRun> stored = new AtomicReference<>();
        echoRunRepo(stored);
        when(misService.getPatient(3330L)).thenReturn(Optional.empty());
        when(quarantineRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));

        runner.run();

        verify(listService, never()).importList(any(), any(), any(), any());
        var quarantineCaptor = ArgumentCaptor.forClass(
                com.superhumans.medicationsheet.entity.ImportQuarantine.class);
        verify(quarantineRepository).save(quarantineCaptor.capture());
        assertThat(quarantineCaptor.getValue().getReason()).isEqualTo("MISSING_PATIENT");
        assertThat(stored.get().getStatus()).isEqualTo("FINISHED");
    }
}
