package com.superhumans.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FluidIntakeRequest {
    private String medicationName;
    private Integer volumeOrdered;
    private Integer volumeActual;
    private Long prescriptionId;
}
