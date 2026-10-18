package com.superhumans.service;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.DomainAuditEmitter;
import com.superhumans.icu.entity.ClinicalDay;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.beans.factory.annotation.Value;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.experimental.NonFinal;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailService {

    JavaMailSender mailSender;
    DomainAuditEmitter auditEmitter;

    @Value("${app.scheduling.day-close-hour:7}")
    @NonFinal
    int dayCloseHour = 7;

    @Value("${app.mail.from:noreply@hospital.local}")
    @NonFinal
    String from = "noreply@hospital.local";

    @Value("${app.mail.frontend-base-url:}")
    @NonFinal
    String frontendBaseUrl = "";

    public void sendEscalationIfUnsigned(ClinicalDay day) {
        if (!Boolean.TRUE.equals(day.getNurseSigned()) || !Boolean.TRUE.equals(day.getDoctorSigned())) {
            String episodeId = day.getEpisode().getId().toString();
            String closeTime = "%02d:00".formatted(dayCloseHour);
            log.warn("ESCALATION: Clinical day {} (episode {}) closed unsigned at {}",
                    day.getId(), episodeId, closeTime);
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setFrom(from);
                msg.setTo("hod@hospital.local");
                msg.setSubject("Необхідна увага: доба не підписана");
                msg.setText("Клінічна доба " + day.getId()
                        + " (епізод " + episodeId + ") закрита без підпису о " + closeTime + "."
                        + systemLink(episodeId));
                mailSender.send(msg);
                log.info("Escalation email sent for clinical day {}", day.getId());
                recordDelivery(day, AuditEvent.AuditOutcome.SUCCESS, null);
            } catch (MailException e) {
                log.error("Failed to send escalation email for clinical day {}: {}", day.getId(), e.getMessage());
                recordDelivery(day, AuditEvent.AuditOutcome.FAILURE, "ICU_ESCALATION_EMAIL_FAILED");
            }
        }
    }

    /**
     * Notifies the newly assigned attending doctor without making the episode
     * mutation depend on SMTP availability.
     */
    public void sendAttendingDoctorChanged(String recipient, UUID episodeId, Long previousDoctorId) {
        if (recipient == null || recipient.isBlank()) {
            return;
        }
        try {
            SimpleMailMessage msg = new SimpleMailMessage();
            msg.setFrom(from);
            msg.setTo(recipient);
            msg.setSubject("Вас призначено відповідальним лікарем");
            msg.setText("Вас призначено відповідальним лікарем епізоду " + episodeId + "."
                    + (previousDoctorId == null ? "" : " Попередній лікар: " + previousDoctorId + ".")
                    + systemLink(episodeId.toString()));
            mailSender.send(msg);
            log.info("Attending doctor assignment email sent for episode {}", episodeId);
        } catch (MailException e) {
            log.warn("Failed to send attending doctor assignment email for episode {}: {}",
                    episodeId, e.getMessage());
        }
    }

    /** Absolute link to the episode page; omitted when no frontend URL is configured. */
    private String systemLink(String episodeId) {
        if (frontendBaseUrl == null || frontendBaseUrl.isBlank()) {
            return "";
        }
        return "\n\nВідкрити карту: " + frontendBaseUrl.strip().replaceAll("/+$", "")
                + "/icu/doctor/episode/" + episodeId;
    }

    private void recordDelivery(ClinicalDay day, AuditEvent.AuditOutcome outcome, String errorCode) {
        final java.util.UUID escalationDayId = day.getId();
        auditEmitter.emit("icu", () -> AuditEvent.builder()
                .actor(AuditActorResolver.system("clinical-day-job"))
                .eventClass(EventClass.BUSINESS)
                .module("icu")
                .functionalArea("notification")
                .action("icu.notification.delivery.outcome")
                .actionType(ActionType.DELIVERY)
                .target(new AuditEvent.AuditTarget(
                        "ClinicalDay", escalationDayId.toString(), null))
                .outcome(outcome)
                .errorCode(errorCode)
                .source(AuditEvent.AuditSource.SCHEDULED_JOB)
                .build());
    }
}
