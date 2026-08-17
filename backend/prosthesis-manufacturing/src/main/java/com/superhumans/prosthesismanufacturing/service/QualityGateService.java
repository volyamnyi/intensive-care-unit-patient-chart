package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.exception.BadRequestException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.FlowInstanceResponse;
import com.superhumans.prosthesismanufacturing.dto.GateDecisionRequest;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.GateDecision;
import com.superhumans.prosthesismanufacturing.entity.GateDecisionType;
import com.superhumans.prosthesismanufacturing.entity.QualityGate;
import com.superhumans.prosthesismanufacturing.entity.ReworkLoop;
import com.superhumans.prosthesismanufacturing.entity.StepExecutionStatus;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.GateDecisionRepository;
import com.superhumans.prosthesismanufacturing.repository.QualityGateRepository;
import com.superhumans.prosthesismanufacturing.repository.ReworkLoopRepository;
import com.superhumans.prosthesismanufacturing.repository.StepExecutionRepository;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotGate;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStage;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import com.superhumans.service.AuditService;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class QualityGateService {

    FlowInstanceRepository instanceRepository;
    QualityGateRepository gateRepository;
    ReworkLoopRepository reworkLoopRepository;
    GateDecisionRepository decisionRepository;
    StepExecutionRepository executionRepository;
    FlowInstanceService instanceService;
    FailureSnapshotService failureSnapshotService;
    TemplateSnapshotParser snapshotParser;
    AuditService auditService;
    ObjectMapper objectMapper;

    @Transactional
    public FlowInstanceResponse decide(UUID instanceId, UUID gateId, GateDecisionRequest request, Long userId,
                                       boolean isApprover) {
        FlowInstance instance = instanceService.requireOwner(instanceId, userId);
        if (instance.getStatus() != FlowInstanceStatus.WAITING_REVIEW) {
            throw new BadRequestException("Quality gate can be decided only while waiting for review");
        }
        SnapshotTemplate snapshot = snapshotParser.parse(instance.getTemplateSnapshot());
        SnapshotStage stage = instanceService.findStage(snapshot, instance.getCurrentStageId());
        if (stage.getGate() == null || !stage.getGate().getId().equals(gateId)) {
            throw new BadRequestException("Gate is not the current gate of the instance");
        }
        SnapshotGate snapshotGate = stage.getGate();
        if ("PROSTHETICS_ADMINISTRATOR".equals(snapshotGate.getRequiredApproverRole()) && !isApprover) {
            throw new BadRequestException("This gate requires a prosthetics administrator decision");
        }

        QualityGate gate = gateRepository.findById(gateId)
                .orElseThrow(() -> new NotFoundException("Gate not found: " + gateId));
        LocalDateTime now = LocalDateTime.now();
        GateDecision decision = GateDecision.builder()
                .instance(instance)
                .gate(gate)
                .decision(request.getDecision())
                .criteriaConfirmed(toJson(request.getCriteriaConfirmed()))
                .comment(request.getComment())
                .decidedBy(userId)
                .decidedAt(now)
                .build();
        decisionRepository.save(decision);

        FlowInstance result = switch (request.getDecision()) {
            case PASS -> pass(instance, stage, now, userId);
            case REWORK -> rework(instance, snapshot, stage, snapshotGate, gate, now, userId);
            case FAIL -> fail(instance, gate, request.getComment(), userId);
        };
        return instanceService.toResponse(result);
    }

    private FlowInstance pass(FlowInstance instance, SnapshotStage stage,
                              LocalDateTime now, Long userId) {
        instanceService.enterStage(instance, stage, now, userId);
        auditService.logAction("QualityGate", instance.getId(), "GATE_PASS", userId);
        return instance;
    }

    private FlowInstance rework(FlowInstance instance, SnapshotTemplate snapshot, SnapshotStage stage,
                                SnapshotGate snapshotGate, QualityGate gate, LocalDateTime now, Long userId) {
        List<ReworkLoop> loops = reworkLoopRepository.findByGateId(gate.getId());
        ReworkLoop loop = loops.stream()
                .filter(l -> l.getTargetStepId() != null)
                .findFirst()
                .orElseGet(() -> loops.isEmpty() ? null : loops.get(0));
        int maxAttempts = loop != null && loop.getMaxAttempts() != null ? loop.getMaxAttempts()
                : (snapshotGate.getReworkLoops() != null && !snapshotGate.getReworkLoops().isEmpty()
                ? snapshotGate.getReworkLoops().get(0).getMaxAttempts() : 1);
        UUID targetStepId = loop != null && loop.getTargetStepId() != null
                ? loop.getTargetStepId()
                : (snapshotGate.getReworkLoops() != null && !snapshotGate.getReworkLoops().isEmpty()
                && snapshotGate.getReworkLoops().get(0).getTargetStepId() != null
                ? snapshotGate.getReworkLoops().get(0).getTargetStepId()
                : stage.getSteps().get(0).getId());

        long attempts = executionRepository.findByInstanceIdAndStepId(instance.getId(), targetStepId).size();
        if (attempts >= maxAttempts) {
            instanceService.markQcFailed(instance,
                    "Перевищено максимальну кількість спроб на контролі якості (" + maxAttempts + ")",
                    userId);
            failureSnapshotService.create(instance, "quality_gate",
                    "Перевищено кількість спроб: " + maxAttempts, instance.getTemplateSnapshot(), userId);
            return instance;
        }

        SnapshotStage reworkStage = snapshot.getStages().stream()
                .filter(s -> s.getSteps().stream().anyMatch(st -> st.getId().equals(targetStepId)))
                .findFirst()
                .orElse(stage);
        instance.setReworkCount(instance.getReworkCount() + 1);
        instance.setCurrentStageId(reworkStage.getId());
        instance.setCurrentStepId(targetStepId);
        instance.setStatus(FlowInstanceStatus.IN_PROGRESS);
        instanceRepository.save(instance);
        instanceService.createExecution(instance, reworkStage.getId(), targetStepId,
                (int) attempts + 1, now);
        auditService.logAction("QualityGate", instance.getId(), "GATE_REWORK", userId);
        return instance;
    }

    private FlowInstance fail(FlowInstance instance, QualityGate gate, String comment, Long userId) {
        instance.setStatus(FlowInstanceStatus.FAILED);
        instance.setFailReason(comment);
        instance.setEndTime(LocalDateTime.now());
        instanceRepository.save(instance);
        failureSnapshotService.create(instance, "quality_gate", comment,
                instance.getTemplateSnapshot(), userId);
        auditService.logAction("QualityGate", instance.getId(), "GATE_FAIL", userId);
        return instance;
    }

    private String toJson(List<String> values) {
        if (values == null) {
            return null;
        }
        try {
            return objectMapper.writeValueAsString(values);
        } catch (JsonProcessingException e) {
            throw new BadRequestException("Invalid criteria list");
        }
    }
}
