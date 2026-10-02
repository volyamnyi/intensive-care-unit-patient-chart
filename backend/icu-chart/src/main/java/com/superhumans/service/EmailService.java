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
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class EmailService {

    JavaMailSender mailSender;
    DomainAuditEmitter auditEmitter;

    public void sendEscalationIfUnsigned(ClinicalDay day) {
        if (!Boolean.TRUE.equals(day.getNurseSigned()) || !Boolean.TRUE.equals(day.getDoctorSigned())) {
            String episodeId = day.getEpisode().getId().toString();
            log.warn("ESCALATION: Clinical day {} (episode {}) closed unsigned at 07:00",
                    day.getId(), episodeId);
            try {
                SimpleMailMessage msg = new SimpleMailMessage();
                msg.setTo("hod@hospital.local");
                msg.setSubject("Необхідна увага: доба не підписана");
                msg.setText("Клінічна доба " + day.getId()
                        + " (епізод " + episodeId + ") закрита без підпису о 07:00.");
                mailSender.send(msg);
                log.info("Escalation email sent for clinical day {}", day.getId());
                recordDelivery(day, AuditEvent.AuditOutcome.SUCCESS, null);
            } catch (MailException e) {
                log.error("Failed to send escalation email for clinical day {}: {}", day.getId(), e.getMessage());
                recordDelivery(day, AuditEvent.AuditOutcome.FAILURE, "ICU_ESCALATION_EMAIL_FAILED");
            }
        }
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
