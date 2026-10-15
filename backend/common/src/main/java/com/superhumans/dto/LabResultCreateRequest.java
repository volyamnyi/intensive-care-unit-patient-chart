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
public class LabResultCreateRequest {
    @NotBlank
    @Size(max = 20)
    String testCode;

    @NotBlank
    @Size(max = 100)
    String testName;

    @NotBlank
    @Size(max = 50)
    String result;

    @Size(max = 30)
    String unit;

    Double referenceMin;

    Double referenceMax;

    @NotNull
    LocalDateTime measuredAt;
}
