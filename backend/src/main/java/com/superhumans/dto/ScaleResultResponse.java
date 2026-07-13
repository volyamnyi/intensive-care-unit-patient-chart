package com.superhumans.dto;

import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ScaleResultResponse {
    private UUID id;
    private UUID clinicalDayId;
    private UUID scaleId;
    private String scaleName;
    private String result;
    private LocalDateTime calculatedAt;
    private UUID calculatedBy;
    private LocalDateTime createdAt;
    private Integer version;
}
