package com.superhumans.medicationsheet.dto;

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
    @DecimalMin("34.0") @DecimalMax("42.0") Double temperature;
    @Min(50) @Max(250) Integer systolicBp;
    @Min(30) @Max(150) Integer diastolicBp;
    @Min(50) @Max(100) Integer spo2;
    @Min(0) @Max(300) Integer pulse;
    String stool;
    @Min(0) @Max(10) Integer painScore;
}
