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
}
