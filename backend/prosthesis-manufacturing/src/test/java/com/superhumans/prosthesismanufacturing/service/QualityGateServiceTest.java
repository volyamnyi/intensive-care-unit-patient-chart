package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.exception.BadRequestException;
import com.superhumans.prosthesismanufacturing.dto.GateDecisionRequest;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.GateDecisionType;
import com.superhumans.prosthesismanufacturing.entity.QualityGate;
import com.superhumans.prosthesismanufacturing.entity.ReworkLoop;
import com.superhumans.prosthesismanufacturing.entity.ReworkType;
import com.superhumans.prosthesismanufacturing.entity.StepExecution;
import com.superhumans.prosthesismanufacturing.entity.StepExecutionStatus;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.GateDecisionRepository;
import com.superhumans.prosthesismanufacturing.repository.QualityGateRepository;
import com.superhumans.prosthesismanufacturing.repository.ReworkLoopRepository;
import com.superhumans.prosthesismanufacturing.repository.StepExecutionRepository;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotGate;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStage;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class QualityGateServiceTest {

    @Mock
    FlowInstanceRepository instanceRepository;
    @Mock
    QualityGateRepository gateRepository;
    @Mock
    ReworkLoopRepository reworkLoopRepository;
    @Mock
    GateDecisionRepository decisionRepository;
    @Mock
    StepExecutionRepository executionRepository;
    @Mock
    FlowInstanceService instanceService;
    @Mock
    FailureSnapshotService failureSnapshotService;
    @Mock
    AuditService auditService;

    QualityGateService service;

    UUID stageId = UUID.randomUUID();
    UUID gateId = UUID.randomUUID();
    UUID stepId = UUID.randomUUID();
    UUID targetStepId = UUID.randomUUID();

    SnapshotStage snapshotStage;

    @BeforeEach
    void setUp() {
        service = new QualityGateService(instanceRepository, gateRepository, reworkLoopRepository,
                decisionRepository, executionRepository, instanceService, failureSnapshotService,
                new TemplateSnapshotParser(new ObjectMapper()), auditService, new ObjectMapper());
        snapshotStage = SnapshotStage.builder()
                .id(stageId)
                .name("Контроль якості")
                .gate(SnapshotGate.builder()
                        .id(gateId)
                        .name("Приймальний контроль")
                        .requiredApproverRole("PROSTHETICS_ADMINISTRATOR")
                        .build())
                .steps(List.of(SnapshotStep.builder()
                        .id(stepId)
                        .name("Фінальна перевірка")
                        .elements(List.of())
                        .build()))
                .build();
    }

    @Test
    void decideRequiresWaitingForReview() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS);
        when(instanceService.requireOwner(instance.getId(), 1L)).thenReturn(instance);

        assertThatThrownBy(() -> service.decide(instance.getId(), gateId, passRequest(), 1L, true))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("waiting for review");
        verify(decisionRepository, never()).save(any());
    }

    @Test
    void decideRejectsNonApproverForAdminGate() {
        FlowInstance instance = newInstance(FlowInstanceStatus.WAITING_REVIEW);
        when(instanceService.requireOwner(instance.getId(), 1L)).thenReturn(instance);
        when(instanceService.findStage(any(), eq(stageId))).thenReturn(snapshotStage);

        assertThatThrownBy(() -> service.decide(instance.getId(), gateId, passRequest(), 1L, false))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("prosthetics administrator");
    }

    @Test
    void reworkCreatesNewAttemptUntilMaxAttempts() {
        FlowInstance instance = newInstance(FlowInstanceStatus.WAITING_REVIEW);
        QualityGate gate = QualityGate.builder()
                .name("Приймальний контроль")
                .requiredApproverRole("PROSTHETICS_ADMINISTRATOR").build();
        gate.setId(gateId);
        when(instanceService.requireOwner(instance.getId(), 1L)).thenReturn(instance);
        when(instanceService.findStage(any(), eq(stageId))).thenReturn(snapshotStage);
        when(gateRepository.findById(gateId)).thenReturn(Optional.of(gate));
        when(reworkLoopRepository.findByGateId(gateId)).thenReturn(List.of(
                ReworkLoop.builder()
                        .gate(gate).reworkType(ReworkType.PARTIAL)
                        .targetStepId(targetStepId).maxAttempts(2).build()));
        when(executionRepository.findByInstanceIdAndStepId(instance.getId(), targetStepId))
                .thenReturn(List.of(execution(stepId)));
        when(decisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(instanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.decide(instance.getId(), gateId,
                new GateDecisionRequest(GateDecisionType.REWORK, null, "підготувати повторно"),
                1L, true);

        assertThat(instance.getReworkCount()).isEqualTo(1);
        assertThat(instance.getStatus()).isEqualTo(FlowInstanceStatus.IN_PROGRESS);
        assertThat(instance.getCurrentStepId()).isEqualTo(targetStepId);
        verify(instanceService).createExecution(any(), any(), any(),
                org.mockito.ArgumentMatchers.eq(2), any());
        verify(auditService).logAction(any(), any(), org.mockito.ArgumentMatchers.eq("GATE_REWORK"), any());
    }

    @Test
    void reworkExceedingMaxAttemptsFailsInstance() {
        FlowInstance instance = newInstance(FlowInstanceStatus.WAITING_REVIEW);
        QualityGate gate = QualityGate.builder()
                .name("Приймальний контроль")
                .requiredApproverRole("PROSTHETICS_ADMINISTRATOR").build();
        gate.setId(gateId);
        when(instanceService.requireOwner(instance.getId(), 1L)).thenReturn(instance);
        when(instanceService.findStage(any(), eq(stageId))).thenReturn(snapshotStage);
        when(gateRepository.findById(gateId)).thenReturn(Optional.of(gate));
        when(reworkLoopRepository.findByGateId(gateId)).thenReturn(List.of(
                ReworkLoop.builder()
                        .gate(gate).reworkType(ReworkType.PARTIAL)
                        .targetStepId(targetStepId).maxAttempts(2).build()));
        when(executionRepository.findByInstanceIdAndStepId(instance.getId(), targetStepId))
                .thenReturn(List.of(execution(stepId), execution(targetStepId)));
        when(decisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.decide(instance.getId(), gateId,
                new GateDecisionRequest(GateDecisionType.REWORK, null, null), 1L, true);

        verify(instanceService).markQcFailed(any(), any(), any());
        verify(failureSnapshotService).create(any(), any(), any(), any(), any());
        verify(instanceService, never()).createExecution(any(), any(), any(), anyInt(), any());
    }

    @Test
    void passMovesToNextStage() {
        FlowInstance instance = newInstance(FlowInstanceStatus.WAITING_REVIEW);
        QualityGate gate = QualityGate.builder()
                .name("Приймальний контроль")
                .requiredApproverRole("PROSTHETICS_ADMINISTRATOR").build();
        gate.setId(gateId);
        when(instanceService.requireOwner(instance.getId(), 1L)).thenReturn(instance);
        when(instanceService.findStage(any(), eq(stageId))).thenReturn(snapshotStage);
        when(gateRepository.findById(gateId)).thenReturn(Optional.of(gate));
        when(decisionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.decide(instance.getId(), gateId, passRequest(), 1L, true);

        verify(instanceService).enterStage(any(), any(), any(), any());
        verify(instanceService, never()).moveToNextStage(any(), any(), any(), any(), any());
        verify(auditService).logAction(any(), any(), org.mockito.ArgumentMatchers.eq("GATE_PASS"), any());
    }

    private GateDecisionRequest passRequest() {
        return new GateDecisionRequest(GateDecisionType.PASS,
                List.of("Відповідність технічному завданню"), "відповідає");
    }

    private FlowInstance newInstance(FlowInstanceStatus status) {
        FlowInstance instance = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .patientId("90001")
                .orderId(UUID.randomUUID())
                .assignedUserId(1L)
                .status(status)
                .currentStageId(stageId)
                .totalActiveSeconds(0L)
                .totalIdleSeconds(0L)
                .reworkCount(0)
                .templateSnapshot(snapshotJson())
                .build();
        instance.setId(UUID.randomUUID());
        return instance;
    }

    private StepExecution execution(UUID stepId) {
        StepExecution execution = StepExecution.builder()
                .instance(newInstance(FlowInstanceStatus.IN_PROGRESS))
                .stageId(stageId)
                .stepId(stepId)
                .attemptNumber(1)
                .status(StepExecutionStatus.COMPLETED)
                .build();
        execution.setId(UUID.randomUUID());
        return execution;
    }

    private String snapshotJson() {
        return new TemplateSnapshotParser(new ObjectMapper()).toJson(SnapshotTemplate.builder()
                .name("TP-UL-01")
                .version(1)
                .stages(List.of(snapshotStage))
                .build());
    }
}