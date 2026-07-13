package com.superhumans.service;

import com.superhumans.dto.OrderExecutionCreateRequest;
import com.superhumans.dto.OrderExecutionPatchRequest;
import com.superhumans.dto.OrderExecutionResponse;
import com.superhumans.entity.ClinicalDay;
import com.superhumans.entity.ClinicalDayStatus;
import com.superhumans.entity.MedicalOrder;
import com.superhumans.entity.MedicalOrderStatus;
import com.superhumans.entity.OrderExecution;
import com.superhumans.entity.OrderExecutionStatus;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.OrderExecutionMapper;
import com.superhumans.repository.ClinicalDayRepository;
import com.superhumans.repository.MedicalOrderRepository;
import com.superhumans.repository.OrderExecutionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class OrderExecutionService {

    private final OrderExecutionRepository orderExecutionRepository;
    private final MedicalOrderRepository medicalOrderRepository;
    private final ClinicalDayRepository clinicalDayRepository;
    private final AuditService auditService;

    public OrderExecutionResponse getExecution(UUID id) {
        OrderExecution execution = orderExecutionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order execution not found: " + id));
        return OrderExecutionMapper.toResponse(execution);
    }

    public List<OrderExecutionResponse> getExecutionsByOrder(UUID orderId) {
        return orderExecutionRepository.findByOrderId(orderId)
                .stream().map(OrderExecutionMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public OrderExecutionResponse createExecution(UUID orderId, OrderExecutionCreateRequest request, UUID userId) {
        MedicalOrder order = medicalOrderRepository.findById(orderId)
                .orElseThrow(() -> new NotFoundException("Medical order not found: " + orderId));

        if (order.getStatus() != MedicalOrderStatus.ACTIVE) {
            throw new DocumentLockedException("Order is not active and cannot be executed");
        }

        ClinicalDay day = order.getClinicalDay();
        if (day.getStatus() == ClinicalDayStatus.DOCTOR_SIGNED
                || day.getStatus() == ClinicalDayStatus.CLOSED) {
            throw new DocumentLockedException("Clinical day is signed and cannot be modified");
        }

        OrderExecution execution = OrderExecutionMapper.toEntity(request);
        execution.setOrder(order);
        execution.setExecutedBy(userId);
        execution.setCreatedBy(userId);
        execution.setUpdatedBy(userId);
        execution = orderExecutionRepository.save(execution);
        auditService.logAction("OrderExecution", execution.getId(), "EXECUTE", userId);
        return OrderExecutionMapper.toResponse(execution);
    }

    @Transactional
    public OrderExecutionResponse updateExecution(UUID id, OrderExecutionPatchRequest request, UUID userId) {
        OrderExecution execution = orderExecutionRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Order execution not found: " + id));

        if (!execution.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Order execution was modified by another user");
        }

        MedicalOrder order = execution.getOrder();
        ClinicalDay day = order.getClinicalDay();
        if (day.getStatus() == ClinicalDayStatus.DOCTOR_SIGNED
                || day.getStatus() == ClinicalDayStatus.CLOSED) {
            throw new DocumentLockedException("Clinical day is signed and cannot be modified");
        }

        if (request.getActualDose() != null) execution.setActualDose(request.getActualDose());
        if (request.getComment() != null) execution.setComment(request.getComment());
        if (request.getExecutedBy() != null) execution.setExecutedBy(request.getExecutedBy());
        execution.setUpdatedBy(userId);
        execution = orderExecutionRepository.save(execution);
        auditService.logUpdate("OrderExecution", id, userId, null, "Updated execution");
        return OrderExecutionMapper.toResponse(execution);
    }
}
