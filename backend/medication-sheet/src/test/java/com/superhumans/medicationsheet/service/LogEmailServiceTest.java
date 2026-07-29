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
}
