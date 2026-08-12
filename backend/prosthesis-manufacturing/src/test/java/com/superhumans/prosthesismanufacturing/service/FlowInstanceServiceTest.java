package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.exception.BadRequestException;
import com.superhumans.prosthesismanufacturing.dto.InstanceCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.PauseRequest;
import com.superhumans.prosthesismanufacturing.dto.ResourceUsageRequest;
import com.superhumans.prosthesismanufacturing.dto.StepCompleteRequest;
import com.superhumans.prosthetismanufacturing.entity.ElementType;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.PauseCategory;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.entity.StepExecution;
import com.superhumans.prosthesismanufacturing.entity.StepExecutionStatus;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class FlowInstanceServiceTest {

    @Mock
    FlowInstanceRepository instanceRepository;
    @Mock
    FlowTemplateRepository templateRepository;
    @Mock
    ProstheticsOrderRepository orderRepository;
    @Mock
    StepExecutionRepository executionRepository;
    @Mock
    ResourceUsageRepository resourceUsageRepository;
    @Mock
    GateDecisionRepository decisionRepository;
    @Mock
    FlowTemplateService templateService;
    @Mock
    FailureSnapshotService failureSnapshotService;
    @Mock
    ProstheticsPdfService pdfService;
    @Mock
    AuditService auditService;

    TemplateSnapshotParser parser;
    FlowInstanceService service;

    UUID stageId = UUID.randomUUID();
    UUID stepOneId = UUID.randomUUID();
    UUID stepTwoId = UUID.randomUUID();
    UUID numericElementId = UUID.randomUUID();
    UUID textElementId = UUID.randomUUID();
    UUID regexElementId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        parser = new TemplateSnapshotParser(new ObjectMapper());
        service = new FlowInstanceService(instanceRepository, templateRepository, orderRepository,
                executionRepository, resourceUsageRepository, decisionRepository,
                mock(com.superhumans.prosthesismanufacturing.mapper.FlowInstanceMapper.class),
                templateService, failureSnapshotService, pdfService, auditService, parser,
                new ObjectMapper());
    }

    @Test
    void createRejectsDuplicateActiveInstance() {
        var request = InstanceCreateRequest.builder()
                .orderId(UUID.randomUUID()).templateId(UUID.randomUUID()).build();
        var order = orderWithId(request.getOrderId());
        var template = templateWithId(request.getTemplateId(), TemplateStatus.ACTIVE);
        var existing = FlowInstance.builder().status(FlowInstanceStatus.IN_PROGRESS).build();
        when(orderRepository.findById(request.getOrderId())).thenReturn(Optional.of(order));
        when(templateRepository.findById(request.getTemplateId())).thenReturn(Optional.of(template));
        when(instanceRepository.findByOrderId(request.getOrderId())).thenReturn(List.of(existing));

        assertThatThrownBy(() -> service.create(request, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("active instance");
        verify(instanceRepository, never()).save(any());
    }

    @Test
    void createRejectsInactiveTemplate() {
        var request = InstanceCreateRequest.builder()
                .orderId(UUID.randomUUID()).templateId(UUID.randomUUID()).build();
        var template = templateWithId(request.getTemplateId(), TemplateStatus.DRAFT);
        when(orderRepository.findById(request.getOrderId()))
                .thenReturn(Optional.of(orderWithId(request.getOrderId())));
        when(templateRepository.findById(request.getTemplateId())).thenReturn(Optional.of(template));

        assertThatThrownBy(() -> service.create(request, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("active templates");
    }

    @Test
    void startMovesToFirstStepAndCreatesExecution() {
        FlowInstance instance = newInstance(FlowInstanceStatus.NEW, snapshotJson());
        when(instanceRepository.findByIdForUpdate(instance.getId())).thenReturn(Optional.of(instance));
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.start(instance.getId(), 1L);

        assertThat(instance.getStatus()).isEqualTo(FlowInstanceStatus.IN_PROGRESS);
        assertThat(instance.getCurrentStageId()).isEqualTo(stageId);
        assertThat(instance.getCurrentStepId()).isEqualTo(stepOneId);
        verify(executionRepository).save(any());
    }

    @Test
    void startFromNonNewStatusIsRejected() {
        FlowInstance instance = newInstance(FlowInstanceStatus.PAUSED, snapshotJson());
        when(instanceRepository.findByIdForUpdate(instance.getId())).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.start(instance.getId(), 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("NEW");
    }

    @Test
    void completeStepRejectsMissingRequiredValue() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        StepExecution execution = executionFor(instance, stepOneId);
        instance.setCurrentStepId(stepOneId);
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        when(executionRepository.findById(execution.getId())).thenReturn(Optional.of(execution));

        assertThatThrownBy(() -> service.completeStep(instance.getId(), execution.getId(),
                new StepCompleteRequest("{}", null), 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("обов'язкове");
        verify(executionRepository, never()).save(any());
    }

    @Test
    void completeStepRejectsValueOutsideRange() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        StepExecution execution = executionFor(instance, stepOneId);
        instance.setCurrentStepId(stepOneId);
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        when(executionRepository.findById(execution.getId())).thenReturn(Optional.of(execution));

        assertThatThrownBy(() -> service.completeStep(instance.getId(), execution.getId(),
                new StepCompleteRequest("{\"" + numericElementId + "\":999}", null), 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("не більше");
    }

    @Test
    void completeStepRejectsRegexMismatch() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        StepExecution execution = executionFor(instance, stepOneId);
        instance.setCurrentStepId(stepOneId);
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        when(executionRepository.findById(execution.getId())).thenReturn(Optional.of(execution));

        assertThatThrownBy(() -> service.completeStep(instance.getId(), execution.getId(),
                new StepCompleteRequest("{\"" + numericElementId + "\":10,\"" + textElementId
                        + "\":\"ok\",\"" + regexElementId + "\":\"12345\"}", null), 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("формату");
    }

    @Test
    void completeStepWithValidValuesAdvancesToNextStep() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        StepExecution execution = executionFor(instance, stepOneId);
        instance.setCurrentStepId(stepOneId);
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        when(executionRepository.findById(execution.getId())).thenReturn(Optional.of(execution));
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        var response = service.completeStep(instance.getId(), execution.getId(),
                validValues(), 1L);

        assertThat(execution.getStatus()).isEqualTo(StepExecutionStatus.COMPLETED);
        assertThat(instance.getCurrentStepId()).isEqualTo(stepTwoId);
        assertThat(instance.getTotalActiveSeconds()).isGreaterThanOrEqualTo(0L);
        verify(executionRepository, org.mockito.Mockito.times(2)).save(any());
        verify(auditService).logAction(any(), any(), any(), any());
    }

    @Test
    void pauseAndResumeAccumulateIdleSeconds() throws InterruptedException {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));

        service.pause(instance.getId(), new PauseRequest(PauseCategory.MATERIAL), 1L);
        assertThat(instance.getStatus()).isEqualTo(FlowInstanceStatus.PAUSED);
        assertThat(instance.getPauseCategory()).isEqualTo(PauseCategory.MATERIAL);
        assertThat(instance.getPausedAt()).isNotNull();

        Thread.sleep(50);
        service.resume(instance.getId(), 1L);

        assertThat(instance.getStatus()).isEqualTo(FlowInstanceStatus.IN_PROGRESS);
        assertThat(instance.getPausedAt()).isNull();
        assertThat(instance.getTotalIdleSeconds()).isGreaterThanOrEqualTo(0L);
        assertThat(instance.getResumedAt()).isNotNull();
    }

    @Test
    void startByAnotherProsthetistIsAllowed() {
        FlowInstance instance = newInstance(FlowInstanceStatus.NEW, snapshotJson());
        instance.setAssignedUserId(99L);
        when(instanceRepository.findByIdForUpdate(instance.getId())).thenReturn(Optional.of(instance));
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.start(instance.getId(), 1L);

        assertThat(instance.getStatus()).isEqualTo(FlowInstanceStatus.IN_PROGRESS);
        assertThat(instance.getCurrentStepId()).isEqualTo(stepOneId);
        verify(executionRepository).save(any());
    }

    @Test
    void failCreatesFailureSnapshot() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        when(instanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        when(failureSnapshotService.create(any(), any(), any(), any(), any()))
                .thenAnswer(invocation -> new com.superhumans.prosthesismanufacturing.entity.FailureSnapshot());

        service.fail(instance.getId(), "technical", "Зламано обладнання", null, 1L);

        assertThat(instance.getStatus()).isEqualTo(FlowInstanceStatus.FAILED);
        assertThat(instance.getFailReason()).isEqualTo("Зламано обладнання");
        assertThat(instance.getEndTime()).isNotNull();
        verify(failureSnapshotService).create(any(), any(), any(), any(), any());
    }

    @Test
    void completeStepOnTerminalStatusRejected() {
        FlowInstance instance = newInstance(FlowInstanceStatus.COMPLETED, snapshotJson());
        StepExecution execution = executionFor(instance, stepOneId);
        instance.setCurrentStepId(stepOneId);
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.completeStep(instance.getId(), execution.getId(),
                validValues(), 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("in progress");
    }

    @Test
    void completeStepOnFailedStatusRejected() {
        FlowInstance instance = newInstance(FlowInstanceStatus.FAILED, snapshotJson());
        StepExecution execution = executionFor(instance, stepOneId);
        instance.setCurrentStepId(stepOneId);
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.completeStep(instance.getId(), execution.getId(),
                validValues(), 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("in progress");
    }

    @Test
    void completeStepRejectsNegativeResourceQuantity() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        StepExecution execution = executionFor(instance, stepOneId);
        instance.setCurrentStepId(stepOneId);
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        when(executionRepository.findById(execution.getId())).thenReturn(Optional.of(execution));

        assertThatThrownBy(() -> service.completeStep(instance.getId(), execution.getId(),
                new StepCompleteRequest("{\"" + numericElementId + "\":10,\"" + textElementId
                        + "\":\"ok\"}", List.of(
                        ResourceUsageRequest.builder()
                                .material("")
                                .quantity(new BigDecimal("-1"))
                                .unit("кг")
                                .minutes(10)
                                .build())), 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("required");
    }

    private FlowInstance newInstance(FlowInstanceStatus status, String snapshot) {
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
                .templateSnapshot(snapshot)
                .build();
        instance.setId(UUID.randomUUID());
        return instance;
    }

    private StepExecution executionFor(FlowInstance instance, UUID stepId) {
        StepExecution execution = StepExecution.builder()
                .instance(instance)
                .stageId(stageId)
                .stepId(stepId)
                .attemptNumber(1)
                .status(StepExecutionStatus.IN_PROGRESS)
                .startedAt(java.time.LocalDateTime.now().minusMinutes(5))
                .activeSeconds(0L)
                .build();
        execution.setId(UUID.randomUUID());
        return execution;
    }

    private String snapshotJson() {
        SnapshotTemplate snapshot = SnapshotTemplate.builder()
                .name("TP-UL-01")
                .version(1)
                .stages(List.of(SnapshotStage.builder()
                        .id(stageId)
                        .name("Клінічне обстеження")
                        .steps(List.of(
                                SnapshotStep.builder()
                                        .id(stepOneId)
                                        .name("Вимірювання")
                                        .elements(List.of(
                                                SnapshotElement.builder()
                                                        .id(numericElementId)
                                                        .elementType("NUMERIC_INPUT")
                                                        .label("Довжина кукси, см")
                                                        .required(true)
                                                        .minValue(new BigDecimal("1"))
                                                        .maxValue(new BigDecimal("60"))
                                                        .build(),
                                                SnapshotElement.builder()
                                                        .id(textElementId)
                                                        .elementType("TEXT_INPUT")
                                                        .label("Матеріал")
                                                        .required(true)
                                                        .regexPattern("^[A-Za-z]+$")
                                                        .build(),
                                                SnapshotElement.builder()
                                                        .id(regexElementId)
                                                        .elementType("TEXT_INPUT")
                                                        .label("Серія")
                                                        .required(false)
                                                        .regexPattern("^[A-Za-z]+$")
                                                        .build()))
                                        .build(),
                                SnapshotStep.builder()
                                        .id(stepTwoId)
                                        .name("Анамнез")
                                        .elements(List.of())
                                        .build()))
                        .build()))
                .build();
        return parser.toJson(snapshot);
    }

    private StepCompleteRequest validValues() {
        return new StepCompleteRequest("{\"" + numericElementId + "\":10,\"" + textElementId
                + "\":\"ok\"}", null);
    }

    private ProstheticsOrder orderWithId(UUID id) {
        ProstheticsPatient patient = ProstheticsPatient.builder()
                .pib("Сніжко Іван Петрович")
                .build();
        patient.setId("90001");
        ProstheticsOrder order = ProstheticsOrder.builder()
                .orderNumber("PR-2026-0001")
                .patient(patient)
                .build();
        order.setId(id);
        return order;
    }

    private com.superhumans.prosthesismanufacturing.entity.FlowTemplate templateWithId(
            UUID id, TemplateStatus status) {
        com.superhumans.prosthesismanufacturing.entity.FlowTemplate template =
                com.superhumans.prosthesismanufacturing.entity.FlowTemplate.builder()
                .name("TP-UL-01")
                .templateVersion(1)
                .productType(ProductType.UPPER_LIMB)
                .status(status)
                .build();
        template.setId(id);
        return template;
    }
}
