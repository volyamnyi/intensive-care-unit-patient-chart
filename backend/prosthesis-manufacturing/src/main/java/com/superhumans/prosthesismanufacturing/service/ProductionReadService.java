package com.superhumans.prosthesismanufacturing.service;

import com.superhumans.exception.BadRequestException;
import com.superhumans.prosthesismanufacturing.dto.ProductionQuery;
import com.superhumans.prosthesismanufacturing.dto.ProductionWorkItemDto;
import com.superhumans.prosthesismanufacturing.entity.FlowInstance;
import com.superhumans.prosthesismanufacturing.entity.FlowInstanceStatus;
import com.superhumans.prosthesismanufacturing.entity.FlowTemplate;
import com.superhumans.prosthesismanufacturing.entity.ProstheticsOrder;
import com.superhumans.prosthesismanufacturing.repository.BrakEventRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowInstanceRepository;
import com.superhumans.prosthesismanufacturing.repository.FlowTemplateRepository;
import com.superhumans.prosthesismanufacturing.repository.ProstheticsOrderRepository;
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
    final FlowTemplateRepository templateRepository;
    final StepExecutionRepository executionRepository;
    final BrakEventRepository brakEventRepository;
    final UserRepository userRepository;
    final TemplateSnapshotParser snapshotParser;

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

        Batch batch = loadBatch(instances);
        LocalDateTime now = LocalDateTime.now(clock);
        List<ProductionWorkItemDto> rows = instances.stream()
                .map(i -> toRow(i, batch, now))
                .filter(r -> matchesQuality(r, query.getQuality()))
                .sorted(comparator(query.getSort()))
                .toList();

        int total = rows.size();
        int from = Math.min(query.getPage() * query.getSize(), total);
        int to = Math.min(from + query.getSize(), total);
        return new PageImpl<>(rows.subList(from, to), PageRequest.of(query.getPage(), query.getSize()), total);
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
