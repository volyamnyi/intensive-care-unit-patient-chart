package com.superhumans.medicationsheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionItemAddRequest {
    @NotBlank @Size(max = 500) String medicineName;
    @Size(max = 255) String medicineMethod;
    @Size(max = 255) String regime;
}
