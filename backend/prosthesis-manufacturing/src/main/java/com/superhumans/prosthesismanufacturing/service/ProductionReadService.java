package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.exception.BadRequestException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.mis.dto.DocumentMisDTO;
import com.superhumans.prosthesismanufacturing.dto.BrakEventResponse;
import com.superhumans.prosthesismanufacturing.dto.FlowInstanceResponse;
import com.superhumans.prosthesismanufacturing.dto.ProductionDetailDto;
import com.superhumans.prosthesismanufacturing.dto.ProductionQuery;
import com.superhumans.prosthesismanufacturing.dto.ProductionTeamRowDto;
import com.superhumans.prosthesismanufacturing.dto.ProductionWorkItemDto;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsOrderResponse;
import com.superhumans.prosthesismanufacturing.dto.ProstheticsPatientResponse;
import com.superhumans.prosthesismanufacturing.dto.StepExecutionResponse;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.FlowTemplate;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsPatient;
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
import com.superhumans.repository.core.UserRepository;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;
import java.util.function.Function;
import java.util.stream.Collectors;

/**
 * Read-model behind the prosthetics production dashboard (manufacturing epic
 * #271, issue #273). Builds {@link ProductionWorkItemDto} rows from the
 * existing domain — no parallel accounting, no new entities.
 *
 * <p>Query shape is fixed: 1 instance query (assignee/status filtered) plus
 * batch IN-queries (orders with patients, templates, users, execution active
 * sums, brak counts, branch children counts) — never per-row fetches.
 * Stage/step names and normative times resolve from the immutable per-instance
 * template snapshot, so later template edits cannot shift dashboard history.
 */
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE)
public class ProductionReadService {

    /** Set when the instance finished with a failure. */
    public static final String FLAG_FAILED = "FAILED";
    /** Set on two or more brak events on the same instance. */
    public static final String FLAG_REPEAT_BRAK = "REPEAT_BRAK";
    /** Set when at least one rework branch was created from the instance. */
    public static final String FLAG_REWORK = "REWORK";
    /** Set when no prosthetist is assigned. */
    public static final String FLAG_NO_ASSIGNEE = "NO_ASSIGNEE";

    final FlowInstanceRepository instanceRepository;
    final ProstheticsOrderRepository orderRepository;
    final ProstheticsPatientRepository patientRepository;
    final FlowTemplateRepository templateRepository;
    final StepExecutionRepository executionRepository;
    final BrakEventRepository brakEventRepository;
    final UserRepository userRepository;
    final TemplateSnapshotParser snapshotParser;
    final FlowInstanceService instanceService;
    final BrakService brakService;
    final ProstheticsOrderService orderService;
    final ProstheticsOrderMapper orderMapper;
    final ProstheticsPatientMapper patientMapper;

    /** Test seam: fixed clock for deterministic elapsed-time math. */
    Clock clock = Clock.systemDefaultZone();

    void setClock(Clock clock) {
        this.clock = clock;
    }

    /**
     * Lists dashboard rows for the given filters, newest activity last —
     * actually sorted per {@link ProductionQuery#getSort()}, paged in memory
     * after mapping (volumes are dashboard-scale; revisited with DB paging in
     * the performance pass, issue #281).
     */
    @Transactional(readOnly = true)
    public Page<ProductionWorkItemDto> list(ProductionQuery query) {
        if (query.getPage() < 0) {
            throw new BadRequestException("Page must not be negative");
        }
        if (query.getSize() <= 0) {
            throw new BadRequestException("Size must be positive");
        }
        FlowInstanceStatus status = parseStatus(query.getStatus());

        List<FlowInstance> instances = loadInstances(query.getAssigneeId(), status);
        if (query.getStageId() != null) {
            instances = instances.stream()
                    .filter(i -> Objects.equals(i.getCurrentStageId(), query.getStageId()))
                    .toList();
        }
        if (query.getDateFrom() != null || query.getDateTo() != null) {
            instances = instances.stream()
                    .filter(i -> withinRange(i.getStartTime(), query.getDateFrom(), query.getDateTo()))
                    .toList();
        }

        List<ProductionWorkItemDto> rows = buildRows(instances).stream()
                .filter(r -> matchesQuality(r, query.getQuality()))
                .sorted(comparator(query.getSort()))
                .toList();

        int total = rows.size();
        int from = Math.min(query.getPage() * query.getSize(), total);
        int to = Math.min(from + query.getSize(), total);
        return new PageImpl<>(rows.subList(from, to), PageRequest.of(query.getPage(), query.getSize()), total);
    }

