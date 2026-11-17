package com.superhumans.medicationsheet.entity;

import com.superhumans.entity.base.BaseEntity;

import jakarta.persistence.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "prescription_executions")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionExecution extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_part_id", nullable = false)
    PrescriptionDayPart dayPart;

    @Column(name = "executed_by")
    UUID executedBy;

    @Column(name = "executed_at")
    LocalDateTime executedAt;

    @Column(name = "actual_dose", length = 100)
    String actualDose;

    @Column(length = 32)
    String status;

    @Column(name = "requires_2p_auth")
    Boolean requires2pAuth;

    @Column(name = "second_person_id")
    UUID secondPersonId;

    /**
     * Actual administration time - the medical fact as recorded/adjusted
     * (#443). Server-defaulted to the operation time; manual entry is flagged
     * by {@link #administeredAtManual}. Naive local (design §6).
     */
    @Column(name = "administered_at")
    LocalDateTime administeredAt;

    /** Actual dispensing time - optional, distinct from administration (#443). */
    @Column(name = "dispensed_at")
    LocalDateTime dispensedAt;

    /**
     * True when {@link #administeredAt} was entered/adjusted manually.
     * NOT NULL in the schema - {@code @Builder} does not apply field defaults,
     * so every construction site must set it explicitly.
     */
    @Column(name = "administered_at_manual", nullable = false)
    Boolean administeredAtManual;

    @Column(columnDefinition = "TEXT")
    String comment;
}
