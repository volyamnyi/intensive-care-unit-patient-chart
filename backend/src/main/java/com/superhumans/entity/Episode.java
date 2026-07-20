package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "episodes")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class Episode extends BaseEntity {

    @Column(name = "patient_id", nullable = false)
    Long patientId;

    @Column(name = "hospitalization_id")
    UUID hospitalizationId;

    @Column(name = "department_id")
    UUID departmentId;

    @Column(name = "admission_date", nullable = false)
    LocalDateTime admissionDate;

    @Column(name = "discharge_date")
    LocalDateTime dischargeDate;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    EpisodeStatus status;

    @Column(name = "height_cm")
    Double heightCm;

    @Column(name = "ward", length = 50)
    String ward;

    @Column(name = "bed_number", length = 20)
    String bedNumber;

    @Column(name = "admission_diagnosis", length = 500)
    String admissionDiagnosis;
}
