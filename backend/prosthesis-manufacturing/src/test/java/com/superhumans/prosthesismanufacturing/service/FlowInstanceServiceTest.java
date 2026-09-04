package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.exception.BadRequestException;
import com.superhumans.prosthesismanufacturing.dto.InstanceCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.PauseRequest;
import com.superhumans.prosthesismanufacturing.dto.ResourceUsageRequest;
import com.superhumans.prosthesismanufacturing.dto.StepCompleteRequest;
import com.superhumans.prosthesismanufacturing.entity.ElementType;
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
                executionRepository, resourceUsageRepository,
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

        service.pause(instance.getId(), new PauseRequest(PauseCategory.VLC_PASSING), 1L);
        assertThat(instance.getStatus()).isEqualTo(FlowInstanceStatus.PAUSED);
        assertThat(instance.getPauseCategory()).isEqualTo(PauseCategory.VLC_PASSING);
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

        service.fail(instance.getId(), "other", "Зламано обладнання", null, 1L);

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

    @Test
    void requireOwner_allowsAssignedOwner() {
        UUID instanceId = UUID.randomUUID();
        FlowInstance instance = ownerInstance(instanceId, 42L);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        assertThat(service.requireOwner(instanceId, 42L)).isSameAs(instance);
    }

    @Test
    void requireOwner_allowsAdminViaAllowAll() {
        UUID instanceId = UUID.randomUUID();
        FlowInstance instance = ownerInstance(instanceId, 42L);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        assertThat(service.requireOwner(instanceId, 99L, true)).isSameAs(instance);
    }

    @Test
    void requireOwner_rejectsNonOwner() {
        UUID instanceId = UUID.randomUUID();
        FlowInstance instance = ownerInstance(instanceId, 42L);
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.requireOwner(instanceId, 99L))
                .isInstanceOf(com.superhumans.exception.NotFoundException.class);
    }

    @Test
    void requireOwner_rejectsUnknownInstance() {
        UUID instanceId = UUID.randomUUID();
        when(instanceRepository.findById(instanceId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.requireOwner(instanceId, 42L))
                .isInstanceOf(com.superhumans.exception.NotFoundException.class);
    }

    @Test
    void updateNote_successOnInProgress() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        StepExecution execution = executionFor(instance, stepOneId);
        instance.setCurrentStepId(stepOneId);
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        when(executionRepository.findById(execution.getId())).thenReturn(Optional.of(execution));
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateNote(instance.getId(), execution.getId(), "note text", 1L);

        assertThat(execution.getNote()).isEqualTo("note text");
        verify(executionRepository).save(execution);
        verify(auditService).logAction(any(), any(), any(), any());
    }

    @Test
    void updateNote_successOnPaused() {
        FlowInstance instance = newInstance(FlowInstanceStatus.PAUSED, snapshotJson());
        StepExecution execution = executionFor(instance, stepOneId);
        instance.setCurrentStepId(stepOneId);
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        when(executionRepository.findById(execution.getId())).thenReturn(Optional.of(execution));
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateNote(instance.getId(), execution.getId(), "paused note", 1L);

        assertThat(execution.getNote()).isEqualTo("paused note");
    }

    @Test
    void updateNote_failsOnCompleted() {
        FlowInstance instance = newInstance(FlowInstanceStatus.COMPLETED, snapshotJson());
        StepExecution execution = executionFor(instance, stepOneId);
        instance.setCurrentStepId(stepOneId);
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.updateNote(instance.getId(), execution.getId(), "note", 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Примітку");
    }

    @Test
    void updateNote_trimsToNull() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        StepExecution execution = executionFor(instance, stepOneId);
        instance.setCurrentStepId(stepOneId);
        execution.setNote("old");
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        when(executionRepository.findById(execution.getId())).thenReturn(Optional.of(execution));
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        service.updateNote(instance.getId(), execution.getId(), "   ", 1L);
        assertThat(execution.getNote()).isNull();

        service.updateNote(instance.getId(), execution.getId(), "", 1L);
        assertThat(execution.getNote()).isNull();

        service.updateNote(instance.getId(), execution.getId(), "  trimmed  ", 1L);
        assertThat(execution.getNote()).isEqualTo("trimmed");
    }

    @Test
    void updateNote_2000Allowed() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        StepExecution execution = executionFor(instance, stepOneId);
        instance.setCurrentStepId(stepOneId);
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        when(executionRepository.findById(execution.getId())).thenReturn(Optional.of(execution));
        when(executionRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
        String note2000 = "a".repeat(2000);

        service.updateNote(instance.getId(), execution.getId(), note2000, 1L);

        assertThat(execution.getNote()).hasSize(2000);
    }

    @Test
    void updateNote_2001Denied() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        StepExecution execution = executionFor(instance, stepOneId);
        instance.setCurrentStepId(stepOneId);
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        when(executionRepository.findById(execution.getId())).thenReturn(Optional.of(execution));
        String note2001 = "a".repeat(2001);

        assertThatThrownBy(() -> service.updateNote(instance.getId(), execution.getId(), note2001, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("2000");
    }

    @Test
    void updateNote_wrongExecutionThrows400() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        FlowInstance otherInstance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        StepExecution execution = StepExecution.builder()
                .instance(otherInstance)
                .stageId(stageId)
                .stepId(stepOneId)
                .attemptNumber(1)
                .status(StepExecutionStatus.IN_PROGRESS)
                .startedAt(java.time.LocalDateTime.now().minusMinutes(5))
                .activeSeconds(0L)
                .build();
        execution.setId(UUID.randomUUID());
        instance.setCurrentStepId(stepOneId);
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        when(executionRepository.findById(execution.getId())).thenReturn(Optional.of(execution));

        assertThatThrownBy(() -> service.updateNote(instance.getId(), execution.getId(), "note", 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("does not belong");
    }

    @Test
    void fail_allowedCategoriesAllSucceed() {
        for (String category : List.of("defect", "materials", "component_damage", "order_cancelled", "patient", "other")) {
            FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
            when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
            when(instanceRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));
            when(failureSnapshotService.create(any(), any(), any(), any(), any()))
                    .thenAnswer(invocation -> new com.superhumans.prosthesismanufacturing.entity.FailureSnapshot());
            service.fail(instance.getId(), category, "desc", null, 1L);
            assertThat(instance.getStatus()).isEqualTo(FlowInstanceStatus.FAILED);
            assertThat(instance.getFailReason()).isEqualTo("desc");
        }
    }

    @Test
    void fail_deniedUppercaseMaterialDefect() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.fail(instance.getId(), "MATERIAL_DEFECT", "desc", null, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Недопустима");
    }

    @Test
    void fail_deniedNullCategory() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.fail(instance.getId(), null, "desc", null, 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Недопустима");
    }

    @Test
    void pauseOperativeIntervention() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        service.pause(instance.getId(), new PauseRequest(PauseCategory.OPERATIVE_INTERVENTION), 1L);
        assertThat(instance.getStatus()).isEqualTo(FlowInstanceStatus.PAUSED);
        assertThat(instance.getPauseCategory()).isEqualTo(PauseCategory.OPERATIVE_INTERVENTION);
    }

    @Test
    void pauseWentAbroad() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        service.pause(instance.getId(), new PauseRequest(PauseCategory.WENT_ABROAD), 1L);
        assertThat(instance.getStatus()).isEqualTo(FlowInstanceStatus.PAUSED);
        assertThat(instance.getPauseCategory()).isEqualTo(PauseCategory.WENT_ABROAD);
    }

    @Test
    void pauseReamputation() {
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotJson());
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        service.pause(instance.getId(), new PauseRequest(PauseCategory.REAMPUTATION), 1L);
        assertThat(instance.getStatus()).isEqualTo(FlowInstanceStatus.PAUSED);
        assertThat(instance.getPauseCategory()).isEqualTo(PauseCategory.REAMPUTATION);
    }

    @Test
    void pauseFailsWhenNotInProgress() {
        FlowInstance instance = newInstance(FlowInstanceStatus.PAUSED, snapshotJson());
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        assertThatThrownBy(() -> service.pause(instance.getId(), new PauseRequest(PauseCategory.VLC_PASSING), 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("in-progress");
        FlowInstance completed = newInstance(FlowInstanceStatus.COMPLETED, snapshotJson());
        when(instanceRepository.findById(completed.getId())).thenReturn(Optional.of(completed));
        assertThatThrownBy(() -> service.pause(completed.getId(), new PauseRequest(PauseCategory.VLC_PASSING), 1L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void backwardAllowBackwardFalseThrows() {
        UUID customStageId = stageId;
        SnapshotTemplate snapshot = SnapshotTemplate.builder()
                .name("TP-UL-01")
                .version(1)
                .stages(List.of(SnapshotStage.builder()
                        .id(customStageId)
                        .name("Stage")
                        .steps(List.of(
                                SnapshotStep.builder().id(stepOneId).name("Step1").allowBackward(false).elements(List.of()).build(),
                                SnapshotStep.builder().id(stepTwoId).name("Step2").allowBackward(true).elements(List.of()).build()))
                        .build()))
                .build();
        String snapshotStr = parser.toJson(snapshot);
        FlowInstance instance = newInstance(FlowInstanceStatus.IN_PROGRESS, snapshotStr);
        instance.setCurrentStageId(customStageId);
        instance.setCurrentStepId(stepTwoId);
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.backward(instance.getId(), 1L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("заборонено");
    }

    @Test
    void backwardFailsWhenNotInProgress() {
        FlowInstance instance = newInstance(FlowInstanceStatus.COMPLETED, snapshotJson());
        when(instanceRepository.findById(instance.getId())).thenReturn(Optional.of(instance));
        assertThatThrownBy(() -> service.backward(instance.getId(), 1L))
                .isInstanceOf(BadRequestException.class);
    }

    private static FlowInstance ownerInstance(UUID id, long assignedUserId) {
        FlowInstance instance = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .orderId(UUID.randomUUID())
                .assignedUserId(assignedUserId)
                .build();
        instance.setId(id);
        return instance;
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
