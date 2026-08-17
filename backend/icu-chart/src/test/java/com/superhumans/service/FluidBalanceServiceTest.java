package com.superhumans.service;

import com.superhumans.dto.FluidBalanceResponse;
import com.superhumans.icu.entity.*;
import com.superhumans.exception.NotFoundException;
import com.superhumans.mapper.FluidBalanceMapper;
import com.superhumans.icu.repository.ClinicalDayRepository;
import com.superhumans.icu.repository.FluidBalanceRepository;
import com.superhumans.icu.repository.HourlyRecordRepository;
import com.superhumans.icu.repository.MedicalOrderRepository;
import com.superhumans.icu.repository.OrderExecutionRepository;
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
class FluidBalanceServiceTest {

    @Mock
    private FluidBalanceRepository fluidBalanceRepository;

    @Mock
    private HourlyRecordRepository hourlyRecordRepository;

    @Mock
    private MedicalOrderRepository medicalOrderRepository;

    @Mock
    private OrderExecutionRepository orderExecutionRepository;

    @Mock
    private ClinicalDayRepository clinicalDayRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private FluidBalanceMapper fluidBalanceMapper;

    @InjectMocks
    private FluidBalanceService fluidBalanceService;

    @Captor
    private ArgumentCaptor<List<FluidBalance>> batchCaptor;

    private UUID clinicalDayId;
    private Long userId;
    private ClinicalDay clinicalDay;

    @BeforeEach
    void setUp() {
        clinicalDayId = UUID.randomUUID();
        userId = 11L;
        clinicalDay = ClinicalDay.builder()
                .status(ClinicalDayStatus.OPEN)
                .build();
        clinicalDay.setId(clinicalDayId);
    }

    @Test
    void getBalances_returnsList() {
        FluidBalance fb = new FluidBalance();
        fb.setId(UUID.randomUUID());
        fb.setClinicalDay(clinicalDay);
        fb.setHour(8);
        fb.setIntake(500.0);
        fb.setOutput(300.0);
        fb.setBalance(200.0);
        fb.setCumulativeBalance(200.0);

        when(fluidBalanceRepository.findByClinicalDayIdOrderByHourAsc(clinicalDayId))
                .thenReturn(List.of(fb));

        FluidBalanceResponse expected = FluidBalanceResponse.builder()
                .hour(8)
                .intake(500.0)
                .output(300.0)
                .balance(200.0)
                .cumulativeBalance(200.0)
                .build();
        when(fluidBalanceMapper.toResponse(fb)).thenReturn(expected);

        List<FluidBalanceResponse> results = fluidBalanceService.getBalances(clinicalDayId);

        assertThat(results).hasSize(1);
        assertThat(results.get(0).getHour()).isEqualTo(8);
        assertThat(results.get(0).getIntake()).isEqualTo(500.0);
        assertThat(results.get(0).getOutput()).isEqualTo(300.0);
        assertThat(results.get(0).getBalance()).isEqualTo(200.0);
        assertThat(results.get(0).getCumulativeBalance()).isEqualTo(200.0);
    }

    @Test
    void getBalances_empty() {
        when(fluidBalanceRepository.findByClinicalDayIdOrderByHourAsc(clinicalDayId))
                .thenReturn(List.of());

        List<FluidBalanceResponse> results = fluidBalanceService.getBalances(clinicalDayId);

        assertThat(results).isEmpty();
    }

