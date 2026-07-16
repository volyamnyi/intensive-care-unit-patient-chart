package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HourlyRecordCreateRequest {
    @NotNull
    LocalDateTime recordTime;
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
}
