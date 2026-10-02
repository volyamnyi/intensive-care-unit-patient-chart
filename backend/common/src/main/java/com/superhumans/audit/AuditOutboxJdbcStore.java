package com.superhumans.audit;

import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.List;
import java.util.UUID;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.RowMapper;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import org.springframework.transaction.support.TransactionTemplate;

/** JDBC implementation shared by the ICU, Medication and Prosthetics local outboxes. */
public class AuditOutboxJdbcStore implements AuditOutboxStore {

    private static final RowMapper<ClaimedAuditEvent> CLAIMED_EVENT_MAPPER =
            AuditOutboxJdbcStore::mapClaimedEvent;

    private final String module;
    private final javax.sql.DataSource dataSource;
    private final JdbcTemplate jdbcTemplate;
    private final TransactionTemplate transactionTemplate;
    private final AuditEventSerializer serializer;

    public AuditOutboxJdbcStore(String module, javax.sql.DataSource dataSource,
                                PlatformTransactionManager transactionManager,
                                AuditEventSerializer serializer) {
        this.module = module;
        this.dataSource = dataSource;
        this.jdbcTemplate = new JdbcTemplate(dataSource);
        this.transactionTemplate = new TransactionTemplate(transactionManager);
        this.serializer = serializer;
    }

    @Override
    public String module() {
        return module;
    }

    /**
     * Appends in the caller's existing module transaction. An unbound/autocommit connection is
     * rejected so successful business mutations cannot emit a detached best-effort event.
     */
    @Override
    public void append(AuditEvent event) {
        if (!module.equals(event.module())) {
            throw new IllegalArgumentException("Audit event module does not match outbox " + module);
        }
        if (!TransactionSynchronizationManager.isActualTransactionActive()) {
            throw new IllegalStateException("Audit outbox append requires an active business transaction");
        }
        var connection = DataSourceUtils.getConnection(dataSource);
        boolean transactionBound = DataSourceUtils.isConnectionTransactional(connection, dataSource);
        DataSourceUtils.releaseConnection(connection, dataSource);
        if (!transactionBound) {
            throw new IllegalStateException("Audit outbox connection is not bound to the business transaction");
        }

        String payload = serializer.serialize(event);
        String payloadHash = serializer.sha256(event);
        jdbcTemplate.update("INSERT INTO audit_outbox "
                        + "(audit_id, payload, payload_hash) VALUES (?, CAST(? AS jsonb), ?)",
                event.auditId(), payload, payloadHash);
    }

    @Override
    public List<ClaimedAuditEvent> claimBatch(int limit, int leaseSeconds) {
        if (limit < 1 || leaseSeconds < 1) {
            throw new IllegalArgumentException("Outbox batch size and lease must be positive");
        }
        return transactionTemplate.execute(status -> {
            List<ClaimedAuditEvent> due = jdbcTemplate.query(
                    "SELECT audit_id, payload::text AS payload, payload_hash, relay_attempts "
                            + "FROM audit_outbox "
                            + "WHERE (relay_status = 'PENDING' AND next_attempt_at <= CURRENT_TIMESTAMP) "
                            + "OR (relay_status = 'PROCESSING' AND lease_until < CURRENT_TIMESTAMP) "
                            + "ORDER BY created_at, audit_id LIMIT ? FOR UPDATE SKIP LOCKED",
                    CLAIMED_EVENT_MAPPER, limit);
            for (ClaimedAuditEvent event : due) {
                jdbcTemplate.update("UPDATE audit_outbox SET relay_status = 'PROCESSING', "
                                + "relay_attempts = relay_attempts + 1, "
                                + "lease_until = CURRENT_TIMESTAMP + (? * INTERVAL '1 second') "
                                + "WHERE audit_id = ?",
                        leaseSeconds, event.auditId());
            }
            return due.stream().map(event -> new ClaimedAuditEvent(event.auditId(), event.payload(),
                    event.payloadHash(), event.attempts() + 1)).toList();
        });
    }

    @Override
    public void markDelivered(UUID auditId) {
        transactionTemplate.executeWithoutResult(status -> jdbcTemplate.update(
                "UPDATE audit_outbox SET relay_status = 'DELIVERED', delivered_at = CURRENT_TIMESTAMP, "
                        + "lease_until = NULL, last_error_code = NULL WHERE audit_id = ? "
                        + "AND relay_status = 'PROCESSING'",
                auditId));
    }

    @Override
    public void scheduleRetry(UUID auditId, int attempts, int maxAttempts,
                              int delaySeconds, String errorCode) {
        String status = attempts >= maxAttempts ? "DEAD" : "PENDING";
        transactionTemplate.executeWithoutResult(transaction -> jdbcTemplate.update(
                "UPDATE audit_outbox SET relay_status = ?, "
                        + "next_attempt_at = CURRENT_TIMESTAMP + (? * INTERVAL '1 second'), "
                        + "lease_until = NULL, last_error_code = ? WHERE audit_id = ? "
                        + "AND relay_status = 'PROCESSING'",
                status, delaySeconds, safeErrorCode(errorCode), auditId));
    }

    @Override
    public void markDead(UUID auditId, String errorCode) {
        transactionTemplate.executeWithoutResult(transaction -> jdbcTemplate.update(
                "UPDATE audit_outbox SET relay_status = 'DEAD', lease_until = NULL, "
                        + "last_error_code = ? WHERE audit_id = ? AND relay_status = 'PROCESSING'",
                safeErrorCode(errorCode), auditId));
    }

    @Override
    public long pendingCount() {
        Long count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM audit_outbox WHERE relay_status IN ('PENDING', 'PROCESSING')",
                Long.class);
        return count == null ? 0L : count;
    }

    private static ClaimedAuditEvent mapClaimedEvent(ResultSet resultSet, int rowNumber) throws SQLException {
        return new ClaimedAuditEvent(
                resultSet.getObject("audit_id", UUID.class),
                resultSet.getString("payload"),
                resultSet.getString("payload_hash"),
                resultSet.getInt("relay_attempts"));
    }

    private static String safeErrorCode(String errorCode) {
        if (errorCode == null || !errorCode.matches("^[A-Z][A-Z0-9_:-]{0,99}$")) {
            return "AUDIT_RELAY_FAILURE";
        }
        return errorCode;
    }
}
