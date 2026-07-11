package com.superhumans.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class VitalSignsRequest {
    private Integer systolicBp;
    private Integer diastolicBp;
    private Integer heartRate;
    private Integer spo2;
    private Double temperature;
    private Integer cvp;
    private Integer respiratoryRate;
    private String ventilatorMode;
    private Integer tidalVolume;
    private Integer minuteVentilation;
    private Integer peep;
    private Integer fio2;
    private Integer ventFrequency;
}
