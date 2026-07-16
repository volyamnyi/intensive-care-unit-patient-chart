package com.superhumans.mis.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class PatientDTO {
    UUID id;
    String fullName;
    LocalDate birthDate;
    String sexCode;
    String address;
    String phone;
    String email;
    String externalId1;
    String externalId2;
    Integer height;
    Integer weight;
    String bloodGroup;
    String rhFactor;
}
