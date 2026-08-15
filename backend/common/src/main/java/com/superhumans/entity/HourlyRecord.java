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
import com.superhumans.entity.base.BaseEntity;

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

    @Min(ClinicalConstants.GCS_MIN) @Max(ClinicalConstants.GCS_MAX)
    @Column(name = "gcs")
    Integer gcs;

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

    @DecimalMin(ClinicalConstants.ETCO2_MIN_STR) @DecimalMax(ClinicalConstants.ETCO2_MAX_STR)
    @Column(name = "etco2")
    Double etco2;

    @DecimalMin(ClinicalConstants.FIO2_MIN_STR) @DecimalMax(ClinicalConstants.FIO2_MAX_STR)
    @Column(name = "fio2")
    Double fio2;

    @DecimalMin(ClinicalConstants.CVP_MIN_STR) @DecimalMax(ClinicalConstants.CVP_MAX_STR)
    @Column(name = "cvp")
    Double cvp;

    @DecimalMin(ClinicalConstants.VASOPRESSOR_MIN_STR) @DecimalMax(ClinicalConstants.VASOPRESSOR_MAX_STR)
    @Column(name = "dopamine")
    Double dopamine;

    @DecimalMin(ClinicalConstants.VASOPRESSOR_MIN_STR) @DecimalMax(ClinicalConstants.VASOPRESSOR_MAX_STR)
    @Column(name = "dobutamine")
    Double dobutamine;

    @DecimalMin(ClinicalConstants.VASOPRESSOR_MIN_STR) @DecimalMax(ClinicalConstants.VASOPRESSOR_MAX_STR)
    @Column(name = "norepinephrine")
    Double norepinephrine;

    @DecimalMin(ClinicalConstants.VASOPRESSOR_MIN_STR) @DecimalMax(ClinicalConstants.VASOPRESSOR_MAX_STR)
    @Column(name = "epinephrine")
    Double epinephrine;

    @DecimalMin(ClinicalConstants.URINE_OUTPUT_MIN_STR)
    @Column(name = "urine_output")
    Double urineOutput;

    @DecimalMin(ClinicalConstants.DRAIN_OUTPUT_MIN_STR)
    @Column(name = "drain_output")
    Double drainOutput;

    @DecimalMin(ClinicalConstants.DRAIN_OUTPUT_MIN_STR)
    @Column(name = "gastric_output")
    Double gastricOutput;

    @Column(columnDefinition = "TEXT")
    String stool;

    @Column(columnDefinition = "TEXT")
    String vomit;

    @Column(name = "bed_position", length = 100)
    String bedPosition;

    @Column(name = "head_end", length = 100)
    String headEnd;

    @Min(ClinicalConstants.PAIN_SCORE_MIN) @Max(ClinicalConstants.PAIN_SCORE_MAX)
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
        if (urineOutput != null && urineOutput < ClinicalConstants.URINE_OUTPUT_MIN)
            throw new IllegalArgumentException("Urine output must be at least " + ClinicalConstants.URINE_OUTPUT_MIN);
        if (drainOutput != null && drainOutput < ClinicalConstants.DRAIN_OUTPUT_MIN)
            throw new IllegalArgumentException("Drain output must be at least " + ClinicalConstants.DRAIN_OUTPUT_MIN);
        if (gastricOutput != null && gastricOutput < ClinicalConstants.DRAIN_OUTPUT_MIN)
            throw new IllegalArgumentException("Gastric output must be at least " + ClinicalConstants.DRAIN_OUTPUT_MIN);
        if (painScore != null && (painScore < ClinicalConstants.PAIN_SCORE_MIN || painScore > ClinicalConstants.PAIN_SCORE_MAX))
            throw new IllegalArgumentException("Pain score must be between " + ClinicalConstants.PAIN_SCORE_MIN + " and " + ClinicalConstants.PAIN_SCORE_MAX);
        if (etco2 != null && (etco2 < ClinicalConstants.ETCO2_MIN || etco2 > ClinicalConstants.ETCO2_MAX))
            throw new IllegalArgumentException("ETCO2 must be between " + ClinicalConstants.ETCO2_MIN + " and " + ClinicalConstants.ETCO2_MAX + " mmHg");
        if (fio2 != null && (fio2 < ClinicalConstants.FIO2_MIN || fio2 > ClinicalConstants.FIO2_MAX))
            throw new IllegalArgumentException("FiO2 must be between " + ClinicalConstants.FIO2_MIN + " and " + ClinicalConstants.FIO2_MAX + "%");
        if (cvp != null && (cvp < ClinicalConstants.CVP_MIN || cvp > ClinicalConstants.CVP_MAX))
            throw new IllegalArgumentException("CVP must be between " + ClinicalConstants.CVP_MIN + " and " + ClinicalConstants.CVP_MAX + " mmHg");
        if (gcs != null && (gcs < ClinicalConstants.GCS_MIN || gcs > ClinicalConstants.GCS_MAX))
            throw new IllegalArgumentException("GCS must be between " + ClinicalConstants.GCS_MIN + " and " + ClinicalConstants.GCS_MAX);
        if (dopamine != null && (dopamine < ClinicalConstants.VASOPRESSOR_MIN || dopamine > ClinicalConstants.VASOPRESSOR_MAX))
            throw new IllegalArgumentException("Dopamine must be between " + ClinicalConstants.VASOPRESSOR_MIN + " and " + ClinicalConstants.VASOPRESSOR_MAX + " мкг/кг/хв");
        if (dobutamine != null && (dobutamine < ClinicalConstants.VASOPRESSOR_MIN || dobutamine > ClinicalConstants.VASOPRESSOR_MAX))
            throw new IllegalArgumentException("Dobutamine must be between " + ClinicalConstants.VASOPRESSOR_MIN + " and " + ClinicalConstants.VASOPRESSOR_MAX + " мкг/кг/хв");
        if (norepinephrine != null && (norepinephrine < ClinicalConstants.VASOPRESSOR_MIN || norepinephrine > ClinicalConstants.VASOPRESSOR_MAX))
            throw new IllegalArgumentException("Norepinephrine must be between " + ClinicalConstants.VASOPRESSOR_MIN + " and " + ClinicalConstants.VASOPRESSOR_MAX + " мкг/кг/хв");
        if (epinephrine != null && (epinephrine < ClinicalConstants.VASOPRESSOR_MIN || epinephrine > ClinicalConstants.VASOPRESSOR_MAX))
            throw new IllegalArgumentException("Epinephrine must be between " + ClinicalConstants.VASOPRESSOR_MIN + " and " + ClinicalConstants.VASOPRESSOR_MAX + " мкг/кг/хв");
    }

}
