package com.superhumans.repository.core;

import com.superhumans.entity.core.AuditLegacyEvent;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

/** Read/insert-only repository for backfilled legacy audit rows. */
public interface AuditLegacyEventRepository extends JpaRepository<AuditLegacyEvent, UUID>,
        JpaSpecificationExecutor<AuditLegacyEvent> {

    @Override
    default void deleteById(UUID id) {
        throw new UnsupportedOperationException("Legacy audit events cannot be deleted");
    }

    @Override
    default void delete(AuditLegacyEvent event) {
        throw new UnsupportedOperationException("Legacy audit events cannot be deleted");
    }

    @Override
    default void deleteAll(Iterable<? extends AuditLegacyEvent> events) {
        throw new UnsupportedOperationException("Legacy audit events cannot be deleted");
    }

    @Override
    default void deleteAllById(Iterable<? extends UUID> ids) {
        throw new UnsupportedOperationException("Legacy audit events cannot be deleted");
    }

    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("Legacy audit events cannot be deleted");
    }

    boolean existsBySourceTableAndSourceId(String sourceTable, UUID sourceId);

    Optional<AuditLegacyEvent> findBySourceTableAndSourceId(String sourceTable, UUID sourceId);

    List<AuditLegacyEvent> findByLegacyEntityAndLegacyEntityIdOrderByOccurredAtAsc(
            String legacyEntity, UUID legacyEntityId);

    Page<AuditLegacyEvent> findByLegacyEntityAndLegacyEntityId(String legacyEntity,
            UUID legacyEntityId, Pageable pageable);
}
