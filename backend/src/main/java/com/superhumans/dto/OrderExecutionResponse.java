package com.superhumans.dto;

import com.superhumans.entity.OrderExecutionStatus;
import lombok.*;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
@FieldDefaults(level = AccessLevel.PRIVATE)
public class OrderExecutionResponse {
    UUID id;
    UUID orderId;
    UUID executedBy;
    LocalDateTime executedAt;
    String actualDose;
    OrderExecutionStatus status;
    String comment;
    UUID createdBy;
    LocalDateTime createdAt;
    UUID updatedBy;
    LocalDateTime updatedAt;
    Integer version;
}
