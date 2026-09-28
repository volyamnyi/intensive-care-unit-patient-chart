package com.superhumans.prosthesismanufacturing.notification;

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

    private final BrakNotificationService notificationService;

    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void onBrakConfirmed(BrakConfirmedEvent event) {
        try {
            notificationService.notifyBrakConfirmed(event);
        } catch (RuntimeException ex) {
            log.error("Brak email listener failed brakEventId={}: {}",
                    event.brakEventId(), ex.getMessage());
        }
    }
}
