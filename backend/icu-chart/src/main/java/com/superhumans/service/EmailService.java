package com.superhumans.service;

import com.superhumans.entity.ClinicalDay;
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
            } catch (MailException e) {
                log.error("Failed to send escalation email for clinical day {}: {}", day.getId(), e.getMessage());
            }
        }
    }
}
