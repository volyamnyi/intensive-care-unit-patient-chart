package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.entity.core.AuditLog;
import com.superhumans.mapper.AuditLogMapper;
import com.superhumans.prosthesismanufacturing.dto.InstanceCreateRequest;
import com.superhumans.prosthesismanufacturing.dto.PauseRequest;
import com.superhumans.prosthesismanufacturing.dto.StepCompleteRequest;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
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
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.service.AuditService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Audit trail for TP-LL-02 process mutations: every mutation
 * (CREATE/START/COMPLETE/PAUSE/RESUME/BACKWARD/FAIL/REPLACEMENT)
 * writes an immutable AuditLog with entity=FlowInstance (or StepExecution),
 * entityId, action and userId.
 */
@ExtendWith(MockitoExtension.class)
class AuditServiceTest {

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
    AuditLogRepository auditLogRepository;

    FlowInstanceService service;
    AuditService auditService;
    TemplateSnapshotParser parser;

    UUID orderId = UUID.randomUUID();
    UUID templateId = UUID.randomUUID();
    UUID stage1Id = UUID.randomUUID();
    UUID stage2Id = UUID.randomUUID();
    UUID stepAId = UUID.randomUUID();
    UUID stepBId = UUID.randomUUID();
    UUID stepCId = UUID.randomUUID();
    UUID instanceId = UUID.randomUUID();
    Long prosthetistId = 1L;

    FlowInstance current;

    @BeforeEach
    void setUp() {
        parser = new TemplateSnapshotParser(new ObjectMapper());
        auditService = new AuditService(auditLogRepository, mock(AuditLogMapper.class));
        service = new FlowInstanceService(instanceRepository, templateRepository, orderRepository,
                executionRepository, resourceUsageRepository, decisionRepository,
                mock(FlowInstanceMapper.class), templateService, failureSnapshotService, pdfService,
                auditService, parser, new ObjectMapper());
        current = instance();
    }

    @Nested
    class TP_LL_02 {

