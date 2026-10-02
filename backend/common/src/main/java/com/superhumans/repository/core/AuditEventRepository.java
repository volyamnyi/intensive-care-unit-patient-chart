package com.superhumans.repository.core;

import com.superhumans.entity.core.AuditEventEntity;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

/** Persistence boundary for append-only Audit v2 events. */
public interface AuditEventRepository extends JpaRepository<AuditEventEntity, UUID> {

    @Override
    default void deleteById(UUID id) {
        throw new UnsupportedOperationException("Audit events cannot be deleted");
    }

    @Override
    default void delete(AuditEventEntity event) {
        throw new UnsupportedOperationException("Audit events cannot be deleted");
    }

    @Override
    default void deleteAll(Iterable<? extends AuditEventEntity> events) {
        throw new UnsupportedOperationException("Audit events cannot be deleted");
    }

    @Override
    default void deleteAllById(Iterable<? extends UUID> ids) {
        throw new UnsupportedOperationException("Audit events cannot be deleted");
    }

    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("Audit events cannot be deleted");
    }

    Page<AuditEventEntity> findByTargetTypeAndTargetIdOrderByOccurredAtDesc(
            String targetType, String targetId, Pageable pageable);
}
