package com.superhumans.config.multidb;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * #375 — inventory of every service that writes a feature database and the core
 * audit table inside one chained transaction.
 *
 * <p>Such a service is exposed to the commit-phase window documented in
 * {@link MultiTransactionConfig}: if a later leg fails while committing, the
 * business rows can be durable while their {@code audit_logs} row is rolled back.
 * The list is intentionally pinned by a test, because the failure mode is invisible
 * in code review — a new {@code auditService.log...} call inside a feature
 * transaction widens the window without anything looking different.
 *
 * <p>Adding a service means adding it to {@link #KNOWN_CROSS_DATABASE_WRITERS} in
 * the same commit, with a note on whether it writes the core audit leg.
 */
class CrossDatabaseWriteInventoryTest {

    /** Feature modules: anything outside {@code common} writes a non-core database. */
    private static final List<String> FEATURE_MODULES =
            List.of("icu-chart", "medication-sheet", "prosthesis-manufacturing");

    /** Services that pair a feature-database write with the core-side legacy audit. */
    private static final Set<String> KNOWN_CROSS_DATABASE_WRITERS = Set.of(
            // icu-chart
            "ClinicalDayService.java",
            "ClinicalScaleService.java",
            "EpisodeService.java",
            "FluidBalanceService.java",
             "HourlyRecordService.java",
             "IcuPdfAuditService.java",
             "LabResultService.java",
            "MedicalNoteService.java",
            "MedicalOrderService.java",
            "OrderExecutionService.java",
            "PatientStateAssessmentService.java",
            "VentilationSettingsService.java",
             // medication-sheet
             "DrugInteractionImportService.java",
             "PrescriptionItemService.java",
             "PrescriptionPdfAuditService.java",
            // prosthesis-manufacturing
            "BrakNotificationDeliveryService.java",
            "BrakService.java",
            "EvidenceFileService.java",
            "FailureSnapshotService.java",
            "FlowInstanceService.java",
            "FlowTemplateService.java",
            "ProductionNormativeService.java");

    private static final Pattern LEGACY_AUDIT_CALL = Pattern.compile(
            "auditService\\.log(Action|Create|Update|Delete|Event|Auth)\\(");

    private static Path backendRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path root = current;
        while (root != null && !Files.exists(root.resolve("common/pom.xml"))) {
            root = root.getParent();
        }
        return Files.isDirectory(root.resolve("common")) ? root : root.getParent();
    }

    @Test
    void onlyKnownServicesWriteTheCoreAuditLegFromAFeatureDatabase() throws IOException {
        Set<String> found = new TreeSet<>();
        Path backend = backendRoot();

        for (String module : FEATURE_MODULES) {
            Path sources = backend.resolve(module + "/src/main/java");
            try (Stream<Path> files = Files.walk(sources)) {
                for (Path file : files.filter(p -> p.toString().endsWith("Service.java")).toList()) {
                    String text = Files.readString(file, StandardCharsets.UTF_8);
                    if (LEGACY_AUDIT_CALL.matcher(text).find()) {
                        found.add(file.getFileName().toString());
                    }
                }
            }
        }

        assertThat(found)
                .describedAs("a feature-module service now calls the core-side legacy audit. "
                        + "Add it to KNOWN_CROSS_DATABASE_WRITERS (it joins the commit-phase "
                        + "window documented in MultiTransactionConfig) or remove the core leg")
                .isEqualTo(new TreeSet<>(KNOWN_CROSS_DATABASE_WRITERS));

        assertThat(found)
                .describedAs("the inventory must not be emptied by accident")
                .isNotEmpty();
    }

    @Test
    void legacyAuditIsASingleChokePointSoTheWindowCanBeClosedOperationally() throws IOException {
        // The mitigation lever: one kill switch removes the core leg from every
        // cross-database write at once. If a legacy audit write ever bypasses
        // AuditService, that lever stops working and the window widens silently.
        String auditService = Files.readString(backendRoot().resolve(
                "common/src/main/java/com/superhumans/service/AuditService.java"),
                StandardCharsets.UTF_8);

        assertThat(auditService)
                .describedAs("the kill switch is the documented mitigation for #375")
                .contains("app.audit.legacy-write-enabled");

        int guards = auditService.split("legacyWriteEnabled", -1).length - 1;
        assertThat(guards)
                .describedAs("both write paths (logEvent, logAuth) must honour the kill switch - "
                        + "one guard each for the field and the check")
                .isGreaterThanOrEqualTo(3);
    }
}