        @Test
        void createLogsAuditWithEntityFlowInstance() {
            stubOrder();
            stubTemplate();
            when(templateService.createSnapshot(templateId)).thenReturn(snapshotJson());
            when(instanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.create(InstanceCreateRequest.builder().orderId(orderId).templateId(templateId).build(),
                    prosthetistId);

            AuditLog log = captureAuditLog();
            assertThat(log.getEntity()).isEqualTo("FlowInstance");
            // id is assigned by JPA @PrePersist on save; the unit-test save stub does not run it
            assertThat(log.getEntityId()).isNull();
            assertThat(log.getAction()).isEqualTo("CREATE");
            assertThat(log.getUserId()).isEqualTo(prosthetistId);
            assertThat(log.getIpAddress()).isNull();
            assertThat(log.getIsDeleted()).isFalse();
        }

        @Test
        void startLogsAudit() {
            current.setStatus(FlowInstanceStatus.NEW);
            stubOwnerForUpdateLookup();
            when(instanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(executionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.start(instanceId, prosthetistId);

            AuditLog log = captureAuditLog();
            assertThat(log.getEntity()).isEqualTo("FlowInstance");
            assertThat(log.getEntityId()).isEqualTo(instanceId);
            assertThat(log.getAction()).isEqualTo("START");
            assertThat(log.getUserId()).isEqualTo(prosthetistId);
        }

        @Test
        void completeStepLogsAuditForTheStepExecution() {
            StepExecution execution = executionFor(stepBId);
            stubOwnerLookup();
            when(executionRepository.findById(execution.getId())).thenReturn(Optional.of(execution));
            when(instanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(executionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.completeStep(instanceId, execution.getId(),
                    new StepCompleteRequest(null, null), prosthetistId);

            AuditLog log = captureAuditLog();
            assertThat(log.getEntity()).isEqualTo("StepExecution");
            assertThat(log.getEntityId()).isEqualTo(execution.getId());
            assertThat(log.getAction()).isEqualTo("COMPLETE");
            assertThat(log.getUserId()).isEqualTo(prosthetistId);
        }

        @Test
        void pauseLogsAudit() {
            stubOwnerLookup();
            when(instanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.pause(instanceId, PauseRequest.builder().build(), prosthetistId);

            AuditLog log = captureAuditLog();
            assertThat(log.getEntity()).isEqualTo("FlowInstance");
            assertThat(log.getEntityId()).isEqualTo(instanceId);
            assertThat(log.getAction()).isEqualTo("PAUSE");
            assertThat(log.getUserId()).isEqualTo(prosthetistId);
        }

        @Test
        void resumeLogsAudit() {
            current.setStatus(FlowInstanceStatus.PAUSED);
            current.setPausedAt(LocalDateTime.now().minusHours(5));
            stubOwnerLookup();
            when(instanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.resume(instanceId, prosthetistId);

            assertThat(captureAuditLog().getAction()).isEqualTo("RESUME");
        }

        @Test
        void backwardLogsAudit() {
            stubOwnerLookup();
            when(instanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));
            when(executionRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.backward(instanceId, prosthetistId);

            AuditLog log = captureAuditLog();
            assertThat(log.getEntity()).isEqualTo("FlowInstance");
            assertThat(log.getEntityId()).isEqualTo(instanceId);
            assertThat(log.getAction()).isEqualTo("BACKWARD");
            assertThat(log.getUserId()).isEqualTo(prosthetistId);
        }

        @Test
        void failLogsAudit() {
            stubOwnerLookup();
            when(instanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.fail(instanceId, "materials", "Гільза тріснула", null, prosthetistId);

            AuditLog log = captureAuditLog();
            assertThat(log.getEntity()).isEqualTo("FlowInstance");
            assertThat(log.getEntityId()).isEqualTo(instanceId);
            assertThat(log.getAction()).isEqualTo("FAIL");
            assertThat(log.getUserId()).isEqualTo(prosthetistId);
        }

        @Test
        void replacementLogsAuditForTheNewInstance() {
            current.setStatus(FlowInstanceStatus.FAILED);
            stubOwnerLookup();
            stubOrder();
            when(instanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

            service.replacement(instanceId, prosthetistId);

            AuditLog log = captureAuditLog();
            assertThat(log.getEntity()).isEqualTo("FlowInstance");
            assertThat(log.getAction()).isEqualTo("REPLACEMENT");
            // the audit entry targets the newly created replacement instance
            assertThat(log.getEntityId()).isNotEqualTo(current.getId());
            assertThat(log.getUserId()).isEqualTo(prosthetistId);
        }
    }

    private FlowInstance instance() {
        FlowInstance instance = FlowInstance.builder()
                .templateId(templateId)
                .patientId("900001")
                .orderId(orderId)
                .assignedUserId(prosthetistId)
                .status(FlowInstanceStatus.IN_PROGRESS)
                .currentStageId(stage1Id)
                .currentStepId(stepBId)
                .totalActiveSeconds(0L)
                .totalIdleSeconds(0L)
                .reworkCount(0)
                .templateSnapshot(snapshotJson())
                .startTime(LocalDateTime.now().minusHours(2))
                .build();
        instance.setId(instanceId);
        return instance;
    }

    private StepExecution executionFor(UUID stepId) {
        StepExecution execution = StepExecution.builder()
                .instance(current)
                .stageId(stage1Id)
                .stepId(stepId)
                .attemptNumber(1)
                .status(StepExecutionStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now().minusMinutes(30))
                .activeSeconds(0L)
                .build();
        execution.setId(UUID.randomUUID());
        return execution;
    }

    private String snapshotJson() {
        SnapshotTemplate snapshot = SnapshotTemplate.builder()
                .name("TP-LL-02")
                .version(1)
                .stages(List.of(
                        SnapshotStage.builder()
                                .id(stage1Id)
                                .name("Зняття мірок")
                                .steps(List.of(
                                        SnapshotStep.builder()
                                                .id(stepAId)
                                                .name("Вимірювання довжин")
                                                .allowBackward(true)
                                                .elements(List.of(SnapshotElement.builder()
                                                        .id(UUID.randomUUID())
                                                        .elementType("TEXT_INPUT")
                                                        .label("Матеріал")
                                                        .required(false)
                                                        .build()))
                                                .build(),
                                        SnapshotStep.builder()
                                                .id(stepBId)
                                                .name("Анамнез")
                                                .allowBackward(true)
                                                .elements(List.of())
                                                .build()))
                                .build(),
                        SnapshotStage.builder()
                                .id(stage2Id)
                                .name("Виробництво")
                                .steps(List.of(SnapshotStep.builder()
                                        .id(stepCId)
                                        .name("Збирання")
                                        .allowBackward(true)
                                        .elements(List.of())
                                        .build()))
                                .build()))
                .build();
        return parser.toJson(snapshot);
    }

    private void stubOwnerLookup() {
        when(instanceRepository.findById(any())).thenAnswer(i -> Optional.of(current));
    }

    private void stubOwnerForUpdateLookup() {
        when(instanceRepository.findByIdForUpdate(instanceId)).thenAnswer(i -> Optional.of(current));
    }

    private void stubOrder() {
        ProstheticsPatient patient = ProstheticsPatient.builder()
                .pib("Гаврилюк Олена Миколаївна")
                .build();
        patient.setId("900001");
        ProstheticsOrder order = ProstheticsOrder.builder()
                .orderNumber("PR-LL-02-0001")
                .productType(ProductType.LOWER_LIMB)
                .patient(patient)
                .build();
        order.setId(orderId);
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
    }

    private void stubTemplate() {
        com.superhumans.prosthesismanufacturing.entity.FlowTemplate template =
                com.superhumans.prosthesismanufacturing.entity.FlowTemplate.builder()
                        .name("TP-LL-02")
                        .templateVersion(1)
                        .productType(ProductType.LOWER_LIMB)
                        .status(TemplateStatus.ACTIVE)
                        .build();
        template.setId(templateId);
        when(templateRepository.findById(templateId)).thenReturn(Optional.of(template));
    }

    private AuditLog captureAuditLog() {
        ArgumentCaptor<AuditLog> captor = ArgumentCaptor.forClass(AuditLog.class);
        org.mockito.Mockito.verify(auditLogRepository).save(captor.capture());
        return captor.getValue();
    }
}
