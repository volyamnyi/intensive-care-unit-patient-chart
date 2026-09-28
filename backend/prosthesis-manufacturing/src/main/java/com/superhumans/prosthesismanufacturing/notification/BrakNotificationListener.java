package com.superhumans.prosthesismanufacturing.notification;

import com.superhumans.prosthesismanufacturing.service.BrakService;
import com.superhumans.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

/**
 * Delivers {@link BrakConfirmedEvent} to the notification service only after
 * the brak transaction commits, so an email can never describe a rolled-back
 * brak. A listener failure is contained here and never affects the caller.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class BrakNotificationListener {

    private final BrakNotificationDeliveryService deliveryService;
    private final AuditService auditService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBrakConfirmed(BrakConfirmedEvent event) {
        try {
            if (!BrakService.STAGE_D17.equals(event.stageId())) {
                log.info("Brak email skipped (not stage 6) brakEventId={} stageId={}",
                        event.brakEventId(), event.stageId());
                auditService.logAction("BrakNotification", event.brakEventId(),
                        "SKIPPED_WRONG_STAGE", event.confirmedByUserId());
                return;
            }
            deliveryService.deliver(event.brakEventId(), event.confirmedByUserId());
        } catch (RuntimeException ex) {
            log.error("Brak email listener failed brakEventId={}: {}",
                    event.brakEventId(), ex.getMessage());
        }
    }
}
