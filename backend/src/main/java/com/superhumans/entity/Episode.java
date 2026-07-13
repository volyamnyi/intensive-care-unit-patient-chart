package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "episodes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Episode extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    private UUID patientId;

    @Column(name = "hospitalization_id")
    private UUID hospitalizationId;

    @Column(name = "department_id")
    private UUID departmentId;

    @Column(name = "admission_date", nullable = false)
    private LocalDateTime admissionDate;

    @Column(name = "discharge_date")
    private LocalDateTime dischargeDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private EpisodeStatus status;
}
