package com.superhumans.service;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class EmailServiceTest {

    @Mock private JavaMailSender mailSender;
    @InjectMocks private EmailService emailService;

    @Test
    void sendEscalation_shouldSendEmailWithCorrectContent() {
        LocalDate date = LocalDate.of(2026, 6, 15);
        emailService.sendEscalation("head@hospital.ua", "Петренко Іван", 3, date);

        ArgumentCaptor<SimpleMailMessage> captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());

        SimpleMailMessage message = captor.getValue();
        assertEquals("head@hospital.ua", message.getTo()[0]);
        assertTrue(message.getSubject().contains("не підписана доба"));
        assertTrue(message.getText().contains("Петренко Іван"));
        assertTrue(message.getText().contains("2026-06-15"));
    }

    @Test
    void sendEscalation_shouldHandleMissingEmailGracefully() {
        emailService.sendEscalation("", "Patient", 1, LocalDate.now());
        verify(mailSender).send(any(SimpleMailMessage.class));
    }
}
