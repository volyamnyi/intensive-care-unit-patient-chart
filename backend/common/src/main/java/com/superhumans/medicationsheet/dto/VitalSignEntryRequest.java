package com.superhumans.medicationsheet.dto;

import jakarta.validation.constraints.*;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VitalSignEntryRequest {
    @NotBlank String prescriptionListId;
    @DecimalMin("34.0") @DecimalMax("42.0") Double temperature;
    @Min(50) @Max(250) Integer systolicBp;
    @Min(30) @Max(150) Integer diastolicBp;
    @Min(50) @Max(100) Integer spo2;
    @Min(0) @Max(300) Integer pulse;
    String stool;
    @Min(0) @Max(10) Integer painScore;
}
