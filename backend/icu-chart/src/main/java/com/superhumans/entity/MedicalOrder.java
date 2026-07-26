package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "medical_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    ClinicalDay clinicalDay;

    @Column(nullable = false, length = 50)
    String category;

    @Column(name = "drug_name", nullable = false, length = 200)
    String drugName;

    @Column(nullable = false, length = 100)
    String dose;

    @Column(nullable = false, length = 50)
    String unit;

    @Column(nullable = false, length = 50)
    String route;

    @Column(nullable = false, length = 100)
    String frequency;

    @Column(name = "start_time", nullable = false)
    LocalDateTime startTime;

    @Column(name = "end_time")
    LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    MedicalOrderStatus status;
}
