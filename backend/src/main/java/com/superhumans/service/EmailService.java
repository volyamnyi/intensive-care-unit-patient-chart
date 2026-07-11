package com.superhumans.service;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    private final JavaMailSender mailSender;

    @Value("${spring.mail.username:}")
    private String fromEmail;

    public void sendEscalation(String to, String patientName, Integer dayNumber, LocalDate date) {
        try {
            SimpleMailMessage message = new SimpleMailMessage();
            message.setFrom(fromEmail);
            message.setTo(to);
            message.setSubject("Ескалація: не підписана доба №" + dayNumber);
            message.setText("Доба №" + dayNumber + " за " + date
                    + " для пацієнта " + patientName
                    + " не була підписана лікарем до 09:00.\n"
                    + "Будь ласка, перевірте та вжийте заходів.");
            mailSender.send(message);
            log.info("Escalation email sent to {} for day {}", to, dayNumber);
        } catch (Exception e) {
            log.error("Failed to send escalation email to {}: {}", to, e.getMessage());
        }
    }
}
