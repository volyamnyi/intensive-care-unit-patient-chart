package com.superhumans.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

import com.superhumans.audit.DomainAuditEmitter;
import com.superhumans.icu.entity.ClinicalDay;
import com.superhumans.icu.entity.Episode;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.util.ReflectionTestUtils;

class EmailServiceTest {

    private static ClinicalDay unsignedDay() {
        Episode episode = new Episode();
        episode.setId(UUID.randomUUID());
        ClinicalDay day = new ClinicalDay();
        day.setId(UUID.randomUUID());
        day.setEpisode(episode);
        day.setNurseSigned(false);
        day.setDoctorSigned(false);
        return day;
    }

    @Test
    void escalationEmail_usesConfiguredDayCloseHour() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService service = new EmailService(mailSender, mock(DomainAuditEmitter.class));
        ReflectionTestUtils.setField(service, "dayCloseHour", 6);

        service.sendEscalationIfUnsigned(unsignedDay());

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).contains("о 06:00");
    }

    @Test
    void escalationEmail_usesConfiguredSenderAndEpisodeLink() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService service = new EmailService(mailSender, mock(DomainAuditEmitter.class));
        ReflectionTestUtils.setField(service, "from", "icu-noreply@hospital.example");
        ReflectionTestUtils.setField(service, "frontendBaseUrl", "http://localhost:5173/");

        ClinicalDay day = unsignedDay();
        service.sendEscalationIfUnsigned(day);

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        SimpleMailMessage message = captor.getValue();
        assertThat(message.getFrom()).isEqualTo("icu-noreply@hospital.example");
        assertThat(message.getText())
                .contains("http://localhost:5173/icu/doctor/episode/" + day.getEpisode().getId());
    }

    @Test
    void escalationEmail_omitsLinkWhenNoFrontendUrlConfigured() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService service = new EmailService(mailSender, mock(DomainAuditEmitter.class));

        service.sendEscalationIfUnsigned(unsignedDay());

        var captor = ArgumentCaptor.forClass(SimpleMailMessage.class);
        verify(mailSender).send(captor.capture());
        assertThat(captor.getValue().getText()).doesNotContain("Відкрити карту");
        assertThat(captor.getValue().getFrom()).isEqualTo("noreply@hospital.local");
    }

    @Test
    void signedDay_sendsNothing() {
        JavaMailSender mailSender = mock(JavaMailSender.class);
        EmailService service = new EmailService(mailSender, mock(DomainAuditEmitter.class));

        ClinicalDay day = unsignedDay();
        day.setNurseSigned(true);
        day.setDoctorSigned(true);
        service.sendEscalationIfUnsigned(day);

        verify(mailSender, never()).send(org.mockito.ArgumentMatchers.any(SimpleMailMessage.class));
    }
}