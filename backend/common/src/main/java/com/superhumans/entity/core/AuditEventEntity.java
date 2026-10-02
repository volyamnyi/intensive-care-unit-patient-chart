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
import lombok.Setter;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/** Searchable projection and immutable JSON payload for the Audit v2 event store. */
@Entity
@Table(name = "audit_events")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditEventEntity {

    @Id
    @Column(name = "audit_id", columnDefinition = "UUID", nullable = false, updatable = false)
    UUID auditId;

    @Column(name = "schema_version", nullable = false)
    Integer schemaVersion;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    Instant occurredAt;

    @Column(name = "recorded_at", nullable = false, updatable = false)
    Instant recordedAt;

    @Column(name = "event_class", nullable = false, length = 32, updatable = false)
    String eventClass;

    @Column(nullable = false, length = 24, updatable = false)
    String module;

    @Column(name = "functional_area", nullable = false, length = 100, updatable = false)
    String functionalArea;

    @Column(nullable = false, length = 160, updatable = false)
    String action;

    @Column(name = "action_type", nullable = false, length = 40, updatable = false)
    String actionType;

    @Column(nullable = false, length = 16, updatable = false)
    String criticality;

    @Column(name = "actor_type", nullable = false, length = 24, updatable = false)
    String actorType;

    @Column(name = "actor_id", length = 128, updatable = false)
    String actorId;

    @Column(name = "actor_login", length = 100, updatable = false)
    String actorLogin;

    @Column(name = "actor_display_name", length = 200, updatable = false)
    String actorDisplayName;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "actor_roles", columnDefinition = "jsonb", nullable = false, updatable = false)
    String actorRoles;

    @Column(name = "target_type", length = 100, updatable = false)
    String targetType;

    @Column(name = "target_id", length = 255, updatable = false)
    String targetId;

    @Column(name = "business_key", length = 255, updatable = false)
    String businessKey;

    @Column(name = "outcome", nullable = false, length = 24, updatable = false)
    String outcome;

    @Column(name = "request_id", columnDefinition = "UUID", updatable = false)
    UUID requestId;

    @Column(name = "user_action_id", columnDefinition = "UUID", updatable = false)
    UUID userActionId;

    @Column(name = "correlation_id", columnDefinition = "UUID", updatable = false)
    UUID correlationId;

    @Column(name = "parent_audit_id", columnDefinition = "UUID", updatable = false)
    UUID parentAuditId;

    @Column(name = "retention_class", nullable = false, length = 32, updatable = false)
    String retentionClass;

    @Column(name = "retention_until", nullable = false, updatable = false)
    Instant retentionUntil;

    @Column(name = "integrity_hash", length = 64, updatable = false)
    String integrityHash;

    @JdbcTypeCode(SqlTypes.JSON)
    @Column(name = "event_payload", columnDefinition = "jsonb", nullable = false, updatable = false)
    String eventPayload;

    @PrePersist
    protected void onCreate() {
        if (auditId == null) {
            auditId = UUID.randomUUID();
        }
        if (recordedAt == null) {
            recordedAt = Instant.now();
        }
    }
}
