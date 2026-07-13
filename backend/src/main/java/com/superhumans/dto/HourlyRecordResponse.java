package com.superhumans.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HourlyRecordResponse {
    private UUID id;
    private UUID clinicalDayId;
    private LocalDateTime recordTime;
    private String consciousness;
    private Double temperature;
    private Integer heartRate;
    private Integer respiratoryRate;
    private Integer systolicBP;
    private Integer diastolicBP;
    private Integer meanArterialPressure;
    private Double spo2;
    private Double etco2;
    private Double fio2;
    private Double cvp;
    private Double urineOutput;
    private Double drainOutput;
    private String stool;
    private String vomit;
    private Integer painScore;
    private String notes;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private UUID updatedBy;
    private LocalDateTime updatedAt;
    private Integer version;
}
