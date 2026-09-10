package com.superhumans.prosthesismanufacturing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superhumans.exception.BadRequestException;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.FlowTemplate;
import com.superhumans.prosthesismanufacturing.entity.LimbSide;
import com.superhumans.prosthesismanufacturing.entity.OrderStatus;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowTemplateRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import com.superhumans.prosthesismanufacturing.service.ProductionNormativeService;
import com.superhumans.prosthesismanufacturing.service.ProductionReadService;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStage;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

/**
 * Normative thresholds over the real DB (manufacturing epic #271, issue
 * #277): PUT/GET roundtrip through {@link ProductionNormativeService} and
 * the resulting OVERDUE flag on dashboard rows.
 */
@SpringBootTest(properties = {"app.seed-data.enabled=false"})
@Transactional("prosthTransactionManager")
class ProductionNormativeIntegrationTest {

    @Autowired private ProductionNormativeService normativeService;
    @Autowired private ProductionReadService readService;
    @Autowired private TemplateSnapshotParser snapshotParser;
    @Autowired private FlowInstanceRepository instanceRepository;
    @Autowired private ProstheticsOrderRepository orderRepository;
    @Autowired private ProstheticsPatientRepository patientRepository;
    @Autowired private FlowTemplateRepository templateRepository;

    @Test
    void update_roundtripsThroughDatabase() {
        ProductionNormativeService.Normative saved = normativeService.update(2.0, 3, 9L);

        assertThat(saved.overdueMultiplier()).isEqualTo(2.0);
        assertThat(saved.staleDays()).isEqualTo(3);
        assertThat(normativeService.get()).isEqualTo(saved);
    }

    @Test
    void update_rejectsOutOfRangeAgainstDatabase() {
        assertThatThrownBy(() -> normativeService.update(0.5, 7, 9L))
                .isInstanceOf(BadRequestException.class);
        // Failed update leaves the defaults intact.
        assertThat(normativeService.get().overdueMultiplier()).isEqualTo(1.5);
    }

    @Test
    void overdueFlag_followsStoredMultiplier() {
        UUID stageId = UUID.randomUUID();
        UUID stepId = UUID.randomUUID();
        String snapshot = snapshotParser.toJson(SnapshotTemplate.builder()
                .name("TP").version(1).productType("LOWER_LIMB")
                .stages(List.of(SnapshotStage.builder().id(stageId).name("Етап").steps(List.of(
                        SnapshotStep.builder().id(stepId).name("Крок")
                                .normDurationMin(60).elements(List.of()).build()
                )).build()))
                .build());
        FlowTemplate template = templateRepository.save(FlowTemplate.builder()
                .name("TP-NORM-" + UUID.randomUUID().toString().substring(0, 8))
                .templateVersion(1)
                .productType(ProductType.LOWER_LIMB)
                .status(TemplateStatus.ACTIVE)
                .build());
        ProstheticsPatient patient = patientRepository.save(ProstheticsPatient.builder()
                .id("9" + String.format("%05d", ThreadLocalRandom.current().nextInt(100000)))
                .pib("Нормативний Пацієнт")
                .build());
        ProstheticsOrder order = orderRepository.save(ProstheticsOrder.builder()
                .orderNumber("PR-NORM-" + UUID.randomUUID().toString().substring(0, 8))
                .patient(patient)
                .productType(ProductType.LOWER_LIMB)
                .limbSide(LimbSide.LEFT)
                .status(OrderStatus.NEW)
                .build());
        // 2h elapsed vs 1h norm: overdue at K=1.0, in norm at K=5.0.
        FlowInstance instance = instanceRepository.save(FlowInstance.builder()
                .templateId(template.getId())
                .patientId(patient.getId())
                .orderId(order.getId())
                .assignedUserId(5L)
                .status(FlowInstanceStatus.IN_PROGRESS)
                .currentStageId(stageId)
                .currentStepId(stepId)
                .startTime(LocalDateTime.now().minusHours(2))
                .totalActiveSeconds(0L)
                .totalIdleSeconds(0L)
                .branchSequence(1)
                .templateSnapshot(snapshot)
                .build());

        normativeService.update(1.0, 30, 9L);
        assertThat(readService.getRow(instance.getId()).getAttentionFlags())
                .contains("OVERDUE");

        normativeService.update(5.0, 30, 9L);
        assertThat(readService.getRow(instance.getId()).getAttentionFlags())
                .doesNotContain("OVERDUE");
    }
}
