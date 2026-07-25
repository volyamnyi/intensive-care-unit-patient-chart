package com.superhumans.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class LogNotificationServiceTest {

    @InjectMocks
    private LogNotificationService service;

    @Test
    void notifyPrescriptionCreated_doesNotThrow() {
        UUID listId = UUID.randomUUID();
        assertThatCode(() -> service.notifyPrescriptionCreated(listId, "Петренко"))
                .doesNotThrowAnyException();
    }

    @Test
    void notifyPrescriptionUpdated_doesNotThrow() {
        UUID listId = UUID.randomUUID();
        assertThatCode(() -> service.notifyPrescriptionUpdated(listId, "Коваленко"))
                .doesNotThrowAnyException();
    }

    @Test
    void notifyDoseExecuted_doesNotThrow() {
        UUID listId = UUID.randomUUID();
        assertThatCode(() -> service.notifyDoseExecuted(listId, "Dopamine", "nurse1"))
                .doesNotThrowAnyException();
    }

    @Test
    void notifyPrescriptionCreated_handlesNullPatientName() {
        UUID listId = UUID.randomUUID();
        assertThatCode(() -> service.notifyPrescriptionCreated(listId, null))
                .doesNotThrowAnyException();
    }

    @Test
    void notifyDoseExecuted_handlesNulls() {
        assertThatCode(() -> service.notifyDoseExecuted(null, null, null))
                .doesNotThrowAnyException();
    }
}
