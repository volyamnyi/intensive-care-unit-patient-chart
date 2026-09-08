package com.superhumans.prosthesismanufacturing.dto;

import lombok.*;
import lombok.experimental.FieldDefaults;
import java.time.LocalDate;

@Getter @Setter @Builder @NoArgsConstructor @AllArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProstheticsPatientResponse {
    String id;
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
    String amputationSite;
    String phone;
    String email;
    String residence;
    String healthStatus;
    String clinicalState;
    String stump;
    /**
     * MIS department id (single source: {@code PatientDTO.departmentId}).
     * Drives prosthetics eligibility (departments 19/27/37, #259); null when
     * the response is built from the local registry only.
     */
    Long departmentId;
}
