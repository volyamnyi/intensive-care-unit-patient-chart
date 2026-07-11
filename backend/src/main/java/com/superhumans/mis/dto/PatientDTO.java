package com.superhumans.mis.dto;

import lombok.*;
import java.time.LocalDate;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class PatientDTO {
    private Integer patientID;
    private String patientName;
    private LocalDate patientBirthDate;
    private String patientSexCode;
    private String patientAddress;
    private String patientPhone;
    private String patientEmail;
    private String patientExternalID1;
    private String patientExternalID2;
    private Integer patientHeight;
    private Integer patientWeight;
    private String bloodGroup;
    private String rhFactor;
}
