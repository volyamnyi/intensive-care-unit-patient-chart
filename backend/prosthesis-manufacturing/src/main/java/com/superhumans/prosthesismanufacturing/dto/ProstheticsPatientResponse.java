package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProstheticsPatientResponse {
    UUID id;
    String pib;
    LocalDate birthDate;
    String gender;
    Integer heightCm;
    Integer weightKg;
    String socialStatus;
    String cause;
    LocalDate amputationDate;
    String affectedLimb;
    String amputationLevel;
    String stump;
}
