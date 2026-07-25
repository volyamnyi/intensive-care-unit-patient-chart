package com.superhumans.dto;

import lombok.*;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class VitalSignEntryResponse {
    String id;
    String dayId;
    String period;
    Double temperature;
    Integer systolicBp;
    Integer diastolicBp;
    Integer spo2;
    Integer pulse;
    String stool;
    Integer painScore;
}
