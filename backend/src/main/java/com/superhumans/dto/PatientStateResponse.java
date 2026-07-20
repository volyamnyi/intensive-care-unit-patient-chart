package com.superhumans.dto;

import lombok.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PatientStateResponse {
    UUID id;
    UUID clinicalDayId;
    Integer recordHour;
    String consciousness;
    String skin;
    String edema;
    String mucousMembranes;
    String peripheralCirculation;
    String bowelSounds;
    String generalCondition;
    String additionalNotes;
    Integer version;
}
