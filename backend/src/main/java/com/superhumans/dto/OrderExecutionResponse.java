package com.superhumans.dto;

import com.superhumans.entity.OrderExecutionStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class OrderExecutionResponse {
    private UUID id;
    private UUID orderId;
    private UUID executedBy;
    private LocalDateTime executedAt;
    private String actualDose;
    private OrderExecutionStatus status;
    private String comment;
    private UUID createdBy;
    private LocalDateTime createdAt;
    private UUID updatedBy;
    private LocalDateTime updatedAt;
    private Integer version;
}
