package com.superhumans.service;

import com.superhumans.dto.OrderExecutionCreateRequest;
import com.superhumans.dto.OrderExecutionPatchRequest;
import com.superhumans.dto.OrderExecutionResponse;
import com.superhumans.entity.*;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.OrderExecutionMapper;
import com.superhumans.repository.ClinicalDayRepository;
import com.superhumans.repository.MedicalOrderRepository;
import com.superhumans.repository.OrderExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class OrderExecutionServiceTest {

    @Mock
    private OrderExecutionRepository orderExecutionRepository;

    @Mock
    private MedicalOrderRepository medicalOrderRepository;

    @Mock
    private ClinicalDayRepository clinicalDayRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private FluidBalanceService fluidBalanceService;

    @Mock
    private OrderExecutionMapper orderExecutionMapper;

    @InjectMocks
    private OrderExecutionService orderExecutionService;

    @Captor
    private ArgumentCaptor<OrderExecution> execCaptor;

    private UUID executionId;
    private UUID orderId;
    private Long userId;
    private ClinicalDay clinicalDay;
    private MedicalOrder medicalOrder;

    @BeforeEach
    void setUp() {
        executionId = UUID.randomUUID();
        orderId = UUID.randomUUID();
        userId = 11L;
        clinicalDay = ClinicalDay.builder()
                .status(ClinicalDayStatus.OPEN)
                .build();
        clinicalDay.setId(UUID.randomUUID());
        medicalOrder = MedicalOrder.builder()
                .status(MedicalOrderStatus.ACTIVE)
                .build();
        medicalOrder.setId(orderId);
        medicalOrder.setClinicalDay(clinicalDay);
        medicalOrder.setVersion(0);
    }

    @Test
    void getExecution_whenFound_returnsResponse() {
        OrderExecution exec = new OrderExecution();
        exec.setId(executionId);
        exec.setOrder(medicalOrder);
        when(orderExecutionRepository.findById(executionId)).thenReturn(Optional.of(exec));

        OrderExecutionResponse expected = OrderExecutionResponse.builder()
                .id(executionId)
                .build();
        when(orderExecutionMapper.toResponse(exec)).thenReturn(expected);

        OrderExecutionResponse res = orderExecutionService.getExecution(executionId);

        assertThat(res.getId()).isEqualTo(executionId);
    }

    @Test
    void getExecution_whenNotFound_throws() {
        when(orderExecutionRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderExecutionService.getExecution(executionId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getExecutionsByOrder_returnsList() {
        when(orderExecutionRepository.findByOrderId(orderId)).thenReturn(List.of());

        var results = orderExecutionService.getExecutionsByOrder(orderId);

        assertThat(results).isEmpty();
    }

    @Test
    void createExecution_createsSuccessfully() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));
        OrderExecutionCreateRequest req = new OrderExecutionCreateRequest(
                userId, LocalDateTime.now(), "10", null);
        OrderExecution saved = new OrderExecution();
        saved.setId(executionId);
        saved.setOrder(medicalOrder);
        saved.setVersion(0);
        when(orderExecutionRepository.save(any(OrderExecution.class))).thenReturn(saved);

        OrderExecution execEntity = new OrderExecution();
        execEntity.setExecutedBy(userId);
        execEntity.setActualDose("10");
        OrderExecutionResponse expected = OrderExecutionResponse.builder()
                .id(executionId)
                .build();
        when(orderExecutionMapper.toEntity(req)).thenReturn(execEntity);
        when(orderExecutionMapper.toResponse(any(OrderExecution.class))).thenReturn(expected);

        OrderExecutionResponse res = orderExecutionService.createExecution(orderId, req, userId);

        verify(orderExecutionRepository).save(execCaptor.capture());
        assertThat(execCaptor.getValue().getOrder()).isEqualTo(medicalOrder);
        assertThat(execCaptor.getValue().getExecutedBy()).isEqualTo(userId);
        assertThat(execCaptor.getValue().getActualDose()).isEqualTo("10");
        verify(auditService).logAction("OrderExecution", executionId, "EXECUTE", userId);
        verify(fluidBalanceService).recalculate(any(), eq(userId));
    }

    @Test
    void createExecution_whenOrderNotActive_throws() {
        medicalOrder.setStatus(MedicalOrderStatus.CANCELLED);
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));

        OrderExecutionCreateRequest req = new OrderExecutionCreateRequest(
                userId, LocalDateTime.now(), "10", null);

        assertThatThrownBy(() -> orderExecutionService.createExecution(orderId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void createExecution_whenDaySigned_throws() {
        clinicalDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));

        OrderExecutionCreateRequest req = new OrderExecutionCreateRequest(
                userId, LocalDateTime.now(), "10", null);

        assertThatThrownBy(() -> orderExecutionService.createExecution(orderId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void updateExecution_updatesFields() {
        OrderExecution exec = new OrderExecution();
        exec.setId(executionId);
        exec.setOrder(medicalOrder);
        exec.setVersion(0);

        when(orderExecutionRepository.findById(executionId)).thenReturn(Optional.of(exec));
        OrderExecution saved = new OrderExecution();
        saved.setId(executionId);
        saved.setOrder(medicalOrder);
        saved.setVersion(1);
        when(orderExecutionRepository.save(any(OrderExecution.class))).thenReturn(saved);

        OrderExecutionPatchRequest req = new OrderExecutionPatchRequest();
        req.setActualDose("20");
        req.setComment("Adjusted");
        req.setVersion(0);

        OrderExecutionResponse res = orderExecutionService.updateExecution(executionId, req, userId);

        verify(orderExecutionRepository).save(execCaptor.capture());
        assertThat(execCaptor.getValue().getActualDose()).isEqualTo("20");
        assertThat(execCaptor.getValue().getComment()).isEqualTo("Adjusted");
        verify(auditService).logUpdate("OrderExecution", executionId, userId, null, "Updated execution");
    }

    @Test
    void updateExecution_withVersionMismatch_throws() {
        OrderExecution exec = new OrderExecution();
        exec.setId(executionId);
        exec.setOrder(medicalOrder);
        exec.setVersion(0);

        when(orderExecutionRepository.findById(executionId)).thenReturn(Optional.of(exec));
        OrderExecutionPatchRequest req = new OrderExecutionPatchRequest();
        req.setVersion(999);

        assertThatThrownBy(() -> orderExecutionService.updateExecution(executionId, req, userId))
                .isInstanceOf(VersionConflictException.class);
    }
}
