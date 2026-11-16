package com.superhumans.prosthesismanufacturing.notification;

import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationKind;
import com.superhumans.repository.core.UserRepository;
import jakarta.annotation.PostConstruct;
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

    @Value("${spring.mail.host:}")
    private String smtpHost;

    @Value("${spring.mail.port:25}")
    private int smtpPort;

    @Value("${spring.mail.username:}")
    private String smtpUsername;

    /**
     * Logs the effective notification configuration once at startup and warns
     * when the sender domain does not belong to the SMTP account (issue #438).
     *
     * <p>Only domains and the host are printed — never the SMTP login or
     * password. A sender domain that the SMTP account does not own is accepted
     * by the mail server but typically fails SPF/DKIM/DMARC afterwards, so the
     * message is rewritten, spam-filed or bounced long after the app already
     * logged "sent" — the classic "the email was not sent" report.
     */
    @PostConstruct
    void logEffectiveConfiguration() {
        String senderDomain = domainOf(from);
        String accountDomain = domainOf(smtpUsername);
        log.info("Brak email notification: senderDomain={} smtpHost={} smtpPort={} smtpAccountDomain={}",
                senderDomain, smtpHost, smtpPort, accountDomain);
        String mismatch = senderDomainMismatch(from, smtpUsername);
        if (mismatch != null) {
            log.warn("Brak email sender domain '{}' is not the SMTP account domain '{}' - the mail "
                            + "server may accept the message and then rewrite, filter or bounce it "
                            + "(no SPF/DKIM/DMARC). Set APP_PROSTHETICS_BRAK_NOTIFICATION_FROM to an "
                            + "address authorized for the SMTP account.",
                    mismatch, accountDomain);
        }
    }

    /**
     * Sender domain of the notification when it does not belong to the SMTP
     * account, {@code null} otherwise (no mismatch, or no SMTP account
     * configured - a local relay has no account domain to compare with).
     */
    static String senderDomainMismatch(String from, String smtpUsername) {
        String senderDomain = domainOf(from);
        String accountDomain = domainOf(smtpUsername);
        if (senderDomain == null || accountDomain == null) {
            return null;
        }
        return senderDomain.equalsIgnoreCase(accountDomain) ? null : senderDomain;
    }

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
     *
     * <p>The log line states the facts an operator needs to answer "was the
     * mail sent and to whom": the notification kind (a brak owns a
     * {@code SINGLE} row and, from the third brak of an order chain, a
     * {@code THRESHOLD} row — two mails for one brak by design), the masked
     * recipient, the subject and the fact that the SMTP server <em>accepted</em>
     * the message (which is not the same as "delivered to the inbox").
     */
    int[] sendToRecipients(String subject, String body, List<User> recipients,
            UUID brakEventId, BrakNotificationKind kind) {
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
                log.info("Brak email accepted by SMTP brakEventId={} kind={} recipient={} subject=\"{}\"",
                        brakEventId, kind, maskAddress(recipient.getEmail()), subject);
            } catch (RuntimeException ex) {
                failed++;
                log.error("Failed to send brak email brakEventId={} kind={} recipient={}: {}",
                        brakEventId, kind, maskAddress(recipient.getEmail()), ex.getMessage());
            }
        }
        return new int[]{sent, failed};
    }

    /**
     * Masks a recipient address for the log: the local part keeps its first
     * character, the domain stays visible (it is the corporate domain, not
     * personal data).
     */
    static String maskAddress(String email) {
        if (email == null || email.isBlank()) {
            return "<none>";
        }
        String trimmed = email.strip();
        int at = trimmed.indexOf('@');
        if (at <= 0) {
            return "***";
        }
        return trimmed.charAt(0) + "***" + trimmed.substring(at);
    }

    /** Domain part of an address, or {@code null} when there is none. */
    private static String domainOf(String address) {
        if (address == null || address.isBlank()) {
            return null;
        }
        int at = address.indexOf('@');
        if (at <= 0 || at == address.length() - 1) {
            return null;
        }
        return address.substring(at + 1).toLowerCase();
    }
}
