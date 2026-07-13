package com.superhumans.mis.dto;

import lombok.*;
import java.time.LocalDate;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PatientDTO {
    private UUID id;
    private String fullName;
    private LocalDate birthDate;
    private String sexCode;
    private String address;
    private String phone;
    private String email;
    private String externalId1;
    private String externalId2;
    private Integer height;
    private Integer weight;
    private String bloodGroup;
    private String rhFactor;
}
