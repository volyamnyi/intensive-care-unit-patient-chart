package com.superhumans.dto;

import com.superhumans.entity.EpisodeStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EpisodeResponse {
    private UUID id;
    private UUID patientId;
    private String patientName;
    private UUID hospitalizationId;
    private UUID departmentId;
    private LocalDateTime admissionDate;
    private LocalDateTime dischargeDate;
    private EpisodeStatus status;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private UUID updatedBy;
    private LocalDateTime updatedAt;
    private Integer version;
}
