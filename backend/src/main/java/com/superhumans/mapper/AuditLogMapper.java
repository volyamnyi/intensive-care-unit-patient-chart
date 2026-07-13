package com.superhumans.mapper;

import com.superhumans.dto.AuditLogResponse;
import com.superhumans.entity.AuditLog;

public class AuditLogMapper {

    public static AuditLogResponse toResponse(AuditLog entity) {
        return AuditLogResponse.builder()
                .id(entity.getId())
                .timestamp(entity.getTimestamp())
                .userId(entity.getUserId())
                .entity(entity.getEntity())
                .entityId(entity.getEntityId())
                .action(entity.getAction())
                .oldValue(entity.getOldValue())
                .newValue(entity.getNewValue())
                .correlationId(entity.getCorrelationId())
                .build();
    }
}
