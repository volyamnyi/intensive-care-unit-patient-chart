package com.superhumans.repository.core;

import com.superhumans.entity.core.AuditLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

public interface AuditLogRepository extends JpaRepository<AuditLog, UUID> {

    @Override
    default void deleteById(UUID uuid) {
        throw new UnsupportedOperationException("Audit logs cannot be deleted");
    }

    @Override
    default void delete(AuditLog entity) {
        throw new UnsupportedOperationException("Audit logs cannot be deleted");
    }

    @Override
    default void deleteAll(Iterable<? extends AuditLog> entities) {
        throw new UnsupportedOperationException("Audit logs cannot be deleted");
    }

    @Override
    default void deleteAllById(Iterable<? extends UUID> ids) {
        throw new UnsupportedOperationException("Audit logs cannot be deleted");
    }

    @Override
    default void deleteAll() {
        throw new UnsupportedOperationException("Audit logs cannot be deleted");
    }

    Page<AuditLog> findByEntityAndEntityIdOrderByTimestampDesc(String entity, UUID entityId, Pageable pageable);
    Page<AuditLog> findByUserIdOrderByTimestampDesc(Long userId, Pageable pageable);
    Page<AuditLog> findByEntityOrderByTimestampDesc(String entity, Pageable pageable);
    Page<AuditLog> findByActionOrderByTimestampDesc(String action, Pageable pageable);
    Page<AuditLog> findByTimestampBetweenOrderByTimestampDesc(LocalDateTime start, LocalDateTime end, Pageable pageable);
    Page<AuditLog> findAllByOrderByTimestampDesc(Pageable pageable);

    @Query("SELECT a FROM AuditLog a WHERE a.isDeleted IS NULL OR a.isDeleted = false")
    List<AuditLog> findAllActive();
}
