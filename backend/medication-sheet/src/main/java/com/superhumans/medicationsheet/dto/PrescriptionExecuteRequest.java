package com.superhumans.medicationsheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionExecuteRequest {
    @NotBlank @Size(max = 100) String actualDose;
    @NotBlank @Size(max = 50) String secondPersonLogin;
    @NotBlank @Size(max = 100) String secondPersonPassword;
}