    /**
     * Single dashboard row by instance id.
     *
     * @throws NotFoundException when the instance does not exist
     */
    @Transactional(readOnly = true)
    public ProductionWorkItemDto getRow(UUID instanceId) {
        FlowInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException("Instance not found: " + instanceId));
        return buildRows(List.of(instance)).get(0);
    }

    /**
     * Detail view for one work item. Ownership is enforced here: without
     * {@code viewAll} only the assignee may read, and strangers get 404
     * (same contract as {@code FlowInstanceService.requireOwner}).
     *
     * @param includePatientDetails whether personal data and MIS documents are
     *                              included (caller must hold
     *                              {@code PROSTHETICS_PRODUCTION_PATIENT_VIEW})
     * @throws NotFoundException when the instance does not exist or is foreign
     */
    @Transactional(readOnly = true)
    public ProductionDetailDto detail(UUID instanceId, Long userId,
            boolean viewAll, boolean includePatientDetails) {
        FlowInstance instance = instanceRepository.findById(instanceId)
                .orElseThrow(() -> new NotFoundException("Instance not found: " + instanceId));
        if (!viewAll && !Objects.equals(instance.getAssignedUserId(), userId)) {
            throw new NotFoundException("Instance not found: " + instanceId);
        }
        ProductionWorkItemDto row = buildRows(List.of(instance)).get(0);
        List<StepExecutionResponse> timeline =
                instanceService.listExecutions(instanceId, userId, true);
        List<BrakEventResponse> brakEvents = brakService.listBrakEvents(instanceId, userId, true);
        List<FlowInstanceResponse> branches = brakService.listBranches(instanceId, userId, true);

        ProstheticsOrderResponse order = null;
        ProstheticsPatientResponse patient = null;
        if (instance.getOrderId() != null) {
            ProstheticsOrder orderEntity = orderRepository.findById(instance.getOrderId()).orElse(null);
            if (orderEntity != null) {
                order = orderMapper.toResponse(orderEntity);
            }
        }
        if (instance.getPatientId() != null) {
            ProstheticsPatient patientEntity =
                    patientRepository.findById(instance.getPatientId()).orElse(null);
            if (patientEntity != null) {
                patient = patientMapper.toResponse(patientEntity);
                if (!includePatientDetails) {
                    patient = ProstheticsPatientResponse.builder()
                            .id(patient.getId())
                            .pib(patient.getPib())
                            .build();
                }
            }
        }

        List<DocumentMisDTO> documents = List.of();
        DocumentMisDTO matched = null;
        boolean documentsUnknown = false;
        if (includePatientDetails && instance.getPatientId() != null) {
            try {
                documents = orderService.getAllLowerLimbsOrdersForByPatientId(instance.getPatientId());
                matched = matchDocument(row.getOrderNumber(), documents);
            } catch (NotFoundException e) {
                documentsUnknown = true;
            }
        }
        return ProductionDetailDto.builder()
                .workItem(row)
                .timeline(timeline)
                .brakEvents(brakEvents)
                .branches(branches)
                .order(order)
                .patient(patient)
                .patientDetailsVisible(includePatientDetails)
                .documents(documents)
                .matchedDocument(matched)
                .documentsUnknown(documentsUnknown)
                .build();
    }

    /**
     * Team workload aggregation: one row per prosthetist with an assigned
     * item. Unassigned items surface through the NO_ASSIGNEE attention flag,
     * not here.
     */
    @Transactional(readOnly = true)
    public List<ProductionTeamRowDto> team() {
        Map<Long, List<ProductionWorkItemDto>> byUser = buildRows(instanceRepository.findAll())
                .stream()
                .filter(r -> r.getProsthetistUserId() != null)
                .collect(Collectors.groupingBy(ProductionWorkItemDto::getProsthetistUserId));
        return byUser.entrySet().stream()
                .map(e -> {
                    List<ProductionWorkItemDto> rows = e.getValue();
                    return ProductionTeamRowDto.builder()
                            .userId(e.getKey())
                            .fullName(rows.get(0).getProsthetistFullName())
                            .inWork((int) rows.stream().filter(r ->
                                    "NEW".equals(r.getStatus())
                                            || "IN_PROGRESS".equals(r.getStatus())).count())
                            .paused((int) rows.stream().filter(r ->
                                    "PAUSED".equals(r.getStatus())
                                            || "BLOCKED_PATIENT".equals(r.getStatus())
                                            || "BLOCKED_MATERIAL".equals(r.getStatus())).count())
                            .completed((int) rows.stream()
                                    .filter(r -> "COMPLETED".equals(r.getStatus())).count())
                            .failed((int) rows.stream().filter(ProductionWorkItemDto::isFailed).count())
                            .brakItems((int) rows.stream()
                                    .filter(r -> r.getBrakCount() > 0).count())
                            .reworkItems((int) rows.stream()
                                    .filter(r -> r.getReworkCount() > 0).count())
                            .activeSeconds(rows.stream()
                                    .mapToLong(r -> r.getActiveSeconds() == null
                                            ? 0L : r.getActiveSeconds()).sum())
                            .build();
                })
                .sorted(Comparator.comparingInt(ProductionTeamRowDto::getInWork).reversed()
                        .thenComparing(ProductionTeamRowDto::getUserId))
                .toList();
    }

    /**
     * Picks the MIS document backing an order: exact match on the
     * {@code MIS-{patientId}-{documentId}} order number first, else the first
     * document with a URL. Never throws.
     */
    static DocumentMisDTO matchDocument(String orderNumber, List<DocumentMisDTO> documents) {
        if (documents == null || documents.isEmpty()) {
            return null;
        }
        Long documentId = parseMisDocumentId(orderNumber);
        if (documentId != null) {
            for (DocumentMisDTO document : documents) {
                if (documentId.equals(document.getDocumentId())) {
                    return document;
                }
            }
        }
        for (DocumentMisDTO document : documents) {
            if (document.getDocumentUrl() != null && !document.getDocumentUrl().isBlank()) {
                return document;
            }
        }
        return null;
    }

    static Long parseMisDocumentId(String orderNumber) {
        if (orderNumber == null) {
            return null;
        }
        String[] parts = orderNumber.split("-");
        if (parts.length != 3 || !"MIS".equals(parts[0])) {
            return null;
        }
        try {
            return Long.parseLong(parts[2]);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private List<ProductionWorkItemDto> buildRows(List<FlowInstance> instances) {
        if (instances.isEmpty()) {
            return List.of();
        }
        Batch batch = loadBatch(instances);
        LocalDateTime now = LocalDateTime.now(clock);
        return instances.stream()
                .map(i -> toRow(i, batch, now))
                .toList();
    }

    private List<FlowInstance> loadInstances(Long assigneeId, FlowInstanceStatus status) {
        if (assigneeId != null && status != null) {
            return instanceRepository.findByAssignedUserIdAndStatus(assigneeId, status);
        }
        if (assigneeId != null) {
            return instanceRepository.findByAssignedUserId(assigneeId);
        }
        if (status != null) {
            return instanceRepository.findByStatus(status);
        }
        return instanceRepository.findAll();
    }

    private FlowInstanceStatus parseStatus(String status) {
        if (status == null) {
            return null;
        }
        try {
            return FlowInstanceStatus.valueOf(status);
        } catch (IllegalArgumentException e) {
            throw new BadRequestException("Unknown instance status: " + status);
        }
    }

    static boolean withinRange(LocalDateTime value, LocalDateTime from, LocalDateTime to) {
        if (value == null) {
            return false;
        }
        if (from != null && value.isBefore(from)) {
            return false;
        }
        return to == null || !value.isAfter(to);
    }

    /** One batch load per aggregate — no per-row queries past this point. */
    private Batch loadBatch(List<FlowInstance> instances) {
        Batch batch = new Batch();
        if (instances.isEmpty()) {
            return batch;
        }
        List<UUID> ids = instances.stream().map(FlowInstance::getId).toList();

        List<UUID> orderIds = instances.stream().map(FlowInstance::getOrderId)
                .filter(Objects::nonNull).distinct().toList();
        if (!orderIds.isEmpty()) {
            batch.orders = orderRepository.findWithPatientByIds(orderIds).stream()
                    .collect(Collectors.toMap(ProstheticsOrder::getId, Function.identity()));
        }
        List<UUID> templateIds = instances.stream().map(FlowInstance::getTemplateId)
                .filter(Objects::nonNull).distinct().toList();
        if (!templateIds.isEmpty()) {
            batch.templates = templateRepository.findAllById(templateIds).stream()
                    .collect(Collectors.toMap(FlowTemplate::getId, Function.identity()));
        }
        List<Long> userIds = instances.stream().map(FlowInstance::getAssignedUserId)
                .filter(Objects::nonNull).distinct().toList();
        if (!userIds.isEmpty()) {
            batch.userNames = userRepository.findAllById(userIds).stream()
                    .collect(Collectors.toMap(
                            u -> u.getId(), u -> u.getFullName(), (a, b) -> a));
        }
        batch.activeSeconds = toLongMap(executionRepository.sumActiveSecondsByInstanceIds(ids));
        batch.brakCounts = toIntMap(brakEventRepository.countByInstanceIds(ids));
        batch.reworkCounts = toIntMap(instanceRepository.countChildrenByParentIds(ids));
        return batch;
    }

    private static Map<UUID, Long> toLongMap(List<Object[]> rows) {
        Map<UUID, Long> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((UUID) row[0], ((Number) row[1]).longValue());
        }
        return map;
    }

    private static Map<UUID, Integer> toIntMap(List<Object[]> rows) {
        Map<UUID, Integer> map = new HashMap<>();
        for (Object[] row : rows) {
            map.put((UUID) row[0], ((Number) row[1]).intValue());
        }
        return map;
    }

    private ProductionWorkItemDto toRow(FlowInstance instance, Batch batch, LocalDateTime now) {
        ProstheticsOrder order = batch.orders.get(instance.getOrderId());
        FlowTemplate template = batch.templates.get(instance.getTemplateId());
        SnapshotNames names = resolveNames(instance);

        long active = batch.activeSeconds.getOrDefault(instance.getId(), 0L);
        long idle = nullToZero(instance.getTotalIdleSeconds());
        Long expected = expectedActiveSeconds(names.snapshot());
        Long deviation = deviation(active, expected);
        int braks = batch.brakCounts.getOrDefault(instance.getId(), 0);
        int reworks = batch.reworkCounts.getOrDefault(instance.getId(), 0);
        boolean failed = instance.getStatus() == FlowInstanceStatus.FAILED;

        ProductionWorkItemDto.ProductionWorkItemDtoBuilder row = ProductionWorkItemDto.builder()
                .instanceId(instance.getId())
                .orderId(instance.getOrderId())
                .patientId(instance.getPatientId())
                .prosthetistUserId(instance.getAssignedUserId())
                .prosthetistFullName(batch.userNames.get(instance.getAssignedUserId()))
                .templateName(template == null ? null : template.getName())
                .currentStageName(names.stageName())
                .currentStepName(names.stepName())
                .status(instance.getStatus() == null ? null : instance.getStatus().name())
                .startTime(instance.getStartTime())
                .endTime(instance.getEndTime())
                .lastActivityAt(instance.getUpdatedAt())
                .createdAt(instance.getCreatedAt())
                .updatedAt(instance.getUpdatedAt())
                .elapsedSeconds(elapsedSeconds(instance, now))
                .activeSeconds(active)
                .idleSeconds(idle)
                .expectedActiveSeconds(expected)
                .activeDeviationSeconds(deviation)
                .brakCount(braks)
                .reworkCount(reworks)
                .failed(failed)
                .attentionFlags(attentionFlags(instance.getStatus(), braks, reworks,
                        instance.getAssignedUserId()));
        if (order != null) {
            row.patientPib(order.getPatient() == null ? null : order.getPatient().getPib());
            row.orderNumber(order.getOrderNumber());
            row.productCode(order.getProductCode());
            row.productType(order.getProductType() == null ? null : order.getProductType().name());
            row.prosthesisType(order.getProsthesisType());
            row.prescriptionDate(order.getPrescriptionDate());
        }
        return row.build();
    }

    private SnapshotNames resolveNames(FlowInstance instance) {
        if (instance.getCurrentStageId() == null || !hasText(instance.getTemplateSnapshot())) {
            return SnapshotNames.empty();
        }
        try {
            SnapshotTemplate snapshot = snapshotParser.parse(instance.getTemplateSnapshot());
            if (snapshot.getStages() == null) {
                return new SnapshotNames(snapshot, null, null);
            }
            for (SnapshotStage stage : snapshot.getStages()) {
                if (Objects.equals(stage.getId(), instance.getCurrentStageId())) {
                    String stepName = null;
                    if (stage.getSteps() != null && instance.getCurrentStepId() != null) {
                        for (SnapshotStep step : stage.getSteps()) {
                            if (Objects.equals(step.getId(), instance.getCurrentStepId())) {
                                stepName = step.getName();
                                break;
                            }
                        }
                    }
                    return new SnapshotNames(snapshot, stage.getName(), stepName);
                }
            }
        } catch (IllegalArgumentException ignored) {
            // Corrupt snapshot: surface the row with null names, like the wizard does.
        }
        return SnapshotNames.empty();
    }

    private record SnapshotNames(SnapshotTemplate snapshot, String stageName, String stepName) {
        static SnapshotNames empty() {
            return new SnapshotNames(null, null, null);
        }
    }

    /**
     * Normative active time in seconds: sum of snapshot step norms when at
     * least one step carries a norm, else the snapshot template estimate,
     * else {@code null} (unknown).
     */
    static Long expectedActiveSeconds(SnapshotTemplate snapshot) {
        if (snapshot == null || snapshot.getStages() == null) {
            return null;
        }
        long normSumMin = 0;
        boolean hasNorm = false;
        for (SnapshotStage stage : snapshot.getStages()) {
            if (stage.getSteps() == null) {
                continue;
            }
            for (SnapshotStep step : stage.getSteps()) {
                if (step.getNormDurationMin() != null && step.getNormDurationMin() > 0) {
                    normSumMin += step.getNormDurationMin();
                    hasNorm = true;
                }
            }
        }
        if (hasNorm) {
            return normSumMin * 60;
        }
        if (snapshot.getEstimatedDurationMin() != null && snapshot.getEstimatedDurationMin() > 0) {
            return (long) snapshot.getEstimatedDurationMin() * 60;
        }
        return null;
    }

    /** Calendar time in seconds; zero when never started. */
    static long elapsedSeconds(FlowInstance instance, LocalDateTime now) {
        if (instance.getStartTime() == null) {
            return 0;
        }
        LocalDateTime end = instance.getEndTime() != null ? instance.getEndTime() : now;
        return Math.max(Duration.between(instance.getStartTime(), end).getSeconds(), 0);
    }

    static Long deviation(long activeSeconds, Long expectedActiveSeconds) {
        if (expectedActiveSeconds == null) {
            return null;
        }
        return activeSeconds - expectedActiveSeconds;
    }

    static Set<String> attentionFlags(FlowInstanceStatus status, int brakCount,
            int reworkCount, Long assignedUserId) {
        Set<String> flags = new HashSet<>();
        if (status == FlowInstanceStatus.FAILED) {
            flags.add(FLAG_FAILED);
        }
        if (brakCount >= 2) {
            flags.add(FLAG_REPEAT_BRAK);
        }
        if (reworkCount >= 1) {
            flags.add(FLAG_REWORK);
        }
        if (assignedUserId == null) {
            flags.add(FLAG_NO_ASSIGNEE);
        }
        return flags;
    }

    static boolean matchesQuality(ProductionWorkItemDto row, ProductionQuery.Quality quality) {
        if (quality == null || quality == ProductionQuery.Quality.ALL) {
            return true;
        }
        return switch (quality) {
            case ALL -> true;
            case CLEAN -> row.getBrakCount() == 0 && row.getReworkCount() == 0;
            case BRAK -> row.getBrakCount() >= 1;
            case REPEAT_BRAK -> row.getBrakCount() >= 2;
            case REWORK -> row.getReworkCount() >= 1;
        };
    }

    static Comparator<ProductionWorkItemDto> comparator(ProductionQuery.Sort sort) {
        Comparator<ProductionWorkItemDto> tieBreak = Comparator
                .comparing(ProductionWorkItemDto::getCreatedAt,
                        Comparator.nullsLast(Comparator.naturalOrder()))
                .reversed()
                .thenComparing(ProductionWorkItemDto::getInstanceId);
        if (sort == null || sort == ProductionQuery.Sort.NEWEST) {
            return tieBreak;
        }
        return switch (sort) {
            case NEWEST -> tieBreak;
            case OLDEST -> Comparator
                    .comparing(ProductionWorkItemDto::getCreatedAt,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .thenComparing(ProductionWorkItemDto::getInstanceId);
            case LONGEST -> Comparator
                    .comparing(ProductionWorkItemDto::getElapsedSeconds,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed().thenComparing(tieBreak);
            case MOST_BRAK -> Comparator
                    .comparingInt(ProductionWorkItemDto::getBrakCount)
                    .reversed().thenComparing(tieBreak);
            case MOST_IDLE -> Comparator
                    .comparing(ProductionWorkItemDto::getIdleSeconds,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed().thenComparing(tieBreak);
            case MOST_DEVIATION -> Comparator
                    .comparing(ProductionWorkItemDto::getActiveDeviationSeconds,
                            Comparator.nullsLast(Comparator.naturalOrder()))
                    .reversed().thenComparing(tieBreak);
        };
    }

    private static long nullToZero(Long value) {
        return value == null ? 0L : value;
    }

    private static boolean hasText(String value) {
        return value != null && !value.isBlank();
    }

    /** Batch-loaded aggregates for one page of instances. */
    private static final class Batch {
        Map<UUID, ProstheticsOrder> orders = new HashMap<>();
        Map<UUID, FlowTemplate> templates = new HashMap<>();
        Map<Long, String> userNames = new HashMap<>();
        Map<UUID, Long> activeSeconds = new HashMap<>();
        Map<UUID, Integer> brakCounts = new HashMap<>();
        Map<UUID, Integer> reworkCounts = new HashMap<>();
    }
}
