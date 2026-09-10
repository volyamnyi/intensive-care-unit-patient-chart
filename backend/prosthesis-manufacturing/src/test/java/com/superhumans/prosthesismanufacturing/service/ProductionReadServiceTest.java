package com.superhumans.prosthesismanufacturing.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.superhumans.exception.BadRequestException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.prosthesismanufacturing.dto.ProductionDetailDto;
import com.superhumans.prosthesismanufacturing.dto.ProductionQuery;
import com.superhumans.prosthesismanufacturing.dto.ProductionSummaryDto;
import com.superhumans.prosthesismanufacturing.dto.ProductionTeamRowDto;
import com.superhumans.prosthesismanufacturing.dto.ProductionWorkItemDto;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsOrderResponse;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsPatientResponse;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.FlowTemplate;
import com.superhumans.prosthesismanufacturing.entity.ProductType;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
import com.superhumans.prosthesismanufacturing.entity.TemplateStatus;
import com.superhumans.prosthesismanufacturing.mapper.ProstheticsOrderMapper;
import com.superhumans.prosthesismanufacturing.mapper.ProstheticsPatientMapper;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowTemplateRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsPatientRepository;
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
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyCollection;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ProductionReadServiceTest {

    private static final UUID STAGE = UUID.fromString("d0000012-0000-0000-0000-000000000012");
    private static final UUID STEP = UUID.fromString("e0000020-0000-0000-0000-000000000020");
    private static final LocalDateTime NOW = LocalDateTime.of(2026, 9, 10, 12, 0, 0);

    @Mock FlowInstanceRepository instanceRepository;
    @Mock ProstheticsOrderRepository orderRepository;
    @Mock ProstheticsPatientRepository patientRepository;
    @Mock FlowTemplateRepository templateRepository;
    @Mock StepExecutionRepository executionRepository;
    @Mock BrakEventRepository brakEventRepository;
    @Mock UserRepository userRepository;
    @Mock FlowInstanceService instanceService;
    @Mock BrakService brakService;
    @Mock ProstheticsOrderService orderService;
    @Mock ProductionNormativeService normativeService;
    @Mock ProstheticsOrderMapper orderMapper;
    @Mock ProstheticsPatientMapper patientMapper;

    TemplateSnapshotParser parser;
    ProductionReadService service;

    static final ProductionNormativeService.Normative NORM =
            new ProductionNormativeService.Normative(1.5, 7);

    @BeforeEach
    void setUp() {
        parser = new TemplateSnapshotParser(new ObjectMapper());
        service = new ProductionReadService(instanceRepository, orderRepository, patientRepository,
                templateRepository, executionRepository, brakEventRepository, userRepository, parser,
                instanceService, brakService, orderService, normativeService, orderMapper, patientMapper);
        service.setClock(Clock.fixed(NOW.atZone(ZoneId.systemDefault()).toInstant(), ZoneId.systemDefault()));
        lenient().when(normativeService.get()).thenReturn(NORM);
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

    private static java.util.Set<String> flags(FlowInstanceStatus status, int brak, int rework,
            Long assignee) {
        return ProductionReadService.attentionFlags(status, brak, rework, assignee,
                100L, 3600L, NOW.minusHours(1), NOW, NORM);
    }

    @Test
    void attentionFlags_eachRuleSeparately() {
        assertThat(flags(FlowInstanceStatus.FAILED, 0, 0, 5L))
                .containsExactly(ProductionReadService.FLAG_FAILED);
        assertThat(flags(FlowInstanceStatus.IN_PROGRESS, 2, 0, 5L))
                .containsExactly(ProductionReadService.FLAG_REPEAT_BRAK);
        assertThat(flags(FlowInstanceStatus.IN_PROGRESS, 1, 0, 5L))
                .isEmpty();
        assertThat(flags(FlowInstanceStatus.BRANCHED, 1, 2, 5L))
                .containsExactlyInAnyOrder(
                        ProductionReadService.FLAG_REWORK);
        assertThat(flags(FlowInstanceStatus.NEW, 0, 0, null))
                .containsExactly(ProductionReadService.FLAG_NO_ASSIGNEE);
        assertThat(flags(FlowInstanceStatus.IN_PROGRESS, 0, 0, 5L))
                .isEmpty();
    }

    @Test
    void attentionFlags_overdueAndStale() {
        // Overdue: elapsed beyond expected * 1.5.
        assertThat(ProductionReadService.attentionFlags(FlowInstanceStatus.IN_PROGRESS, 0, 0, 5L,
                5401L, 3600L, NOW.minusHours(1), NOW, NORM))
                .containsExactly(ProductionReadService.FLAG_OVERDUE);
        // Boundary: exactly at the norm is not overdue.
        assertThat(ProductionReadService.attentionFlags(FlowInstanceStatus.IN_PROGRESS, 0, 0, 5L,
                5400L, 3600L, NOW.minusHours(1), NOW, NORM))
                .isEmpty();
        // Unknown norm never flags overdue.
        assertThat(ProductionReadService.attentionFlags(FlowInstanceStatus.IN_PROGRESS, 0, 0, 5L,
                999999L, null, NOW.minusHours(1), NOW, NORM))
                .isEmpty();
        // Stale: open item idle for 7+ days.
        assertThat(ProductionReadService.attentionFlags(FlowInstanceStatus.PAUSED, 0, 0, 5L,
                100L, 3600L, NOW.minusDays(7), NOW, NORM))
                .containsExactly(ProductionReadService.FLAG_STALE);
        assertThat(ProductionReadService.attentionFlags(FlowInstanceStatus.PAUSED, 0, 0, 5L,
                100L, 3600L, NOW.minusDays(6), NOW, NORM))
                .isEmpty();
        // Terminal statuses are never stale.
        assertThat(ProductionReadService.attentionFlags(FlowInstanceStatus.COMPLETED, 0, 0, 5L,
                100L, 3600L, NOW.minusDays(30), NOW, NORM))
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
        // 10h elapsed vs 1h norm (K=1.5) also trips OVERDUE.
        assertThat(row.getAttentionFlags()).containsExactlyInAnyOrder(
                ProductionReadService.FLAG_REPEAT_BRAK, ProductionReadService.FLAG_OVERDUE);
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

    // --- document matching ---

    @Test
    void matchDocument_prefersMisOrderNumber() {
        DocumentMisDTO first = DocumentMisDTO.builder().documentId(77L)
                .documentUrl("https://mis.local/77").build();
        DocumentMisDTO exact = DocumentMisDTO.builder().documentId(55L)
                .documentUrl("https://mis.local/55").build();

        assertThat(ProductionReadService.matchDocument("MIS-900001-55", List.of(first, exact)))
                .isEqualTo(exact);
    }

    @Test
    void matchDocument_fallsBackToFirstWithUrl() {
        DocumentMisDTO noUrl = DocumentMisDTO.builder().documentId(1L).build();
        DocumentMisDTO withUrl = DocumentMisDTO.builder().documentId(2L)
                .documentUrl("https://mis.local/2").build();

        assertThat(ProductionReadService.matchDocument("PR-LOCAL-1", List.of(noUrl, withUrl)))
                .isEqualTo(withUrl);
        assertThat(ProductionReadService.matchDocument("MIS-900001-999", List.of(noUrl, withUrl)))
                .isEqualTo(withUrl);
        assertThat(ProductionReadService.matchDocument("MIS-900001-55", List.of(noUrl))).isNull();
        assertThat(ProductionReadService.matchDocument("MIS-900001-55", List.of())).isNull();
        assertThat(ProductionReadService.matchDocument("MIS-900001-55", null)).isNull();
    }

    @Test
    void parseMisDocumentId_matrix() {
        assertThat(ProductionReadService.parseMisDocumentId("MIS-900001-55")).isEqualTo(55L);
        assertThat(ProductionReadService.parseMisDocumentId("PR-LOCAL-1")).isNull();
        assertThat(ProductionReadService.parseMisDocumentId("MIS-900001-x")).isNull();
        assertThat(ProductionReadService.parseMisDocumentId(null)).isNull();
    }

    // --- getRow / team ---
    @Test
    void getRow_unknownId_throwsNotFound() {
        when(instanceRepository.findById(any(UUID.class))).thenReturn(Optional.empty());

        assertThatThrownBy(() -> service.getRow(UUID.randomUUID()))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void team_groupsByAssigneeAndSkipsUnassigned() {
        FlowInstance a1 = baseInstance(FlowInstanceStatus.IN_PROGRESS);
        a1.setId(UUID.randomUUID());
        a1.setAssignedUserId(5L);
        a1.setCreatedAt(NOW.minusDays(2));
        a1.setUpdatedAt(NOW);
        FlowInstance a2 = baseInstance(FlowInstanceStatus.PAUSED);
        a2.setId(UUID.randomUUID());
        a2.setAssignedUserId(5L);
        a2.setCreatedAt(NOW.minusDays(1));
        a2.setUpdatedAt(NOW);
        FlowInstance b1 = baseInstance(FlowInstanceStatus.COMPLETED);
        b1.setId(UUID.randomUUID());
        b1.setAssignedUserId(6L);
        b1.setCreatedAt(NOW);
        b1.setUpdatedAt(NOW);
        FlowInstance free = baseInstance(FlowInstanceStatus.NEW);
        free.setId(UUID.randomUUID());
        free.setCreatedAt(NOW);
        free.setUpdatedAt(NOW);

        when(instanceRepository.findAll()).thenReturn(List.of(a1, a2, b1, free));
        when(orderRepository.findWithPatientByIds(anyCollection())).thenReturn(List.of());
        when(templateRepository.findAllById(anyCollection())).thenReturn(List.of());
        User user5 = User.builder().login("p1").fullName("Протезист Один")
                .role(com.superhumans.entity.core.UserRole.PROSTHETIST).build();
        user5.setId(5L);
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of(user5));
        when(executionRepository.sumActiveSecondsByInstanceIds(anyCollection()))
                .thenReturn(List.<Object[]>of());
        when(brakEventRepository.countByInstanceIds(anyCollection()))
                .thenReturn(List.<Object[]>of());
        when(instanceRepository.countChildrenByParentIds(anyCollection()))
                .thenReturn(List.<Object[]>of());

        List<ProductionTeamRowDto> team = service.team();

        assertThat(team).hasSize(2);
        ProductionTeamRowDto first = team.get(0);
        assertThat(first.getUserId()).isEqualTo(5L);
        assertThat(first.getFullName()).isEqualTo("Протезист Один");
        assertThat(first.getInWork()).isEqualTo(1);
        assertThat(first.getPaused()).isEqualTo(1);
        ProductionTeamRowDto second = team.get(1);
        assertThat(second.getUserId()).isEqualTo(6L);
        assertThat(second.getCompleted()).isEqualTo(1);
    }

    // --- detail ---

    @Test
    void detail_maskedWithoutPatientView() {
        UUID instanceId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        FlowInstance instance = baseInstance(FlowInstanceStatus.IN_PROGRESS);
        instance.setId(instanceId);
        instance.setOrderId(orderId);
        instance.setPatientId("900001");
        instance.setAssignedUserId(5L);
        instance.setCreatedAt(NOW.minusHours(3));
        instance.setUpdatedAt(NOW.minusHours(1));
        ProstheticsOrder order = ProstheticsOrder.builder().orderNumber("MIS-900001-55")
                .patient(ProstheticsPatient.builder().id("900001").pib("Сніжко").build()).build();
        order.setId(orderId);

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(orderRepository.findWithPatientByIds(anyCollection())).thenReturn(List.of(order));
        when(templateRepository.findAllById(anyCollection())).thenReturn(List.of());
        when(executionRepository.sumActiveSecondsByInstanceIds(anyCollection()))
                .thenReturn(List.<Object[]>of());
        when(brakEventRepository.countByInstanceIds(anyCollection()))
                .thenReturn(List.<Object[]>of());
        when(instanceRepository.countChildrenByParentIds(anyCollection()))
                .thenReturn(List.<Object[]>of());
        when(instanceService.listExecutions(instanceId, 5L, true)).thenReturn(List.of());
        when(brakService.listBrakEvents(instanceId, 5L, true)).thenReturn(List.of());
        when(brakService.listBranches(instanceId, 5L, true)).thenReturn(List.of());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(patientRepository.findById("900001")).thenReturn(Optional.of(order.getPatient()));
        when(orderMapper.toResponse(order)).thenReturn(
                ProstheticsOrderResponse.builder().orderNumber("MIS-900001-55").build());
        when(patientMapper.toResponse(order.getPatient())).thenReturn(
                ProstheticsPatientResponse.builder().id("900001").pib("Сніжко")
                        .birthDate(LocalDate.of(1990, 1, 1)).build());

        ProductionDetailDto detail = service.detail(instanceId, 5L, false, false);

        assertThat(detail.getWorkItem().getInstanceId()).isEqualTo(instanceId);
        assertThat(detail.isPatientDetailsVisible()).isFalse();
        assertThat(detail.getPatient().getPib()).isEqualTo("Сніжко");
        assertThat(detail.getPatient().getBirthDate()).isNull();
        assertThat(detail.getDocuments()).isEmpty();
        assertThat(detail.getMatchedDocument()).isNull();
        assertThat(detail.isDocumentsUnknown()).isFalse();
    }

    @Test
    void detail_fullWithPatientViewMatchesDocument() {
        UUID instanceId = UUID.randomUUID();
        UUID orderId = UUID.randomUUID();
        FlowInstance instance = baseInstance(FlowInstanceStatus.IN_PROGRESS);
        instance.setId(instanceId);
        instance.setOrderId(orderId);
        instance.setPatientId("900001");
        instance.setAssignedUserId(5L);
        instance.setCreatedAt(NOW.minusHours(3));
        instance.setUpdatedAt(NOW.minusHours(1));
        ProstheticsPatient patient =
                ProstheticsPatient.builder().id("900001").pib("Сніжко").build();
        ProstheticsOrder order = ProstheticsOrder.builder().orderNumber("MIS-900001-55")
                .patient(patient).build();
        order.setId(orderId);
        DocumentMisDTO doc55 = DocumentMisDTO.builder().documentId(55L)
                .documentUrl("https://mis.local/55").build();
        DocumentMisDTO doc77 = DocumentMisDTO.builder().documentId(77L)
                .documentUrl("https://mis.local/77").build();

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(orderRepository.findWithPatientByIds(anyCollection())).thenReturn(List.of(order));
        when(templateRepository.findAllById(anyCollection())).thenReturn(List.of());
        when(executionRepository.sumActiveSecondsByInstanceIds(anyCollection()))
                .thenReturn(List.<Object[]>of());
        when(brakEventRepository.countByInstanceIds(anyCollection()))
                .thenReturn(List.<Object[]>of());
        when(instanceRepository.countChildrenByParentIds(anyCollection()))
                .thenReturn(List.<Object[]>of());
        when(instanceService.listExecutions(instanceId, 5L, true)).thenReturn(List.of());
        when(brakService.listBrakEvents(instanceId, 5L, true)).thenReturn(List.of());
        when(brakService.listBranches(instanceId, 5L, true)).thenReturn(List.of());
        when(orderRepository.findById(orderId)).thenReturn(Optional.of(order));
        when(patientRepository.findById("900001")).thenReturn(Optional.of(patient));
        when(orderMapper.toResponse(order)).thenReturn(
                ProstheticsOrderResponse.builder().orderNumber("MIS-900001-55").build());
        when(patientMapper.toResponse(patient)).thenReturn(
                ProstheticsPatientResponse.builder().id("900001").pib("Сніжко")
                        .birthDate(LocalDate.of(1990, 1, 1)).build());
        when(orderService.getAllLowerLimbsOrdersForByPatientId("900001"))
                .thenReturn(List.of(doc77, doc55));

        ProductionDetailDto detail = service.detail(instanceId, 5L, false, true);

        assertThat(detail.isPatientDetailsVisible()).isTrue();
        assertThat(detail.getPatient().getBirthDate()).isEqualTo(LocalDate.of(1990, 1, 1));
        assertThat(detail.getDocuments()).hasSize(2);
        assertThat(detail.getMatchedDocument()).isEqualTo(doc55);
        assertThat(detail.isDocumentsUnknown()).isFalse();
    }

    @Test
    void detail_foreignWithoutViewAll_throwsNotFound() {
        UUID instanceId = UUID.randomUUID();
        FlowInstance instance = baseInstance(FlowInstanceStatus.IN_PROGRESS);
        instance.setId(instanceId);
        instance.setAssignedUserId(5L);

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));

        assertThatThrownBy(() -> service.detail(instanceId, 99L, false, true))
                .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> service.detail(UUID.randomUUID(), 5L, true, true))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void detail_brokenPatientLink_marksDocumentsUnknown() {
        UUID instanceId = UUID.randomUUID();
        FlowInstance instance = baseInstance(FlowInstanceStatus.IN_PROGRESS);
        instance.setId(instanceId);
        instance.setPatientId("not-a-number");
        instance.setAssignedUserId(5L);
        instance.setCreatedAt(NOW.minusHours(1));
        instance.setUpdatedAt(NOW);

        when(instanceRepository.findById(instanceId)).thenReturn(Optional.of(instance));
        when(orderRepository.findWithPatientByIds(anyCollection())).thenReturn(List.of());
        when(templateRepository.findAllById(anyCollection())).thenReturn(List.of());
        when(executionRepository.sumActiveSecondsByInstanceIds(anyCollection()))
                .thenReturn(List.<Object[]>of());
        when(brakEventRepository.countByInstanceIds(anyCollection()))
                .thenReturn(List.<Object[]>of());
        when(instanceRepository.countChildrenByParentIds(anyCollection()))
                .thenReturn(List.<Object[]>of());
        when(instanceService.listExecutions(instanceId, 5L, true)).thenReturn(List.of());
        when(brakService.listBrakEvents(instanceId, 5L, true)).thenReturn(List.of());
        when(brakService.listBranches(instanceId, 5L, true)).thenReturn(List.of());
        when(patientRepository.findById("not-a-number")).thenReturn(Optional.empty());
        when(orderService.getAllLowerLimbsOrdersForByPatientId("not-a-number"))
                .thenThrow(new NotFoundException("bad id"));

        ProductionDetailDto detail = service.detail(instanceId, 5L, false, true);

        assertThat(detail.isDocumentsUnknown()).isTrue();
        assertThat(detail.getDocuments()).isEmpty();
        assertThat(detail.getMatchedDocument()).isNull();
    }

    @Test
    void summary_aggregatesScope() {
        FlowInstance open = baseInstance(FlowInstanceStatus.IN_PROGRESS);
        open.setId(UUID.randomUUID());
        open.setAssignedUserId(5L);
        open.setStartTime(NOW.minusHours(2));
        open.setCreatedAt(NOW.minusHours(2));
        open.setUpdatedAt(NOW);
        FlowInstance paused = baseInstance(FlowInstanceStatus.PAUSED);
        paused.setId(UUID.randomUUID());
        paused.setAssignedUserId(5L);
        paused.setCreatedAt(NOW.minusDays(1));
        paused.setUpdatedAt(NOW);
        FlowInstance failed = baseInstance(FlowInstanceStatus.FAILED);
        failed.setId(UUID.randomUUID());
        failed.setAssignedUserId(6L);
        failed.setStartTime(NOW.minusHours(4));
        failed.setEndTime(NOW.minusHours(3));
        failed.setCreatedAt(NOW.minusHours(4));
        failed.setUpdatedAt(NOW.minusHours(3));

        when(instanceRepository.findAll()).thenReturn(List.of(open, paused, failed));
        when(instanceRepository.findByAssignedUserId(5L)).thenReturn(List.of(open, paused));
        when(orderRepository.findWithPatientByIds(anyCollection())).thenReturn(List.of());
        when(templateRepository.findAllById(anyCollection())).thenReturn(List.of());
        when(userRepository.findAllById(anyCollection())).thenReturn(List.of());
        when(executionRepository.sumActiveSecondsByInstanceIds(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{open.getId(), 3600L}));
        when(brakEventRepository.countByInstanceIds(anyCollection()))
                .thenReturn(List.<Object[]>of(new Object[]{failed.getId(), 1L}));
        when(instanceRepository.countChildrenByParentIds(anyCollection()))
                .thenReturn(List.<Object[]>of());

        var all = service.summary(null);
        assertThat(all.getTotalItems()).isEqualTo(3);
        assertThat(all.getInWork()).isEqualTo(1);
        assertThat(all.getActive()).isEqualTo(1);
        assertThat(all.getPaused()).isEqualTo(1);
        assertThat(all.getCompleted()).isZero();
        assertThat(all.getFailed()).isEqualTo(1);
        assertThat(all.getBrakItems()).isEqualTo(1);
        assertThat(all.getReworkItems()).isZero();
        assertThat(all.getAvgElapsedSeconds()).isEqualTo((2 * 3600L + 0L + 3600L) / 3);
        assertThat(all.getAvgActiveSeconds()).isEqualTo(3600L / 3);

        var own = service.summary(5L);
        assertThat(own.getTotalItems()).isEqualTo(2);
        assertThat(own.getFailed()).isZero();
        assertThat(own.getAvgElapsedSeconds()).isEqualTo((2 * 3600L) / 2);

        var empty = service.summary(99L);
        assertThat(empty.getTotalItems()).isZero();
        assertThat(empty.getAvgElapsedSeconds()).isNull();
        assertThat(empty.getAvgActiveSeconds()).isNull();
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
