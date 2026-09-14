package com.superhumans.medicationsheet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * Quarantined source record (staging, table {@code import_quarantine}).
 * A re-run retries quarantined old IDs (they have no {@link ImportIdMap} row),
 * so the quarantine count is stable instead of growing.
 */
@Entity
@Table(name = "import_quarantine")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportQuarantine {

    @Id
    @Column(name = "id", nullable = false, updatable = false)
    UUID id;

    @Column(name = "run_id", nullable = false, updatable = false)
    UUID runId;

    @Column(name = "old_list_id", nullable = false, length = 64, updatable = false)
    String oldListId;

    @Column(name = "old_kind", nullable = false, length = 16, updatable = false)
    String oldKind;

    @Column(name = "reason", nullable = false, length = 64, updatable = false)
    String reason;

    @Column(name = "payload", columnDefinition = "TEXT", updatable = false)
    String payload;
}
