package com.superhumans.dto;

import lombok.*;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VentilationResponse {
    UUID id;
    UUID clinicalDayId;
    Integer recordHour;
    String mode;
    Double fio2;
    Double peep;
    Double tidalVolume;
    Double minuteVolume;
    Double pinsp;
    Double psupport;
    String triggerType;
    String ieRatio;
    Integer respiratoryRate;
    Double plateauPressure;
    Double meanAirwayPressure;
    Integer version;
}
