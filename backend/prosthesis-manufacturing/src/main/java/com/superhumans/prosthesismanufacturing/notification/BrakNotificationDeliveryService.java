package com.superhumans.prosthesismanufacturing.notification;

import com.superhumans.entity.core.User;
import com.superhumans.prosthesismanufacturing.entity.BrakEvent;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationOutbox;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationStatus;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.BrakNotificationOutboxRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStage;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import com.superhumans.repository.core.UserRepository;
import com.superhumans.service.AuditService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;

/**
 * Guaranteed delivery of queued brak emails (issue #320, v2).
 *
 * <p>Each confirmed stage-6 brak owns exactly one outbox row (written in the
 * brak transaction itself). This service delivers it on a schedule until it
 * reaches a terminal state ({@code SENT}, {@code SKIPPED}) or exhausts
 * {@code outbox-max-attempts} ({@code DEAD}, operator attention required).
 * Delivery runs only here (single-instance scheduler, consistent with the other
 * {@code @Scheduled} jobs in the codebase) — deliberately no eager
 * after-commit listener: under the ChainedTransactionManager such a listener
 * fires per-leg mid-chain without a usable transaction context
 * (issue #321: HeuristicCompletion with mixed outcome).
 */
@Slf4j
@Service
@RequiredArgsConstructor
public class BrakNotificationDeliveryService {

    static final String NOTIFICATION_ENTITY = "BrakNotification";
    static final String ACTION_SENT = "SENT";
    static final String ACTION_FAILED = "FAILED";
    static final String ACTION_SKIPPED_NO_RECIPIENTS = "SKIPPED_NO_RECIPIENTS";
    static final String ACTION_SKIPPED_INSTANCE_NOT_FOUND = "SKIPPED_INSTANCE_NOT_FOUND";
    static final String ACTION_SKIPPED_DISABLED = "SKIPPED_DISABLED";

    private static final String FALLBACK_STAGE_LABEL =
            "Примірювання та коректування тренувального протеза";
    private static final String FALLBACK_STEP_LABEL =
            "Примірювання та коректування тренувального протеза";

    private final BrakNotificationOutboxRepository outboxRepository;
    private final BrakEventRepository brakEventRepository;
    private final FlowInstanceRepository instanceRepository;
    private final ProstheticsOrderRepository orderRepository;
    private final UserRepository userRepository;
    private final AuditService auditService;
    private final TemplateSnapshotParser snapshotParser;
    private final BrakNotificationComposer composer;
    private final BrakNotificationService notificationService;

    @Value("${app.prosthetics.brak-notification.enabled:true}")
    private boolean enabled;

    @Value("${app.prosthetics.brak-notification.frontend-base-url:}")
    private String frontendBaseUrl;

    @Value("${app.prosthetics.brak-notification.outbox-max-attempts:5}")
    private int maxAttempts;

    @Value("${app.prosthetics.brak-notification.outbox-batch-size:20}")
    private int batchSize;

