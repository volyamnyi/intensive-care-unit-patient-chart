package com.superhumans.service;

import com.superhumans.dto.OrderExecutionCreateRequest;
import com.superhumans.dto.OrderExecutionFinishRequest;
import com.superhumans.dto.OrderExecutionPatchRequest;
import com.superhumans.dto.OrderExecutionPlanRequest;
import com.superhumans.dto.OrderExecutionResponse;
import com.superhumans.entity.*;
import com.superhumans.exception.BusinessException;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.OrderExecutionMapper;
import com.superhumans.repository.icu.ClinicalDayRepository;
import com.superhumans.repository.icu.MedicalOrderRepository;
import com.superhumans.repository.icu.OrderExecutionRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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

    private OrderExecution plannedExec() {
        OrderExecution e = new OrderExecution();
        e.setId(executionId);
        e.setOrder(medicalOrder);
        e.setHour(10);
        e.setPlanned(true);
        e.setPlannedBy(userId);
        e.setPlannedDose("10");
        e.setStatus(OrderExecutionStatus.PLANNED);
        e.setVersion(0);
        return e;
    }

    private void stubMapperResponse() {
        when(orderExecutionMapper.toResponse(any(OrderExecution.class)))
                .thenReturn(OrderExecutionResponse.builder().id(executionId).build());
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
    void plan_createsNewExecution() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));
        when(orderExecutionRepository.findByOrderIdAndHour(orderId, 10)).thenReturn(Optional.empty());
        OrderExecution saved = new OrderExecution();
        saved.setId(executionId);
        saved.setOrder(medicalOrder);
        saved.setVersion(0);
        when(orderExecutionRepository.save(any(OrderExecution.class))).thenReturn(saved);
        stubMapperResponse();

        OrderExecutionPlanRequest req = new OrderExecutionPlanRequest(10, "5 мг");
        OrderExecutionResponse res = orderExecutionService.plan(orderId, req, userId);

        verify(orderExecutionRepository).save(execCaptor.capture());
        OrderExecution captured = execCaptor.getValue();
        assertThat(captured.getOrder()).isEqualTo(medicalOrder);
        assertThat(captured.getHour()).isEqualTo(10);
        assertThat(captured.isPlanned()).isTrue();
        assertThat(captured.getPlannedBy()).isEqualTo(userId);
        assertThat(captured.getPlannedAt()).isNotNull();
        assertThat(captured.getPlannedDose()).isEqualTo("5 мг");
        assertThat(captured.getStatus()).isEqualTo(OrderExecutionStatus.PLANNED);
        verify(auditService).logAction("OrderExecution", executionId, "PLAN", userId);
    }

    @Test
    void plan_updatesExistingPlannedRow() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));
        OrderExecution existing = plannedExec();
        existing.setPlannedDose("3 мг");
        when(orderExecutionRepository.findByOrderIdAndHour(orderId, 10)).thenReturn(Optional.of(existing));
        when(orderExecutionRepository.save(any(OrderExecution.class))).thenReturn(existing);
        stubMapperResponse();

        orderExecutionService.plan(orderId, new OrderExecutionPlanRequest(10, "6 мг"), userId);

        verify(orderExecutionRepository).save(execCaptor.capture());
        assertThat(execCaptor.getValue().getPlannedDose()).isEqualTo("6 мг");
    }

    @Test
    void plan_whenAlreadyCompleted_throws() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));
        OrderExecution existing = plannedExec();
        existing.setStatus(OrderExecutionStatus.COMPLETED);
        existing.setCompletedFinished(true);
        when(orderExecutionRepository.findByOrderIdAndHour(orderId, 10)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> orderExecutionService.plan(orderId, new OrderExecutionPlanRequest(10, "5 мг"), userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void plan_whenHourOutOfRange_throws() {
        medicalOrder.setStartTime(LocalDateTime.now().withHour(9).truncatedTo(ChronoUnit.HOURS));
        medicalOrder.setEndTime(LocalDateTime.now().withHour(18).truncatedTo(ChronoUnit.HOURS));
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));

        assertThatThrownBy(() -> orderExecutionService.plan(orderId, new OrderExecutionPlanRequest(20, "5 мг"), userId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void plan_whenOrderNotActive_throws() {
        medicalOrder.setStatus(MedicalOrderStatus.CANCELLED);
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));

        assertThatThrownBy(() -> orderExecutionService.plan(orderId, new OrderExecutionPlanRequest(10, "5 мг"), userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void planFinish_marksPlanFinished() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));
        OrderExecution existing = plannedExec();
        when(orderExecutionRepository.findByOrderIdAndHour(orderId, 10)).thenReturn(Optional.of(existing));
        when(orderExecutionRepository.save(any(OrderExecution.class))).thenReturn(existing);
        stubMapperResponse();

        orderExecutionService.planFinish(orderId, new OrderExecutionFinishRequest(10).getHour(), userId);

        verify(orderExecutionRepository).save(execCaptor.capture());
        assertThat(execCaptor.getValue().isPlannedFinished()).isTrue();
        verify(auditService).logAction("OrderExecution", executionId, "PLAN_FINISH", userId);
    }

    @Test
    void planFinish_whenNotPlanned_throws() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));
        when(orderExecutionRepository.findByOrderIdAndHour(orderId, 10)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> orderExecutionService.planFinish(orderId, 10, userId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void cancel_cancelsPlannedExecution() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));
        OrderExecution existing = plannedExec();
        when(orderExecutionRepository.findByOrderIdAndHour(orderId, 10)).thenReturn(Optional.of(existing));
        when(orderExecutionRepository.save(any(OrderExecution.class))).thenReturn(existing);
        stubMapperResponse();

        orderExecutionService.cancel(orderId, 10, userId);

        verify(orderExecutionRepository).save(execCaptor.capture());
        assertThat(execCaptor.getValue().getStatus()).isEqualTo(OrderExecutionStatus.CANCELLED);
        assertThat(execCaptor.getValue().isPlannedFinished()).isTrue();
        verify(auditService).logAction("OrderExecution", executionId, "CANCEL", userId);
    }

    @Test
    void cancel_whenCompleted_throws() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));
        OrderExecution existing = plannedExec();
        existing.setStatus(OrderExecutionStatus.COMPLETED);
        when(orderExecutionRepository.findByOrderIdAndHour(orderId, 10)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> orderExecutionService.cancel(orderId, 10, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void execute_completesPlannedExecution() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));
        OrderExecution existing = plannedExec();
        when(orderExecutionRepository.findByOrderIdAndHour(orderId, 10)).thenReturn(Optional.of(existing));
        when(orderExecutionRepository.save(any(OrderExecution.class))).thenReturn(existing);
        stubMapperResponse();

        OrderExecutionCreateRequest req = new OrderExecutionCreateRequest(10, "10", "ok");
        OrderExecutionResponse res = orderExecutionService.execute(orderId, req, userId);

        verify(orderExecutionRepository).save(execCaptor.capture());
        OrderExecution captured = execCaptor.getValue();
        assertThat(captured.getExecutedBy()).isEqualTo(userId);
        assertThat(captured.getExecutedAt()).isNotNull();
        assertThat(captured.getActualDose()).isEqualTo("10");
        assertThat(captured.getComment()).isEqualTo("ok");
        assertThat(captured.getStatus()).isEqualTo(OrderExecutionStatus.COMPLETED);
        verify(auditService).logAction("OrderExecution", executionId, "EXECUTE", userId);
        verify(fluidBalanceService).recalculate(any(), eq(userId));
    }

    @Test
    void execute_whenNotPlanned_throws() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));
        when(orderExecutionRepository.findByOrderIdAndHour(orderId, 10)).thenReturn(Optional.empty());

        OrderExecutionCreateRequest req = new OrderExecutionCreateRequest(10, "10", null);
        assertThatThrownBy(() -> orderExecutionService.execute(orderId, req, userId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void execute_whenPlanFinished_throws() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));
        OrderExecution existing = plannedExec();
        existing.setPlannedFinished(true);
        when(orderExecutionRepository.findByOrderIdAndHour(orderId, 10)).thenReturn(Optional.of(existing));

        OrderExecutionCreateRequest req = new OrderExecutionCreateRequest(10, "10", null);
        assertThatThrownBy(() -> orderExecutionService.execute(orderId, req, userId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void execute_partiallyCompleted_whenActualDoseLessThanPlanned() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));
        OrderExecution existing = plannedExec();
        existing.setPlannedDose("10");
        when(orderExecutionRepository.findByOrderIdAndHour(orderId, 10)).thenReturn(Optional.of(existing));
        when(orderExecutionRepository.save(any(OrderExecution.class))).thenReturn(existing);
        stubMapperResponse();

        OrderExecutionCreateRequest req = new OrderExecutionCreateRequest(10, "5", null);
        orderExecutionService.execute(orderId, req, userId);

        verify(orderExecutionRepository).save(execCaptor.capture());
        assertThat(execCaptor.getValue().getStatus()).isEqualTo(OrderExecutionStatus.PARTIALLY_COMPLETED);
    }

    @Test
    void execute_logsBackEntry_whenPastHour() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));
        OrderExecution existing = plannedExec();
        when(orderExecutionRepository.findByOrderIdAndHour(any(), any())).thenReturn(Optional.of(existing));
        when(orderExecutionRepository.save(any(OrderExecution.class))).thenReturn(existing);
        stubMapperResponse();

        int nowHour = LocalDateTime.now().getHour();
        int pastHour = (nowHour - 2 + 24) % 24;
        boolean expectBackEntry = pastHour < nowHour;

        OrderExecutionCreateRequest req = new OrderExecutionCreateRequest(pastHour, "10", null);
        orderExecutionService.execute(orderId, req, userId);

        verify(auditService).logAction("OrderExecution", executionId, "EXECUTE", userId);
        if (expectBackEntry) {
            verify(auditService).logAction("OrderExecution", executionId, "BACK_ENTRY", userId);
        } else {
            verify(auditService, never()).logAction("OrderExecution", executionId, "BACK_ENTRY", userId);
        }
    }

    @Test
    void execute_whenDaySigned_throws() {
        clinicalDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));

        OrderExecutionCreateRequest req = new OrderExecutionCreateRequest(10, "10", null);
        assertThatThrownBy(() -> orderExecutionService.execute(orderId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void executeFinish_marksCompletedFinished() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));
        OrderExecution existing = plannedExec();
        existing.setStatus(OrderExecutionStatus.COMPLETED);
        when(orderExecutionRepository.findByOrderIdAndHour(orderId, 10)).thenReturn(Optional.of(existing));
        when(orderExecutionRepository.save(any(OrderExecution.class))).thenReturn(existing);
        stubMapperResponse();

        orderExecutionService.executeFinish(orderId, 10, userId);

        verify(orderExecutionRepository).save(execCaptor.capture());
        assertThat(execCaptor.getValue().isCompletedFinished()).isTrue();
        verify(auditService).logAction("OrderExecution", executionId, "EXECUTE_FINISH", userId);
    }

    @Test
    void executeFinish_whenNotCompleted_throws() {
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));
        OrderExecution existing = plannedExec();
        when(orderExecutionRepository.findByOrderIdAndHour(orderId, 10)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> orderExecutionService.executeFinish(orderId, 10, userId))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    void updateExecution_updatesFields() {
        OrderExecution exec = plannedExec();
        when(orderExecutionRepository.findById(executionId)).thenReturn(Optional.of(exec));
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));
        OrderExecution saved = plannedExec();
        saved.setVersion(1);
        when(orderExecutionRepository.save(any(OrderExecution.class))).thenReturn(saved);
        stubMapperResponse();

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
    void updateExecution_whenCompleted_throws() {
        OrderExecution exec = plannedExec();
        exec.setStatus(OrderExecutionStatus.COMPLETED);
        when(orderExecutionRepository.findById(executionId)).thenReturn(Optional.of(exec));
        when(medicalOrderRepository.findById(orderId)).thenReturn(Optional.of(medicalOrder));

        OrderExecutionPatchRequest req = new OrderExecutionPatchRequest();
        req.setActualDose("20");
        req.setVersion(0);

        assertThatThrownBy(() -> orderExecutionService.updateExecution(executionId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void updateExecution_withVersionMismatch_throws() {
        OrderExecution exec = plannedExec();
        when(orderExecutionRepository.findById(executionId)).thenReturn(Optional.of(exec));

        OrderExecutionPatchRequest req = new OrderExecutionPatchRequest();
        req.setVersion(999);

        assertThatThrownBy(() -> orderExecutionService.updateExecution(executionId, req, userId))
                .isInstanceOf(VersionConflictException.class);
    }
}
