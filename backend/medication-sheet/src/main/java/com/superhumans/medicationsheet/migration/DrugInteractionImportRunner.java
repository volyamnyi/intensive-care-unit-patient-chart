package com.superhumans.medicationsheet.migration;

import com.superhumans.medicationsheet.dto.DrugInteractionImportReport;
import com.superhumans.medicationsheet.service.DrugInteractionImportService;
import com.superhumans.medicationsheet.service.DrugInteractionImportService.ImportError;
import java.nio.file.Files;
import java.nio.file.Path;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

/**
 * CLI driver for the drug-interactions dataset import (issue #304).
 * Active only under the {@code migration} profile AND when
 * {@code app.drug-interaction.import.file} points to the JSON document, e.g.
 * <pre>
 * java -jar app.jar --spring.profiles.active=migration
 *     --app.drug-interaction.import-file=C:\datasets\drug_interactions_dataset.json
 * </pre>
 * The runner is a no-op when the property is not set, so it coexists with
 * {@link MedicineImportRunner} on the same profile.
 */
@Slf4j
@Component
@Profile("migration")
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class DrugInteractionImportRunner implements CommandLineRunner {

    DrugInteractionImportService importService;
    DrugInteractionImportProperties properties;

    @Override
    public void run(String... args) throws Exception {
        String file = properties.getImportFile();
        if (file == null || file.isBlank()) {
            log.info("Drug-interactions import skipped: app.drug-interaction.import-file not set");
            return;
        }
        Path path = Path.of(file);
        if (!Files.isRegularFile(path)) {
            throw new IllegalStateException("Interactions dataset not found: " + path);
        }
        byte[] content = Files.readAllBytes(path);
        try {
            DrugInteractionImportReport report = importService.importDataset(content, 0L);
            log.info("Drug-interactions import done: drugs={}, pairs={}, skipped={}, {} ms, hash={}",
                    report.getDrugs(), report.getInteractions(), report.getSkipped(),
                    report.getDurationMs(), report.getSourceHash());
        } catch (ImportError e) {
            log.error("Drug-interactions import rejected: {} valid rows, {} bad rows",
                    e.getSkippedDetails().stream().count(), e.getSkippedDetails().size());
            throw new IllegalStateException("Interactions import rejected — no valid rows", e);
        }
    }
}
