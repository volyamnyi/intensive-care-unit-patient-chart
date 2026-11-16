package com.superhumans.prosthesismanufacturing.notification;

import com.superhumans.entity.core.User;
import com.superhumans.prosthesismanufacturing.entity.BrakEvent;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationKind;
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
import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.DataClass;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.DomainAuditEmitter;
import com.superhumans.repository.core.UserRepository;
import com.superhumans.service.AuditService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.PageRequest;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;

/**
 * Guaranteed delivery of queued brak emails (issue #320, v2).
 *
 * <p>Each confirmed stage-6 brak owns a {@code SINGLE} outbox row (written in
 * the brak transaction itself); braks that push their order chain to count
 * {@code >= 3} additionally own one {@code THRESHOLD} row per triggering
 * event (epic #322). This service delivers every row on a schedule until it
 * reaches a terminal state ({@code SENT}, {@code SKIPPED}) or exhausts
 * {@code outbox-max-attempts} ({@code DEAD}, operator attention required).
 * Delivery runs only here (single-instance scheduler, consistent with the other
 * {@code @Scheduled} jobs in the codebase) — deliberately no eager
 * after-commit listener: under the ChainedTransactionManager such a listener
 * fires per-leg mid-chain without a usable transaction context
 * (issue #321: HeuristicCompletion with mixed outcome).
 *
 * <p>Because the sweep runs outside any business transaction, the delivery
 * outcome is audited in a short, own transaction on the module manager
 * (issue #436) — otherwise the module outbox would reject every append and
 * the delivery would leave no trace in the Audit console.
 */
