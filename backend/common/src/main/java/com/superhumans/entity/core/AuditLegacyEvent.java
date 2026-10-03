package com.superhumans.entity.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/**
 * Frozen, clearly-marked copy of one {@code audit_logs} row (F8 backfill,
 * §H.4). Nothing is reconstructed: original columns are stored verbatim,
 * the outcome is always {@code UNKNOWN_LEGACY}, and the naive source
 * timestamp is kept as a UTC-assumed instant flagged {@code LEGACY_NAIVE}.
 * Legacy rows are never presented as proof of a successful operation.
 */
@Entity
@Table(name = "audit_legacy_events")
@org.hibernate.annotations.Immutable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLegacyEvent {

    @Id
    @Column(name = "audit_id", columnDefinition = "UUID", nullable = false, updatable = false)
    UUID auditId;

    @Column(name = "source_table", nullable = false, length = 64, updatable = false)
    String sourceTable;

    @Column(name = "source_id", columnDefinition = "UUID", nullable = false, updatable = false)
    UUID sourceId;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    Instant occurredAt;

    @Column(name = "legacy_kind", nullable = false, length = 32, updatable = false)
    String legacyKind;

    @Column(name = "legacy_entity", nullable = false, length = 100, updatable = false)
    String legacyEntity;

    @Column(name = "legacy_entity_id", columnDefinition = "UUID", updatable = false)
    UUID legacyEntityId;

    @Column(name = "legacy_action", nullable = false, length = 100, updatable = false)
    String legacyAction;

    @Column(name = "legacy_user_id", updatable = false)
    Long legacyUserId;

    @Column(name = "legacy_user_role", length = 255, updatable = false)
    String legacyUserRole;

    @Column(name = "legacy_ip_address", length = 255, updatable = false)
    String legacyIpAddress;

    @Column(name = "legacy_old_value", columnDefinition = "TEXT", updatable = false)
    String legacyOldValue;

    @Column(name = "legacy_new_value", columnDefinition = "TEXT", updatable = false)
    String legacyNewValue;

    @Column(name = "legacy_details", columnDefinition = "TEXT", updatable = false)
    String legacyDetails;

    @Column(name = "legacy_correlation_id", length = 100, updatable = false)
    String legacyCorrelationId;

    @Column(name = "legacy_is_deleted", nullable = false, updatable = false)
    Boolean legacyIsDeleted;

    @Column(name = "schema_version", nullable = false, updatable = false)
    Integer schemaVersion;

    @Column(name = "context_completeness", nullable = false, length = 32, updatable = false)
    String contextCompleteness;

    @Column(name = "timestamp_precision", nullable = false, length = 32, updatable = false)
    String timestampPrecision;

    @Column(name = "outcome", nullable = false, length = 32, updatable = false)
    String outcome;

    @Column(name = "checksum", length = 64, nullable = false, updatable = false)
    String checksum;

    @Column(name = "retention_until", nullable = false, updatable = false)
    Instant retentionUntil;

    @Column(name = "backfilled_at", nullable = false, updatable = false)
    Instant backfilledAt;

    @PrePersist
    protected void onCreate() {
        if (auditId == null) {
            auditId = UUID.randomUUID();
        }
        if (backfilledAt == null) {
            backfilledAt = Instant.now();
        }
    }
}
