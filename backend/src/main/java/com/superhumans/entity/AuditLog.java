package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.Where;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "audit_logs")
@Where(clause = "is_deleted IS NULL OR is_deleted = false")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class AuditLog {

    @Id
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    UUID id;

    @Column(nullable = false)
    LocalDateTime timestamp;

    @Column(name = "user_id")
    UUID userId;

    @Column(nullable = false, length = 100)
    String entity;

    @Column(name = "entity_id")
    UUID entityId;

    @Column(nullable = false, length = 100)
    String action;

    @Column(name = "old_value", columnDefinition = "TEXT")
    String oldValue;

    @Column(name = "new_value", columnDefinition = "TEXT")
    String newValue;

    @Column(name = "correlation_id", length = 100)
    String correlationId;

    @Column(columnDefinition = "TEXT")
    String details;

    @Column(name = "ip_address")
    String ipAddress;

    @Column(name = "user_role")
    String userRole;

    @Column(name = "is_deleted")
    Boolean isDeleted = false;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        if (timestamp == null) {
            timestamp = LocalDateTime.now();
        }
    }
}
