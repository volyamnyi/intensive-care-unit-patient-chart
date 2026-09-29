package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.prosthesismanufacturing.entity.BrakNotificationKind;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationOutbox;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationStatus;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.BrakNotificationOutboxRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;

import java.util.UUID;

/**
 * Enqueues the order-wide brak escalation (epic #322, issue #324).
 *
 * <p>Called from {@link BrakService#createBrakAndBranch} after the brak event
 * is saved, inside the same transaction: the chain count already includes
 * the just-saved event. When the count reaches {@code >= 3} a
 * {@code THRESHOLD} outbox row is queued for the scheduled delivery sweep
 * ({@code BrakNotificationDeliveryService}) — the same guaranteed-delivery
 * path as the per-brak {@code SINGLE} rows from issue #320.
 *
 * <p>Idempotency: exactly one {@code THRESHOLD} row per triggering brak
 * event, enforced by {@code UNIQUE(brak_event_id)}. A concurrent duplicate
 * enqueue hits the constraint and is swallowed here (logged) so the brak
 * itself is never rolled back because of the notification.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrakThresholdService {

    private final BrakEventRepository brakEventRepository;
    private final BrakNotificationOutboxRepository outboxRepository;

    /**
     * Queues a {@code THRESHOLD} notification when the order chain of the
     * just-saved brak event reached the escalation count. Never throws for
     * notification problems.
     *
     * @param orderId    order chain the brak belongs to
     * @param brakEventId just-saved brak event (owner of the queued row)
     * @param actorId    user who confirmed the brak (delivery context)
     */
    public void maybeEnqueue(UUID orderId, UUID brakEventId, Long actorId) {
        long brakCount;
        try {
            brakCount = brakEventRepository.countByOrderId(orderId);
        } catch (RuntimeException ex) {
            log.error("Brak threshold check failed, skipping escalation orderId={} brakEventId={}: {}",
                    orderId, brakEventId, ex.getMessage());
            return;
        }
        if (!BrakThresholdPolicy.shouldNotify(brakCount)) {
            return;
        }
        try {
            BrakNotificationOutbox outbox = BrakNotificationOutbox.builder()
                    .brakEventId(brakEventId)
                    .kind(BrakNotificationKind.THRESHOLD)
                    .orderId(orderId)
                    .status(BrakNotificationStatus.PENDING)
                    .attempts(0)
                    .build();
            outbox.setCreatedBy(actorId);
            outbox.setUpdatedBy(actorId);
            outboxRepository.save(outbox);
            log.info("Brak threshold escalation queued orderId={} brakEventId={} brakCount={}",
                    orderId, brakEventId, brakCount);
        } catch (DataIntegrityViolationException ex) {
            log.info("Brak threshold escalation already queued, skipping duplicate "
                    + "orderId={} brakEventId={}", orderId, brakEventId);
        } catch (RuntimeException ex) {
            log.error("Brak threshold enqueue failed, skipping escalation "
                    + "orderId={} brakEventId={}: {}", orderId, brakEventId, ex.getMessage());
        }
    }
}
