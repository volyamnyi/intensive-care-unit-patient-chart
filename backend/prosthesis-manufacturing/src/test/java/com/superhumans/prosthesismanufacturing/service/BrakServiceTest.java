package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.exception.BadRequestException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.prosthesismanufacturing.dto.BrakCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.BranchResponse;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.mapper.FlowInstanceMapper;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.StepExecutionRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class BrakServiceTest {

    @Mock FlowInstanceRepository instanceRepository;
    @Mock BrakEventRepository brakEventRepository;
    @Mock StepExecutionRepository executionRepository;
    @Mock FlowInstanceMapper instanceMapper;
    @Mock AuditService auditService;

    TemplateSnapshotParser parser;
    BrakService service;

    UUID ORDER_ID = UUID.randomUUID();
    UUID INSTANCE_ID = UUID.randomUUID();
    static final UUID STAGE_D12 = UUID.fromString("d0000012-0000-0000-0000-000000000012");
    static final UUID STAGE_D13 = UUID.fromString("d0000013-0000-0000-0000-000000000013");
    static final UUID STAGE_D14 = UUID.fromString("d0000014-0000-0000-0000-000000000014");
    static final UUID STAGE_D15 = UUID.fromString("d0000015-0000-0000-0000-000000000015");
    static final UUID STAGE_D16 = UUID.fromString("d0000016-0000-0000-0000-000000000016");
    static final UUID STAGE_D17 = UUID.fromString("d0000017-0000-0000-0000-000000000017");
    static final UUID STEP_E0020 = UUID.fromString("e0000020-0000-0000-0000-000000000020");
    static final UUID STEP_E0022 = UUID.fromString("e0000022-0000-0000-0000-000000000022");
    static final UUID STEP_E0024 = UUID.fromString("e0000024-0000-0000-0000-000000000024");
    static final UUID STEP_E0028 = UUID.fromString("e0000028-0000-0000-0000-000000000028");

    @BeforeEach
    void setUp() {
        parser = new TemplateSnapshotParser(new ObjectMapper());
        service = new BrakService(instanceRepository, brakEventRepository, executionRepository,
                instanceMapper, parser, auditService, new ObjectMapper());
    }

    private String tpLl02Snapshot() {
        SnapshotTemplate snapshot = SnapshotTemplate.builder()
                .name("TP-LL-02")
                .version(1)
                .productType("LOWER_LIMB")
                .stages(List.of(
                        SnapshotStage.builder().id(STAGE_D12).name("Виготовлення гіпсового негатива").steps(List.of(
                                SnapshotStep.builder().id(STEP_E0020).name("Зняття та внесення об'ємних розмірів").elements(List.of()).build()
                        )).build(),
                        SnapshotStage.builder().id(STAGE_D13).name("Виготовлення гіпсової моделі кукси").steps(List.of(
                                SnapshotStep.builder().id(STEP_E0022).name("Виготовлення гіпсового позитива").elements(List.of()).build()
                        )).build(),
                        SnapshotStage.builder().id(STAGE_D14).name("Виготовлення тренувальної гільзи").steps(List.of(
                                SnapshotStep.builder().id(STEP_E0024).name("Виготовлення тренувальної гільзи").elements(List.of()).build()
                        )).build(),
                        SnapshotStage.builder().id(STAGE_D15).name("Примірка тренувальної гільзи").steps(List.of(
                                SnapshotStep.builder().id(UUID.randomUUID()).name("Примірка").elements(List.of()).build()
                        )).build(),
                        SnapshotStage.builder().id(STAGE_D16).name("Складання тренувального протеза").steps(List.of(
                                SnapshotStep.builder().id(UUID.randomUUID()).name("Складання").elements(List.of()).build()
                        )).build(),
                        SnapshotStage.builder().id(STAGE_D17).name("Примірювання та коректування тренувального протеза").steps(List.of(
                                SnapshotStep.builder().id(STEP_E0028).name("Примірювання та коректування тренувального протеза").elements(List.of()).build()
                        )).build()
                ))
                .build();
        return parser.toJson(snapshot);
    }

    private FlowInstance inProgressInstance() {
        FlowInstance inst = FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .patientId("900002")
                .orderId(ORDER_ID)
                .assignedUserId(5L)
                .status(FlowInstanceStatus.IN_PROGRESS)
                .currentStageId(STAGE_D17)
                .currentStepId(STEP_E0028)
                .totalActiveSeconds(0L)
                .totalIdleSeconds(0L)
                .reworkCount(0)
                .branchSequence(1)
                .templateSnapshot(tpLl02Snapshot())
                .build();
        inst.setId(INSTANCE_ID);
        return inst;
    }

    private BrakCreateRequest request(UUID returnStageId, boolean soft, boolean pain, String note) {
        return new BrakCreateRequest(returnStageId, soft, pain, note);
    }

    private void mockCommon(FlowInstance instance) {
        when(instanceRepository.findByIdForUpdate(INSTANCE_ID)).thenReturn(Optional.of(instance));
        when(instanceRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(instance));
        when(brakEventRepository.save(any())).thenAnswer(inv -> {
            var e = (com.superhumans.prosthesismanufacturing.entity.BrakEvent) inv.getArgument(0);
            e.setId(UUID.randomUUID());
            return e;
        });
        when(instanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    @Test
    void createBrakAndBranch_success_stage1() {
        FlowInstance instance = inProgressInstance();
        mockCommon(instance);
        BrakCreateRequest req = request(STAGE_D12, true, false, "примітка");
        BranchResponse res = service.createBrakAndBranch(INSTANCE_ID, req, 5L);
        assertThat(res.getReturnStageId()).isEqualTo(STAGE_D12);
        assertThat(res.getOriginalInstanceId()).isEqualTo(INSTANCE_ID);
        assertThat(res.getNewInstanceId()).isNotNull();
        assertThat(instance.getStatus()).isEqualTo(FlowInstanceStatus.BRANCHED);
        verify(brakEventRepository).save(any());
        verify(auditService, times(3)).logAction(any(), any(), any(), eq(5L));
        ArgumentCaptor<FlowInstance> captor = ArgumentCaptor.forClass(FlowInstance.class);
        verify(instanceRepository, times(2)).save(captor.capture());
        FlowInstance branch = captor.getAllValues().stream().filter(f -> !f.getId().equals(INSTANCE_ID)).findFirst().orElse(null);
        assertThat(branch).isNotNull();
        assertThat(branch.getParentInstanceId()).isEqualTo(INSTANCE_ID);
        assertThat(branch.getCurrentStageId()).isEqualTo(STAGE_D12);
        assertThat(branch.getCurrentStepId()).isEqualTo(STEP_E0020);
    }

    @Test
    void createBrakAndBranch_success_stage2() {
        FlowInstance instance = inProgressInstance();
        mockCommon(instance);
        BranchResponse res = service.createBrakAndBranch(INSTANCE_ID, request(STAGE_D13, false, true, null), 5L);
        assertThat(res.getReturnStageId()).isEqualTo(STAGE_D13);
        ArgumentCaptor<FlowInstance> captor = ArgumentCaptor.forClass(FlowInstance.class);
        verify(instanceRepository, times(2)).save(captor.capture());
        FlowInstance branch = captor.getAllValues().stream().filter(f -> !f.getId().equals(INSTANCE_ID)).findFirst().orElse(null);
        assertThat(branch.getCurrentStageId()).isEqualTo(STAGE_D13);
        assertThat(branch.getCurrentStepId()).isEqualTo(STEP_E0022);
    }

    @Test
    void createBrakAndBranch_success_stage3() {
        FlowInstance instance = inProgressInstance();
        mockCommon(instance);
        BranchResponse res = service.createBrakAndBranch(INSTANCE_ID, request(STAGE_D14, false, false, ""), 5L);
        assertThat(res.getReturnStageId()).isEqualTo(STAGE_D14);
        ArgumentCaptor<FlowInstance> captor = ArgumentCaptor.forClass(FlowInstance.class);
        verify(instanceRepository, times(2)).save(captor.capture());
        FlowInstance branch = captor.getAllValues().stream().filter(f -> !f.getId().equals(INSTANCE_ID)).findFirst().orElse(null);
        assertThat(branch.getCurrentStageId()).isEqualTo(STAGE_D14);
        assertThat(branch.getCurrentStepId()).isEqualTo(STEP_E0024);
    }

    @Test
    void createBrakAndBranch_success_bothCheckboxes() {
        FlowInstance instance = inProgressInstance();
        mockCommon(instance);
        BranchResponse res = service.createBrakAndBranch(INSTANCE_ID, request(STAGE_D12, true, true, "тест"), 5L);
        ArgumentCaptor<com.superhumans.prosthesismanufacturing.entity.BrakEvent> captor = ArgumentCaptor.forClass(com.superhumans.prosthesismanufacturing.entity.BrakEvent.class);
        verify(brakEventRepository).save(captor.capture());
        assertThat(captor.getValue().getSoftTissueMisalignment()).isTrue();
        assertThat(captor.getValue().getPainDiscomfort()).isTrue();
        assertThat(captor.getValue().getNote()).isEqualTo("тест");
    }

    @Test
    void createBrakAndBranch_success_onlyNote() {
        FlowInstance instance = inProgressInstance();
        mockCommon(instance);
        service.createBrakAndBranch(INSTANCE_ID, request(STAGE_D12, false, false, "примітка"), 5L);
        ArgumentCaptor<com.superhumans.prosthesismanufacturing.entity.BrakEvent> captor = ArgumentCaptor.forClass(com.superhumans.prosthesismanufacturing.entity.BrakEvent.class);
        verify(brakEventRepository).save(captor.capture());
        assertThat(captor.getValue().getNote()).isEqualTo("примітка");
    }

    @Test
    void createBrakAndBranch_success_onlyCheckboxes() {
        FlowInstance instance = inProgressInstance();
        mockCommon(instance);
        service.createBrakAndBranch(INSTANCE_ID, request(STAGE_D12, true, false, ""), 5L);
        ArgumentCaptor<com.superhumans.prosthesismanufacturing.entity.BrakEvent> captor = ArgumentCaptor.forClass(com.superhumans.prosthesismanufacturing.entity.BrakEvent.class);
        verify(brakEventRepository).save(captor.capture());
        assertThat(captor.getValue().getNote()).isNull();
    }

    @Test
    void createBrakAndBranch_rejects_stage4() {
        FlowInstance instance = inProgressInstance();
        when(instanceRepository.findByIdForUpdate(INSTANCE_ID)).thenReturn(Optional.of(instance));
        assertThatThrownBy(() -> service.createBrakAndBranch(INSTANCE_ID, request(STAGE_D15, false, false, null), 5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Недозволений");
    }

    @Test
    void createBrakAndBranch_rejects_stage5() {
        FlowInstance instance = inProgressInstance();
        when(instanceRepository.findByIdForUpdate(INSTANCE_ID)).thenReturn(Optional.of(instance));
        assertThatThrownBy(() -> service.createBrakAndBranch(INSTANCE_ID, request(STAGE_D16, false, false, null), 5L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createBrakAndBranch_rejects_stage6() {
        FlowInstance instance = inProgressInstance();
        when(instanceRepository.findByIdForUpdate(INSTANCE_ID)).thenReturn(Optional.of(instance));
        assertThatThrownBy(() -> service.createBrakAndBranch(INSTANCE_ID, request(STAGE_D17, false, false, null), 5L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createBrakAndBranch_rejects_unknownStage() {
        FlowInstance instance = inProgressInstance();
        when(instanceRepository.findByIdForUpdate(INSTANCE_ID)).thenReturn(Optional.of(instance));
        assertThatThrownBy(() -> service.createBrakAndBranch(INSTANCE_ID, request(UUID.randomUUID(), false, false, null), 5L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createBrakAndBranch_rejects_nonInProgressStatus() {
        FlowInstance instance = inProgressInstance();
        instance.setStatus(FlowInstanceStatus.NEW);
        when(instanceRepository.findByIdForUpdate(INSTANCE_ID)).thenReturn(Optional.of(instance));
        assertThatThrownBy(() -> service.createBrakAndBranch(INSTANCE_ID, request(STAGE_D12, false, false, null), 5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("Брак можливий");
    }

    @Test
    void createBrakAndBranch_rejects_failedStatus() {
        FlowInstance instance = inProgressInstance();
        instance.setStatus(FlowInstanceStatus.FAILED);
        when(instanceRepository.findByIdForUpdate(INSTANCE_ID)).thenReturn(Optional.of(instance));
        assertThatThrownBy(() -> service.createBrakAndBranch(INSTANCE_ID, request(STAGE_D12, false, false, null), 5L))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void createBrakAndBranch_rejects_wrongStep() {
        FlowInstance instance = inProgressInstance();
        instance.setCurrentStepId(UUID.randomUUID());
        when(instanceRepository.findByIdForUpdate(INSTANCE_ID)).thenReturn(Optional.of(instance));
        assertThatThrownBy(() -> service.createBrakAndBranch(INSTANCE_ID, request(STAGE_D12, false, false, null), 5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("кроці 1");
    }

    @Test
    void createBrakAndBranch_rejects_notOwner() {
        FlowInstance instance = inProgressInstance();
        when(instanceRepository.findByIdForUpdate(INSTANCE_ID)).thenReturn(Optional.of(instance));
        assertThatThrownBy(() -> service.createBrakAndBranch(INSTANCE_ID, request(STAGE_D12, false, false, null), 99L))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void createBrakAndBranch_rejects_longNote() {
        FlowInstance instance = inProgressInstance();
        when(instanceRepository.findByIdForUpdate(INSTANCE_ID)).thenReturn(Optional.of(instance));
        String longNote = "a".repeat(2000);
        assertThatThrownBy(() -> service.createBrakAndBranch(INSTANCE_ID, request(STAGE_D12, false, false, longNote), 5L))
                .isInstanceOf(BadRequestException.class)
                .hasMessageContaining("1000");
    }

    @Test
    void createBrakAndBranch_preservesHistory() {
        FlowInstance instance = inProgressInstance();
        // simulate existing executions count 2 for old instance
        when(instanceRepository.findByIdForUpdate(INSTANCE_ID)).thenReturn(Optional.of(instance));
        when(instanceRepository.findByOrderId(ORDER_ID)).thenReturn(List.of(instance));
        when(brakEventRepository.save(any())).thenAnswer(inv -> {
            var e = (com.superhumans.prosthesismanufacturing.entity.BrakEvent) inv.getArgument(0);
            e.setId(UUID.randomUUID()); return e;
        });
        when(instanceRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        when(executionRepository.save(any())).thenAnswer(inv -> inv.getArgument(0));
        service.createBrakAndBranch(INSTANCE_ID, request(STAGE_D12, false, false, null), 5L);
        // old executions not deleted — verify no delete call
        verify(executionRepository, never()).delete(any());
        // new branch has its own execution
        verify(executionRepository).save(any());
    }

    @Test
    void createBrakAndBranch_branchStartsAtFirstStep() {
        FlowInstance instance = inProgressInstance();
        mockCommon(instance);
        service.createBrakAndBranch(INSTANCE_ID, request(STAGE_D12, false, false, null), 5L);
        ArgumentCaptor<com.superhumans.prosthesismanufacturing.entity.StepExecution> captor = ArgumentCaptor.forClass(com.superhumans.prosthesismanufacturing.entity.StepExecution.class);
        verify(executionRepository).save(captor.capture());
        assertThat(captor.getValue().getStepId()).isEqualTo(STEP_E0020);
        assertThat(captor.getValue().getAttemptNumber()).isEqualTo(1);
        assertThat(captor.getValue().getValues()).isNull();
    }

    @Test
    void createBrakAndBranch_parentLink() {
        FlowInstance instance = inProgressInstance();
        mockCommon(instance);
        BranchResponse res = service.createBrakAndBranch(INSTANCE_ID, request(STAGE_D12, false, false, null), 5L);
        ArgumentCaptor<FlowInstance> captor = ArgumentCaptor.forClass(FlowInstance.class);
        verify(instanceRepository, times(2)).save(captor.capture());
        FlowInstance branch = captor.getAllValues().stream().filter(f -> !f.getId().equals(INSTANCE_ID)).findFirst().orElse(null);
        assertThat(branch.getParentInstanceId()).isEqualTo(INSTANCE_ID);
        assertThat(branch.getBranchSequence()).isEqualTo(2);
        assertThat(res.getOriginalInstanceId()).isEqualTo(INSTANCE_ID);
        assertThat(res.getNewInstanceId()).isEqualTo(branch.getId());
    }
}
