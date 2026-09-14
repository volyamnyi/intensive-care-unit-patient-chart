package com.superhumans.medicationsheet.migration;

import java.util.UUID;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;

/**
 * {@code app.import.*} settings for the one-shot migration run.
 * Only bound when the {@code migration} profile is active.
 */
@Component
@ConfigurationProperties(prefix = "app.import")
@Getter
@Setter
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportProperties {

    /** Directory holding {@code MedicineList.csv} and {@code MedicineListItem.csv}. */
    String csvDir;

    /** {@code import} (default), {@code rollback}, {@code validate} or {@code recon}. */
    String mode = "import";

    /** Required in {@code rollback} mode. */
    UUID rollbackRunId;

    /** Required in {@code validate} mode. */
    UUID validateRunId;

    /** Required in {@code recon} mode: where the patient census CSV goes. */
    String reconOut;

    /** Write IMPORT audit rows (disable for dry-runs without a core copy). */
    boolean auditEnabled = true;

    /** Skip the MIS patient pre-check (local dry-runs without MIS access). */
    boolean skipPatientCheck = false;

    /**
     * Proceed when patients are missing from MIS (their lists go to
     * quarantine with reason {@code MISSING_PATIENT}); otherwise abort.
     */
    boolean allowMissingPatients = false;
}
