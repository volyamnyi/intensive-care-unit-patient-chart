package com.superhumans.prosthesismanufacturing.integration;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.superhumans.prosthesismanufacturing.entity.BrakEvent;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationKind;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationOutbox;
import com.superhumans.prosthesismanufacturing.entity.BrakNotificationStatus;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.FlowTemplate;
import com.superhumans.prosthesismanufacturing.entity.LimbSide;
import com.superhumans.prosthesismanufacturing.entity.OrderStatus;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.BrakNotificationOutboxRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowTemplateRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
import java.time.LocalDate;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.transaction.annotation.Transactional;

/**
 * Repository coverage for the threshold escalation groundwork (epic #322,
 * issue #323): {@code BrakEventRepository.countByOrderId} counts the whole
 * order chain across branch instances, and new outbox rows default to
 * {@code SINGLE} with a {@code null} order anchor.
 */
@SpringBootTest(properties = {"app.seed-data.enabled=false", "management.health.mail.enabled=false"})
@Transactional("prosthTransactionManager")
class BrakEventRepositoryIntegrationTest {

    private static final UUID STAGE_D17 = UUID.fromString("d0000017-0000-0000-0000-000000000017");
    private static final UUID STEP_E0028 = UUID.fromString("e0000028-0000-0000-0000-000000000028");
    private static final UUID STAGE_D20 = UUID.fromString("d0000020-0000-0000-0000-000000000020");
    private static final UUID STEP_E0032 = UUID.fromString("e0000032-0000-0000-0000-000000000032");
    private static final UUID RETURN_D12 = UUID.fromString("d0000012-0000-0000-0000-000000000012");

    @Autowired private BrakEventRepository brakEventRepository;
    @Autowired private FlowInstanceRepository instanceRepository;
    @Autowired private ProstheticsOrderRepository orderRepository;
    @Autowired private ProstheticsPatientRepository patientRepository;
    @Autowired private BrakNotificationOutboxRepository outboxRepository;
    @Autowired private FlowTemplateRepository templateRepository;

    private UUID templateId;

    /**
     * Neutralizes the scheduled brak email sweep: without this mock a sweep
     * firing mid-suite would attempt real SMTP to localhost:1025.
     */
    @MockitoBean
    private org.springframework.mail.javamail.JavaMailSender mailSender;

    @Test
    void countByOrderId_sumsChainAcrossBranchInstances() {
        UUID orderA = newOrder();
        UUID orderB = newOrder();

        UUID a1 = newInstance(orderA, FlowInstanceStatus.BRANCHED);
        UUID a2 = newInstance(orderA, FlowInstanceStatus.BRANCHED);
        UUID a3 = newInstance(orderA, FlowInstanceStatus.IN_PROGRESS);
        UUID b1 = newInstance(orderB, FlowInstanceStatus.IN_PROGRESS);

        newBrak(a1, STAGE_D17, STEP_E0028);
        newBrak(a2, STAGE_D17, STEP_E0028);
        newBrak(a3, STAGE_D20, STEP_E0032);
        newBrak(b1, STAGE_D17, STEP_E0028);

        assertThat(brakEventRepository.countByOrderId(orderA)).isEqualTo(3L);
        assertThat(brakEventRepository.countByOrderId(orderB)).isEqualTo(1L);
        assertThat(brakEventRepository.countByOrderId(UUID.randomUUID())).isZero();
    }