    @Test
    void recalculate_aggregatesFromHourlyAndOrders() {
        HourlyRecord r1 = new HourlyRecord();
        r1.setId(UUID.randomUUID());
        r1.setRecordTime(LocalDateTime.now().withHour(8));
        r1.setUrineOutput(400.0);
        r1.setDrainOutput(100.0);

        HourlyRecord r2 = new HourlyRecord();
        r2.setId(UUID.randomUUID());
        r2.setRecordTime(LocalDateTime.now().withHour(10));
        r2.setUrineOutput(300.0);
        r2.setStool("Yes");
        r2.setVomit("Yes");

        MedicalOrder order = new MedicalOrder();
        order.setId(UUID.randomUUID());

        OrderExecution exec = new OrderExecution();
        exec.setId(UUID.randomUUID());
        exec.setOrder(order);
        exec.setExecutedAt(LocalDateTime.now().withHour(8));
        exec.setActualDose("500");

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        when(hourlyRecordRepository.findByClinicalDayIdOrderByRecordTimeAsc(clinicalDayId))
                .thenReturn(List.of(r1, r2));
        when(medicalOrderRepository.findByClinicalDayIdOrderByStartTimeAsc(clinicalDayId))
                .thenReturn(List.of(order));
        when(orderExecutionRepository.findByOrderId(order.getId())).thenReturn(List.of(exec));

        FluidBalance fb1 = new FluidBalance();
        fb1.setId(UUID.randomUUID());
        fb1.setClinicalDay(clinicalDay);
        fb1.setHour(8);

        FluidBalance fb2 = new FluidBalance();
        fb2.setId(UUID.randomUUID());
        fb2.setClinicalDay(clinicalDay);
        fb2.setHour(10);

        when(fluidBalanceRepository.saveAll(anyList())).thenReturn(List.of(fb1, fb2));

        FluidBalanceResponse resp1 = FluidBalanceResponse.builder()
                .hour(8).intake(500.0).output(500.0).balance(0.0).build();
        FluidBalanceResponse resp2 = FluidBalanceResponse.builder()
                .hour(10).intake(0.0).output(600.0).balance(-600.0).build();
        when(fluidBalanceMapper.toResponse(fb1)).thenReturn(resp1);
        when(fluidBalanceMapper.toResponse(fb2)).thenReturn(resp2);

        List<FluidBalanceResponse> results = fluidBalanceService.recalculate(clinicalDayId, userId);

        verify(fluidBalanceRepository).deleteByClinicalDayId(clinicalDayId);
        verify(fluidBalanceRepository).saveAll(batchCaptor.capture());
        assertThat(batchCaptor.getValue()).hasSize(2);

        FluidBalance savedFb8 = batchCaptor.getValue().get(0);
        assertThat(savedFb8.getHour()).isEqualTo(8);
        assertThat(savedFb8.getIntake()).isEqualTo(500.0);
        assertThat(savedFb8.getOutput()).isEqualTo(500.0);

        FluidBalance savedFb10 = batchCaptor.getValue().get(1);
        assertThat(savedFb10.getHour()).isEqualTo(10);
        assertThat(savedFb10.getIntake()).isEqualTo(0.0);
        assertThat(savedFb10.getOutput()).isEqualTo(400.0);

        verify(auditService).logAction("FluidBalance", clinicalDayId, "RECALCULATE", userId);
    }

    @Test
    void recalculate_withNoData_savesNothing() {
        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        when(hourlyRecordRepository.findByClinicalDayIdOrderByRecordTimeAsc(clinicalDayId))
                .thenReturn(List.of());
        when(medicalOrderRepository.findByClinicalDayIdOrderByStartTimeAsc(clinicalDayId))
                .thenReturn(List.of());

        List<FluidBalanceResponse> results = fluidBalanceService.recalculate(clinicalDayId, userId);

        verify(fluidBalanceRepository).deleteByClinicalDayId(clinicalDayId);
        verify(fluidBalanceRepository).saveAll(batchCaptor.capture());
        assertThat(batchCaptor.getValue()).isEmpty();
        verify(auditService).logAction("FluidBalance", clinicalDayId, "RECALCULATE", userId);
    }

    @Test
    void recalculate_whenClinicalDayNotFound_throws() {
        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> fluidBalanceService.recalculate(clinicalDayId, userId))
                .isInstanceOf(RuntimeException.class);
    }
}
