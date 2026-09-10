package com.superhumans.prosthesismanufacturing.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.prosthesismanufacturing.dto.ProductionQuery;
import com.superhumans.prosthesismanufacturing.dto.ProductionWorkItemDto;
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
import com.superhumans.prosthesismanufacturing.service.ProductionReadService;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStage;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.ThreadLocalRandom;
import java.util.stream.Collectors;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.transaction.annotation.Transactional;

/**
 * Integration coverage for the production read-model (manufacturing epic #271,
 * issue #273): real DB queries — instance load, batch aggregates, snapshot
 * name/norm resolution, filters, sorts and pagination.
 *
 * <p>Seed: patient A owns TWO orders (multi-order coverage), six instances
 * covering IN_PROGRESS / COMPLETED / FAILED / BRANCHED + branch child / NEW
 * unassigned, executions with active seconds, single + repeated braks.
 */
@SpringBootTest(properties = {"app.seed-data.enabled=false"})
@Transactional("prosthTransactionManager")
class ProductionReadServiceIntegrationTest {

    private static final UUID STAGE_S1 = UUID.fromString("d0000012-0000-0000-0000-000000000012");
    private static final UUID STEP_E1 = UUID.fromString("e0000020-0000-0000-0000-000000000020");
    private static final UUID STAGE_S2 = UUID.fromString("d0000013-0000-0000-0000-000000000013");
    private static final UUID STEP_E2 = UUID.fromString("e0000022-0000-0000-0000-000000000022");

    @Autowired private ProductionReadService readService;
    @Autowired private FlowInstanceRepository instanceRepository;
    @Autowired private ProstheticsOrderRepository orderRepository;
    @Autowired private ProstheticsPatientRepository patientRepository;
    @Autowired private FlowTemplateRepository templateRepository;
    @Autowired private StepExecutionRepository executionRepository;
    @Autowired private BrakEventRepository brakEventRepository;

    private final TemplateSnapshotParser snapshotParser =
            new TemplateSnapshotParser(new ObjectMapper());

    private UUID templateId;
    private UUID order1Id;
    private UUID order2Id;
    private UUID instance1Id;

    @BeforeEach
    void seed() {
        FlowTemplate template = templateRepository.save(FlowTemplate.builder()
                .name("TP-PROD-" + UUID.randomUUID().toString().substring(0, 8))
                .templateVersion(1)
                .productType(ProductType.LOWER_LIMB)
                .status(TemplateStatus.ACTIVE)
                .estimatedDurationMin(600)
                .build());
        templateId = template.getId();

        ProstheticsPatient patientA = patientRepository.save(ProstheticsPatient.builder()
                .id(digitsId())
                .pib("Пацієнт А " + UUID.randomUUID().toString().substring(0, 6))
                .build());
        ProstheticsPatient patientB = patientRepository.save(ProstheticsPatient.builder()
                .id(digitsId())
                .pib("Пацієнт Б " + UUID.randomUUID().toString().substring(0, 6))
                .build());
        ProstheticsOrder order1 = saveOrder(patientA, "06 24 09");
        ProstheticsOrder order2 = saveOrder(patientA, "06 24 10");
        ProstheticsOrder order3 = saveOrder(patientB, "06 25 03");
        order1Id = order1.getId();
        order2Id = order2.getId();

        // I1: open workhorse — executions, idle, repeated braks, snapshot norms.
        FlowInstance i1 = saveInstance(order1.getId(), patientA.getId(), 5L,
                FlowInstanceStatus.IN_PROGRESS, STAGE_S1, STEP_E1,
                LocalDateTime.now().minusDays(5), null, 600L, snapshotJson());
        instance1Id = i1.getId();
        saveExecution(i1, STAGE_S1, STEP_E1, 1000L);
        saveExecution(i1, STAGE_S1, UUID.randomUUID(), 2000L);
        saveBrak(i1.getId());
        saveBrak(i1.getId());

        // I2: completed, second order of the SAME patient, own norm.
        FlowInstance i2 = saveInstance(order2.getId(), patientA.getId(), 5L,
                FlowInstanceStatus.COMPLETED, null, null,
                LocalDateTime.now().minusDays(4), LocalDateTime.now().minusDays(3), 100L,
                snapshotWithNorm(45));
        saveExecution(i2, STAGE_S1, STEP_E1, 500L);

        // I3: failed with a single brak.
        FlowInstance i3 = saveInstance(order3.getId(), patientB.getId(), 6L,
                FlowInstanceStatus.FAILED, null, null,
                LocalDateTime.now().minusHours(3), LocalDateTime.now().minusHours(2), 0L, null);
        i3.setFailReason("defect");
        instanceRepository.save(i3);
        saveBrak(i3.getId());

        // I4/I5: branched original + rework child (child on another stage).
        FlowInstance i4 = saveInstance(order3.getId(), patientB.getId(), 5L,
                FlowInstanceStatus.BRANCHED, null, null,
                LocalDateTime.now().minusDays(2), LocalDateTime.now().minusDays(1), 0L, null);
        FlowInstance i5 = saveInstance(order3.getId(), patientB.getId(), 5L,
                FlowInstanceStatus.IN_PROGRESS, STAGE_S2, STEP_E2,
                LocalDateTime.now().minusMinutes(10), null, 0L, snapshotWithNorm(30));
        i5.setParentInstanceId(i4.getId());
        i5.setBranchSequence(2);
        instanceRepository.save(i5);
        saveBrak(i4.getId());

        // I6: fresh NEW instance, no assignee, no start, no snapshot.
        saveInstance(order1.getId(), patientA.getId(), null,
                FlowInstanceStatus.NEW, null, null, null, null, 0L, null);
    }

