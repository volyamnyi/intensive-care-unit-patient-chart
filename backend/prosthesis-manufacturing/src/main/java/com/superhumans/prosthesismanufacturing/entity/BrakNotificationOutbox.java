package com.superhumans.prosthesismanufacturing.entity;

import com.superhumans.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import java.util.UUID;

/**
 * Outbox row for one confirmed stage-6 brak email (issue #320, v2).
 *
 * <p>Inserted in the same transaction as the {@code BrakEvent} itself, so a
 * brak can never exist without its queued notification. Exactly one row per
 * brak ({@code UNIQUE(brak_event_id)}), which also makes redelivery idempotent.
 *
 * <p>Epic #322 adds {@link BrakNotificationKind#THRESHOLD} rows: one per
 * triggering brak event whose order chain reached brak count &gt;= 3 (both
 * trigger steps summed per orderId). {@code orderId} anchors those rows to
 * the order chain; it stays {@code null} for {@code SINGLE} rows.
 */
@Entity
@Table(name = "prosthetics_brak_notifications")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class BrakNotificationOutbox extends BaseEntity {

    @Column(name = "brak_event_id", nullable = false, unique = true)
    UUID brakEventId;

    @Enumerated(EnumType.STRING)
    @Column(name = "kind", nullable = false, length = 16)
    @Builder.Default
    BrakNotificationKind kind = BrakNotificationKind.SINGLE;

    @Column(name = "order_id")
    UUID orderId;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 16)
    @Builder.Default
    BrakNotificationStatus status = BrakNotificationStatus.PENDING;

    @Column(name = "attempts", nullable = false)
    @Builder.Default
    Integer attempts = 0;

    @Column(name = "last_error", columnDefinition = "TEXT")
    String lastError;

    @PrePersist
    @PreUpdate
    public void validate() {
        if (brakEventId == null) {
            throw new IllegalArgumentException("brakEventId is required");
        }
        if (kind == null) {
            throw new IllegalArgumentException("kind is required");
        }
        if (kind == BrakNotificationKind.THRESHOLD && orderId == null) {
            throw new IllegalArgumentException("orderId is required for THRESHOLD rows");
        }
        if (status == null) {
            throw new IllegalArgumentException("status is required");
        }
        if (attempts == null || attempts < 0) {
            throw new IllegalArgumentException("attempts must not be negative");
        }
    }
}
