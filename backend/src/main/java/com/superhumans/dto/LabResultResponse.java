package com.superhumans.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class LabResultResponse {
    UUID id;
    UUID clinicalDayId;
    String testCode;
    String testName;
    String result;
    String unit;
    Double referenceMin;
    Double referenceMax;
    Boolean isAbnormal;
    LocalDateTime measuredAt;
    LocalDateTime createdAt;
    Integer version;
}
