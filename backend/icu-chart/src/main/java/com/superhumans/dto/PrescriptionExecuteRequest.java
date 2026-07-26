package com.superhumans.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PrescriptionExecuteRequest {
    @NotBlank String actualDose;
    boolean requires2pAuth;
    UUID secondPersonId;
}
