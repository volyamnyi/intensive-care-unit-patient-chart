package com.superhumans.mapper;

import com.superhumans.dto.AuditLogResponse;
import com.superhumans.entity.AuditLog;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface AuditLogMapper {

    AuditLogResponse toResponse(AuditLog entity);
}
