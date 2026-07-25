package com.superhumans.dto;

import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HourlyRecordPatchRequest {
    String consciousness;
    @DecimalMin("34.0") @DecimalMax("42.0")
    Double temperature;
    @Min(0) @Max(300)
    Integer heartRate;
    @Min(0) @Max(60)
    Integer respiratoryRate;
    @Min(50) @Max(250)
    Integer systolicBP;
    @Min(30) @Max(150)
    Integer diastolicBP;
    Integer meanArterialPressure;
    @DecimalMin("50.0") @DecimalMax("100.0")
    Double spo2;
    @DecimalMin("0.0") @DecimalMax("100.0")
    Double etco2;
    @DecimalMin("0.0") @DecimalMax("1.0")
    Double fio2;
    @DecimalMin("0.0") @DecimalMax("30.0")
    Double cvp;
    @DecimalMin("0.0")
    Double urineOutput;
    @DecimalMin("0.0")
    Double drainOutput;
    String stool;
    String vomit;
    @Min(0) @Max(10)
    Integer painScore;
    String notes;
    @NotNull
    Integer version;
}
