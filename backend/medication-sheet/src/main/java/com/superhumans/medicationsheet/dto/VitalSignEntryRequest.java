package com.superhumans.medicationsheet.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VitalSignEntryRequest {
    String prescriptionListId;
    Double temperature;
    Integer systolicBp;
    Integer diastolicBp;
    Integer spo2;
    Integer pulse;
    String stool;
    @Min(0) @Max(10) Integer painScore;
}
