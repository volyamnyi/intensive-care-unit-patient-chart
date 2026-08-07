package com.superhumans.prosthesismanufacturing.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.Pattern;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;
import java.time.LocalDate;
import java.time.LocalDateTime;

@Entity
@Table(name = "prosthetics_patients")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProstheticsPatient {

    /**
     * Patient ID is a digits-only string (e.g. "1234") — no letters,
     * separators or spaces. Enforced by @Pattern and a DB CHECK constraint
     * (see changesets/013-patient-id-digits.sql).
     */
    @Id
    @Column(name = "id", length = 32, updatable = false, nullable = false)
    @Pattern(regexp = "\\d+",
            message = "ID пацієнта має містити лише цифри, без символів та пробілів")
    String id;

    @Column(name = "pib", nullable = false)
    String pib;

    @Column(name = "birth_date")
    LocalDate birthDate;

    @Column(name = "gender", length = 16)
    String gender;

    @Column(name = "height_cm")
    Integer heightCm;

    @Column(name = "weight_kg")
    Integer weightKg;

    @Column(name = "social_status", length = 64)
    String socialStatus;

    @Column(name = "cause")
    String cause;

    @Column(name = "amputation_date")
    LocalDate amputationDate;

    @Column(name = "affected_limb", length = 16)
    String affectedLimb;

    @Column(name = "amputation_level", length = 64)
    String amputationLevel;

    @Column(name = "stump", columnDefinition = "jsonb")
    @JdbcTypeCode(SqlTypes.JSON)
    String stump;

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
            id = String.valueOf(System.nanoTime());
        }
        LocalDateTime now = LocalDateTime.now();
        if (createdAt == null) {
            createdAt = now;
        }
        if (updatedAt == null) {
            updatedAt = now;
        }
        if (createdBy == null) {
            createdBy = 0L;
        }
        if (updatedBy == null) {
            updatedBy = 0L;
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
