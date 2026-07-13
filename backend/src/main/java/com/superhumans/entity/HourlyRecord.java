package com.superhumans.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hourly_records")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HourlyRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    private ClinicalDay clinicalDay;

    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;

    @Column(length = 50)
    private String consciousness;

    private Double temperature;

    @Column(name = "heart_rate")
    private Integer heartRate;

    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    @Column(name = "systolic_bp")
    private Integer systolicBP;

    @Column(name = "diastolic_bp")
    private Integer diastolicBP;

    @Column(name = "mean_arterial_pressure")
    private Integer meanArterialPressure;

    @Column(name = "spo2")
    private Double spo2;

    @Column(name = "etco2")
    private Double etco2;

    @Column(name = "fio2")
    private Double fio2;

    @Column(name = "cvp")
    private Double cvp;

    @Column(name = "urine_output")
    private Double urineOutput;

    @Column(name = "drain_output")
    private Double drainOutput;

    @Column(columnDefinition = "TEXT")
    private String stool;

    @Column(columnDefinition = "TEXT")
    private String vomit;

    @Column(name = "pain_score")
    private Integer painScore;

    @Column(columnDefinition = "TEXT")
    private String notes;
}
