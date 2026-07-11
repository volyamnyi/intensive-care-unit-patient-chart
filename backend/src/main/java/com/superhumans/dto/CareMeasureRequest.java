package com.superhumans.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class CareMeasureRequest {
    private Integer hour;
    private String procedure;
    private Boolean performed;
}
