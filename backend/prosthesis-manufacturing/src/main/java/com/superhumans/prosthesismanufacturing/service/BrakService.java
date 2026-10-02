package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.exception.BadRequestException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.BrakCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.BrakEventResponse;
import com.superhumans.prosthesismanufacturing.dto.BranchResponse;
import com.superhumans.prosthesismanufacturing.dto.FlowInstanceResponse;
import com.superhumans.prosthesismanufacturing.entity.BrakEvent;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationOutbox;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationStatus;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.StepExecution;
import com.superhumans.prosthesismanufacturing.entity.StepExecutionStatus;
import com.superhumans.prosthesismanufacturing.mapper.FlowInstanceMapper;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.BrakNotificationOutboxRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.StepExecutionRepository;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStage;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActionDefinition.DataClass;
import com.superhumans.audit.AuditActionDefinition.EventClass;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditChanges;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.DomainAuditEmitter;
import com.superhumans.service.AuditService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class BrakService {

    public static final UUID STAGE_D12 = UUID.fromString("d0000012-0000-0000-0000-000000000012");
    public static final UUID STAGE_D13 = UUID.fromString("d0000013-0000-0000-0000-000000000013");
    public static final UUID STAGE_D14 = UUID.fromString("d0000014-0000-0000-0000-000000000014");
    public static final UUID STAGE_D17 = UUID.fromString("d0000017-0000-0000-0000-000000000017");
    public static final UUID STEP_E0000028 = UUID.fromString("e0000028-0000-0000-0000-000000000028");
    public static final UUID STAGE_D20 = UUID.fromString("d0000020-0000-0000-0000-000000000020");
    public static final UUID STEP_E0000032 = UUID.fromString("e0000032-0000-0000-0000-000000000032");

    public static final Set<UUID> ALLOWED_RETURN_STAGE_IDS = Set.of(STAGE_D12, STAGE_D13, STAGE_D14);

    static boolean isBrakTrigger(UUID stageId, UUID stepId) {
        return (STAGE_D17.equals(stageId) && STEP_E0000028.equals(stepId))
                || (STAGE_D20.equals(stageId) && STEP_E0000032.equals(stepId));
    }

    FlowInstanceRepository instanceRepository;
    BrakEventRepository brakEventRepository;
    StepExecutionRepository executionRepository;
    FlowInstanceMapper instanceMapper;
    TemplateSnapshotParser snapshotParser;
    AuditService auditService;
    DomainAuditEmitter auditEmitter;
    ObjectMapper objectMapper;
    BrakNotificationOutboxRepository outboxRepository;
    BrakThresholdService thresholdService;

    @Transactional
    public BranchResponse createBrakAndBranch(UUID instanceId, BrakCreateRequest request, Long userId) {
        FlowInstance instance = instanceRepository.findByIdForUpdate(instanceId)
                .orElseThrow(() -> new NotFoundException("Instance not found: " + instanceId));
        if (!instance.getAssignedUserId().equals(userId)) {
            throw new NotFoundException("Instance not found: " + instanceId);
        }
        if (instance.getStatus() != FlowInstanceStatus.IN_PROGRESS) {
            throw new BadRequestException("Брак можливий лише під час виконання");
        }
        if (!isBrakTrigger(instance.getCurrentStageId(), instance.getCurrentStepId())) {
            throw new BadRequestException("Брак доступний лише на кроці 1 етапу 6 або 9");
        }
        if (request.returnStageId() == null || !ALLOWED_RETURN_STAGE_IDS.contains(request.returnStageId())) {
            throw new BadRequestException("Недозволений етап повернення. Дозволені: d0000012, d0000013, d0000014");
        }
        String snapshotJson = instance.getTemplateSnapshot();
        SnapshotTemplate snapshot = snapshotParser.parse(snapshotJson);
        SnapshotStage returnStage = snapshot.getStages().stream()
                .filter(s -> s.getId().equals(request.returnStageId()))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Stage is missing from the template snapshot"));
        SnapshotStep firstStep = returnStage.getSteps().stream()
                .min(Comparator.comparingInt(s -> indexOf(returnStage.getSteps(), s.getId())))
                .orElseThrow(() -> new BadRequestException("Stage " + returnStage.getName() + " has no steps"));

        String note = request.note() == null ? null : request.note().trim();
        if (note != null && note.isEmpty()) {
            note = null;
        }
        if (note != null && note.length() > 1000) {
            throw new BadRequestException("Note must not exceed 1000 characters");
        }

        LocalDateTime now = LocalDateTime.now();

        BrakEvent event = BrakEvent.builder()
                .instanceId(instance.getId())
                .stageId(instance.getCurrentStageId())
                .stepId(instance.getCurrentStepId())
                .softTissueMisalignment(request.softTissueMisalignment())
                .painDiscomfort(request.painDiscomfort())
                .note(note)
                .returnStageId(request.returnStageId())
                .build();
        brakEventRepository.save(event);
        auditService.logAction("BrakEvent", event.getId(), "CREATE", userId);

        // update old branch to BRANCHED
        instance.setStatus(FlowInstanceStatus.BRANCHED);
        instance.setOriginStageId(instance.getCurrentStageId());
        instance.setOriginStepId(instance.getCurrentStepId());
        instance.setEndTime(now);
        // defect_payload as JSON for audit/history
        try {
            Map<String, Object> payload = new LinkedHashMap<>();
            payload.put("softTissueMisalignment", request.softTissueMisalignment());
            payload.put("painDiscomfort", request.painDiscomfort());
            payload.put("note", note);
            payload.put("returnStageId", request.returnStageId().toString());
            instance.setDefectPayload(objectMapper.writeValueAsString(payload));
        } catch (Exception ignored) {
        }
        instanceRepository.save(instance);
        auditService.logAction("FlowInstance", instance.getId(), "BRANCH", userId);

        int nextSequence = instanceRepository.findByOrderId(instance.getOrderId()).stream()
                .map(FlowInstance::getBranchSequence)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(1) + 1;

        FlowInstance branch = FlowInstance.builder()
                .templateId(instance.getTemplateId())
                .patientId(instance.getPatientId())
                .orderId(instance.getOrderId())
                .assignedUserId(userId)
                .status(FlowInstanceStatus.IN_PROGRESS)
                .currentStageId(returnStage.getId())
                .currentStepId(firstStep.getId())
                .parentInstanceId(instance.getId())
                .branchSequence(nextSequence)
                .originStageId(instance.getCurrentStageId())
                .originStepId(instance.getCurrentStepId())
                .totalActiveSeconds(0L)
                .totalIdleSeconds(0L)
                .startTime(now)
                .resumedAt(now)
                .templateSnapshot(instance.getTemplateSnapshot())
                .build();
        instanceRepository.save(branch);

        // create first execution for target stage
        StepExecution execution = StepExecution.builder()
                .instance(branch)
                .stageId(returnStage.getId())
                .stepId(firstStep.getId())
                .attemptNumber(1)
                .status(StepExecutionStatus.IN_PROGRESS)
                .startedAt(now)
                .activeSeconds(0L)
                .build();
        executionRepository.save(execution);
        auditService.logAction("FlowInstance", branch.getId(), "CREATE_BRANCH", userId);

        event.setNewInstanceId(branch.getId());
        brakEventRepository.save(event);

        // Outbox row in the SAME transaction as the brak (atomic: no brak
        // without a queued notification and vice versa). Only stage-6 braks
        // are queued — stage 9 is handled without email by design.
        // NOTE: deliberately same-tx, not REQUIRES_NEW (a surviving row for a
        // rolled-back brak would be a ghost notification).
        if (STAGE_D17.equals(instance.getCurrentStageId())) {
            BrakNotificationOutbox outbox = BrakNotificationOutbox.builder()
                    .brakEventId(event.getId())
                    .status(BrakNotificationStatus.PENDING)
                    .attempts(0)
                    .build();
            outbox.setCreatedBy(userId);
            outbox.setUpdatedBy(userId);
            outboxRepository.save(outbox);
        }

        // Delivery is handled exclusively by the scheduled outbox sweep
        // (BrakNotificationDeliveryService.sweep): no eager delivery here, because
        // @TransactionalEventListener(AFTER_COMMIT) fires per-leg mid-chain under
        // the ChainedTransactionManager, without a usable transaction context
        // (issue #321: HeuristicCompletion with mixed outcome).
        //
        // Order-wide escalation (epic #322): a THRESHOLD row is queued in this
        // same transaction whenever the order chain reached brak count >= 3.
        thresholdService.maybeEnqueue(instance.getOrderId(), event.getId(), userId);
        final UUID brakEventId = event.getId();
        final UUID originalInstanceId = instance.getId();
        final UUID newBranchId = branch.getId();
        final UUID orderId = instance.getOrderId();
        UUID rootId = UUID.randomUUID();
        try (var ignored = auditEmitter.beginOperation(rootId)) {
            auditEmitter.emit("prosthetics", () -> AuditEvent.builder()
                    .actor(AuditActorResolver.fromCurrentContext())
                    .eventClass(EventClass.BUSINESS)
                    .module("prosthetics")
                    .functionalArea("brak")
                    .action("prosthetics.brak.branch")
                    .actionType(ActionType.BRANCH)
                    .target(new AuditEvent.AuditTarget("FlowInstance", newBranchId.toString(), null))
                    .parentTarget(new AuditEvent.AuditTarget(
                            "FlowInstance", originalInstanceId.toString(), null))
                    .outcome(AuditEvent.AuditOutcome.SUCCESS)
                    .changes(List.of(AuditChanges.statusChanged("IN_PROGRESS", "BRANCHED")))
                    .source(AuditEvent.AuditSource.API)
                    .build());
            auditEmitter.emit("prosthetics", () -> AuditEvent.builder()
                    .actor(AuditActorResolver.fromCurrentContext())
                    .eventClass(EventClass.BUSINESS)
                    .module("prosthetics")
                    .functionalArea("notification")
                    .action("prosthetics.notification.queue")
                    .actionType(ActionType.QUEUE)
                    .target(new AuditEvent.AuditTarget("BrakEvent", brakEventId.toString(), null))
                    .parentTarget(new AuditEvent.AuditTarget(
                            "ProstheticsOrder", orderId.toString(), null))
                    .outcome(AuditEvent.AuditOutcome.SUCCESS)
                    .source(AuditEvent.AuditSource.API)
                    .build());
            auditEmitter.emit("prosthetics", () -> AuditEvent.builder()
                    .auditId(rootId)
                    .actor(AuditActorResolver.fromCurrentContext())
                    .eventClass(EventClass.BUSINESS)
                    .module("prosthetics")
                    .functionalArea("brak")
                    .action("prosthetics.brak.confirm")
                    .actionType(ActionType.BRAK_CONFIRM)
                    .target(new AuditEvent.AuditTarget("BrakEvent", brakEventId.toString(), null))
                    .parentTarget(new AuditEvent.AuditTarget(
                            "FlowInstance", originalInstanceId.toString(), null))
                    .relatedEntities(List.of(new AuditEvent.AuditTarget(
                            "FlowInstance", newBranchId.toString(), null)))
                    .outcome(AuditEvent.AuditOutcome.SUCCESS)
                    .changes(List.of(
                            new AuditEvent.AuditChange("returnStageId", AuditEvent.ChangeType.SET,
                                    DataClass.IDENTIFIER, null, request.returnStageId().toString()),
                            AuditChanges.fieldChanged("brakReason", DataClass.NARRATIVE)))
                    .source(AuditEvent.AuditSource.API)
                    .build());
        }
        return BranchResponse.builder()
                .brakEventId(event.getId())
                .originalInstanceId(instance.getId())
                .newInstanceId(branch.getId())
                .returnStageId(returnStage.getId())
                .returnStageName(returnStage.getName())
                .newStatus(branch.getStatus().name())
                .build();
    }

    @Transactional(readOnly = true)
    public List<BrakEventResponse> listBrakEvents(UUID instanceId, Long userId, boolean allowAll) {
        requireOwner(instanceId, userId, allowAll);
        SnapshotTemplate snapshot = null;
        try {
            String json = instanceRepository.findById(instanceId)
                    .map(FlowInstance::getTemplateSnapshot).orElse(null);
            if (json != null) {
                snapshot = snapshotParser.parse(json);
            }
        } catch (Exception ignored) {
        }
        Map<UUID, String> stageNames = new LinkedHashMap<>();
        if (snapshot != null) {
            for (SnapshotStage s : snapshot.getStages()) {
                stageNames.put(s.getId(), s.getName());
            }
        }
        List<BrakEvent> events = brakEventRepository.findByInstanceId(instanceId);
        // also include events where this instance is the child? For history we show parent's events
        // but current spec expects GET /instances/{id}/brak-events returns events where instance_id == id
        Map<UUID, String> finalStageNames = stageNames;
        return events.stream()
                .sorted(Comparator.comparing(BrakEvent::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(e -> toResponse(e, finalStageNames))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FlowInstanceResponse> listBranches(UUID instanceId, Long userId, boolean allowAll) {
        requireOwner(instanceId, userId, allowAll);
        return instanceRepository.findByParentInstanceId(instanceId).stream()
                .sorted(Comparator.comparing(FlowInstance::getCreatedAt))
                .map(this::toInstanceResponse)
                .toList();
    }

    private FlowInstanceResponse toInstanceResponse(FlowInstance instance) {
        FlowInstanceResponse response = instanceMapper.toResponse(instance);
        if (response == null) {
            return null;
        }
        // currentExecutionId enrichment similar to FlowInstanceService.toResponse
        UUID stepId = instance.getCurrentStepId();
        if (stepId != null) {
            executionRepository.findByInstanceIdAndStepId(instance.getId(), stepId).stream()
                    .filter(e -> e.getStatus() == StepExecutionStatus.IN_PROGRESS)
                    .findFirst()
                    .ifPresent(e -> response.setCurrentExecutionId(e.getId()));
        }
        return response;
    }

    private BrakEventResponse toResponse(BrakEvent e, Map<UUID, String> stageNames) {
        return BrakEventResponse.builder()
                .id(e.getId())
                .instanceId(e.getInstanceId())
                .stageId(e.getStageId())
                .stepId(e.getStepId())
                .softTissueMisalignment(e.getSoftTissueMisalignment())
                .painDiscomfort(e.getPainDiscomfort())
                .note(e.getNote())
                .returnStageId(e.getReturnStageId())
                .returnStageName(stageNames.get(e.getReturnStageId()))
                .newInstanceId(e.getNewInstanceId())
                .createdBy(e.getCreatedBy())
                .createdAt(e.getCreatedAt())
                .build();
    }

    private void requireOwner(UUID instanceId, Long userId, boolean allowAll) {
        FlowInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException("Instance not found: " + instanceId));
        if (allowAll || instance.getAssignedUserId().equals(userId)) {
            return;
        }
        throw new NotFoundException("Instance not found: " + instanceId);
    }

    private int indexOf(List<SnapshotStep> steps, UUID stepId) {
        for (int i = 0; i < steps.size(); i++) {
            if (steps.get(i).getId().equals(stepId)) {
                return i;
            }
        }
        return Integer.MAX_VALUE;
    }
}
