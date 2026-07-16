package com.superhumans.dto;

import com.superhumans.entity.MedicalOrderStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class MedicalOrderResponse {
    UUID id;
    UUID clinicalDayId;
    String category;
    String drugName;
    String dose;
    String unit;
    String route;
    String frequency;
    LocalDateTime startTime;
    LocalDateTime endTime;
    MedicalOrderStatus status;
    UUID createdBy;
    LocalDateTime createdAt;
    UUID updatedBy;
    LocalDateTime updatedAt;
    Integer version;
}