    @Test
    void list_mapsAggregatesAndSnapshotData() {
        Map<UUID, ProductionWorkItemDto> rows = allRows();

        assertThat(rows).hasSize(6);
        ProductionWorkItemDto i1 = rows.get(instance1Id);
        assertThat(i1.getOrderId()).isEqualTo(order1Id);
        assertThat(i1.getPatientPib()).startsWith("Пацієнт А");
        assertThat(i1.getProsthetistUserId()).isEqualTo(5L);
        assertThat(i1.getProductCode()).isEqualTo("06 24 09");
        assertThat(i1.getTemplateName()).startsWith("TP-PROD-");
        assertThat(i1.getCurrentStageName()).isEqualTo("Етап один");
        assertThat(i1.getCurrentStepName()).isEqualTo("Крок один");
        assertThat(i1.getStatus()).isEqualTo("IN_PROGRESS");
        assertThat(i1.getActiveSeconds()).isEqualTo(3000L);
        assertThat(i1.getIdleSeconds()).isEqualTo(600L);
        assertThat(i1.getElapsedSeconds()).isGreaterThanOrEqualTo(5 * 86400L - 120);
        assertThat(i1.getExpectedActiveSeconds()).isEqualTo(3600L);
        assertThat(i1.getActiveDeviationSeconds()).isEqualTo(3000L - 3600L);
        assertThat(i1.getBrakCount()).isEqualTo(2);
        assertThat(i1.getReworkCount()).isZero();
        assertThat(i1.isFailed()).isFalse();
        assertThat(i1.getAttentionFlags()).containsExactly("REPEAT_BRAK");
        assertThat(i1.getLastActivityAt()).isNotNull();
    }

    @Test
    void list_resolvesBothOrdersOfSamePatient() {
        Map<UUID, ProductionWorkItemDto> rows = allRows();

        List<ProductionWorkItemDto> patientA = rows.values().stream()
                .filter(r -> r.getPatientPib() != null && r.getPatientPib().startsWith("Пацієнт А"))
                .toList();
        // I1 (order1) + I2 (order2) + I6 (order1): same patient, distinct orders.
        assertThat(patientA).extracting(ProductionWorkItemDto::getOrderId)
                .contains(order1Id, order2Id);
        assertThat(rows.values().stream()
                .filter(r -> r.getOrderId().equals(order2Id)).findFirst().orElseThrow()
                .getProductCode()).isEqualTo("06 24 10");
    }

    @Test
    void list_failedAndReworkFlags() {
        Map<UUID, ProductionWorkItemDto> rows = allRows();

        ProductionWorkItemDto failed = rows.values().stream()
                .filter(ProductionWorkItemDto::isFailed).findFirst().orElseThrow();
        assertThat(failed.getAttentionFlags()).contains("FAILED");
        assertThat(failed.getBrakCount()).isEqualTo(1);

        ProductionWorkItemDto rework = rows.values().stream()
                .filter(r -> r.getReworkCount() == 1).findFirst().orElseThrow();
        assertThat(rework.getStatus()).isEqualTo("BRANCHED");
        assertThat(rework.getAttentionFlags()).contains("REWORK");
    }

    @Test
    void list_filters() {
        // Assignee.
        assertThat(readService.list(ProductionQuery.builder().assigneeId(5L).size(50).build())
                .getTotalElements()).isEqualTo(4);
        // Status.
        assertThat(readService.list(ProductionQuery.builder().status("IN_PROGRESS").size(50).build())
                .getTotalElements()).isEqualTo(2);
        // Stage.
        assertThat(readService.list(ProductionQuery.builder().stageId(STAGE_S1).size(50).build())
                .getTotalElements()).isEqualTo(1);
        // Quality.
        assertThat(readService.list(ProductionQuery.builder()
                .quality(ProductionQuery.Quality.CLEAN).size(50).build()).getTotalElements())
                .isEqualTo(3);
        assertThat(readService.list(ProductionQuery.builder()
                .quality(ProductionQuery.Quality.BRAK).size(50).build()).getTotalElements())
                .isEqualTo(3);
        assertThat(readService.list(ProductionQuery.builder()
                .quality(ProductionQuery.Quality.REPEAT_BRAK).size(50).build()).getTotalElements())
                .isEqualTo(1);
        assertThat(readService.list(ProductionQuery.builder()
                .quality(ProductionQuery.Quality.REWORK).size(50).build()).getTotalElements())
                .isEqualTo(1);
        // Period excludes the 5-day-old instance.
        assertThat(readService.list(ProductionQuery.builder()
                .dateFrom(LocalDateTime.now().minusDays(2)).size(50).build())
                .getContent()).extracting(ProductionWorkItemDto::getInstanceId)
                .doesNotContain(instance1Id);
        // Empty result.
        assertThat(readService.list(ProductionQuery.builder().assigneeId(999L).build())
                .getTotalElements()).isZero();
    }

