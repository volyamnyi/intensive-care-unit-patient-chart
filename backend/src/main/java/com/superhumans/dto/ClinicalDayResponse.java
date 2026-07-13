package com.superhumans.dto;

import com.superhumans.entity.ClinicalDayStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ClinicalDayResponse {
    private UUID id;
    private UUID episodeId;
    private Integer dayNumber;
    private LocalDateTime startDateTime;
    private LocalDateTime endDateTime;
    private ClinicalDayStatus status;
    private Boolean doctorSigned;
    private Boolean nurseSigned;
    private LocalDateTime closedAt;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private UUID updatedBy;
    private LocalDateTime updatedAt;
    private Integer version;
}
