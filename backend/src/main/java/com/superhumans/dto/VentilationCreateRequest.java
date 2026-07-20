package com.superhumans.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VentilationCreateRequest {
    @NotNull
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
}
