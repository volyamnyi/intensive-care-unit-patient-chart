package com.superhumans.entity.core;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.IdClass;
import jakarta.persistence.Table;
import java.time.Instant;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.experimental.FieldDefaults;

/** Indexed entity-reference projection used to build cross-aggregate history. */
@Entity
@Table(name = "audit_event_targets")
@IdClass(AuditEventTargetId.class)
@org.hibernate.annotations.Immutable
@Getter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditEventTargetEntity {

    @Id
    @Column(name = "audit_id", nullable = false, updatable = false)
    UUID auditId;

    @Id
    @Column(name = "relation_type", nullable = false, length = 16, updatable = false)
    String relationType;

    @Id
    @Column(name = "entity_type", nullable = false, length = 100, updatable = false)
    String entityType;

    @Id
    @Column(name = "entity_id", nullable = false, length = 255, updatable = false)
    String entityId;

    @Column(name = "business_key", length = 255, updatable = false)
    String businessKey;

    @Column(name = "occurred_at", nullable = false, updatable = false)
    Instant occurredAt;
}
