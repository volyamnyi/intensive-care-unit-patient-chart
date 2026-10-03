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

    /**
     * Creation time of the oldest not-yet-delivered row, or empty when the
     * outbox is drained. Drives the relay-stall age gauge (F8 monitoring).
     */
    java.util.Optional<java.time.Instant> oldestPendingAt();

    record ClaimedAuditEvent(UUID auditId, String payload, String payloadHash, int attempts) {
    }
}
