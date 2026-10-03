package com.superhumans.repository.core;

import com.superhumans.entity.core.AuditLog;
import java.util.List;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

/**
 * Backfill reader over {@code audit_logs} (F8, §H). The projection
 * deliberately bypasses the entity {@code @SQLRestriction}: soft-deleted
 * rows must be preserved by the backfill (§H.1).
 */
@Repository
public interface LegacyAuditLogReader extends JpaRepository<AuditLog, UUID> {

    interface LegacyAuditRow {
        UUID getId();
        java.time.LocalDateTime getTimestamp();
        Long getUserId();
        String getEntity();
        UUID getEntityId();
        String getAction();
        String getOldValue();
        String getNewValue();
        String getCorrelationId();
        String getDetails();
        String getIpAddress();
        String getUserRole();
        Boolean getIsDeleted();
    }

    @Query(value = "SELECT id AS id, timestamp AS timestamp, user_id AS userId, "
            + "entity AS entity, entity_id AS entityId, action AS action, "
            + "old_value AS oldValue, new_value AS newValue, correlation_id AS correlationId, "
            + "details AS details, ip_address AS ipAddress, user_role AS userRole, "
            + "is_deleted AS isDeleted FROM audit_logs ORDER BY id",
            nativeQuery = true)
    List<LegacyAuditRow> findAllOrdered();

    @Query(value = "SELECT COUNT(*) FROM audit_logs", nativeQuery = true)
    long countAll();
}
