package com.superhumans.mis.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class HospitalizationDTO {
    private UUID id;
    private UUID patientId;
    private UUID departmentId;
    private LocalDateTime admissionDate;
    private String diagnosis;
    private String departmentName;
    private String room;
    private String bed;
}
