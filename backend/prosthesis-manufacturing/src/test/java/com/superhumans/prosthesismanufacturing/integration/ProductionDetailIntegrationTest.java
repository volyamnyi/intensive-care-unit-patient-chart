package com.superhumans.prosthesismanufacturing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.when;

import com.superhumans.exception.NotFoundException;
import com.superhumans.mis.MisService;
import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.prosthesismanufacturing.dto.ProductionDetailDto;
import com.superhumans.prosthesismanufacturing.entity.BrakEvent;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.FlowTemplate;
import com.superhumans.prosthesismanufacturing.entity.LimbSide;
import com.superhumans.prosthesismanufacturing.entity.OrderStatus;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.entity.StepExecution;
import com.superhumans.prosthesismanufacturing.entity.StepExecutionStatus;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowTemplateRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import com.superhumans.prosthesismanufacturing.repository.StepExecutionRepository;
import com.superhumans.prosthesismanufacturing.service.DocumentUrlAvailability;
import com.superhumans.prosthesismanufacturing.service.ProductionReadService;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Detail-view integration (manufacturing epic #271, issue #274): timeline,
 * quality, order/patient and MIS document matching over the real DB with a
 * stubbed MIS boundary.
 */
@SpringBootTest(properties = {"app.seed-data.enabled=false"})
@Transactional("prosthTransactionManager")
class ProductionDetailIntegrationTest {

    private static final UUID STAGE = UUID.fromString("d0000012-0000-0000-0000-000000000012");
    private static final UUID STEP = UUID.fromString("e0000020-0000-0000-0000-000000000020");

    @Autowired private ProductionReadService readService;
    @Autowired private FlowInstanceRepository instanceRepository;
    @Autowired private ProstheticsOrderRepository orderRepository;
    @Autowired private ProstheticsPatientRepository patientRepository;
    @Autowired private FlowTemplateRepository templateRepository;
    @Autowired private StepExecutionRepository executionRepository;
    @Autowired private BrakEventRepository brakEventRepository;

    @MockitoBean private MisService misService;
    @MockitoBean private DocumentUrlAvailability documentUrlAvailability;

    private UUID instanceId;
    private UUID orderId;
    private UUID templateId;
    private String patientId;
    private long patientNumericId;

    @BeforeEach
    void seed() {
        // Unique digits-only ids per run: never collide with dev leftovers or
        // other runs (the suite must not assume empty tables).
        patientId = "9" + String.format("%05d",
                java.util.concurrent.ThreadLocalRandom.current().nextInt(100000));
        patientNumericId = Long.parseLong(patientId);
        FlowTemplate template = templateRepository.save(FlowTemplate.builder()
                .name("TP-DET-" + UUID.randomUUID().toString().substring(0, 8))
                .templateVersion(1)
                .productType(ProductType.LOWER_LIMB)
                .status(TemplateStatus.ACTIVE)
                .build());
        templateId = template.getId();
        ProstheticsPatient patient = patientRepository.save(ProstheticsPatient.builder()
                .id(patientId)
                .pib("Детальний Пацієнт")
                .build());
        ProstheticsOrder order = orderRepository.save(ProstheticsOrder.builder()
                .orderNumber("MIS-" + patientId + "-55")
                .patient(patient)
                .productType(ProductType.LOWER_LIMB)
                .limbSide(LimbSide.LEFT)
                .productCode("06 24 09")
                .status(OrderStatus.NEW)
                .build());
        orderId = order.getId();
        FlowInstance instance = instanceRepository.save(FlowInstance.builder()
                .templateId(template.getId())
                .patientId(patient.getId())
                .orderId(order.getId())
                .assignedUserId(5L)
                .status(FlowInstanceStatus.IN_PROGRESS)
                .currentStageId(STAGE)
                .currentStepId(STEP)
                .startTime(LocalDateTime.now().minusHours(4))
                .totalActiveSeconds(0L)
                .totalIdleSeconds(0L)
                .branchSequence(1)
                .build());
        instanceId = instance.getId();
        executionRepository.save(StepExecution.builder()
                .instance(instance)
                .stageId(STAGE)
                .stepId(STEP)
                .status(StepExecutionStatus.IN_PROGRESS)
                .startedAt(LocalDateTime.now().minusHours(1))
                .activeSeconds(900L)
                .completedBy(5L)
                .build());
        brakEventRepository.save(BrakEvent.builder()
                .instanceId(instance.getId())
                .stageId(STAGE)
                .stepId(STEP)
                .returnStageId(STAGE)
                .note("тріщина гільзи")
                .build());

        when(misService.getPatientDocuments(patientNumericId)).thenReturn(List.of(
                DocumentMisDTO.builder().documentId(77L).documentTemplateId(121L)
                        .documentUrl("https://mis.local/77").build(),
                DocumentMisDTO.builder().documentId(55L).documentTemplateId(121L)
                        .documentUrl("https://mis.local/55").build()));
        when(documentUrlAvailability.isAvailable(anyString())).thenReturn(true);
    }

    @Test
    void detail_full() {
        ProductionDetailDto detail = readService.detail(instanceId, 5L, true, true);

        assertThat(detail.getWorkItem().getInstanceId()).isEqualTo(instanceId);
        assertThat(detail.getWorkItem().getOrderId()).isEqualTo(orderId);
        assertThat(detail.getWorkItem().getActiveSeconds()).isEqualTo(900L);
        assertThat(detail.getWorkItem().getBrakCount()).isEqualTo(1);
        assertThat(detail.getTimeline()).hasSize(1);
        assertThat(detail.getTimeline().get(0).getActiveSeconds()).isEqualTo(900L);
        assertThat(detail.getBrakEvents()).hasSize(1);
        assertThat(detail.getBrakEvents().get(0).getNote()).isEqualTo("тріщина гільзи");
        assertThat(detail.getBranches()).isEmpty();
        assertThat(detail.getOrder().getOrderNumber()).isEqualTo("MIS-" + patientId + "-55");
        assertThat(detail.getPatient().getPib()).isEqualTo("Детальний Пацієнт");
        assertThat(detail.isPatientDetailsVisible()).isTrue();
        assertThat(detail.getDocuments()).hasSize(2);
        assertThat(detail.getMatchedDocument().getDocumentId()).isEqualTo(55L);
        assertThat(detail.isDocumentsUnknown()).isFalse();
    }

    @Test
    void detail_masked() {
        ProductionDetailDto detail = readService.detail(instanceId, 5L, false, false);

        assertThat(detail.isPatientDetailsVisible()).isFalse();
        assertThat(detail.getPatient().getPib()).isEqualTo("Детальний Пацієнт");
        assertThat(detail.getDocuments()).isEmpty();
        assertThat(detail.getMatchedDocument()).isNull();
        assertThat(detail.getTimeline()).hasSize(1);
        assertThat(detail.getBrakEvents()).hasSize(1);
    }

    @Test
    void detail_foreignWithoutViewAll_throwsNotFound() {
        assertThatThrownBy(() -> readService.detail(instanceId, 99L, false, true))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void detail_unknownId_throwsNotFound() {
        assertThatThrownBy(() -> readService.detail(UUID.randomUUID(), 5L, true, true))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void detail_nullPatient_hasNoDocuments() {
        // patient_id is nullable (FK only constrains non-null values):
        // no patient, no documents, nothing unknown.
        FlowInstance orphan = instanceRepository.save(FlowInstance.builder()
                .templateId(templateId)
                .patientId(null)
                .orderId(orderId)
                .assignedUserId(5L)
                .status(FlowInstanceStatus.COMPLETED)
                .startTime(LocalDateTime.now().minusDays(1))
                .endTime(LocalDateTime.now())
                .totalActiveSeconds(0L)
                .totalIdleSeconds(0L)
                .branchSequence(1)
                .build());

        ProductionDetailDto detail = readService.detail(orphan.getId(), 5L, true, true);

        assertThat(detail.getPatient()).isNull();
        assertThat(detail.isDocumentsUnknown()).isFalse();
        assertThat(detail.getDocuments()).isEmpty();
        assertThat(detail.getMatchedDocument()).isNull();
        assertThat(detail.getWorkItem().getElapsedSeconds()).isGreaterThan(0L);
    }
}
