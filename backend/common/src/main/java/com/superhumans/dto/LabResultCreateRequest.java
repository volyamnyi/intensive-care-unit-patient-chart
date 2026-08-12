package com.superhumans.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import java.time.LocalDateTime;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LabResultCreateRequest {
    @NotBlank
    String testCode;

    @NotBlank
    String testName;

    @NotBlank
    String result;

    String unit;

    Double referenceMin;

    Double referenceMax;

    @NotNull
    LocalDateTime measuredAt;
}
