package com.superhumans.prosthesismanufacturing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superhumans.prosthesismanufacturing.entity.BrakNotificationKind;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationOutbox;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationStatus;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.BrakNotificationOutboxRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

/**
 * Unit tests for the threshold enqueue (epic #322, issue #324).
 */
@ExtendWith(MockitoExtension.class)
class BrakThresholdServiceTest {

    @Mock BrakEventRepository brakEventRepository;
    @Mock BrakNotificationOutboxRepository outboxRepository;

    BrakThresholdService service;

    UUID ORDER_ID = UUID.randomUUID();
    UUID EVENT_ID = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        service = new BrakThresholdService(brakEventRepository, outboxRepository);
    }

    @Test
    void maybeEnqueue_belowThreshold_savesNothing() {
        when(brakEventRepository.countByOrderId(ORDER_ID)).thenReturn(2L);

        service.maybeEnqueue(ORDER_ID, EVENT_ID, 5L);

        verify(outboxRepository, never()).save(any());
    }

    @Test
    void maybeEnqueue_atThreshold_queuesThresholdRow() {
        when(brakEventRepository.countByOrderId(ORDER_ID)).thenReturn(3L);

        service.maybeEnqueue(ORDER_ID, EVENT_ID, 5L);

        ArgumentCaptor<BrakNotificationOutbox> captor =
                ArgumentCaptor.forClass(BrakNotificationOutbox.class);
        verify(outboxRepository, times(1)).save(captor.capture());
        BrakNotificationOutbox row = captor.getValue();
        assertThat(row.getBrakEventId()).isEqualTo(EVENT_ID);
        assertThat(row.getKind()).isEqualTo(BrakNotificationKind.THRESHOLD);
        assertThat(row.getOrderId()).isEqualTo(ORDER_ID);
        assertThat(row.getStatus()).isEqualTo(BrakNotificationStatus.PENDING);
        assertThat(row.getAttempts()).isZero();
        assertThat(row.getCreatedBy()).isEqualTo(5L);
    }

    @Test
    void maybeEnqueue_aboveThreshold_queuesAgain() {
        when(brakEventRepository.countByOrderId(ORDER_ID)).thenReturn(4L);

        service.maybeEnqueue(ORDER_ID, EVENT_ID, 5L);

        verify(outboxRepository, times(1)).save(any());
    }

    @Test
    void maybeEnqueue_duplicateEvent_swallowsConstraintViolation() {
        when(brakEventRepository.countByOrderId(ORDER_ID)).thenReturn(3L);
        when(outboxRepository.save(any()))
                .thenThrow(new DataIntegrityViolationException("uq_prosthetics_brak_notifications_event"));

        service.maybeEnqueue(ORDER_ID, EVENT_ID, 5L);

        verify(outboxRepository, times(1)).save(any());
    }

    @Test
    void maybeEnqueue_countQueryFails_skipsSilently() {
        when(brakEventRepository.countByOrderId(ORDER_ID))
                .thenThrow(new RuntimeException("db down"));

        service.maybeEnqueue(ORDER_ID, EVENT_ID, 5L);

        verify(outboxRepository, never()).save(any());
    }
}
