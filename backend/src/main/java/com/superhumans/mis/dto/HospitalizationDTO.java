package com.superhumans.mis.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class HospitalizationDTO {
    UUID id;
    Long patientId;
    UUID departmentId;
    LocalDateTime admissionDate;
    String diagnosis;
    String departmentName;
    String room;
    String bed;
}
