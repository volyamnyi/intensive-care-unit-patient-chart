package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.exception.BadRequestException;
import com.superhumans.prosthesismanufacturing.dto.ProductionQuery;
import com.superhumans.prosthesismanufacturing.dto.ProductionWorkItemDto;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.FlowTemplate;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowTemplateRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.StepExecutionRepository;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStage;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotStep;
import com.superhumans.prosthesismanufacturing.service.TemplateSnapshotParser.SnapshotTemplate;
import com.superhumans.entity.core.User;
import com.superhumans.repository.core.UserRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;

import java.time.Clock;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionReadServiceTest {

    private static final UUID STAGE = UUID.fromString("d0000012-0000-0000-0000-000000000012");
    private static final UUID STEP = UUID.fromString("e0000020-0000-0000-0000-000000000020");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 10, 12, 0, 0);

    @Mock FlowInstanceRepository instanceRepository;
    @Mock ProstheticsOrderRepository orderRepository;
    @Mock FlowTemplateRepository templateRepository;
    @Mock StepExecutionRepository executionRepository;
    @Mock BrakEventRepository brakEventRepository;
    @Mock UserRepository userRepository;

    TemplateSnapshotParser parser;
    ProductionReadService service;

    @BeforeEach
    void setUp() {
        parser = new TemplateSnapshotParser(new ObjectMapper());
        service = new ProductionReadService(instanceRepository, orderRepository, templateRepository,
                executionRepository, brakEventRepository, userRepository, parser);
        service.setClock(Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
    }

    // --- elapsed ---

    @Test
    void elapsed_neverStarted_isZero() {
        FlowInstance instance = baseInstance(FlowInstanceStatus.NEW);
        instance.setStartTime(null);

        assertThat(ProductionReadService.elapsedSeconds(instance, NOW)).isZero();
    }

    @Test
    void elapsed_open_usesNow() {
        FlowInstance instance = baseInstance(FlowInstanceStatus.IN_PROGRESS);
        instance.setStartTime(NOW.minusHours(5));

        assertThat(ProductionReadService.elapsedSeconds(instance, NOW)).isEqualTo(5 * 3600);
    }

    @Test
    void elapsed_completed_usesEndTime() {
        FlowInstance instance = baseInstance(FlowInstanceStatus.COMPLETED);
        instance.setStartTime(NOW.minusDays(2));
        instance.setEndTime(NOW.minusDays(1));

        assertThat(ProductionReadService.elapsedSeconds(instance, NOW)).isEqualTo(24 * 3600);
    }

    @Test
    void elapsed_endBeforeStart_clampedToZero() {
        FlowInstance instance = baseInstance(FlowInstanceStatus.FAILED);
        instance.setStartTime(NOW);
        instance.setEndTime(NOW.minusHours(1));

        assertThat(ProductionReadService.elapsedSeconds(instance, NOW)).isZero();
    }

    // --- expected / deviation ---

    @Test
    void expected_prefersStepNormSum() {
        SnapshotTemplate snapshot = SnapshotTemplate.builder()
                .estimatedDurationMin(600)
                .stages(List.of(SnapshotStage.builder().id(STAGE).name("S").steps(List.of(
                        SnapshotStep.builder().id(STEP).name("E1").normDurationMin(60)
                                .elements(List.of()).build(),
                        SnapshotStep.builder().id(UUID.randomUUID()).name("E2").normDurationMin(30)
                                .elements(List.of()).build()
                )).build()))
                .build();

        assertThat(ProductionReadService.expectedActiveSeconds(snapshot)).isEqualTo(90 * 60);
    }

    @Test
    void expected_fallsBackToTemplateEstimate() {
        SnapshotTemplate snapshot = SnapshotTemplate.builder()
                .estimatedDurationMin(120)
                .stages(List.of(SnapshotStage.builder().id(STAGE).name("S").steps(List.of(
                        SnapshotStep.builder().id(STEP).name("E1").elements(List.of()).build()
                )).build()))
                .build();

        assertThat(ProductionReadService.expectedActiveSeconds(snapshot)).isEqualTo(120 * 60);
    }

    @Test
    void expected_unknownWithoutNorms() {
        assertThat(ProductionReadService.expectedActiveSeconds(null)).isNull();
        SnapshotTemplate snapshot = SnapshotTemplate.builder().stages(List.of()).build();
        assertThat(ProductionReadService.expectedActiveSeconds(snapshot)).isNull();
        assertThat(ProductionReadService.deviation(100L, null)).isNull();
    }

    @Test
    void deviation_activeMinusExpected() {
        assertThat(ProductionReadService.deviation(5000L, 3600L)).isEqualTo(1400L);
        assertThat(ProductionReadService.deviation(1000L, 3600L)).isEqualTo(-2600L);
    }

    // --- attention flags ---

    @Test
    void attentionFlags_eachRuleSeparately() {
        assertThat(ProductionReadService.attentionFlags(FlowInstanceStatus.FAILED, 0, 0, 5L))
                .containsExactly(ProductionReadService.FLAG_FAILED);
        assertThat(ProductionReadService.attentionFlags(FlowInstanceStatus.IN_PROGRESS, 2, 0, 5L))
                .containsExactly(ProductionReadService.FLAG_REPEAT_BRAK);
        assertThat(ProductionReadService.attentionFlags(FlowInstanceStatus.IN_PROGRESS, 1, 0, 5L))
                .isEmpty();
        assertThat(ProductionReadService.attentionFlags(FlowInstanceStatus.BRANCHED, 1, 2, 5L))
                .containsExactlyInAnyOrder(
                        ProductionReadService.FLAG_REWORK);
        assertThat(ProductionReadService.attentionFlags(FlowInstanceStatus.NEW, 0, 0, null))
                .containsExactly(ProductionReadService.FLAG_NO_ASSIGNEE);
        assertThat(ProductionReadService.attentionFlags(FlowInstanceStatus.IN_PROGRESS, 0, 0, 5L))
                .isEmpty();
    }

    // --- quality filter ---

    @Test
    void matchesQuality_matrix() {
        ProductionWorkItemDto clean = ProductionWorkItemDto.builder().brakCount(0).reworkCount(0).build();
        ProductionWorkItemDto brak = ProductionWorkItemDto.builder().brakCount(1).reworkCount(0).build();
        ProductionWorkItemDto repeat = ProductionWorkItemDto.builder().brakCount(3).reworkCount(1).build();

        assertThat(ProductionReadService.matchesQuality(clean, ProductionQuery.Quality.ALL)).isTrue();
        assertThat(ProductionReadService.matchesQuality(clean, ProductionQuery.Quality.CLEAN)).isTrue();
        assertThat(ProductionReadService.matchesQuality(brak, ProductionQuery.Quality.CLEAN)).isFalse();
        assertThat(ProductionReadService.matchesQuality(brak, ProductionQuery.Quality.BRAK)).isTrue();
        assertThat(ProductionReadService.matchesQuality(brak, ProductionQuery.Quality.REPEAT_BRAK)).isFalse();
        assertThat(ProductionReadService.matchesQuality(repeat, ProductionQuery.Quality.REPEAT_BRAK)).isTrue();
        assertThat(ProductionReadService.matchesQuality(repeat, ProductionQuery.Quality.REWORK)).isTrue();
        assertThat(ProductionReadService.matchesQuality(clean, ProductionQuery.Quality.REWORK)).isFalse();
    }

    // --- list() mapping ---

    @Test
    void list_mapsRowFromBatches() {
        UUID instanceId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        UUID templateId = UUID.randomUUID();
        FlowInstance instance = baseInstance(FlowInstanceStatus.IN_PROGRESS);
        instance.setId(instanceId);
        instance.setOrderId(orderId);
        instance.setTemplateId(templateId);
        instance.setAssignedUserId(5L);
        instance.setCurrentStageId(STAGE);
        instance.setCurrentStepId(STEP);
        instance.setStartTime(NOW.minusHours(10));
        instance.setTotalIdleSeconds(600L);
        instance.setCreatedAt(NOW.minusHours(11));
        instance.setUpdatedAt(NOW.minusHours(1));
        instance.setTemplateSnapshot(snapshotJson());

        ProstheticsPatient patient = ProstheticsPatient.builder().id("900001").pib("Сніжко Іван").build();
        ProstheticsOrder order = ProstheticsOrder.builder().orderNumber("MIS-900001-77")
                .patient(patient).productType(ProductType.LOWER_LIMB).prosthesisType("Модульний")
                .productCode("06 24 09").prescriptionDate(LocalDate.of(2026, 9, 1)).build();
        order.setId(orderId);
        FlowTemplate template = FlowTemplate.builder().name("TP-LL-01").templateVersion(1)
                .productType(ProductType.LOWER_LIMB).status(TemplateStatus.ACTIVE).build();
        template.setId(templateId);
        User user = User.builder().login("prosthetist1").fullName("Іваненко Іван")
                .role(com.superhumans.entity.core.UserRole.PROSTHETIST).build();
        user.setId(5L);

        when(instanceRepository.findAll()).thenReturn(List.of(instance));
        when(orderRepository.findWithPatientByIds(anyCollection())).thenReturn(List.of(order));
        when(templateRepository.findAllById(anyCollection())).thenReturn(List.of(template));
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user));
        when(executionRepository.sumActiveSecondsByInstanceIds(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{instanceId, 3000L}));
        when(brakEventRepository.countByInstanceIds(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{instanceId, 2L}));
        when(instanceRepository.countChildrenByParentIds(anyCollection())).thenReturn(List.of());

        Page<ProductionWorkItemDto> page = service.list(ProductionQuery.unfiltered());

        assertThat(page.getTotalElements()).isEqualTo(1);
        ProductionWorkItemDto row = page.getContent().get(0);
        assertThat(row.getInstanceId()).isEqualTo(instanceId);
        assertThat(row.getPatientId()).isEqualTo("900001");
        assertThat(row.getPatientPib()).isEqualTo("Сніжко Іван");
        assertThat(row.getProsthetistUserId()).isEqualTo(5L);
        assertThat(row.getProsthetistFullName()).isEqualTo("Іваненко Іван");
        assertThat(row.getOrderNumber()).isEqualTo("MIS-900001-77");
        assertThat(row.getProductCode()).isEqualTo("06 24 09");
        assertThat(row.getCurrentStageName()).isEqualTo("Етап один");
        assertThat(row.getCurrentStepName()).isEqualTo("Крок один");
        assertThat(row.getTemplateName()).isEqualTo("TP-LL-01");
        assertThat(row.getElapsedSeconds()).isEqualTo(10 * 3600);
        assertThat(row.getActiveSeconds()).isEqualTo(3000L);
        assertThat(row.getIdleSeconds()).isEqualTo(600L);
        assertThat(row.getExpectedActiveSeconds()).isEqualTo(60 * 60);
        assertThat(row.getActiveDeviationSeconds()).isEqualTo(3000L - 3600L);
        assertThat(row.getBrakCount()).isEqualTo(2);
        assertThat(row.getAttentionFlags()).containsExactly(ProductionReadService.FLAG_REPEAT_BRAK);
        assertThat(row.isFailed()).isFalse();
    }

    @Test
    void list_missingOrderAndCorruptSnapshot_survivesWithNulls() {
        FlowInstance instance = baseInstance(FlowInstanceStatus.NEW);
        instance.setId(UUID.randomUUID());
        instance.setTemplateSnapshot("{corrupt");
        instance.setCreatedAt(NOW.minusHours(2));
        instance.setUpdatedAt(NOW.minusHours(2));

        when(instanceRepository.findAll()).thenReturn(List.of(instance));
        when(orderRepository.findWithPatientByIds(anyCollection())).thenReturn(List.of());
        when(templateRepository.findAllById(anyCollection())).thenReturn(List.of());
        when(executionRepository.sumActiveSecondsByInstanceIds(anyCollection()))
                .thenReturn(List.<Object[]>of());
        when(brakEventRepository.countByInstanceIds(anyCollection())).thenReturn(List.<Object[]>of());
        when(instanceRepository.countChildrenByParentIds(anyCollection())).thenReturn(List.<Object[]>of());

        Page<ProductionWorkItemDto> page = service.list(ProductionQuery.unfiltered());

        assertThat(page.getTotalElements()).isEqualTo(1);
        ProductionWorkItemDto row = page.getContent().get(0);
        assertThat(row.getOrderNumber()).isNull();
        assertThat(row.getCurrentStageName()).isNull();
        assertThat(row.getProsthetistFullName()).isNull();
        assertThat(row.getElapsedSeconds()).isZero();
        assertThat(row.getExpectedActiveSeconds()).isNull();
        assertThat(row.getAttentionFlags()).containsExactly(ProductionReadService.FLAG_NO_ASSIGNEE);
    }

    @Test
    void list_emptyRepository_returnsEmptyPage() {
        when(instanceRepository.findAll()).thenReturn(List.of());

        Page<ProductionWorkItemDto> page = service.list(ProductionQuery.unfiltered());

        assertThat(page.getTotalElements()).isZero();
        assertThat(page.getContent()).isEmpty();
    }

    @Test
    void list_rejectsBadInput() {
        assertThatThrownBy(() -> service.list(ProductionQuery.builder().status("NOPE").build()))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.list(ProductionQuery.builder().page(-1).build()))
                .isInstanceOf(BadRequestException.class);
        assertThatThrownBy(() -> service.list(ProductionQuery.builder().size(0).build()))
                .isInstanceOf(BadRequestException.class);
    }

    @Test
    void list_sortsAndPaginates() {
        FlowInstance oldest = baseInstance(FlowInstanceStatus.IN_PROGRESS);
        oldest.setId(UUID.randomUUID());
        oldest.setStartTime(NOW.minusDays(5));
        oldest.setCreatedAt(NOW.minusDays(5));
        oldest.setUpdatedAt(NOW);
        FlowInstance newest = baseInstance(FlowInstanceStatus.IN_PROGRESS);
        newest.setId(UUID.randomUUID());
        newest.setStartTime(NOW.minusHours(1));
        newest.setCreatedAt(NOW.minusHours(1));
        newest.setUpdatedAt(NOW);

        when(instanceRepository.findAll()).thenReturn(List.of(newest, oldest));
        when(orderRepository.findWithPatientByIds(anyCollection())).thenReturn(List.of());
        when(templateRepository.findAllById(anyCollection())).thenReturn(List.of());
        when(executionRepository.sumActiveSecondsByInstanceIds(anyCollection()))
                .thenReturn(List.<Object[]>of());
        when(brakEventRepository.countByInstanceIds(anyCollection())).thenReturn(List.<Object[]>of());
        when(instanceRepository.countChildrenByParentIds(anyCollection())).thenReturn(List.<Object[]>of());

        Page<ProductionWorkItemDto> longest = service.list(
                ProductionQuery.builder().sort(ProductionQuery.Sort.LONGEST).build());
        assertThat(longest.getContent()).extracting(ProductionWorkItemDto::getInstanceId)
                .containsExactly(oldest.getId(), newest.getId());

        Page<ProductionWorkItemDto> second = service.list(
                ProductionQuery.builder().sort(ProductionQuery.Sort.LONGEST).page(1).size(1).build());
        assertThat(second.getTotalElements()).isEqualTo(2);
        assertThat(second.getContent()).extracting(ProductionWorkItemDto::getInstanceId)
                .containsExactly(newest.getId());
    }

    private FlowInstance baseInstance(FlowInstanceStatus status) {
        return FlowInstance.builder()
                .templateId(UUID.randomUUID())
                .patientId("900001")
                .orderId(UUID.randomUUID())
                .status(status)
                .totalActiveSeconds(0L)
                .totalIdleSeconds(0L)
                .branchSequence(1)
                .build();
    }

    private String snapshotJson() {
        SnapshotTemplate snapshot = SnapshotTemplate.builder()
                .name("TP").version(1).productType("LOWER_LIMB")
                .stages(List.of(SnapshotStage.builder().id(STAGE).name("Етап один").steps(List.of(
                        SnapshotStep.builder().id(STEP).name("Крок один").normDurationMin(60)
                                .elements(List.of()).build()
                )).build()))
                .build();
        return parser.toJson(snapshot);
    }
}
