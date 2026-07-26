package com.superhumans.entity;

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

    @Column(columnDefinition = "TEXT")
    String comment;
}
