package com.superhumans.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PatientStatePatchRequest {
    @Size(max = 30)
    String consciousness;
    @Size(max = 30)
    String skin;
    @Size(max = 20)
    String edema;
    @Size(max = 30)
    String mucousMembranes;
    @Size(max = 30)
    String peripheralCirculation;
    @Size(max = 30)
    String bowelSounds;
    @Size(max = 30)
    String generalCondition;
    @Size(max = 500)
    String additionalNotes;

    @NotNull
    Integer version;
}
