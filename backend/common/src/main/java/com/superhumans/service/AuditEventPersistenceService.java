package com.superhumans.service;

import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditIntegrityException;
import com.superhumans.audit.AuditMetrics;
import com.superhumans.entity.core.AuditEventEntity;
import com.superhumans.mapper.AuditEventEntityMapper;
import com.superhumans.repository.core.AuditEventRepository;
import com.superhumans.repository.core.AuditEventTargetRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Atomically persists the canonical event and every indexed object-history reference in core. */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditEventPersistenceService {

    AuditEventRepository auditEventRepository;
    AuditEventTargetRepository auditEventTargetRepository;
    AuditEventEntityMapper mapper;
    AuditMetrics auditMetrics;

    @Transactional(transactionManager = "coreTransactionManager")
    public void persist(AuditEvent event, String integrityHash) {
        AuditEventEntity existing = auditEventRepository.findById(event.auditId()).orElse(null);
        if (existing != null) {
            if (!integrityHash.equals(existing.getIntegrityHash())) {
                throw new AuditIntegrityException("Audit event ID already exists with a different integrity hash");
            }
            auditMetrics.duplicate(event.module());
            return;
        }
        auditEventRepository.save(mapper.toEntity(event, integrityHash));
        auditEventTargetRepository.saveAll(mapper.toTargetEntities(event));
        auditMetrics.stored(event.module());
    }
}
