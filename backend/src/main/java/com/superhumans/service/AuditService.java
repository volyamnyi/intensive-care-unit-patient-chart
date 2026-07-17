package com.superhumans.service;

import com.superhumans.dto.AuditLogResponse;
import com.superhumans.entity.AuditLog;
import com.superhumans.exception.NotFoundException;
import com.superhumans.mapper.AuditLogMapper;
import com.superhumans.repository.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditService {

    AuditLogRepository auditLogRepository;
    AuditLogMapper auditLogMapper;

    public AuditLogResponse getAuditLog(UUID id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Audit log not found: " + id));
        return auditLogMapper.toResponse(log);
    }

    public Page<AuditLogResponse> getAuditLogs(Long userId, String entity, UUID entityId, String action,
                                                LocalDateTime dateFrom, LocalDateTime dateTo, Pageable pageable) {
        Page<AuditLog> logs;
        if (userId != null) {
            logs = auditLogRepository.findByUserIdOrderByTimestampDesc(userId, pageable);
        } else if (entity != null && entityId != null) {
            logs = auditLogRepository.findByEntityAndEntityIdOrderByTimestampDesc(entity, entityId, pageable);
        } else if (entity != null) {
            logs = auditLogRepository.findByEntityOrderByTimestampDesc(entity, pageable);
        } else if (action != null) {
            logs = auditLogRepository.findByActionOrderByTimestampDesc(action, pageable);
        } else if (dateFrom != null && dateTo != null) {
            logs = auditLogRepository.findByTimestampBetweenOrderByTimestampDesc(dateFrom, dateTo, pageable);
        } else {
            logs = auditLogRepository.findAllByOrderByTimestampDesc(pageable);
        }
        return logs.map(auditLogMapper::toResponse);
    }

    @Transactional
    public AuditLog logEvent(String entity, UUID entityId, String action, Long userId,
                             String oldValue, String newValue) {
        return logEvent(entity, entityId, action, userId, oldValue, newValue, null);
    }

    @Transactional
    public AuditLog logEvent(String entity, UUID entityId, String action, Long userId,
                             String oldValue, String newValue, String correlationId) {
        AuditLog log = AuditLog.builder()
                .entity(entity)
                .entityId(entityId)
                .action(action)
                .userId(userId)
                .oldValue(oldValue)
                .newValue(newValue)
                .correlationId(correlationId)
                .build();
        return auditLogRepository.save(log);
    }

    @Transactional
    public void logCreate(String entity, UUID entityId, Long userId) {
        logEvent(entity, entityId, "CREATE", userId, null, null);
    }

    @Transactional
    public void logUpdate(String entity, UUID entityId, Long userId, String oldValue, String newValue) {
        logEvent(entity, entityId, "UPDATE", userId, oldValue, newValue);
    }

    @Transactional
    public void logDelete(String entity, UUID entityId, Long userId) {
        logEvent(entity, entityId, "DELETE", userId, null, null);
    }

    @Transactional
    public void logAction(String entity, UUID entityId, String action, Long userId) {
        logEvent(entity, entityId, action, userId, null, null);
    }

    @Async
    public void logAsync(AuditLog auditLog) {
        auditLogRepository.save(auditLog);
    }
}
