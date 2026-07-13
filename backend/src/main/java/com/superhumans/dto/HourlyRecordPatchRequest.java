package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class HourlyRecordPatchRequest {
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
    @NotNull
    private Integer version;
}
