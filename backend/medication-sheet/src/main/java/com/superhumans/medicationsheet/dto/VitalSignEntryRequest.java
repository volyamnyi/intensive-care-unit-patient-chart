package com.superhumans.medicationsheet.dto;

import com.superhumans.util.ClinicalConstants;
import jakarta.validation.constraints.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

/**
 * Create request for a vital-sign entry (#406): {@code dayId} + {@code period}
 * are the natural key — the create path is idempotent (writes the existing
 * slot in place) instead of scanning for the first empty slot, which let a
 * double-submit duplicate the same vitals into the next period.
 */
@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VitalSignEntryRequest {
    @NotBlank String dayId;
    @NotBlank String period;
    // Ranges from ClinicalConstants (#427 item 7) — same source as
    // HourlyRecord, so patient-safety-adjacent bounds cannot drift.
    @DecimalMin(ClinicalConstants.TEMPERATURE_MIN_STR) @DecimalMax(ClinicalConstants.TEMPERATURE_MAX_STR)
    Double temperature;
    @Min(ClinicalConstants.SYSTOLIC_BP_MIN) @Max(ClinicalConstants.SYSTOLIC_BP_MAX)
    Integer systolicBp;
    @Min(ClinicalConstants.DIASTOLIC_BP_MIN) @Max(ClinicalConstants.DIASTOLIC_BP_MAX)
    Integer diastolicBp;
    @DecimalMin(ClinicalConstants.SPO2_MIN_STR) @DecimalMax(ClinicalConstants.SPO2_MAX_STR)
    Integer spo2;
    @Min(ClinicalConstants.HEART_RATE_MIN) @Max(ClinicalConstants.HEART_RATE_MAX)
    Integer pulse;
    String stool;
    @Min(ClinicalConstants.PAIN_SCORE_MIN) @Max(ClinicalConstants.PAIN_SCORE_MAX)
    Integer painScore;
}
