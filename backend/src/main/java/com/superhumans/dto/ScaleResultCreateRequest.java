package com.superhumans.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class ScaleResultCreateRequest {
    @NotNull
    private UUID scaleId;
    @NotBlank
    private String result;
}
