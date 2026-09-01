package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.exception.BadRequestException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.FlowInstanceResponse;
import com.superhumans.prosthesismanufacturing.dto.FailureSnapshotResponse;
import com.superhumans.prosthesismanufacturing.dto.GateDecisionResponse;
import com.superhumans.prosthesismanufacturing.dto.InstanceCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.PauseRequest;
import com.superhumans.prosthesismanufacturing.dto.ResourceUsageRequest;
import com.superhumans.prosthesismanufacturing.dto.ResourceUsageResponse;
import com.superhumans.prosthesismanufacturing.dto.StepCompleteRequest;
import com.superhumans.prosthesismanufacturing.dto.StepExecutionResponse;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.GateDecision;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ResourceUsage;
import com.superhumans.prosthesismanufacturing.entity.StepExecution;
import com.superhumans.prosthesismanufacturing.entity.StepExecutionStatus;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import com.superhumans.prosthesismanufacturing.mapper.FlowInstanceMapper;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowTemplateRepository;
import com.superhumans.prosthesismanufacturing.repository.GateDecisionRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.ResourceUsageRepository;
import com.superhumans.prosthesismanufacturing.repository.StepExecutionRepository;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotElement;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStage;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import com.superhumans.service.AuditService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FlowInstanceService {

    /** Transition rule for MEASUREMENT steps: at least this many filled values to proceed. */
    private static final int MIN_MEASUREMENT_VALUES = 3;

    FlowInstanceRepository instanceRepository;
    FlowTemplateRepository templateRepository;
    ProstheticsOrderRepository orderRepository;
    StepExecutionRepository executionRepository;
    ResourceUsageRepository resourceUsageRepository;
    GateDecisionRepository decisionRepository;
    FlowInstanceMapper instanceMapper;
    FlowTemplateService templateService;
    FailureSnapshotService failureSnapshotService;
    ProstheticsPdfService pdfService;
    AuditService auditService;
    TemplateSnapshotParser snapshotParser;
    ObjectMapper objectMapper;

    @Transactional
    public FlowInstanceResponse create(InstanceCreateRequest request, Long userId) {
        ProstheticsOrder order = orderRepository.findById(request.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + request.getOrderId()));
        var template = templateRepository.findById(request.getTemplateId())
                .orElseThrow(() -> new NotFoundException("Template not found: " + request.getTemplateId()));
        if (template.getStatus() != TemplateStatus.ACTIVE) {
            throw new BadRequestException("Only active templates can be used for new instances");
        }
        boolean duplicate = instanceRepository.findByOrderId(request.getOrderId()).stream()
                .anyMatch(i -> i.getStatus() != FlowInstanceStatus.COMPLETED
                        && i.getStatus() != FlowInstanceStatus.FAILED
                        && i.getStatus() != FlowInstanceStatus.FAILED_QC);
        if (duplicate) {
            throw new BadRequestException("An active instance already exists for this order");
        }

        FlowInstance instance = FlowInstance.builder()
                .templateId(request.getTemplateId())
                .patientId(order.getPatient().getId())
                .orderId(request.getOrderId())
                .assignedUserId(userId)
                .status(FlowInstanceStatus.NEW)
                .totalActiveSeconds(0L)
                .totalIdleSeconds(0L)
                .reworkCount(0)
                .templateSnapshot(templateService.createSnapshot(request.getTemplateId()))
                .build();
        instanceRepository.save(instance);
        auditService.logAction("FlowInstance", instance.getId(), "CREATE", userId);
        return toResponse(instance);
    }

    public FlowInstanceResponse toResponse(FlowInstance instance) {
        FlowInstanceResponse response = instanceMapper.toResponse(instance);
        if (response == null) {
            return null;
        }
        UUID stepId = instance.getCurrentStepId();
        if (stepId != null) {
            executionRepository.findByInstanceIdAndStepId(instance.getId(), stepId).stream()
                    .filter(e -> e.getStatus() == StepExecutionStatus.IN_PROGRESS)
                    .findFirst()
                    .ifPresent(e -> response.setCurrentExecutionId(e.getId()));
        }
        templateRepository.findById(instance.getTemplateId())
                .ifPresent(t -> response.setTemplateName(t.getName()));
        orderRepository.findById(instance.getOrderId())
                .ifPresent(o -> {
                    response.setOrderNumber(o.getOrderNumber());
                    response.setPatientPib(resolvePatientPib(o));
                });
        // Surface values from completed steps so the wizard can render read-only
        // summaries of earlier steps (e.g. the measurement form carried forward
        // to «Перевірка якості гіпсового позитива»).
        Map<UUID, String> priorValues = new LinkedHashMap<>();
        executionRepository.findByInstanceId(instance.getId()).stream()
                .filter(e -> e.getStatus() == StepExecutionStatus.COMPLETED && StringUtils.hasText(e.getValues()))
                .forEach(e -> priorValues.put(e.getStepId(), e.getValues()));
        if (!priorValues.isEmpty()) {
            response.setPriorStepValues(priorValues);
        }
        if (stepId != null && StringUtils.hasText(instance.getTemplateSnapshot())) {
            try {
                SnapshotTemplate snapshot = snapshotParser.parse(instance.getTemplateSnapshot());
                SnapshotStage currentStage = findStage(snapshot, instance.getCurrentStageId());
                response.setCurrentStageName(currentStage.getName());
                SnapshotStep currentStep = findStep(currentStage, stepId);
                response.setCurrentStepName(currentStep.getName());
            } catch (BadRequestException ignored) {
                // stage/step may be absent for NEW / terminal statuses
            }
        }
        return response;
    }

    @Transactional
    public FlowInstanceResponse start(UUID instanceId, Long userId) {
        FlowInstance instance = instanceRepository.findByIdForUpdate(instanceId)
                .orElseThrow(() -> new NotFoundException("Instance not found: " + instanceId));
        if (instance.getStatus() != FlowInstanceStatus.NEW) {
            if (instance.getStatus() == FlowInstanceStatus.IN_PROGRESS) {
                // Idempotent start: a concurrent request already started the instance
                return toResponse(instance);
            }
            throw new BadRequestException("Instance can be started only from NEW status");
        }
        SnapshotTemplate snapshot = snapshotParser.parse(instance.getTemplateSnapshot());
        SnapshotStage firstStage = snapshot.getStages().stream()
                .min(Comparator.comparingInt(stageIndex(snapshot)))
                .orElseThrow(() -> new BadRequestException("Template has no stages"));
        SnapshotStep firstStep = firstStage.getSteps().stream()
                .min(Comparator.comparingInt(stepIndex(firstStage)))
                .orElseThrow(() -> new BadRequestException("Stage " + firstStage.getName() + " has no steps"));

        LocalDateTime now = LocalDateTime.now();
        instance.setStatus(FlowInstanceStatus.IN_PROGRESS);
        instance.setCurrentStageId(firstStage.getId());
        instance.setCurrentStepId(firstStep.getId());
        instance.setStartTime(now);
        instance.setResumedAt(now);
        instanceRepository.save(instance);

        createExecution(instance, firstStage.getId(), firstStep.getId(),
                nextAttemptNumber(instance, firstStep.getId()), now);
        auditService.logAction("FlowInstance", instance.getId(), "START", userId);
        return toResponse(instance);
    }

    @Transactional
    public FlowInstanceResponse completeStep(UUID instanceId, UUID executionId, StepCompleteRequest request,
                                             Long userId) {
        FlowInstance instance = requireOwner(instanceId, userId);
        if (instance.getStatus() != FlowInstanceStatus.IN_PROGRESS) {
            throw new BadRequestException("Step can be completed only while the instance is in progress");
        }
        StepExecution execution = executionRepository.findById(executionId)
                .orElseThrow(() -> new NotFoundException("Execution not found: " + executionId));
        if (!instanceId.equals(execution.getInstance().getId())) {
            throw new BadRequestException("Execution does not belong to this instance");
        }
        if (!instance.getCurrentStepId().equals(execution.getStepId())) {
            throw new BadRequestException("Only the current step can be completed");
        }
        if (execution.getStatus() != StepExecutionStatus.IN_PROGRESS) {
            throw new BadRequestException("Execution is not in progress");
        }

        SnapshotTemplate snapshot = snapshotParser.parse(instance.getTemplateSnapshot());
        SnapshotStage stage = findStage(snapshot, instance.getCurrentStageId());
        SnapshotStep step = findStep(stage, execution.getStepId());
        validateValues(request.getValues(), step);

        LocalDateTime now = LocalDateTime.now();
        long activeDelta = secondsSince(execution.getStartedAt(), instance.getResumedAt(), now);
        execution.setValues(request.getValues());
        execution.setStatus(StepExecutionStatus.COMPLETED);
        execution.setCompletedAt(now);
        execution.setCompletedBy(userId);
        execution.setActiveSeconds((execution.getActiveSeconds() == null ? 0L : execution.getActiveSeconds())
                + activeDelta);
        executionRepository.save(execution);

        instance.setTotalActiveSeconds(instance.getTotalActiveSeconds() + activeDelta);
        instanceRepository.save(instance);

        saveResources(instance, execution, request.getResources(), userId);

        advance(instance, snapshot, stage, step, now, userId);
        auditService.logAction("StepExecution", execution.getId(), "COMPLETE", userId);
        return toResponse(instance);
    }

    @Transactional
    public FlowInstanceResponse pause(UUID instanceId, PauseRequest request, Long userId) {
        FlowInstance instance = requireOwner(instanceId, userId);
        if (instance.getStatus() != FlowInstanceStatus.IN_PROGRESS) {
            throw new BadRequestException("Only an in-progress instance can be paused");
        }
        instance.setStatus(FlowInstanceStatus.PAUSED);
        instance.setPausedAt(LocalDateTime.now());
        instance.setPauseCategory(request.getCategory());
        instanceRepository.save(instance);
        auditService.logAction("FlowInstance", instance.getId(), "PAUSE", userId);
        return toResponse(instance);
    }

    @Transactional
    public FlowInstanceResponse resume(UUID instanceId, Long userId) {
        FlowInstance instance = requireOwner(instanceId, userId);
        if (instance.getStatus() != FlowInstanceStatus.PAUSED) {
            throw new BadRequestException("Only a paused instance can be resumed");
        }
        LocalDateTime now = LocalDateTime.now();
        long idleDelta = instance.getPausedAt() == null ? 0L
                : Math.max(0L, Duration.between(instance.getPausedAt(), now).getSeconds());
        instance.setTotalIdleSeconds(instance.getTotalIdleSeconds() + idleDelta);
        instance.setStatus(FlowInstanceStatus.IN_PROGRESS);
        instance.setPausedAt(null);
        instance.setPauseCategory(null);
        instance.setResumedAt(now);
        instanceRepository.save(instance);
        auditService.logAction("FlowInstance", instance.getId(), "RESUME", userId);
        return toResponse(instance);
    }

    @Transactional
    public FlowInstanceResponse backward(UUID instanceId, Long userId) {
        FlowInstance instance = requireOwner(instanceId, userId);
        if (instance.getStatus() != FlowInstanceStatus.IN_PROGRESS) {
            throw new BadRequestException("Повернення можливе лише під час виконання процесу");
        }
        if (instance.getCurrentStageId() == null || instance.getCurrentStepId() == null) {
            throw new BadRequestException("Поточний крок не знайдено");
        }
        SnapshotTemplate snapshot = snapshotParser.parse(instance.getTemplateSnapshot());
        SnapshotStage stage = findStage(snapshot, instance.getCurrentStageId());
        SnapshotStep step = findStep(stage, instance.getCurrentStepId());
        int stepIdx = stepIndex(stage).applyAsInt(step);
        SnapshotStep target;
        UUID targetStageId = stage.getId();
        if (stepIdx == 0) {
            int stageIdx = stageIndex(snapshot).applyAsInt(stage);
            if (stageIdx == 0) {
                throw new BadRequestException("Повернення неможливе: це перший крок процесу");
            }
            SnapshotStage prevStage = snapshot.getStages().get(stageIdx - 1);
            target = prevStage.getSteps().stream()
                    .max(Comparator.comparingInt(stepIndex(prevStage)))
                    .orElseThrow(() -> new BadRequestException(
                            "Stage " + prevStage.getName() + " has no steps"));
            targetStageId = prevStage.getId();
        } else {
            target = stage.getSteps().get(stepIdx - 1);
        }
        if (!target.isAllowBackward()) {
            throw new BadRequestException(
                    "Повернення до кроку «" + target.getName() + "» заборонено правилами шаблону");
        }

        LocalDateTime now = LocalDateTime.now();
        executionRepository.findByInstanceIdAndStepId(instance.getId(), instance.getCurrentStepId()).stream()
                .filter(e -> e.getStatus() == StepExecutionStatus.IN_PROGRESS)
                .findFirst()
                .ifPresent(e -> {
                    e.setStatus(StepExecutionStatus.CANCELLED);
                    e.setCompletedAt(now);
                    executionRepository.save(e);
                });

        int nextAttempt = nextAttemptNumber(instance, target.getId());
        instance.setCurrentStageId(targetStageId);
        instance.setCurrentStepId(target.getId());
        instanceRepository.save(instance);
        createExecution(instance, targetStageId, target.getId(), nextAttempt, now);
        auditService.logAction("FlowInstance", instance.getId(), "BACKWARD", userId);
        return toResponse(instance);
    }

    @Transactional(readOnly = true)
    public List<StepExecutionResponse> listExecutions(UUID instanceId, Long userId, boolean allowAll) {
        requireOwner(instanceId, userId, allowAll);
        return executionRepository.findByInstanceId(instanceId).stream()
                .sorted(Comparator.comparing(StepExecution::getStartedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(instanceMapper::toExecutionResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public List<FlowInstanceResponse> list(Long assigneeId, String status) {
        List<FlowInstance> instances;
        if (assigneeId != null && status != null) {
            instances = instanceRepository.findByAssignedUserIdAndStatus(assigneeId,
                    FlowInstanceStatus.valueOf(status));
        } else if (assigneeId != null) {
            instances = instanceRepository.findByAssignedUserId(assigneeId);
        } else if (status != null) {
            instances = instanceRepository.findByStatus(FlowInstanceStatus.valueOf(status));
        } else {
            instances = instanceRepository.findAll();
        }
        return instances.stream()
                .sorted(Comparator.comparing(FlowInstance::getCreatedAt).reversed())
                .map(this::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FlowInstanceResponse get(UUID instanceId, Long userId, boolean allowAll) {
        FlowInstance instance = requireOwner(instanceId, userId, allowAll);
        return toResponse(instance);
    }

    @Transactional(readOnly = true)
    public Map<String, Object> getSnapshot(UUID instanceId, Long userId, boolean allowAll) {
        String json = requireOwner(instanceId, userId, allowAll).getTemplateSnapshot();
        if (!StringUtils.hasText(json)) {
            return Map.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Snapshot could not be parsed");
        }
    }

    @Transactional(readOnly = true)
    public FailureSnapshotResponse getFailureSnapshot(UUID instanceId, Long userId, boolean allowAll) {
        requireOwner(instanceId, userId, allowAll);
        return failureSnapshotService.getByInstance(instanceId);
    }

    @Transactional(readOnly = true)
    public List<GateDecisionResponse> listGateDecisions(UUID instanceId, Long userId, boolean allowAll) {
        requireOwner(instanceId, userId, allowAll);
        SnapshotTemplate snapshot = snapshotParser.parse(instanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException("Instance not found: " + instanceId))
                .getTemplateSnapshot());
        return decisionRepository.findByInstanceId(instanceId).stream()
                .sorted(Comparator.comparing(GateDecision::getDecidedAt))
                .map(d -> toDecisionResponse(d, snapshot))
                .toList();
    }

    @Transactional(readOnly = true)
    public List<ResourceUsageResponse> listResources(UUID instanceId, Long userId, boolean allowAll) {
        requireOwner(instanceId, userId, allowAll);
        SnapshotTemplate snapshot = snapshotParser.parse(instanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException("Instance not found: " + instanceId))
                .getTemplateSnapshot());
        return resourceUsageRepository.findByInstanceId(instanceId).stream()
                .sorted(Comparator.comparing(ResourceUsage::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder())))
                .map(u -> toResourceResponse(u, snapshot))
                .toList();
    }

    @Transactional(readOnly = true)
    public byte[] generateReport(UUID instanceId, Long userId, boolean allowAll) {
        FlowInstance instance = requireOwner(instanceId, userId, allowAll);
        ProstheticsOrder order = orderRepository.findById(instance.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + instance.getOrderId()));
        SnapshotTemplate snapshot = snapshotParser.parse(instance.getTemplateSnapshot());
        List<StepExecution> executions = executionRepository.findByInstanceId(instanceId);
        List<GateDecision> decisions = decisionRepository.findByInstanceId(instanceId);
        List<ResourceUsage> resources = resourceUsageRepository.findByInstanceId(instanceId);
        if (instance.getStatus() == FlowInstanceStatus.FAILED
                || instance.getStatus() == FlowInstanceStatus.FAILED_QC) {
            String category = failureSnapshotService.getByInstance(instanceId).getCategory();
            String description = instance.getFailReason();
            return pdfService.generateFailureReport(instance, order, snapshot, category, description);
        }
        return pdfService.generateFinalReport(instance, order, snapshot, executions, decisions, resources);
    }

    @Transactional
    public FlowInstanceResponse replacement(UUID instanceId, Long userId) {
        FlowInstance original = requireOwner(instanceId, userId);
        if (original.getStatus() != FlowInstanceStatus.FAILED
                && original.getStatus() != FlowInstanceStatus.FAILED_QC) {
            throw new BadRequestException("Replacement is allowed only for failed instances");
        }
        ProstheticsOrder order = orderRepository.findById(original.getOrderId())
                .orElseThrow(() -> new NotFoundException("Order not found: " + original.getOrderId()));
        FlowInstance instance = FlowInstance.builder()
                .templateId(original.getTemplateId())
                .patientId(order.getPatient().getId())
                .orderId(original.getOrderId())
                .assignedUserId(userId)
                .status(FlowInstanceStatus.NEW)
                .totalActiveSeconds(0L)
                .totalIdleSeconds(0L)
                .reworkCount(0)
                .templateSnapshot(original.getTemplateSnapshot())
                .build();
        instanceRepository.save(instance);
        auditService.logAction("FlowInstance", instance.getId(), "REPLACEMENT", userId);
        return toResponse(instance);
    }

    @Transactional
    public FlowInstanceResponse fail(UUID instanceId, String category, String description, String snapshotJson,
                                     Long userId) {
        FlowInstance instance = requireOwner(instanceId, userId);
        if (instance.getStatus() != FlowInstanceStatus.IN_PROGRESS
                && instance.getStatus() != FlowInstanceStatus.WAITING_REVIEW) {
            throw new BadRequestException("Instance cannot be failed from its current status");
        }
        instance.setStatus(FlowInstanceStatus.FAILED);
        instance.setFailReason(description);
        instance.setEndTime(LocalDateTime.now());
        instanceRepository.save(instance);
        failureSnapshotService.create(instance, category, description, snapshotJson, userId);
        auditService.logAction("FlowInstance", instance.getId(), "FAIL", userId);
        return toResponse(instance);
    }

    public void markQcFailed(FlowInstance instance, String reason, Long userId) {
        instance.setStatus(FlowInstanceStatus.FAILED_QC);
        instance.setFailReason(reason);
        instance.setEndTime(LocalDateTime.now());
        instanceRepository.save(instance);
        auditService.logAction("FlowInstance", instance.getId(), "QC_FAIL", userId);
    }

    void advance(FlowInstance instance, SnapshotTemplate snapshot, SnapshotStage stage, SnapshotStep step,
                 LocalDateTime now, Long userId) {
        int stepIdx = stepIndex(stage).applyAsInt(step);
        if (stepIdx + 1 < stage.getSteps().size()) {
            SnapshotStep next = stage.getSteps().get(stepIdx + 1);
            instance.setCurrentStepId(next.getId());
            instanceRepository.save(instance);
            createExecution(instance, stage.getId(), next.getId(),
                    nextAttemptNumber(instance, next.getId()), now);
            return;
        }
        int stageIdx = stageIndex(snapshot).applyAsInt(stage);
        SnapshotStage nextStage = stageIdx + 1 < snapshot.getStages().size()
                ? snapshot.getStages().get(stageIdx + 1) : null;
        if (nextStage != null && nextStage.getGate() != null) {
            instance.setStatus(FlowInstanceStatus.WAITING_REVIEW);
            instance.setCurrentStageId(nextStage.getId());
            instanceRepository.save(instance);
            return;
        }
        moveToNextStage(instance, snapshot, stage, now, userId);
    }

    void enterStage(FlowInstance instance, SnapshotStage stage, LocalDateTime now, Long userId) {
        SnapshotStep firstStep = stage.getSteps().stream()
                .min(Comparator.comparingInt(stepIndex(stage)))
                .orElseThrow(() -> new BadRequestException(
                        "Stage " + stage.getName() + " has no steps"));
        instance.setStatus(FlowInstanceStatus.IN_PROGRESS);
        instance.setCurrentStageId(stage.getId());
        instance.setCurrentStepId(firstStep.getId());
        instanceRepository.save(instance);
        createExecution(instance, stage.getId(), firstStep.getId(),
                nextAttemptNumber(instance, firstStep.getId()), now);
    }

    void moveToNextStage(FlowInstance instance, SnapshotTemplate snapshot, SnapshotStage currentStage,
                         LocalDateTime now, Long userId) {
        int stageIdx = stageIndex(snapshot).applyAsInt(currentStage);
        if (stageIdx + 1 < snapshot.getStages().size()) {
            SnapshotStage nextStage = snapshot.getStages().get(stageIdx + 1);
            SnapshotStep firstStep = nextStage.getSteps().stream()
                    .min(Comparator.comparingInt(stepIndex(nextStage)))
                    .orElseThrow(() -> new BadRequestException(
                            "Stage " + nextStage.getName() + " has no steps"));
            instance.setStatus(FlowInstanceStatus.IN_PROGRESS);
            instance.setCurrentStageId(nextStage.getId());
            instance.setCurrentStepId(firstStep.getId());
            instanceRepository.save(instance);
            createExecution(instance, nextStage.getId(), firstStep.getId(),
                    nextAttemptNumber(instance, firstStep.getId()), now);
        } else {
            instance.setStatus(FlowInstanceStatus.COMPLETED);
            instance.setCurrentStageId(null);
            instance.setCurrentStepId(null);
            instance.setEndTime(now);
            instanceRepository.save(instance);
        }
    }

    private int nextAttemptNumber(FlowInstance instance, UUID stepId) {
        return executionRepository.findByInstanceIdAndStepId(instance.getId(), stepId).stream()
                .map(StepExecution::getAttemptNumber)
                .filter(Objects::nonNull)
                .max(Integer::compareTo)
                .orElse(0) + 1;
    }

    void createExecution(FlowInstance instance, UUID stageId, UUID stepId, int attemptNumber, LocalDateTime now) {
        StepExecution execution = StepExecution.builder()
                .instance(instance)
                .stageId(stageId)
                .stepId(stepId)
                .attemptNumber(attemptNumber)
                .status(StepExecutionStatus.IN_PROGRESS)
                .startedAt(now)
                .activeSeconds(0L)
                .build();
        executionRepository.save(execution);
    }

    private void saveResources(FlowInstance instance, StepExecution execution,
                               List<ResourceUsageRequest> requests, Long userId) {
        if (requests == null) {
            return;
        }
        for (ResourceUsageRequest request : requests) {
            if (request.getMaterial() == null || request.getMaterial().isBlank()) {
                throw new BadRequestException("Resource material is required");
            }
            ResourceUsage usage = ResourceUsage.builder()
                    .instance(instance)
                    .stepExecution(execution)
                    .material(request.getMaterial())
                    .qty(request.getQuantity() == null ? BigDecimal.ZERO : request.getQuantity())
                    .unit(request.getUnit())
                    .minutes(request.getMinutes())
                    .recordedBy(userId)
                    .build();
            resourceUsageRepository.save(usage);
        }
    }

    void validateValues(String valuesJson, SnapshotStep step) {
        Map<String, Object> values = parseValues(valuesJson);
        if ("MEASUREMENT".equals(step.getStepType())) {
            // The measurement step collects values via the visual measurement forms:
            // the transition rule is "at least MIN_MEASUREMENT_VALUES filled values"
            // (any form field). Required CHECKBOX elements (e.g. the ЗІЗ
            // acknowledgment merged from the documentation step) gate separately and
            // do NOT count towards the fill threshold.
            Set<String> checkboxKeys = step.getElements().stream()
                    .filter(element -> "CHECKBOX".equals(element.getElementType()))
                    .map(element -> element.getId().toString())
                    .collect(Collectors.toSet());
            long filled = values.entrySet().stream()
                    .filter(entry -> !checkboxKeys.contains(entry.getKey()))
                    .filter(entry -> entry.getValue() != null
                            && !String.valueOf(entry.getValue()).isBlank())
                    .count();
            if (filled < MIN_MEASUREMENT_VALUES) {
                throw new BadRequestException(
                        "Заповніть щонайменше " + MIN_MEASUREMENT_VALUES
                                + " значення вимірювань для переходу до наступного кроку");
            }
            for (SnapshotElement element : step.getElements()) {
                if ("CHECKBOX".equals(element.getElementType()) && element.isRequired()
                        && !Boolean.TRUE.equals(values.get(element.getId().toString()))) {
                    throw new BadRequestException("Поле «" + element.getLabel() + "» обов'язкове");
                }
            }
            // The ЗІЗ confirmation on the «Зняття мірок» step is a hardcoded
            // wizard field (not a DB element): the step cannot advance without it.
            if (UUID.fromString("e0000002-0000-0000-0000-000000000002").equals(step.getId())
                    && !Boolean.TRUE.equals(values.get("ppe-measurement-non-sterile-gloves"))) {
                throw new BadRequestException(
                        "Поле «Нестерильні оглядові нітрилові рукавички» обов'язкове");
            }
            return;
        }
        for (SnapshotElement element : step.getElements()) {
            Object value = values.get(element.getId().toString());
            if (element.isRequired() && isBlank(value)) {
                throw new BadRequestException("Поле «" + element.getLabel() + "» обов'язкове");
            }
            if (value == null) {
                continue;
            }
            if (element.getElementType() != null && element.getElementType().startsWith("TEXT")) {
                String text = String.valueOf(value);
                if (element.getRegexPattern() != null && !text.matches(element.getRegexPattern())) {
                    throw new BadRequestException(
                            "Поле «" + element.getLabel() + "» не відповідає формату");
                }
            }
            if ("NUMERIC_INPUT".equals(element.getElementType())) {
                BigDecimal numeric = toBigDecimal(value);
                if (numeric == null) {
                    throw new BadRequestException("Поле «" + element.getLabel() + "» має бути числом");
                }
                if (element.getMinValue() != null && numeric.compareTo(element.getMinValue()) < 0) {
                    throw new BadRequestException(
                            "Поле «" + element.getLabel() + "» має бути не менше " + element.getMinValue());
                }
                if (element.getMaxValue() != null && numeric.compareTo(element.getMaxValue()) > 0) {
                    throw new BadRequestException(
                            "Поле «" + element.getLabel() + "» має бути не більше " + element.getMaxValue());
                }
            }
            if (("DROPDOWN".equals(element.getElementType()) || "RADIO".equals(element.getElementType()))
                    && element.getOptions() != null && !element.getOptions().isEmpty()) {
                String option = String.valueOf(value);
                if (!element.getOptions().contains(option)) {
                    throw new BadRequestException(
                            "Поле «" + element.getLabel() + "» має недопустиме значення");
                }
            }
            if (element.getMinCount() != null || element.getMaxCount() != null) {
                int count = countValues(value);
                if (element.getMinCount() != null && count < element.getMinCount()) {
                    throw new BadRequestException(
                            "Поле «" + element.getLabel() + "» вимагає щонайменше "
                                    + element.getMinCount() + " значень");
                }
                if (element.getMaxCount() != null && count > element.getMaxCount()) {
                    throw new BadRequestException(
                            "Поле «" + element.getLabel() + "» допускає не більше "
                                    + element.getMaxCount() + " значень");
                }
            }
        }
    }

    private boolean isBlank(Object value) {
        if (value == null) {
            return true;
        }
        if (value instanceof String s) {
            return s.isBlank();
        }
        if (value instanceof Boolean b) {
            return !b;
        }
        if (value instanceof List<?> list) {
            return list.isEmpty();
        }
        return false;
    }

    private int countValues(Object value) {
        if (value instanceof List<?> list) {
            return list.size();
        }
        return 1;
    }

    private BigDecimal toBigDecimal(Object value) {
        if (value instanceof BigDecimal bd) {
            return bd;
        }
        if (value instanceof Number n) {
            return BigDecimal.valueOf(n.doubleValue());
        }
        try {
            return new BigDecimal(String.valueOf(value));
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private Map<String, Object> parseValues(String valuesJson) {
        if (!StringUtils.hasText(valuesJson)) {
            return new LinkedHashMap<>();
        }
        try {
            return objectMapper.readValue(valuesJson, new TypeReference<LinkedHashMap<String, Object>>() {
            });
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Step values are not valid JSON");
        }
    }

    private long secondsSince(LocalDateTime startedAt, LocalDateTime resumedAt, LocalDateTime now) {
        LocalDateTime since = resumedAt != null && resumedAt.isAfter(startedAt) ? resumedAt : startedAt;
        return Math.max(0L, Duration.between(since, now).getSeconds());
    }

    private GateDecisionResponse toDecisionResponse(GateDecision decision, SnapshotTemplate snapshot) {
        String gateName = findGateName(snapshot, decision.getGate().getId());
        return GateDecisionResponse.builder()
                .id(decision.getId())
                .instanceId(decision.getInstance().getId())
                .gateId(decision.getGate().getId())
                .gateName(gateName != null ? gateName : decision.getGate().getName())
                .decision(decision.getDecision().name())
                .criteriaConfirmed(parseCriteria(decision.getCriteriaConfirmed()))
                .comment(decision.getComment())
                .decidedBy(decision.getDecidedBy())
                .decidedAt(decision.getDecidedAt())
                .build();
    }

    private ResourceUsageResponse toResourceResponse(ResourceUsage usage, SnapshotTemplate snapshot) {
        StepExecution execution = usage.getStepExecution();
        UUID stepId = execution == null ? null : execution.getStepId();
        return ResourceUsageResponse.builder()
                .id(usage.getId())
                .stepExecutionId(execution == null ? null : execution.getId())
                .stepId(stepId)
                .stepName(stepId != null ? findStepName(snapshot, stepId) : null)
                .material(usage.getMaterial())
                .qty(usage.getQty())
                .unit(usage.getUnit())
                .minutes(usage.getMinutes())
                .recordedBy(usage.getRecordedBy())
                .createdAt(usage.getCreatedAt())
                .build();
    }

    private String findStepName(SnapshotTemplate snapshot, UUID stepId) {
        for (SnapshotStage stage : snapshot.getStages()) {
            for (SnapshotStep step : stage.getSteps()) {
                if (step.getId().equals(stepId)) {
                    return step.getName();
                }
            }
        }
        return null;
    }

    private String findGateName(SnapshotTemplate snapshot, UUID gateId) {
        for (SnapshotStage stage : snapshot.getStages()) {
            if (stage.getGate() != null && stage.getGate().getId().equals(gateId)) {
                return stage.getGate().getName();
            }
        }
        return null;
    }

    private List<String> parseCriteria(String json) {
        if (!StringUtils.hasText(json)) {
            return List.of();
        }
        try {
            return objectMapper.readValue(json, new TypeReference<List<String>>() {
            });
        } catch (JsonProcessingException e) {
            return List.of();
        }
    }

    FlowInstance requireOwner(UUID instanceId, Long userId) {
        return requireOwner(instanceId, userId, false);
    }

    FlowInstance requireOwner(UUID instanceId, Long userId, boolean allowAll) {
        FlowInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException("Instance not found: " + instanceId));
        if (allowAll || instance.getAssignedUserId().equals(userId)) {
            return instance;
        }
        throw new NotFoundException("Instance not found: " + instanceId);
    }

    SnapshotStage findStage(SnapshotTemplate snapshot, UUID stageId) {
        return snapshot.getStages().stream()
                .filter(s -> s.getId().equals(stageId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Stage is missing from the template snapshot"));
    }
    private SnapshotStep findStep(SnapshotStage stage, UUID stepId) {
        return stage.getSteps().stream()
                .filter(s -> s.getId().equals(stepId))
                .findFirst()
                .orElseThrow(() -> new BadRequestException("Step is missing from the template snapshot"));
    }

    private java.util.function.ToIntFunction<SnapshotStage> stageIndex(SnapshotTemplate snapshot) {
        return stage -> {
            for (int i = 0; i < snapshot.getStages().size(); i++) {
                if (snapshot.getStages().get(i).getId().equals(stage.getId())) {
                    return i;
                }
            }
            return Integer.MAX_VALUE;
        };
    }

    private java.util.function.ToIntFunction<SnapshotStep> stepIndex(SnapshotStage stage) {
        return step -> {
            for (int i = 0; i < stage.getSteps().size(); i++) {
                if (stage.getSteps().get(i).getId().equals(step.getId())) {
                    return i;
                }
            }
            return Integer.MAX_VALUE;
        };
    }

    /**
     * Resolves patient full name via the MIS Integration Layer (single source of
     * truth for patient demographics). Falls back to the locally stored name when
     * the MIS layer is unavailable (e.g. unit tests without Spring context).
     */
    private String resolvePatientPib(ProstheticsOrder order) {
        if (order.getPatient() == null) {
            return null;
        }
        String localPib = order.getPatient().getPib();
        try {
            var patientService = com.superhumans.config.SpringContext
                    .getBean(ProstheticsPatientService.class);
            return patientService.get(order.getPatient().getId()).getPib();
        } catch (Exception e) {
            return localPib;
        }
    }
}
