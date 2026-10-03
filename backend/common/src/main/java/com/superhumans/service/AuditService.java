package com.superhumans.service;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventRecorder;
import com.superhumans.dto.AuditLogResponse;
import com.superhumans.entity.core.AuditLog;
import com.superhumans.exception.NotFoundException;
import com.superhumans.mapper.AuditLogMapper;
import com.superhumans.repository.core.AuditLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditService {

    AuditLogRepository auditLogRepository;
    AuditLogMapper auditLogMapper;
    AuditEventRecorder auditEventRecorder;

    /**
     * Legacy-table write gate (F8 cutover). While {@code true} (default) every
     * business operation dual-writes: the canonical v2 event plus the legacy
     * {@code audit_logs} row. Flipping it to {@code false} makes the legacy
     * table read-only — new history lives only in {@code audit_events} (plus
     * backfilled {@code audit_legacy_events}) — without a code change. Reads
     * are unaffected in both positions. No caller uses the returned entity.
     */
    @Value("${app.audit.legacy-write-enabled:true}")
    @NonFinal
    boolean legacyWriteEnabled = true;

    @Transactional
    public AuditLogResponse getAuditLog(UUID id) {
        AuditLog log = auditLogRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Audit log not found: " + id));
        AuditLogResponse response = auditLogMapper.toResponse(log);
        recordIfUser(AuditEvent.builder()
                .eventClass(EventClass.SECURITY)
                .module("platform")
                .functionalArea("audit")
                .action("platform.audit.detail.view")
                .actionType(ActionType.VIEW)
                .target(new AuditEvent.AuditTarget("AuditLog", id.toString(), null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.ADMIN_TOOL));
        return response;
    }

    @Transactional
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
        Page<AuditLogResponse> responses = logs.map(auditLogMapper::toResponse);
        final long resultTotal = responses.getTotalElements();
        recordIfUser(AuditEvent.builder()
                .eventClass(EventClass.SECURITY)
                .module("platform")
                .functionalArea("audit")
                .action("platform.audit.search")
                .actionType(ActionType.SEARCH)
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .affectedRecords(resultTotal > Integer.MAX_VALUE ? Integer.MAX_VALUE : (int) resultTotal)
                .source(AuditEvent.AuditSource.ADMIN_TOOL));
        return responses;
    }

    /**
     * Records a console self-audit event only for real user actors. Both
     * actions carry an ActorPolicy.USER catalog policy, so building the
     * event with an UNKNOWN actor (non-request callers, unit tests) would
     * fail validation and must never break the read itself.
     */
    private void recordIfUser(AuditEvent.AuditEventBuilder builder) {
        AuditEvent.AuditActor actor = AuditActorResolver.fromCurrentContext();
        if (actor.type() != AuditEvent.ActorType.USER) {
            return;
        }
        auditEventRecorder.record(builder.actor(actor).build());
    }

    @Transactional
    public AuditLog logEvent(String entity, UUID entityId, String action, Long userId,
                             String oldValue, String newValue) {
        return logEvent(entity, entityId, action, userId, oldValue, newValue, null);
    }

    @Transactional
    public AuditLog logEvent(String entity, UUID entityId, String action, Long userId,
                             String oldValue, String newValue, String correlationId) {
        if (!legacyWriteEnabled) {
            return null;
        }
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

    @Transactional
    public void logAuth(String action, Long userId, String userRole, String ipAddress, String details) {
        if (!legacyWriteEnabled) {
            return;
        }
        AuditLog log = AuditLog.builder()
                .entity("AUTH")
                .action(action)
                .userId(userId)
                .userRole(userRole)
                .ipAddress(ipAddress)
                .details(details)
                .build();
        auditLogRepository.save(log);
    }

}
