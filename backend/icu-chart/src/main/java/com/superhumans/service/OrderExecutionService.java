package com.superhumans.service;

import com.superhumans.dto.OrderExecutionCreateRequest;
import com.superhumans.dto.OrderExecutionPatchRequest;
import com.superhumans.dto.OrderExecutionPlanRequest;
import com.superhumans.dto.OrderExecutionResponse;
import com.superhumans.entity.ClinicalDay;
import com.superhumans.entity.ClinicalDayStatus;
import com.superhumans.entity.MedicalOrder;
import com.superhumans.entity.MedicalOrderStatus;
import com.superhumans.entity.OrderExecution;
import com.superhumans.entity.OrderExecutionStatus;
import com.superhumans.exception.BusinessException;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.ErrorCode;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.OrderExecutionMapper;
import com.superhumans.repository.ClinicalDayRepository;
import com.superhumans.repository.MedicalOrderRepository;
import com.superhumans.repository.OrderExecutionRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Slf4j
@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class OrderExecutionService {

    OrderExecutionRepository orderExecutionRepository;
    MedicalOrderRepository medicalOrderRepository;
    ClinicalDayRepository clinicalDayRepository;
    AuditService auditService;
    FluidBalanceService fluidBalanceService;
    OrderExecutionMapper orderExecutionMapper;

    public OrderExecutionResponse getExecution(UUID id) {
        OrderExecution execution = orderExecutionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order execution not found: " + id));
        return orderExecutionMapper.toResponse(execution);
    }

    public List<OrderExecutionResponse> getExecutionsByOrder(UUID orderId) {
        return orderExecutionRepository.findByOrderId(orderId)
                .stream().map(orderExecutionMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public OrderExecutionResponse plan(UUID orderId, OrderExecutionPlanRequest request, Long userId) {
        MedicalOrder order = assertActiveAndOpen(orderId);
        assertHourInRange(order, request.getHour());

        OrderExecution execution = orderExecutionRepository
                .findByOrderIdAndHour(orderId, request.getHour())
                .orElseGet(() -> OrderExecution.builder()
                        .order(order)
                        .hour(request.getHour())
                        .build());

        if (execution.isCompletedFinished()
                || execution.getStatus() == OrderExecutionStatus.COMPLETED
                || execution.getStatus() == OrderExecutionStatus.PARTIALLY_COMPLETED) {
            throw new DocumentLockedException("Execution for this hour is already completed");
        }

        execution.setPlanned(true);
        execution.setPlannedBy(userId);
        execution.setPlannedAt(LocalDateTime.now());
        execution.setPlannedDose(request.getDose());
        execution.setPlannedFinished(false);
        execution.setStatus(OrderExecutionStatus.PLANNED);
        execution.setCreatedBy(userId);
        execution.setUpdatedBy(userId);
        execution = orderExecutionRepository.save(execution);

        auditService.logAction("OrderExecution", execution.getId(), "PLAN", userId);
        return orderExecutionMapper.toResponse(execution);
    }

    @Transactional
    public OrderExecutionResponse planFinish(UUID orderId, Integer hour, Long userId) {
        MedicalOrder order = assertActiveAndOpen(orderId);
        OrderExecution execution = requireExecution(orderId, hour);

        if (execution.getStatus() != OrderExecutionStatus.PLANNED || !execution.isPlanned()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Execution is not planned for this hour");
        }
        if (execution.isPlannedFinished()) {
            throw new DocumentLockedException("Plan for this hour is already finished");
        }

        execution.setPlannedFinished(true);
        execution.setUpdatedBy(userId);
        execution = orderExecutionRepository.save(execution);

        auditService.logAction("OrderExecution", execution.getId(), "PLAN_FINISH", userId);
        return orderExecutionMapper.toResponse(execution);
    }

    @Transactional
    public OrderExecutionResponse cancel(UUID orderId, Integer hour, Long userId) {
        MedicalOrder order = assertActiveAndOpen(orderId);
        OrderExecution execution = requireExecution(orderId, hour);

        if (execution.getStatus() == OrderExecutionStatus.CANCELLED) {
            throw new DocumentLockedException("Execution for this hour is already cancelled");
        }
        if (execution.getStatus() == OrderExecutionStatus.COMPLETED
                || execution.getStatus() == OrderExecutionStatus.PARTIALLY_COMPLETED) {
            throw new DocumentLockedException("Execution for this hour is already completed");
        }

        execution.setStatus(OrderExecutionStatus.CANCELLED);
        execution.setPlannedFinished(true);
        execution.setUpdatedBy(userId);
        execution = orderExecutionRepository.save(execution);

        auditService.logAction("OrderExecution", execution.getId(), "CANCEL", userId);
        return orderExecutionMapper.toResponse(execution);
    }

    @Transactional
    public OrderExecutionResponse execute(UUID orderId, OrderExecutionCreateRequest request, Long userId) {
        MedicalOrder order = assertActiveAndOpen(orderId);
        assertHourInRange(order, request.getHour());

        OrderExecution execution = requireExecution(orderId, request.getHour());
        if (!execution.isPlanned() || execution.isPlannedFinished()
                || execution.getStatus() != OrderExecutionStatus.PLANNED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Execution is not planned for this hour");
        }

        execution.setExecutedBy(userId);
        execution.setExecutedAt(LocalDateTime.now());
        execution.setActualDose(request.getActualDose());
        execution.setComment(request.getComment());
        execution.setStatus(resolveStatus(request.getActualDose(), execution.getPlannedDose()));
        execution.setUpdatedBy(userId);
        execution = orderExecutionRepository.save(execution);

        if (request.getHour() < LocalDateTime.now().getHour()) {
            log.info("BACK_ENTRY: OrderExecution {} executed for past hour {}", execution.getId(), request.getHour());
            auditService.logAction("OrderExecution", execution.getId(), "BACK_ENTRY", userId);
        }

        auditService.logAction("OrderExecution", execution.getId(), "EXECUTE", userId);
        fluidBalanceService.recalculate(order.getClinicalDay().getId(), userId);
        return orderExecutionMapper.toResponse(execution);
    }

    @Transactional
    public OrderExecutionResponse executeFinish(UUID orderId, Integer hour, Long userId) {
        MedicalOrder order = assertActiveAndOpen(orderId);
        OrderExecution execution = requireExecution(orderId, hour);

        if (execution.getStatus() != OrderExecutionStatus.COMPLETED
                && execution.getStatus() != OrderExecutionStatus.PARTIALLY_COMPLETED) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Execution is not completed for this hour");
        }
        if (execution.isCompletedFinished()) {
            throw new DocumentLockedException("Execution for this hour is already finished");
        }

        execution.setCompletedFinished(true);
        execution.setUpdatedBy(userId);
        execution = orderExecutionRepository.save(execution);

        auditService.logAction("OrderExecution", execution.getId(), "EXECUTE_FINISH", userId);
        return orderExecutionMapper.toResponse(execution);
    }

    @Transactional
    public OrderExecutionResponse updateExecution(UUID id, OrderExecutionPatchRequest request, Long userId) {
        OrderExecution execution = orderExecutionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order execution not found: " + id));

        if (!execution.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Order execution was modified by another user");
        }

        assertActiveAndOpen(execution.getOrder().getId());

        if (execution.isCompletedFinished()
                || execution.getStatus() == OrderExecutionStatus.COMPLETED
                || execution.getStatus() == OrderExecutionStatus.PARTIALLY_COMPLETED
                || execution.getStatus() == OrderExecutionStatus.CANCELLED) {
            throw new DocumentLockedException("Order execution is finished and cannot be modified");
        }

        if (request.getActualDose() != null) execution.setActualDose(request.getActualDose());
        if (request.getComment() != null) execution.setComment(request.getComment());
        execution.setUpdatedBy(userId);
        execution = orderExecutionRepository.save(execution);
        auditService.logUpdate("OrderExecution", id, userId, null, "Updated execution");
        return orderExecutionMapper.toResponse(execution);
    }

    private MedicalOrder assertActiveAndOpen(UUID orderId) {
        MedicalOrder order = medicalOrderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Medical order not found: " + orderId));

        if (order.getStatus() != MedicalOrderStatus.ACTIVE) {
            throw new DocumentLockedException("Order is not active and cannot be modified");
        }

        ClinicalDay day = order.getClinicalDay();
        if (day.getStatus() == ClinicalDayStatus.NURSE_SIGNED
                || day.getStatus() == ClinicalDayStatus.DOCTOR_SIGNED
                || day.getStatus() == ClinicalDayStatus.CLOSED) {
            throw new DocumentLockedException("Clinical day is signed and cannot be modified");
        }
        return order;
    }

    private void assertHourInRange(MedicalOrder order, Integer hour) {
        if (order.getStartTime() != null && hour < order.getStartTime().getHour()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Hour is before the order start time");
        }
        if (order.getEndTime() != null && hour > order.getEndTime().getHour()) {
            throw new BusinessException(ErrorCode.BUSINESS_RULE,
                    "Hour is after the order end time");
        }
    }

    private OrderExecution requireExecution(UUID orderId, Integer hour) {
        return orderExecutionRepository.findByOrderIdAndHour(orderId, hour)
                .orElseThrow(() -> new BusinessException(ErrorCode.BUSINESS_RULE,
                        "No execution record for this hour"));
    }

    private OrderExecutionStatus resolveStatus(String actualDose, String plannedDose) {
        try {
            double actual = Double.parseDouble(actualDose);
            double planned = Double.parseDouble(plannedDose);
            if (actual < planned) {
                return OrderExecutionStatus.PARTIALLY_COMPLETED;
            }
        } catch (NumberFormatException ignored) {
            // non-numeric doses are treated as fully completed
        }
        return OrderExecutionStatus.COMPLETED;
    }
}
