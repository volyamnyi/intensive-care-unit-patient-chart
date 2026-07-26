package com.superhumans.medicationsheet.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionItemAddRequest {
    @NotBlank String medicineName;
    String medicineMethod;
    String regime;
}
