package com.superhumans.dto;

import com.superhumans.icu.entity.OrderExecutionStatus;
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
    Integer hour;
    boolean planned;
    Long plannedBy;
    LocalDateTime plannedAt;
    String plannedDose;
    boolean plannedFinished;
    boolean completedFinished;
    Long executedBy;
    LocalDateTime executedAt;
    String actualDose;
    OrderExecutionStatus status;
    String comment;
    Long createdBy;
    LocalDateTime createdAt;
    Long updatedBy;
    LocalDateTime updatedAt;
    Integer version;
}
