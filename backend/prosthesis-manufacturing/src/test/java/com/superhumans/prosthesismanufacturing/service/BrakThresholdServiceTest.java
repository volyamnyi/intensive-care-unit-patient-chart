package com.superhumans.prosthesismanufacturing.service;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.BrakNotificationOutboxRepository;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

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
        when(outboxRepository.insertThresholdIfAbsent(any(), any(), any(), any())).thenReturn(1);

        service.maybeEnqueue(ORDER_ID, EVENT_ID, 5L);

        verify(outboxRepository, times(1)).insertThresholdIfAbsent(
                any(), org.mockito.ArgumentMatchers.eq(EVENT_ID),
                org.mockito.ArgumentMatchers.eq(ORDER_ID), org.mockito.ArgumentMatchers.eq(5L));
        verify(outboxRepository, never()).save(any());
    }

    @Test
    void maybeEnqueue_aboveThreshold_queuesAgain() {
        when(brakEventRepository.countByOrderId(ORDER_ID)).thenReturn(4L);
        when(outboxRepository.insertThresholdIfAbsent(any(), any(), any(), any())).thenReturn(1);

        service.maybeEnqueue(ORDER_ID, EVENT_ID, 5L);

        verify(outboxRepository, times(1)).insertThresholdIfAbsent(any(), any(), any(), any());
    }

    @Test
    void maybeEnqueue_duplicateEvent_swallowsConstraintViolation() {
        when(brakEventRepository.countByOrderId(ORDER_ID)).thenReturn(3L);
        when(outboxRepository.insertThresholdIfAbsent(any(), any(), any(), any())).thenReturn(0);

        service.maybeEnqueue(ORDER_ID, EVENT_ID, 5L);

        verify(outboxRepository, times(1)).insertThresholdIfAbsent(any(), any(), any(), any());
    }

    @Test
    void maybeEnqueue_countQueryFails_skipsSilently() {
        when(brakEventRepository.countByOrderId(ORDER_ID))
                .thenThrow(new RuntimeException("db down"));

        service.maybeEnqueue(ORDER_ID, EVENT_ID, 5L);

        verify(outboxRepository, never()).save(any());
    }
}
