package com.superhumans.medicationsheet.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThatCode;

@ExtendWith(MockitoExtension.class)
class LogEmailServiceTest {

    @InjectMocks
    private LogEmailService service;

    @Test
    void sendEmail_logsWithoutThrowing() {
        assertThatCode(() -> service.sendEmail("test@superhumans.com", "Subject", "Body text"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendEmail_handlesNullTo() {
        assertThatCode(() -> service.sendEmail(null, "Subject", "Body"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendEmail_handlesNullSubject() {
        assertThatCode(() -> service.sendEmail("test@superhumans.com", null, "Body"))
                .doesNotThrowAnyException();
    }

    @Test
    void sendEmail_handlesNullText() {
        assertThatCode(() -> service.sendEmail("test@superhumans.com", "Subject", null))
                .doesNotThrowAnyException();
    }

    @Test
    void sendEmail_handlesEmptyText() {
        assertThatCode(() -> service.sendEmail("test@superhumans.com", "Subject", ""))
                .doesNotThrowAnyException();
    }
}
