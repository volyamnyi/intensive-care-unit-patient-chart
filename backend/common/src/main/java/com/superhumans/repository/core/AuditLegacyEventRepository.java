package com.superhumans.repository.core;

import com.superhumans.entity.core.AuditLegacyEvent;
import java.util.Collection;
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

    Optional<AuditLegacyEvent> findBySourceTableAndSourceId(String sourceTable, UUID sourceId);

    /**
     * Batched lookup for the backfill's per-page existence and checksum pass.
     *
     * <p>Explicit JPQL: the derived name {@code findBySourceTableAndSourceIds}
     * parses {@code SourceIds} as a property and fails at context startup.
     */
    @org.springframework.data.jpa.repository.Query("SELECT e FROM AuditLegacyEvent e "
            + "WHERE e.sourceTable = :sourceTable AND e.sourceId IN :sourceIds")
    List<AuditLegacyEvent> findBySourceTableAndSourceIds(
            @org.springframework.data.repository.query.Param("sourceTable") String sourceTable,
            @org.springframework.data.repository.query.Param("sourceIds") Collection<UUID> sourceIds);

    List<AuditLegacyEvent> findByLegacyEntityAndLegacyEntityIdOrderByOccurredAtAsc(
            String legacyEntity, UUID legacyEntityId);

    Page<AuditLegacyEvent> findByLegacyEntityAndLegacyEntityId(String legacyEntity,
            UUID legacyEntityId, Pageable pageable);
}
