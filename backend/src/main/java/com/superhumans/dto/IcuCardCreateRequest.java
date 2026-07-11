package com.superhumans.dto;

import lombok.*;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor
public class IcuCardCreateRequest {
    private Long patientId;
    private String patientName;
    private String medicalCardNumber;
    private String diagnosis;
    private Integer apacheIi;
    private Integer sofa;
    private Integer patientHeight;
    private Integer patientWeight;
    private String bloodGroup;
    private String rhFactor;
    private String patientSexCode;
    private String patientBirthDate;
}
