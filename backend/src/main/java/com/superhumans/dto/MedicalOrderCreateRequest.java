package com.superhumans.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class MedicalOrderCreateRequest {
    @NotBlank
    private String category;
    @NotBlank
    private String drugName;
    @NotBlank
    private String dose;
    @NotBlank
    private String unit;
    @NotBlank
    private String route;
    @NotBlank
    private String frequency;
    @NotNull
    private LocalDateTime startTime;
    private LocalDateTime endTime;
}