    /**
     * Delivers one queued notification, claiming its row with a write lock so
     * a concurrent deliverer (eager listener vs sweep) sees the terminal state
     * and skips instead of double-sending. Never throws for mail problems.
     */
    @Transactional
    public void deliver(UUID brakEventId, Long actorId) {
        if (!enabled) {
            log.info("Brak email delivery disabled, keeping it queued brakEventId={}",
                    brakEventId);
            auditService.logEvent(NOTIFICATION_ENTITY, brakEventId,
                    ACTION_SKIPPED_DISABLED, actorId, null, null);
            return;
        }
        BrakNotificationOutbox row = outboxRepository
                .findByBrakEventIdForUpdate(brakEventId)
                .orElse(null);
        if (row == null) {
            log.warn("Brak email skipped, no outbox row brakEventId={}", brakEventId);
            return;
        }
        if (row.getStatus() == BrakNotificationStatus.SENT
                || row.getStatus() == BrakNotificationStatus.SKIPPED
                || row.getStatus() == BrakNotificationStatus.DEAD) {
            log.info("Brak email already terminal ({}), skipping repeat brakEventId={}",
                    row.getStatus(), brakEventId);
            return;
        }
        if (row.getStatus() == BrakNotificationStatus.FAILED
                && row.getAttempts() >= maxAttempts) {
            row.setStatus(BrakNotificationStatus.DEAD);
            row.setLastError("attempts exhausted (" + row.getAttempts() + ")");
            log.error("Brak email dead, attempts exhausted brakEventId={} attempts={}",
                    brakEventId, row.getAttempts());
            return;
        }
        BrakEvent brakEvent = brakEventRepository.findById(brakEventId).orElse(null);
        if (brakEvent == null) {
            // Cannot happen (same-transaction enqueue), but fail closed and
            // terminal so the sweep does not spin on it forever.
            row.setStatus(BrakNotificationStatus.SKIPPED);
            log.warn("Brak email skipped, BrakEvent not found brakEventId={}", brakEventId);
            return;
        }
        FlowInstance instance = instanceRepository.findById(brakEvent.getInstanceId())
                .orElse(null);
        if (instance == null) {
            row.setStatus(BrakNotificationStatus.SKIPPED);
            log.warn("Brak email skipped, FlowInstance not found brakEventId={} instanceId={}",
                    brakEventId, brakEvent.getInstanceId());
            auditService.logEvent(NOTIFICATION_ENTITY, brakEventId,
                    ACTION_SKIPPED_INSTANCE_NOT_FOUND, actorId, null, null);
            return;
        }
        List<User> recipients = notificationService.resolveRecipients();
        if (recipients.isEmpty()) {
            row.setStatus(BrakNotificationStatus.SKIPPED);
            log.warn("Brak email skipped, no prosthetics administrators with an address "
                    + "brakEventId={}", brakEventId);
            auditService.logEvent(NOTIFICATION_ENTITY, brakEventId,
                    ACTION_SKIPPED_NO_RECIPIENTS, actorId, null, null);
            return;
        }

        ProstheticsOrder order = orderRepository.findById(instance.getOrderId()).orElse(null);
        if (order == null) {
            log.warn("Brak email proceeds without order brakEventId={} orderId={}",
                    brakEventId, instance.getOrderId());
        }
        User confirmer = actorId == null ? null
                : userRepository.findById(actorId).orElse(null);
        SnapshotTemplate snapshot = parseSnapshot(instance, brakEventId);

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
                confirmer == null ? String.valueOf(actorId) : confirmer.getLogin(),
                Boolean.TRUE.equals(brakEvent.getSoftTissueMisalignment()),
                Boolean.TRUE.equals(brakEvent.getPainDiscomfort()),
                brakEvent.getNote(),
                returnStageName(snapshot, brakEvent),
                frontendBaseUrl);
        int[] result = notificationService.sendToRecipients(
                composer.buildSubject(data), composer.buildBody(data), recipients,
                brakEventId);

        row.setAttempts(row.getAttempts() + 1);
        String outcome = "sent:" + result[0] + " failed:" + result[1];
        if (result[1] == 0) {
            row.setStatus(BrakNotificationStatus.SENT);
            row.setLastError(null);
        } else if (row.getAttempts() >= maxAttempts) {
            row.setStatus(BrakNotificationStatus.DEAD);
            row.setLastError(outcome);
            log.error("Brak email dead, attempts exhausted brakEventId={} attempts={} {}",
                    brakEventId, row.getAttempts(), outcome);
        } else {
            row.setStatus(BrakNotificationStatus.FAILED);
            row.setLastError(outcome);
        }
        outboxRepository.save(row);
        auditService.logEvent(NOTIFICATION_ENTITY, brakEventId,
                result[1] == 0 ? ACTION_SENT : ACTION_FAILED, actorId, null, outcome);
        if (result[1] > 0 && row.getStatus() != BrakNotificationStatus.DEAD) {
            log.error("Brak email completed with failures brakEventId={} sent={} failed={}",
                    brakEventId, result[0], result[1]);
        }
    }

    /**
     * Retry sweep over deliverable rows, oldest first. Fixed-delay (never
     * overlapping itself); one bad row never aborts the sweep.
     */
    @Scheduled(fixedDelayString = "${app.prosthetics.brak-notification.outbox-poll-ms:60000}")
    @Transactional
    public void sweep() {
        if (!enabled) {
            log.debug("Brak email sweep skipped (disabled)");
            return;
        }
        List<BrakNotificationOutbox> due = outboxRepository
                .findByStatusInAndAttemptsLessThanOrderByCreatedAtAsc(
                        List.of(BrakNotificationStatus.PENDING, BrakNotificationStatus.FAILED),
                        maxAttempts,
                        PageRequest.of(0, batchSize));
        for (BrakNotificationOutbox row : due) {
            try {
                deliver(row.getBrakEventId(), row.getCreatedBy());
            } catch (RuntimeException ex) {
                log.error("Brak email sweep failed for row brakEventId={}: {}",
                        row.getBrakEventId(), ex.getMessage());
            }
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
}
