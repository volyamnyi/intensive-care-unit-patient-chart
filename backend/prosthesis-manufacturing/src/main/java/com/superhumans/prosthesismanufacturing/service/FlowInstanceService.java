package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.exception.BadRequestException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.FlowInstanceResponse;
import com.superhumans.prosthesismanufacturing.dto.InstanceCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.PauseRequest;
import com.superhumans.prosthesismanufacturing.dto.ResourceUsageRequest;
import com.superhumans.prosthesismanufacturing.dto.StepCompleteRequest;
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
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FlowInstanceService {

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
        return instanceMapper.toResponse(instance);
    }

    @Transactional
    public FlowInstanceResponse start(UUID instanceId, Long userId) {
        FlowInstance instance = requireOwner(instanceId, userId);
        if (instance.getStatus() != FlowInstanceStatus.NEW) {
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

        createExecution(instance, firstStage.getId(), firstStep.getId(), 1, now);
        auditService.logAction("FlowInstance", instance.getId(), "START", userId);
        return instanceMapper.toResponse(instance);
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
        return instanceMapper.toResponse(instance);
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
        return instanceMapper.toResponse(instance);
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
        return instanceMapper.toResponse(instance);
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
                .map(instanceMapper::toResponse)
                .toList();
    }

    @Transactional(readOnly = true)
    public FlowInstanceResponse get(UUID instanceId, Long userId, boolean allowAll) {
        FlowInstance instance = requireOwner(instanceId, userId, allowAll);
        return instanceMapper.toResponse(instance);
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
        return instanceMapper.toResponse(instance);
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
        return instanceMapper.toResponse(instance);
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
            createExecution(instance, stage.getId(), next.getId(), 1, now);
            return;
        }
        if (stage.getGate() != null) {
            instance.setStatus(FlowInstanceStatus.WAITING_REVIEW);
            instanceRepository.save(instance);
            return;
        }
        moveToNextStage(instance, snapshot, stage, now, userId);
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
            createExecution(instance, nextStage.getId(), firstStep.getId(), 1, now);
        } else {
            instance.setStatus(FlowInstanceStatus.COMPLETED);
            instance.setCurrentStageId(null);
            instance.setCurrentStepId(null);
            instance.setEndTime(now);
            instanceRepository.save(instance);
        }
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

    FlowInstance requireOwner(UUID instanceId, Long userId) {
        return requireOwner(instanceId, userId, false);
    }

    FlowInstance requireOwner(UUID instanceId, Long userId, boolean allowAll) {
        FlowInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException("Instance not found: " + instanceId));
        if (!allowAll && instance.getAssignedUserId() != null
                && !instance.getAssignedUserId().equals(userId)) {
            throw new BadRequestException("Instance belongs to another prosthetist");
        }
        return instance;
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
}
