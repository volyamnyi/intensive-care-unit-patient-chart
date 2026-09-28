package com.superhumans.prosthesismanufacturing.notification;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import com.superhumans.prosthesismanufacturing.service.BrakService;
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

    @Mock BrakNotificationService notificationService;

    private BrakNotificationListener listener;

    @BeforeEach
    void setUp() {
        listener = new BrakNotificationListener(notificationService);
    }

    @Test
    void onBrakConfirmed_delegatesSameEventToService() {
        BrakConfirmedEvent event = new BrakConfirmedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BrakService.STAGE_D17, 7L);

        listener.onBrakConfirmed(event);

        verify(notificationService, times(1)).notifyBrakConfirmed(event);
    }

    @Test
    void onBrakConfirmed_serviceFailureDoesNotPropagate() {
        BrakConfirmedEvent event = new BrakConfirmedEvent(
                UUID.randomUUID(), UUID.randomUUID(), BrakService.STAGE_D17, 7L);
        doThrow(new IllegalStateException("audit store down"))
                .when(notificationService).notifyBrakConfirmed(event);

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
