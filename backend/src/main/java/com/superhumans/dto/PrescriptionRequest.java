package com.superhumans.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class PrescriptionRequest {
    private String medication;
    private String dose;
    private String route;
    private String frequency;
    private Integer startHour;
    private Integer endHour;
}
