package com.superhumans.prosthesismanufacturing.notification;

import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.repository.core.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Recipient resolution and SMTP sending for brak emails (issue #320, v2).
 *
 * <p>Orchestration (gates, outbox states, retries, audits) lives in
 * {@link BrakNotificationDeliveryService}; this bean stays free of business
 * decisions so it is trivially unit-testable.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrakNotificationService {

    private final UserRepository userRepository;
    private final JavaMailSender mailSender;

    @Value("${app.prosthetics.brak-notification.from:noreply@hospital.local}")
    private String from;

    /**
     * Returns all prosthetics administrators eligible for the notification:
     * not deleted, with a non-blank address, deduplicated by normalized email.
     * Reads the role fresh on every call; LDAP and LOCAL users share
     * the same {@code users} table so no provider distinction is needed.
     */
    List<User> resolveRecipients() {
        Map<String, User> byEmail = new LinkedHashMap<>();
        for (User admin : userRepository.findByRole(UserRole.PROSTHETICS_ADMINISTRATOR)) {
            if (Boolean.TRUE.equals(admin.getDeleted())) {
                continue;
            }
            String email = admin.getEmail();
            if (email == null || email.isBlank()) {
                log.warn("Skipping brak email recipient without address: userId={} login={}",
                        admin.getId(), admin.getLogin());
                continue;
            }
            byEmail.putIfAbsent(email.strip().toLowerCase(), admin);
        }
        return new ArrayList<>(byEmail.values());
    }

    /**
     * Sends one message per recipient. A failure is contained per address and
     * reported in the returned {@code [sent, failed]} counts — never thrown,
     * so a broken address can neither cancel the batch nor roll back a brak.
     */
    int[] sendToRecipients(String subject, String body, List<User> recipients,
            UUID brakEventId) {
        int sent = 0;
        int failed = 0;
        for (User recipient : recipients) {
            try {
                SimpleMailMessage message = new SimpleMailMessage();
                message.setFrom(from);
                message.setTo(recipient.getEmail().strip());
                message.setSubject(subject);
                message.setText(body);
                mailSender.send(message);
                sent++;
                log.info("Brak email sent brakEventId={} userId={}",
                        brakEventId, recipient.getId());
            } catch (RuntimeException ex) {
                failed++;
                log.error("Failed to send brak email brakEventId={} userId={}: {}",
                        brakEventId, recipient.getId(), ex.getMessage());
            }
        }
        return new int[]{sent, failed};
    }
}
