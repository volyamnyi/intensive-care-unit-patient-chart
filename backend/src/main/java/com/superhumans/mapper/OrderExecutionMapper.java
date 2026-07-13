package com.superhumans.mapper;

import com.superhumans.dto.OrderExecutionCreateRequest;
import com.superhumans.dto.OrderExecutionResponse;
import com.superhumans.entity.OrderExecution;
import com.superhumans.entity.OrderExecutionStatus;

public class OrderExecutionMapper {

    public static OrderExecutionResponse toResponse(OrderExecution entity) {
        return OrderExecutionResponse.builder()
                .id(entity.getId())
                .orderId(entity.getOrder().getId())
                .executedBy(entity.getExecutedBy())
                .executedAt(entity.getExecutedAt())
                .actualDose(entity.getActualDose())
                .status(entity.getStatus())
                .comment(entity.getComment())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }

    public static OrderExecution toEntity(OrderExecutionCreateRequest request) {
        return OrderExecution.builder()
                .executedBy(request.getExecutedBy())
                .executedAt(request.getExecutedAt())
                .actualDose(request.getActualDose())
                .status(OrderExecutionStatus.COMPLETED)
                .comment(request.getComment())
                .build();
    }
}
