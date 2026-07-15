package com.superhumans.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "hourly_records", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"clinical_day_id", "record_hour"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HourlyRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    private ClinicalDay clinicalDay;

    @Column(name = "record_time", nullable = false)
    private LocalDateTime recordTime;

    @Column(name = "record_hour", nullable = false)
    private Integer recordHour;

    @Column(length = 50)
    private String consciousness;

    @DecimalMin("34.0") @DecimalMax("42.0")
    private Double temperature;

    @Min(0) @Max(300)
    @Column(name = "heart_rate")
    private Integer heartRate;

    @Min(0) @Max(60)
    @Column(name = "respiratory_rate")
    private Integer respiratoryRate;

    @Min(50) @Max(250)
    @Column(name = "systolic_bp")
    private Integer systolicBP;

    @Min(30) @Max(150)
    @Column(name = "diastolic_bp")
    private Integer diastolicBP;

    @Column(name = "mean_arterial_pressure")
    private Integer meanArterialPressure;

    @DecimalMin("50.0") @DecimalMax("100.0")
    @Column(name = "spo2")
    private Double spo2;

    @DecimalMin("1.0") @DecimalMax("30.0")
    private Double glucose;

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

    @PrePersist
    @PreUpdate
    public void validateClinicalRanges() {
        if (recordTime != null) {
            this.recordHour = recordTime.getHour();
        }
        if (heartRate != null && (heartRate < 0 || heartRate > 300))
            throw new IllegalArgumentException("Heart rate must be between 0 and 300 bpm");
        if (systolicBP != null && (systolicBP < 50 || systolicBP > 250))
            throw new IllegalArgumentException("Systolic BP must be between 50 and 250 mmHg");
        if (diastolicBP != null && (diastolicBP < 30 || diastolicBP > 150))
            throw new IllegalArgumentException("Diastolic BP must be between 30 and 150 mmHg");
        if (temperature != null && (temperature < 34.0 || temperature > 42.0))
            throw new IllegalArgumentException("Temperature must be between 34.0 and 42.0 °C");
        if (spo2 != null && (spo2 < 50.0 || spo2 > 100.0))
            throw new IllegalArgumentException("Oxygen saturation must be between 50 and 100%");
        if (respiratoryRate != null && (respiratoryRate < 0 || respiratoryRate > 60))
            throw new IllegalArgumentException("Respiratory rate must be between 0 and 60 breaths/min");
        if (glucose != null && (glucose < 1.0 || glucose > 30.0))
            throw new IllegalArgumentException("Glucose must be between 1.0 and 30.0 mmol/L");
    }

}