    @Test
    void list_sortsAndPaginates() {
        Page<ProductionWorkItemDto> longest = readService.list(
                ProductionQuery.builder().sort(ProductionQuery.Sort.LONGEST).size(50).build());
        assertThat(longest.getContent().get(0).getInstanceId()).isEqualTo(instance1Id);

        Page<ProductionWorkItemDto> page = readService.list(
                ProductionQuery.builder().sort(ProductionQuery.Sort.LONGEST).page(1).size(2).build());
        assertThat(page.getTotalElements()).isEqualTo(6);
        assertThat(page.getTotalPages()).isEqualTo(3);
        assertThat(page.getContent()).hasSize(2);

        Page<ProductionWorkItemDto> mostBrak = readService.list(
                ProductionQuery.builder().sort(ProductionQuery.Sort.MOST_BRAK).size(50).build());
        assertThat(mostBrak.getContent().get(0).getInstanceId()).isEqualTo(instance1Id);
    }

    private Map<UUID, ProductionWorkItemDto> allRows() {
        return readService.list(ProductionQuery.builder().size(50).build()).getContent().stream()
                .collect(Collectors.toMap(ProductionWorkItemDto::getInstanceId, r -> r));
    }

    private String digitsId() {
        return "9" + String.format("%05d", ThreadLocalRandom.current().nextInt(100000));
    }

    private ProstheticsOrder saveOrder(ProstheticsPatient patient, String productCode) {
        return orderRepository.save(ProstheticsOrder.builder()
                .orderNumber("PR-PROD-" + UUID.randomUUID().toString().substring(0, 8))
                .patient(patient)
                .productType(ProductType.LOWER_LIMB)
                .limbSide(LimbSide.LEFT)
                .productCode(productCode)
                .status(OrderStatus.NEW)
                .build());
    }

    private FlowInstance saveInstance(UUID orderId, String patientId, Long assignee,
            FlowInstanceStatus status, UUID stageId, UUID stepId,
            LocalDateTime start, LocalDateTime end, Long idle, String snapshot) {
        return instanceRepository.save(FlowInstance.builder()
                .templateId(templateId)
                .patientId(patientId)
                .orderId(orderId)
                .assignedUserId(assignee)
                .status(status)
                .currentStageId(stageId)
                .currentStepId(stepId)
                .startTime(start)
                .endTime(end)
                .totalActiveSeconds(0L)
                .totalIdleSeconds(idle)
                .branchSequence(1)
                .templateSnapshot(snapshot)
                .build());
    }

    private void saveExecution(FlowInstance instance, UUID stageId, UUID stepId, long activeSeconds) {
        executionRepository.save(StepExecution.builder()
                .instance(instance)
                .stageId(stageId)
                .stepId(stepId)
                .status(StepExecutionStatus.COMPLETED)
                .startedAt(LocalDateTime.now().minusDays(1))
                .completedAt(LocalDateTime.now().minusDays(1).plusMinutes(10))
                .activeSeconds(activeSeconds)
                .completedBy(5L)
                .build());
    }

    private void saveBrak(UUID instanceId) {
        brakEventRepository.save(BrakEvent.builder()
                .instanceId(instanceId)
                .stageId(STAGE_S1)
                .stepId(STEP_E1)
                .returnStageId(STAGE_S1)
                .build());
    }

    private String snapshotJson() {
        return snapshotParser.toJson(SnapshotTemplate.builder()
                .name("TP").version(1).productType("LOWER_LIMB")
                .stages(List.of(
                        SnapshotStage.builder().id(STAGE_S1).name("Етап один").steps(List.of(
                                SnapshotStep.builder().id(STEP_E1).name("Крок один")
                                        .normDurationMin(60).elements(List.of()).build()
                        )).build(),
                        SnapshotStage.builder().id(STAGE_S2).name("Етап два").steps(List.of(
                                SnapshotStep.builder().id(STEP_E2).name("Крок два")
                                        .normDurationMin(30).elements(List.of()).build()
                        )).build()))
                .build());
    }

    private String snapshotWithNorm(int minutes) {
        return snapshotParser.toJson(SnapshotTemplate.builder()
                .name("TP").version(1).productType("LOWER_LIMB")
                .stages(List.of(SnapshotStage.builder().id(STAGE_S1).name("Етап один").steps(List.of(
                        SnapshotStep.builder().id(STEP_E1).name("Крок один")
                                .normDurationMin(minutes).elements(List.of()).build()
                )).build()))
                .build());
    }
}
