package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "medical_orders")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MedicalOrder extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    private ClinicalDay clinicalDay;

    @Column(nullable = false, length = 50)
    private String category;

    @Column(name = "drug_name", nullable = false, length = 200)
    private String drugName;

    @Column(nullable = false, length = 100)
    private String dose;

    @Column(nullable = false, length = 50)
    private String unit;

    @Column(nullable = false, length = 50)
    private String route;

    @Column(nullable = false, length = 100)
    private String frequency;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time")
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private MedicalOrderStatus status;
}
