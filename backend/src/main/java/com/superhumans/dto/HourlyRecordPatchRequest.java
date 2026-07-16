package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HourlyRecordPatchRequest {
    String consciousness;
    Double temperature;
    Integer heartRate;
    Integer respiratoryRate;
    Integer systolicBP;
    Integer diastolicBP;
    Integer meanArterialPressure;
    Double spo2;
    Double etco2;
    Double fio2;
    Double cvp;
    Double urineOutput;
    Double drainOutput;
    String stool;
    String vomit;
    Integer painScore;
    String notes;
    @NotNull
    Integer version;
}
