package com.superhumans.dto;

import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotNull;
import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VentilationCreateRequest {
    @NotNull
    Integer recordHour;

    @Size(max = 30)
    String mode;

    Double fio2;
    Double peep;
    Double tidalVolume;
    Double minuteVolume;
    Double pinsp;
    Double psupport;
    @Size(max = 50)
    String triggerType;
    @Size(max = 10)
    String ieRatio;
    Integer respiratoryRate;
    Double plateauPressure;
    Double meanAirwayPressure;
}
