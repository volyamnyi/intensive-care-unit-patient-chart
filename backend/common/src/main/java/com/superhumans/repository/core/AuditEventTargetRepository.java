package com.superhumans.repository.core;

import com.superhumans.entity.core.AuditEventTargetEntity;
import com.superhumans.entity.core.AuditEventTargetId;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/** Read/insert-only repository for target references (no mutation/delete API). */
public interface AuditEventTargetRepository extends JpaRepository<AuditEventTargetEntity, AuditEventTargetId> {

    @Override
    default void deleteById(AuditEventTargetId id) {
        throw new UnsupportedOperationException("Audit event targets cannot be deleted");
    }

    @Override
    default void delete(AuditEventTargetEntity target) {
        throw new UnsupportedOperationException("Audit event targets cannot be deleted");
    }

    @Override
    default void deleteAll(Iterable<? extends AuditEventTargetEntity> targets) {
        throw new UnsupportedOperationException("Audit event targets cannot be deleted");
    }

    @Override
    default void deleteAllById(Iterable<? extends AuditEventTargetId> ids) {
        throw new UnsupportedOperationException("Audit event targets cannot be deleted");
    }

    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("Audit event targets cannot be deleted");
    }

    List<AuditEventTargetEntity> findByEntityTypeAndEntityIdOrderByOccurredAtDesc(
            String entityType, String entityId);

    List<AuditEventTargetEntity> findByAuditId(UUID auditId);
}
