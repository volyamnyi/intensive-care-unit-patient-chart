package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.Getter;
import lombok.Setter;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@SuppressWarnings("PMD.AbstractClassWithoutAbstractMethod")

@MappedSuperclass
@Getter @Setter
public abstract class BaseEntity {

    @Id
    @Column(columnDefinition = "UUID", updatable = false, nullable = false)
    UUID id;

    @Column(name = "created_at", nullable = false, updatable = false)
    LocalDateTime createdAt;

    @Column(name = "created_by", nullable = false, updatable = false)
    Long createdBy;

    @Column(name = "updated_at", nullable = false)
    LocalDateTime updatedAt;

    @Column(name = "updated_by", nullable = false)
    Long updatedBy;

    @Version
    @Column(nullable = false)
    Integer version;

    @Column(name = "is_deleted")
    Boolean deleted = false;

    @PrePersist
    protected void onCreate() {
        if (id == null) {
            id = UUID.randomUUID();
        }
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (version == null) {
            version = 0;
        }
    }

    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
}
