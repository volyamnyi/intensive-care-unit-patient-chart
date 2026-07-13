package com.superhumans.dto;

import com.superhumans.entity.MedicalOrderStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class MedicalOrderResponse {
    private UUID id;
    private UUID clinicalDayId;
    private String category;
    private String drugName;
    private String dose;
    private String unit;
    private String route;
    private String frequency;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private MedicalOrderStatus status;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private UUID updatedBy;
    private LocalDateTime updatedAt;
    private Integer version;
}
