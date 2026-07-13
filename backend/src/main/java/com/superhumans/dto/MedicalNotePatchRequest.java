package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MedicalNotePatchRequest {
    private String text;
    @NotNull
    private Integer version;
}
