package com.superhumans.medicationsheet.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionExecuteRequest {
    @NotBlank @Size(max = 100) String actualDose;
    boolean requires2pAuth;
    UUID secondPersonId;
}
