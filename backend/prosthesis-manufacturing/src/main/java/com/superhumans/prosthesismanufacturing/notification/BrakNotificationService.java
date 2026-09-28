package com.superhumans.prosthesismanufacturing.notification;

import com.superhumans.entity.core.AuditLog;
import com.superhumans.entity.core.User;
import com.superhumans.entity.core.UserRole;
import com.superhumans.prosthesismanufacturing.entity.BrakEvent;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.service.BrakService;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStage;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.repository.core.UserRepository;
import com.superhumans.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;

/**
 * Sends the confirmed-brak email to prosthetics administrators.
 *
 * <p>Entry point is {@link #notifyBrakConfirmed(BrakConfirmedEvent)}, invoked
 * after the brak transaction commits (see {@code BrakNotificationListener}).
 * A mail failure is logged and audited but never propagates: a confirmed
 * brak must not be rolled back because of SMTP.
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrakNotificationService {

    static final String NOTIFICATION_ENTITY = "BrakNotification";
    static final String ACTION_SENT = "SENT";
    static final String ACTION_FAILED = "FAILED";
    static final String ACTION_SKIPPED_WRONG_STAGE = "SKIPPED_WRONG_STAGE";
    static final String ACTION_SKIPPED_DISABLED = "SKIPPED_DISABLED";
    static final String ACTION_SKIPPED_EVENT_NOT_FOUND = "SKIPPED_EVENT_NOT_FOUND";
    static final String ACTION_SKIPPED_INSTANCE_NOT_FOUND = "SKIPPED_INSTANCE_NOT_FOUND";
    static final String ACTION_SKIPPED_NO_RECIPIENTS = "SKIPPED_NO_RECIPIENTS";

    private static final String FALLBACK_STAGE_LABEL =
            "Примірювання та коректування тренувального протеза";
    private static final String FALLBACK_STEP_LABEL =
            "Примірювання та коректування тренувального протеза";

    private final BrakEventRepository brakEventRepository;
    private final FlowInstanceRepository instanceRepository;
    private final ProstheticsOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AuditLogRepository auditLogRepository;
    private final AuditService auditService;
    private final TemplateSnapshotParser snapshotParser;
    private final BrakNotificationComposer composer;
    private final JavaMailSender mailSender;

    @Value("${app.prosthetics.brak-notification.enabled:true}")
    private boolean enabled;

    @Value("${app.prosthetics.brak-notification.from:noreply@hospital.local}")
    private String from;

    @Value("${app.prosthetics.brak-notification.frontend-base-url:}")
    private String frontendBaseUrl;

    /**
     * Resolves the notification context and sends one email per active
     * prosthetics administrator. Never throws for mail or lookup problems
     * (each is logged and audited); only stage-6 braks are notified.
     */
    public void notifyBrakConfirmed(BrakConfirmedEvent event) {
        if (!enabled) {
            log.info("Brak email notification disabled, skipping brakEventId={}",
                    event.brakEventId());
            audit(event, ACTION_SKIPPED_DISABLED, null);
            return;
        }
        if (!BrakService.STAGE_D17.equals(event.stageId())) {
            log.info("Brak email skipped (not stage 6) brakEventId={} stageId={}",
                    event.brakEventId(), event.stageId());
            audit(event, ACTION_SKIPPED_WRONG_STAGE, null);
            return;
        }
        BrakEvent brakEvent = brakEventRepository.findById(event.brakEventId()).orElse(null);
        if (brakEvent == null) {
            log.warn("Brak email skipped, BrakEvent not found brakEventId={}",
                    event.brakEventId());
            audit(event, ACTION_SKIPPED_EVENT_NOT_FOUND, null);
            return;
        }
        if (alreadyNotified(event.brakEventId())) {
            log.info("Brak email already sent, skipping repeat brakEventId={}",
                    event.brakEventId());
            return;
        }
        FlowInstance instance = instanceRepository.findById(event.instanceId()).orElse(null);
        if (instance == null) {
            log.warn("Brak email skipped, FlowInstance not found brakEventId={} instanceId={}",
                    event.brakEventId(), event.instanceId());
            audit(event, ACTION_SKIPPED_INSTANCE_NOT_FOUND, null);
            return;
        }
        List<User> recipients = resolveRecipients();
        if (recipients.isEmpty()) {
            log.warn("Brak email skipped, no prosthetics administrators with an address "
                    + "brakEventId={}", event.brakEventId());
            audit(event, ACTION_SKIPPED_NO_RECIPIENTS, null);
            return;
        }

        ProstheticsOrder order = orderRepository.findById(instance.getOrderId()).orElse(null);
        if (order == null) {
            log.warn("Brak email proceeds without order brakEventId={} orderId={}",
                    event.brakEventId(), instance.getOrderId());
        }
        User confirmer = event.confirmedByUserId() == null ? null
                : userRepository.findById(event.confirmedByUserId()).orElse(null);
        SnapshotTemplate snapshot = parseSnapshot(instance, event.brakEventId());

        BrakNotificationData data = new BrakNotificationData(
                instance.getPatientId(),
                order == null ? null : order.getOrderNumber(),
                instance.getOrderId(),
                instance.getId(),
                brakEvent.getNewInstanceId(),
                templateName(snapshot),
                stageLabel(snapshot, brakEvent),
                stepLabel(snapshot, brakEvent),
                brakEvent.getStageId(),
                brakEvent.getStepId(),
                brakEvent.getCreatedAt(),
                confirmer == null ? null : confirmer.getFullName(),
                confirmer == null ? String.valueOf(event.confirmedByUserId())
                        : confirmer.getLogin(),
                Boolean.TRUE.equals(brakEvent.getSoftTissueMisalignment()),
                Boolean.TRUE.equals(brakEvent.getPainDiscomfort()),
                brakEvent.getNote(),
                returnStageName(snapshot, brakEvent),
                frontendBaseUrl);
        String subject = composer.buildSubject(data);
        String body = composer.buildBody(data);

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
                        event.brakEventId(), recipient.getId());
            } catch (RuntimeException ex) {
                failed++;
                log.error("Failed to send brak email brakEventId={} userId={}: {}",
                        event.brakEventId(), recipient.getId(), ex.getMessage());
            }
        }
        String action = failed == 0 ? ACTION_SENT : ACTION_FAILED;
        audit(event, action, "sent:" + sent + " failed:" + failed);
        if (failed > 0) {
            log.error("Brak email completed with failures brakEventId={} sent={} failed={}",
                    event.brakEventId(), sent, failed);
        }
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

    private boolean alreadyNotified(UUID brakEventId) {
        try {
            List<AuditLog> logs = auditLogRepository
                    .findByEntityAndEntityIdOrderByTimestampDesc(
                            NOTIFICATION_ENTITY, brakEventId, PageRequest.of(0, 10))
                    .getContent();
            return logs.stream().anyMatch(entry -> ACTION_SENT.equals(entry.getAction()));
        } catch (RuntimeException ex) {
            log.warn("Brak email idempotency check failed, proceeding brakEventId={}: {}",
                    brakEventId, ex.getMessage());
            return false;
        }
    }

    private SnapshotTemplate parseSnapshot(FlowInstance instance, UUID brakEventId) {
        try {
            String json = instance.getTemplateSnapshot();
            if (json == null || json.isBlank()) {
                return null;
            }
            return snapshotParser.parse(json);
        } catch (RuntimeException ex) {
            log.warn("Brak email proceeds without snapshot names brakEventId={}: {}",
                    brakEventId, ex.getMessage());
            return null;
        }
    }

    private String templateName(SnapshotTemplate snapshot) {
        if (snapshot == null || snapshot.getName() == null) {
            return null;
        }
        return snapshot.getVersion() == null ? snapshot.getName()
                : snapshot.getName() + " v" + snapshot.getVersion();
    }

    private String stageLabel(SnapshotTemplate snapshot, BrakEvent brakEvent) {
        SnapshotStage stage = findStage(snapshot, brakEvent.getStageId());
        if (stage == null || stage.getName() == null) {
            return FALLBACK_STAGE_LABEL;
        }
        return stage.getName();
    }

    private String stepLabel(SnapshotTemplate snapshot, BrakEvent brakEvent) {
        SnapshotStage stage = findStage(snapshot, brakEvent.getStageId());
        if (stage == null || stage.getSteps() == null) {
            return FALLBACK_STEP_LABEL;
        }
        for (SnapshotStep step : stage.getSteps()) {
            if (step != null && brakEvent.getStepId() != null
                    && brakEvent.getStepId().equals(step.getId())) {
                return step.getName() == null ? FALLBACK_STEP_LABEL : step.getName();
            }
        }
        return FALLBACK_STEP_LABEL;
    }

    private String returnStageName(SnapshotTemplate snapshot, BrakEvent brakEvent) {
        SnapshotStage stage = findStage(snapshot, brakEvent.getReturnStageId());
        return stage == null ? null : stage.getName();
    }

    private SnapshotStage findStage(SnapshotTemplate snapshot, UUID stageId) {
        if (snapshot == null || snapshot.getStages() == null || stageId == null) {
            return null;
        }
        for (SnapshotStage stage : snapshot.getStages()) {
            if (stage != null && stageId.equals(stage.getId())) {
                return stage;
            }
        }
        return null;
    }

    private void audit(BrakConfirmedEvent event, String action, String newValue) {
        auditService.logEvent(NOTIFICATION_ENTITY, event.brakEventId(), action,
                event.confirmedByUserId(), null, newValue);
    }
}