@Slf4j
@Service
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
    private final DomainAuditEmitter auditEmitter;
    private final TemplateSnapshotParser snapshotParser;
    private final BrakNotificationComposer composer;
    private final BrakNotificationService notificationService;

    /**
     * Short, own transaction for the delivery-outcome audit event (issue #436).
     *
     * <p>The sweep runs on a scheduler thread <em>without</em> an ambient
     * transaction, and the module outbox only accepts appends inside a
     * transaction bound to the module DataSource
     * ({@code AuditOutboxJdbcStore.append}) — without one, every
     * {@code prosthetics.notification.delivery.outcome} event was dropped with
     * an {@code IllegalStateException}, so the Audit console showed the queue
     * but never the delivery.
     *
     * <p>Deliberately the module's own manager, not the chained one: one audit
     * INSERT must not reserve a connection in all four pools, and
     * {@code REQUIRES_NEW} on the chained manager is a documented trap
     * (suspends four legs, begins four more — see
     * {@code MultiTransactionConfig}).
     */
    private final TransactionTemplate auditTransactionTemplate;

    public BrakNotificationDeliveryService(
            BrakNotificationOutboxRepository outboxRepository,
            BrakEventRepository brakEventRepository,
            FlowInstanceRepository instanceRepository,
            ProstheticsOrderRepository orderRepository,
            UserRepository userRepository,
            AuditService auditService,
            DomainAuditEmitter auditEmitter,
            TemplateSnapshotParser snapshotParser,
            BrakNotificationComposer composer,
            BrakNotificationService notificationService,
            @Qualifier("prosthTransactionManager") PlatformTransactionManager prostheticsTransactionManager) {
        this.outboxRepository = outboxRepository;
        this.brakEventRepository = brakEventRepository;
        this.instanceRepository = instanceRepository;
        this.orderRepository = orderRepository;
        this.userRepository = userRepository;
        this.auditService = auditService;
        this.auditEmitter = auditEmitter;
        this.snapshotParser = snapshotParser;
        this.composer = composer;
        this.notificationService = notificationService;
        this.auditTransactionTemplate = new TransactionTemplate(prostheticsTransactionManager);
    }

    @Value("${app.prosthetics.brak-notification.enabled:true}")
    private boolean enabled;

    @Value("${app.prosthetics.brak-notification.frontend-base-url:}")
    private String frontendBaseUrl;

    @Value("${app.prosthetics.brak-notification.outbox-max-attempts:5}")
    private int maxAttempts;

    @Value("${app.prosthetics.brak-notification.outbox-batch-size:20}")
    private int batchSize;

    /**
     * Delivers one queued notification of the given kind, claiming its row
     * with a write lock so a concurrent deliverer (eager listener vs sweep)
     * sees the terminal state and skips instead of double-sending. The kind
     * is part of the claim key: since epic #322 a stage-6 brak that pushed
     * its order chain to count {@code >= 3} owns both a {@code SINGLE} and a
     * {@code THRESHOLD} row for the same event. Never throws for mail
     * problems.
     */
    public void deliver(UUID brakEventId, Long actorId, BrakNotificationKind kind) {
        if (!enabled) {
            log.info("Brak email delivery disabled, keeping it queued brakEventId={}",
                    brakEventId);
            auditService.logEvent(NOTIFICATION_ENTITY, brakEventId,
                    ACTION_SKIPPED_DISABLED, actorId, null, null);
            emitDeliveryOutcome(brakEventId, kind, actorId,
                    AuditEvent.AuditOutcome.CANCELLED, "BRAK_NOTIFICATION_SKIPPED_DISABLED", null);
            return;
        }
        BrakNotificationOutbox row = claim(brakEventId, kind, actorId);
        if (row == null) {
            return;
        }
        BrakEvent brakEvent = brakEventRepository.findById(brakEventId).orElse(null);
        if (brakEvent == null) {
            // Cannot happen (same-transaction enqueue), but fail closed and
            // terminal so the sweep does not spin on it forever.
            row.setStatus(BrakNotificationStatus.SKIPPED);
            row.setLeaseUntil(null);
            row.setClaimedBy(null);
            outboxRepository.save(row);
            log.warn("Brak email skipped, BrakEvent not found brakEventId={}", brakEventId);
            return;
        }
        FlowInstance instance = instanceRepository.findById(brakEvent.getInstanceId())
                .orElse(null);
        if (instance == null) {
            row.setStatus(BrakNotificationStatus.SKIPPED);
            row.setLeaseUntil(null);
            row.setClaimedBy(null);
            outboxRepository.save(row);
            log.warn("Brak email skipped, FlowInstance not found brakEventId={} instanceId={}",
                    brakEventId, brakEvent.getInstanceId());
            auditService.logEvent(NOTIFICATION_ENTITY, brakEventId,
                    ACTION_SKIPPED_INSTANCE_NOT_FOUND, actorId, null, null);
            emitDeliveryOutcome(brakEventId, kind, actorId,
                    AuditEvent.AuditOutcome.CANCELLED, "BRAK_NOTIFICATION_SKIPPED_INSTANCE_NOT_FOUND",
                    null);
            return;
        }
        List<User> recipients = notificationService.resolveRecipients();
        if (recipients.isEmpty()) {
            row.setStatus(BrakNotificationStatus.SKIPPED);
            row.setLeaseUntil(null);
            row.setClaimedBy(null);
            outboxRepository.save(row);
            log.warn("Brak email skipped, no prosthetics administrators with an address "
                    + "brakEventId={}", brakEventId);
            auditService.logEvent(NOTIFICATION_ENTITY, brakEventId,
                    ACTION_SKIPPED_NO_RECIPIENTS, actorId, null, null);
            java.util.Map<String, Object> noRecipientMetadata = new java.util.TreeMap<>();
            noRecipientMetadata.put("recipientCount", 0);
            noRecipientMetadata.put("outcomeCode", BrakNotificationStatus.SKIPPED.name());
            emitDeliveryOutcome(brakEventId, kind, actorId,
                    AuditEvent.AuditOutcome.CANCELLED, "BRAK_NOTIFICATION_SKIPPED_NO_RECIPIENTS",
                    noRecipientMetadata);
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

        String subject;
        String body;
        if (kind == BrakNotificationKind.THRESHOLD) {
            java.time.LocalDateTime cutoff = brakEvent.getCreatedAt();
            long brakCount = cutoff == null
                    ? brakEventRepository.countByOrderId(instance.getOrderId())
                    : brakEventRepository.countByOrderIdUpTo(instance.getOrderId(), cutoff);
            List<BrakHistoryEntry> history = collectHistory(instance.getOrderId(), snapshot, cutoff);
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
                    frontendBaseUrl,
                    brakCount,
                    history);
            subject = composer.buildThresholdSubject(data);
            body = composer.buildThresholdBody(data);
        } else {
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
                    frontendBaseUrl,
                    1L,
                    List.of());
            subject = composer.buildSubject(data);
            body = composer.buildBody(data);
        }
        int[] result = notificationService.sendToRecipients(
                subject, body, recipients,
                brakEventId, kind);

        int attempts = row.getAttempts() + 1;
        String outcome = (kind == BrakNotificationKind.THRESHOLD ? "kind=THRESHOLD " : "")
                + "sent:" + result[0] + " failed:" + result[1];
        BrakNotificationStatus finalStatus;
        if (result[1] == 0) {
            finalStatus = BrakNotificationStatus.SENT;
        } else if (attempts >= maxAttempts) {
            finalStatus = BrakNotificationStatus.DEAD;
            log.error("Brak email dead, attempts exhausted brakEventId={} attempts={} {}",
                    brakEventId, attempts, outcome);
        } else {
            finalStatus = BrakNotificationStatus.FAILED;
        }
        outboxRepository.recordDelivery(row.getId(), finalStatus, attempts,
                result[1] == 0 ? null : outcome, BrakNotificationStatus.PROCESSING);
        row.setStatus(finalStatus);
        row.setAttempts(attempts);
        row.setLastError(result[1] == 0 ? null : outcome);
        auditService.logEvent(NOTIFICATION_ENTITY, brakEventId,
                result[1] == 0 ? ACTION_SENT : ACTION_FAILED, actorId, null, outcome);
        java.util.Map<String, Object> deliveryMetadata = new java.util.TreeMap<>();
        deliveryMetadata.put("recipientCount", recipients.size());
        deliveryMetadata.put("outcomeCode", finalStatus.name());
        deliveryMetadata.put("attempt", attempts);
        emitDeliveryOutcome(brakEventId, kind, actorId,
                result[1] == 0 ? AuditEvent.AuditOutcome.SUCCESS : AuditEvent.AuditOutcome.FAILURE,
                row.getStatus() == BrakNotificationStatus.DEAD ? "BRAK_NOTIFICATION_DEAD" : null,
                deliveryMetadata);
        if (result[1] > 0 && row.getStatus() != BrakNotificationStatus.DEAD) {
            log.error("Brak email completed with failures brakEventId={} sent={} failed={}",
                    brakEventId, result[0], result[1]);
        }
    }

    /**
     * Records the delivery outcome in the canonical audit trail.
     *
     * <p>The emit runs inside a short, own transaction on the module manager:
     * the sweep has no ambient transaction, and the module outbox rejects
     * appends outside one (issue #436 — every delivery-outcome event was
     * silently dropped). The transaction holds nothing but the audit INSERT:
     * the SMTP call already happened, so no external I/O runs inside it.
     *
     * <p>Fail-safe: an audit failure must never re-classify an already
     * delivered notification, so it is logged and swallowed here.
     *
     * @param extraMetadata delivery facts for the audit trail (recipient count,
     *                      subject, SMTP outcome) — may be {@code null}
     */
    private void emitDeliveryOutcome(UUID brakEventId, BrakNotificationKind kind, Long actorId,
            AuditEvent.AuditOutcome outcome, String errorCode,
            java.util.Map<String, Object> extraMetadata) {
        final UUID deliveryEventId = brakEventId;
        try {
            auditTransactionTemplate.executeWithoutResult(status -> auditEmitter.emit("prosthetics", () -> {
                AuditEvent.AuditActor initiatedBy = actorId == null ? null
                        : new AuditEvent.AuditActor(AuditEvent.ActorType.USER,
                                actorId.toString(), null, null, java.util.Set.of(), null, null);
                java.util.Map<String, Object> metadata = new java.util.TreeMap<>();
                metadata.put("deliveryKind", kind == null ? "UNKNOWN" : kind.name());
                if (extraMetadata != null) {
                    metadata.putAll(extraMetadata);
                }
                return AuditEvent.builder()
                        .actor(new AuditEvent.AuditActor(AuditEvent.ActorType.SERVICE,
                                "brak-notification-sweep", null, null, java.util.Set.of(),
                                initiatedBy, null))
                        .eventClass(EventClass.BUSINESS)
                        .module("prosthetics")
                        .functionalArea("notification")
                        .action("prosthetics.notification.delivery.outcome")
                        .actionType(ActionType.DELIVERY)
                        .target(new AuditEvent.AuditTarget("BrakEvent", deliveryEventId.toString(), null))
                        .outcome(outcome)
                        .errorCode(errorCode)
                        .changes(java.util.List.of(com.superhumans.audit.AuditChanges.fieldChanged(
                                "deliveryStatus", DataClass.IDENTIFIER)))
                        .metadata(metadata)
                        .source(AuditEvent.AuditSource.SCHEDULED_JOB)
                        .build();
            }));
        } catch (RuntimeException ex) {
            log.warn("Brak delivery outcome not recorded in the audit trail brakEventId={} kind={}: {}",
                    brakEventId, kind, ex.getMessage());
        }
    }

    /**
     * Retry sweep over deliverable rows, oldest first. Fixed-delay (never
     * overlapping itself); one bad row never aborts the sweep.
     */
    @Scheduled(fixedDelayString = "${app.prosthetics.brak-notification.outbox-poll-ms:60000}")
    public void sweep() {
        if (!enabled) {
            log.debug("Brak email sweep skipped (disabled)");
            return;
        }
        List<BrakNotificationOutbox> due = outboxRepository
                .findDueForDelivery(
                        List.of(BrakNotificationStatus.PENDING, BrakNotificationStatus.FAILED),
                        BrakNotificationStatus.PROCESSING,
                        maxAttempts,
                        PageRequest.of(0, batchSize));
        for (BrakNotificationOutbox row : due) {
            try {
                deliver(row.getBrakEventId(), row.getCreatedBy(), row.getKind());
            } catch (RuntimeException ex) {
                log.error("Brak email sweep failed for row brakEventId={}: {}",
                        row.getBrakEventId(), ex.getMessage());
            }
        }
    }

    private BrakNotificationOutbox claim(UUID brakEventId, BrakNotificationKind kind, Long actorId) {
        BrakNotificationOutbox row = outboxRepository
                .findByBrakEventIdAndKindForUpdate(brakEventId, kind)
                .orElse(null);
        if (row == null) {
            log.warn("Brak email skipped, no outbox row brakEventId={} kind={}", brakEventId, kind);
            return null;
        }
        if (row.getStatus() == BrakNotificationStatus.SENT
                || row.getStatus() == BrakNotificationStatus.SKIPPED
                || row.getStatus() == BrakNotificationStatus.DEAD) {
            log.info("Brak email already terminal ({}), skipping repeat brakEventId={}",
                    row.getStatus(), brakEventId);
            return null;
        }
        java.time.LocalDateTime now = java.time.LocalDateTime.now();
        if (row.getStatus() == BrakNotificationStatus.PROCESSING
                && row.getLeaseUntil() != null && row.getLeaseUntil().isAfter(now)) {
            return null;
        }
        if (row.getStatus() == BrakNotificationStatus.FAILED
                && row.getAttempts() >= maxAttempts) {
            row.setStatus(BrakNotificationStatus.DEAD);
            row.setLastError("attempts exhausted (" + row.getAttempts() + ")");
            outboxRepository.save(row);
            return null;
        }
        String claimedBy = "brak-notification-" + UUID.randomUUID();
        int claimed = outboxRepository.claimForDelivery(row.getId(),
                BrakNotificationStatus.PROCESSING,
                List.of(BrakNotificationStatus.PENDING, BrakNotificationStatus.FAILED),
                now, now.plusMinutes(5), claimedBy);
        if (claimed == 0) {
            return null;
        }
        row.setStatus(BrakNotificationStatus.PROCESSING);
        row.setLeaseUntil(now.plusMinutes(5));
        row.setClaimedBy(claimedBy);
        return row;
    }

    /**
     * Collects the order-wide brak history (epic #322), oldest first: every
     * brak event of every instance in the order chain, with stage/step labels
     * resolved from the template snapshot and confirmer names resolved from
     * the users table. Missing values stay {@code null} — the composer
     * renders them as {@code «—»}.
     *
     * <p>When {@code cutoff} is non-null, only events confirmed no later than
     * the triggering event are included, so a delayed delivery never
     * backfills later braks into an earlier escalation email.
     */
    private List<BrakHistoryEntry> collectHistory(UUID orderId, SnapshotTemplate snapshot,
            java.time.LocalDateTime cutoff) {
        List<FlowInstance> chain = new ArrayList<>(instanceRepository.findByOrderId(orderId));
        chain.sort(Comparator.comparing(FlowInstance::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder())));
        List<BrakHistoryEntry> history = new ArrayList<>();
        for (FlowInstance chainInstance : chain) {
            List<BrakEvent> events = new ArrayList<>(
                    brakEventRepository.findByInstanceId(chainInstance.getId()));
            events.sort(Comparator.comparing(BrakEvent::getCreatedAt,
                    Comparator.nullsLast(Comparator.naturalOrder())));
            for (BrakEvent event : events) {
                if (cutoff != null && event.getCreatedAt() != null
                        && event.getCreatedAt().isAfter(cutoff)) {
                    continue;
                }
                User eventConfirmer = event.getCreatedBy() == null ? null
                        : userRepository.findById(event.getCreatedBy()).orElse(null);
                history.add(new BrakHistoryEntry(
                        stageLabel(snapshot, event),
                        stepLabel(snapshot, event),
                        event.getCreatedAt(),
                        eventConfirmer == null ? null : eventConfirmer.getFullName(),
                        returnStageName(snapshot, event)));
            }
        }
        return history;
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
