package com.superhumans.medicationsheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionListCreateRequest {
    @NotBlank @Size(min = 1, max = 20)
    @Pattern(regexp = "\\d+",
            message = "ID пацієнта має містити лише цифри, без символів та пробілів")
    String patientId;
}
