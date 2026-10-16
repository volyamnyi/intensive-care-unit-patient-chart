package com.superhumans.medicationsheet.entity;

import com.superhumans.entity.base.BaseEntity;
import com.superhumans.util.ClinicalConstants;

import jakarta.persistence.*;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Entity
@Table(name = "vital_sign_entries")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VitalSignEntry extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "day_id", nullable = false)
    VitalSignDay day;

    @Column(nullable = false, length = 8)
    String period;

    @DecimalMin(ClinicalConstants.TEMPERATURE_MIN_STR)
    @DecimalMax(ClinicalConstants.TEMPERATURE_MAX_STR)
    Double temperature;

    // Clinical ranges mirror VitalSignEntryRequest (#390, HourlyRecord
    // two-layer pattern: DTO range + entity range) from ClinicalConstants
    // (#427 item 7).
    @Min(ClinicalConstants.SYSTOLIC_BP_MIN) @Max(ClinicalConstants.SYSTOLIC_BP_MAX)
    @Column(name = "systolic_bp")
    Integer systolicBp;

    @Min(ClinicalConstants.DIASTOLIC_BP_MIN) @Max(ClinicalConstants.DIASTOLIC_BP_MAX)
    @Column(name = "diastolic_bp")
    Integer diastolicBp;

    @DecimalMin(ClinicalConstants.SPO2_MIN_STR) @DecimalMax(ClinicalConstants.SPO2_MAX_STR)
    Integer spo2;

    @Min(ClinicalConstants.HEART_RATE_MIN) @Max(ClinicalConstants.HEART_RATE_MAX)
    Integer pulse;

    @Column(length = 50)
    String stool;

    @Min(ClinicalConstants.PAIN_SCORE_MIN) @Max(ClinicalConstants.PAIN_SCORE_MAX)
    @Column(name = "pain_score")
    Integer painScore;
}