    @Test
    void countByOrderIdUpTo_countsEventsUpToCutoff() {
        UUID orderId = newOrder();
        UUID i1 = newInstance(orderId, FlowInstanceStatus.BRANCHED);
        UUID i2 = newInstance(orderId, FlowInstanceStatus.BRANCHED);
        UUID i3 = newInstance(orderId, FlowInstanceStatus.IN_PROGRESS);

        newBrakAt(i1, STAGE_D17, STEP_E0028,
                java.time.LocalDateTime.of(2026, 9, 26, 10, 5));
        newBrakAt(i2, STAGE_D17, STEP_E0028,
                java.time.LocalDateTime.of(2026, 9, 27, 14, 40));
        newBrakAt(i3, STAGE_D20, STEP_E0032,
                java.time.LocalDateTime.of(2026, 9, 28, 11, 20));

        assertThat(brakEventRepository.countByOrderIdUpTo(orderId,
                java.time.LocalDateTime.of(2026, 9, 27, 14, 40))).isEqualTo(2L);
        assertThat(brakEventRepository.countByOrderIdUpTo(orderId,
                java.time.LocalDateTime.of(2026, 9, 28, 11, 20))).isEqualTo(3L);
        assertThat(brakEventRepository.countByOrderIdUpTo(orderId,
                java.time.LocalDateTime.of(2026, 9, 25, 0, 0))).isZero();
    }

    @Test
    void outboxRow_defaultsToSingleWithNullOrder() {
        BrakNotificationOutbox row = outboxRepository.save(BrakNotificationOutbox.builder()
                .brakEventId(UUID.randomUUID())
                .status(BrakNotificationStatus.PENDING)
                .attempts(0)
                .build());

        assertThat(row.getKind()).isEqualTo(BrakNotificationKind.SINGLE);
        assertThat(row.getOrderId()).isNull();
    }

    @Test
    void outboxRow_rejectsThresholdWithoutOrder() {
        BrakNotificationOutbox row = BrakNotificationOutbox.builder()
                .brakEventId(UUID.randomUUID())
                .kind(BrakNotificationKind.THRESHOLD)
                .status(BrakNotificationStatus.PENDING)
                .attempts(0)
                .build();

        assertThatThrownBy(() -> outboxRepository.saveAndFlush(row))
                .hasStackTraceContaining("orderId is required for THRESHOLD rows");
    }

    private UUID newOrder() {
        ProstheticsPatient patient = patientRepository.save(ProstheticsPatient.builder()
                .pib("Лічильник Пацієнт " + UUID.randomUUID().toString().substring(0, 6))
                .birthDate(LocalDate.of(1990, 1, 1))
                .gender("Чоловіча")
                .build());
        ProstheticsOrder order = orderRepository.save(ProstheticsOrder.builder()
                .orderNumber("PR-CNT-" + UUID.randomUUID().toString().substring(0, 8))
                .patient(patient)
                .productType(ProductType.LOWER_LIMB)
                .limbSide(LimbSide.LEFT)
                .status(OrderStatus.NEW)
                .build());
        return order.getId();
    }

    private UUID newInstance(UUID orderId, FlowInstanceStatus status) {
        if (templateId == null) {
            FlowTemplate template = FlowTemplate.builder()
                    .name("TP-LL-02-COUNT-TEST")
                    .description("TP-LL-02 for brak count integration")
                    .templateVersion(1)
                    .productType(ProductType.LOWER_LIMB)
                    .amputationLevel("generic_lower_limb")
                    .limbSide(LimbSide.LEFT)
                    .status(TemplateStatus.ACTIVE)
                    .estimatedDurationMin(540)
                    .build();
            templateId = templateRepository.save(template).getId();
        }
        FlowInstance instance = instanceRepository.save(FlowInstance.builder()
                .templateId(templateId)
                .orderId(orderId)
                .assignedUserId(5L)
                .status(status)
                .build());
        return instance.getId();
    }

    private void newBrak(UUID instanceId, UUID stageId, UUID stepId) {
        newBrakAt(instanceId, stageId, stepId, null);
    }

    private void newBrakAt(UUID instanceId, UUID stageId, UUID stepId,
            java.time.LocalDateTime createdAt) {
        BrakEvent event = BrakEvent.builder()
                .instanceId(instanceId)
                .stageId(stageId)
                .stepId(stepId)
                .returnStageId(RETURN_D12)
                .build();
        if (createdAt != null) {
            event.setCreatedAt(createdAt);
        }
        brakEventRepository.save(event);
    }
}
