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
    Integer gcs;
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
    Double dopamine;
    Double dobutamine;
    Double norepinephrine;
    Double epinephrine;
    Double urineOutput;
    Double drainOutput;
    String stool;
    String vomit;
    Integer painScore;
    String notes;
    Long createdBy;
    LocalDateTime createdAt;
    Long updatedBy;
    LocalDateTime updatedAt;
    Integer version;
}
