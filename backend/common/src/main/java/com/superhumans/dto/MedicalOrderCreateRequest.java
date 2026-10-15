package com.superhumans.dto;

import jakarta.validation.constraints.Size;
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
    @Size(max = 50)
    String category;
    @NotBlank
    @Size(max = 200)
    String drugName;
    @NotBlank
    @Size(max = 100)
    String dose;
    @NotBlank
    @Size(max = 50)
    String unit;
    @NotBlank
    @Size(max = 50)
    String route;
    @NotBlank
    @Size(max = 100)
    String frequency;
    @NotNull
    LocalDateTime startTime;
    LocalDateTime endTime;
}
