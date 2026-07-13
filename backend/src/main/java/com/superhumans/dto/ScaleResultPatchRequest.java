package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ScaleResultPatchRequest {
    private String result;
    @NotNull
    private Integer version;
}
