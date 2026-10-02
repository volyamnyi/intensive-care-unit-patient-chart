package com.superhumans.audit;

import java.util.List;
import java.util.UUID;

/** Module-local transactional writer and relay cursor for one database outbox. */
public interface AuditOutboxStore extends AuditEventWriter {

    String module();

    List<ClaimedAuditEvent> claimBatch(int limit, int leaseSeconds);

    void markDelivered(UUID auditId);

    void scheduleRetry(UUID auditId, int attempts, int maxAttempts, int delaySeconds, String errorCode);

    void markDead(UUID auditId, String errorCode);

    long pendingCount();

    record ClaimedAuditEvent(UUID auditId, String payload, String payloadHash, int attempts) {
    }
}
