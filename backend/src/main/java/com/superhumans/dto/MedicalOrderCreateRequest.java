package com.superhumans.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalOrderCreateRequest {
    @NotBlank
    String category;
    @NotBlank
    String drugName;
    @NotBlank
    String dose;
    @NotBlank
    String unit;
    @NotBlank
    String route;
    @NotBlank
    String frequency;
    @NotNull
    LocalDateTime startTime;
    LocalDateTime endTime;
}
