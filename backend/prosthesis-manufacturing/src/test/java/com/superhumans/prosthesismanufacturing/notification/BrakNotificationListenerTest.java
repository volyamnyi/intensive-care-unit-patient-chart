package com.superhumans.prosthesismanufacturing.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;

import com.superhumans.prosthesismanufacturing.service.BrakService;
import com.superhumans.service.AuditService;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

@ExtendWith(MockitoExtension.class)
class BrakNotificationListenerTest {

    @Mock BrakNotificationDeliveryService deliveryService;
    @Mock AuditService auditService;

    private BrakNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new BrakNotificationListener(deliveryService, auditService);
    }

    @Test
    void onBrakConfirmed_delegatesToDelivery() {
        BrakConfirmedEvent event = new BrakConfirmedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BrakService.STAGE_D17, 7L);

        listener.onBrakConfirmed(event);

        verify(deliveryService, times(1)).deliver(event.brakEventId(), 7L);
        verifyNoInteractions(auditService);
    }

    @Test
    void onBrakConfirmed_wrongStageAuditsAndSkipsDelivery() {
        BrakConfirmedEvent event = new BrakConfirmedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BrakService.STAGE_D20, 7L);

        listener.onBrakConfirmed(event);

        verify(auditService).logAction("BrakNotification", event.brakEventId(),
                "SKIPPED_WRONG_STAGE", 7L);
        verifyNoInteractions(deliveryService);
    }

    @Test
    void onBrakConfirmed_deliveryFailureDoesNotPropagate() {
        BrakConfirmedEvent event = new BrakConfirmedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BrakService.STAGE_D17, 7L);
        doThrow(new IllegalStateException("outbox store down"))
                .when(deliveryService).deliver(event.brakEventId(), 7L);

        assertThatCode(() -> listener.onBrakConfirmed(event)).doesNotThrowAnyException();
    }

    @Test
    void onBrakConfirmed_runsAfterCommit() throws Exception {
        TransactionalEventListener annotation = BrakNotificationListener.class
                .getMethod("onBrakConfirmed", BrakConfirmedEvent.class)
                .getAnnotation(TransactionalEventListener.class);

        assertThat(annotation).isNotNull();
        assertThat(annotation.phase()).isEqualTo(TransactionPhase.AFTER_COMMIT);
    }
}
