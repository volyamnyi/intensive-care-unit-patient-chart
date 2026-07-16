package com.superhumans.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HourlyRecordResponse {
    UUID id;
    UUID clinicalDayId;
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
    UUID createdBy;
    LocalDateTime createdAt;
    UUID updatedBy;
    LocalDateTime updatedAt;
    Integer version;
}
