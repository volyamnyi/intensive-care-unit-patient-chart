package com.superhumans.entity;

import com.superhumans.util.ClinicalConstants;
import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "hourly_records", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"clinical_day_id", "record_hour"})
})
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HourlyRecord extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "clinical_day_id", nullable = false)
    ClinicalDay clinicalDay;

    @Column(name = "record_time", nullable = false)
    LocalDateTime recordTime;

    @Column(name = "record_hour", nullable = false)
    Integer recordHour;

    @Column(length = 50)
    String consciousness;

    @DecimalMin(ClinicalConstants.TEMPERATURE_MIN_STR) @DecimalMax(ClinicalConstants.TEMPERATURE_MAX_STR)
    Double temperature;

    @Min(ClinicalConstants.HEART_RATE_MIN) @Max(ClinicalConstants.HEART_RATE_MAX)
    @Column(name = "heart_rate")
    Integer heartRate;

    @Min(ClinicalConstants.RESPIRATORY_RATE_MIN) @Max(ClinicalConstants.RESPIRATORY_RATE_MAX)
    @Column(name = "respiratory_rate")
    Integer respiratoryRate;

    @Min(ClinicalConstants.SYSTOLIC_BP_MIN) @Max(ClinicalConstants.SYSTOLIC_BP_MAX)
    @Column(name = "systolic_bp")
    Integer systolicBP;

    @Min(ClinicalConstants.DIASTOLIC_BP_MIN) @Max(ClinicalConstants.DIASTOLIC_BP_MAX)
    @Column(name = "diastolic_bp")
    Integer diastolicBP;

    @Column(name = "mean_arterial_pressure")
    Integer meanArterialPressure;

    @DecimalMin(ClinicalConstants.SPO2_MIN_STR) @DecimalMax(ClinicalConstants.SPO2_MAX_STR)
    @Column(name = "spo2")
    Double spo2;

    @DecimalMin(ClinicalConstants.GLUCOSE_MIN_STR) @DecimalMax(ClinicalConstants.GLUCOSE_MAX_STR)
    Double glucose;

    @Column(name = "etco2")
    Double etco2;

    @Column(name = "fio2")
    Double fio2;

    @Column(name = "cvp")
    Double cvp;

    @Column(name = "urine_output")
    Double urineOutput;

    @Column(name = "drain_output")
    Double drainOutput;

    @Column(columnDefinition = "TEXT")
    String stool;

    @Column(columnDefinition = "TEXT")
    String vomit;

    @Column(name = "pain_score")
    Integer painScore;

    @Column(columnDefinition = "TEXT")
    String notes;

    @PrePersist
    @PreUpdate
    public void validateClinicalRanges() {
        if (recordTime != null) {
            this.recordHour = recordTime.getHour();
        }
        if (heartRate != null && (heartRate < ClinicalConstants.HEART_RATE_MIN || heartRate > ClinicalConstants.HEART_RATE_MAX))
            throw new IllegalArgumentException("Heart rate must be between " + ClinicalConstants.HEART_RATE_MIN + " and " + ClinicalConstants.HEART_RATE_MAX + " bpm");
        if (systolicBP != null && (systolicBP < ClinicalConstants.SYSTOLIC_BP_MIN || systolicBP > ClinicalConstants.SYSTOLIC_BP_MAX))
            throw new IllegalArgumentException("Systolic BP must be between " + ClinicalConstants.SYSTOLIC_BP_MIN + " and " + ClinicalConstants.SYSTOLIC_BP_MAX + " mmHg");
        if (diastolicBP != null && (diastolicBP < ClinicalConstants.DIASTOLIC_BP_MIN || diastolicBP > ClinicalConstants.DIASTOLIC_BP_MAX))
            throw new IllegalArgumentException("Diastolic BP must be between " + ClinicalConstants.DIASTOLIC_BP_MIN + " and " + ClinicalConstants.DIASTOLIC_BP_MAX + " mmHg");
        if (temperature != null && (temperature < ClinicalConstants.TEMPERATURE_MIN || temperature > ClinicalConstants.TEMPERATURE_MAX))
            throw new IllegalArgumentException("Temperature must be between " + ClinicalConstants.TEMPERATURE_MIN + " and " + ClinicalConstants.TEMPERATURE_MAX + " °C");
        if (spo2 != null && (spo2 < ClinicalConstants.SPO2_MIN || spo2 > ClinicalConstants.SPO2_MAX))
            throw new IllegalArgumentException("Oxygen saturation must be between " + ClinicalConstants.SPO2_MIN + " and " + ClinicalConstants.SPO2_MAX + "%");
        if (respiratoryRate != null && (respiratoryRate < ClinicalConstants.RESPIRATORY_RATE_MIN || respiratoryRate > ClinicalConstants.RESPIRATORY_RATE_MAX))
            throw new IllegalArgumentException("Respiratory rate must be between " + ClinicalConstants.RESPIRATORY_RATE_MIN + " and " + ClinicalConstants.RESPIRATORY_RATE_MAX + " breaths/min");
        if (glucose != null && (glucose < ClinicalConstants.GLUCOSE_MIN || glucose > ClinicalConstants.GLUCOSE_MAX))
            throw new IllegalArgumentException("Glucose must be between " + ClinicalConstants.GLUCOSE_MIN + " and " + ClinicalConstants.GLUCOSE_MAX + " mmol/L");
    }

}
