package com.superhumans.medicationsheet.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.experimental.FieldDefaults;

/**
 * One importer execution (staging, table {@code import_run}).
 * Survives rollback with status {@code ROLLED_BACK} for audit trail.
 */
@Entity
@Table(name = "import_run")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ImportRun {

    @Id
    @Column(name = "run_id", nullable = false, updatable = false)
    UUID runId;

    @Column(name = "source_hash", nullable = false, length = 128, updatable = false)
    String sourceHash;

    @Column(name = "started_at", nullable = false, updatable = false)
    LocalDateTime startedAt;

    @Column(name = "finished_at")
    LocalDateTime finishedAt;

    @Column(name = "status", nullable = false, length = 16)
    String status;

    @Column(name = "counts", columnDefinition = "TEXT")
    String counts;
}
