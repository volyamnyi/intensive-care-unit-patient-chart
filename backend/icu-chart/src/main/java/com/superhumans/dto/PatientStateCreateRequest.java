package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PatientStateCreateRequest {
    @NotNull
    Integer recordHour;

    String consciousness;
    String skin;
    String edema;
    String mucousMembranes;
    String peripheralCirculation;
    String bowelSounds;
    String generalCondition;
    String additionalNotes;
}
