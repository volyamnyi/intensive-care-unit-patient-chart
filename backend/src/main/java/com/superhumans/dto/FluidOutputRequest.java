package com.superhumans.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class FluidOutputRequest {
    private String type;
    private Integer volume;
    private Boolean isPresent;
}
