package com.superhumans.service;

import com.superhumans.entity.*;
import com.superhumans.repository.*;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class FluidBalanceServiceTest {

    @Mock
    private FluidIntakeRepository fluidIntakeRepository;

    @Mock
    private FluidOutputRepository fluidOutputRepository;

    @Mock
    private FluidBalanceRepository fluidBalanceRepository;

    @Mock
    private IcuDayRepository icuDayRepository;

    @InjectMocks
    private FluidBalanceService fluidBalanceService;

    @Test
    void getBalance_shouldCalculateCorrectly() {
        when(fluidIntakeRepository.findByIcuDayId(1L)).thenReturn(List.of(
                FluidIntake.builder().volumeActual(500).status(ExecutionStatus.DONE).build(),
                FluidIntake.builder().volumeActual(300).status(ExecutionStatus.DONE).build(),
                FluidIntake.builder().volumeActual(200).status(ExecutionStatus.PENDING).build()
        ));
        when(fluidOutputRepository.findByIcuDayId(1L)).thenReturn(List.of(
                FluidOutput.builder().volume(400).build(),
                FluidOutput.builder().volume(100).build()
        ));
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(
                IcuDay.builder().id(1L).icuCard(IcuCard.builder().id(1L).build()).build()
        ));
        when(icuDayRepository.findByIcuCardIdOrderByDayNumberAsc(1L)).thenReturn(List.of(
                IcuDay.builder().id(1L).build()
        ));

        var result = fluidBalanceService.getBalance(1L);

        assertEquals(1L, result.getIcuDayId());
        // Only DONE intakes count: 500 + 300 = 800
        assertEquals(800, result.getTotalIntake());
        // All outputs count: 400 + 100 = 500
        assertEquals(500, result.getTotalOutput());
        // Daily balance: 800 - 500 = 300
        assertEquals(300, result.getDailyBalance());
        // Cumulative: 300 (only current day)
        assertEquals(300, result.getCumulativeBalance());
    }

    @Test
    void getBalance_shouldHandleEmptyData() {
        when(fluidIntakeRepository.findByIcuDayId(1L)).thenReturn(List.of());
        when(fluidOutputRepository.findByIcuDayId(1L)).thenReturn(List.of());
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(
                IcuDay.builder().id(1L).icuCard(IcuCard.builder().id(1L).build()).build()
        ));
        when(icuDayRepository.findByIcuCardIdOrderByDayNumberAsc(1L)).thenReturn(List.of(
                IcuDay.builder().id(1L).build()
        ));

        var result = fluidBalanceService.getBalance(1L);

        assertEquals(0, result.getTotalIntake());
        assertEquals(0, result.getTotalOutput());
        assertEquals(0, result.getDailyBalance());
        assertEquals(0, result.getCumulativeBalance());
    }

    @Test
    void calculateAndSave_shouldPersistBalance() {
        when(fluidIntakeRepository.findByIcuDayId(1L)).thenReturn(List.of());
        when(fluidOutputRepository.findByIcuDayId(1L)).thenReturn(List.of());
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(
                IcuDay.builder().id(1L).icuCard(IcuCard.builder().id(1L).build()).build()
        ));
        when(icuDayRepository.findByIcuCardIdOrderByDayNumberAsc(1L)).thenReturn(List.of(
                IcuDay.builder().id(1L).build()
        ));
        when(fluidBalanceRepository.findByIcuDayId(1L)).thenReturn(Optional.empty());

        IcuDay day = IcuDay.builder().id(1L).build();
        var result = fluidBalanceService.calculateAndSave(day);

        assertNotNull(result);
        verify(fluidBalanceRepository).save(argThat(fb ->
                fb.getIcuDayId() == 1L &&
                fb.getTotalIntake() == 0 &&
                fb.getTotalOutput() == 0 &&
                fb.getDailyBalance() == 0 &&
                fb.getCumulativeBalance() == 0
        ));
    }

    @Test
    void getBalance_negativeBalance_shouldReturnNegative() {
        when(fluidIntakeRepository.findByIcuDayId(1L)).thenReturn(List.of(
                FluidIntake.builder().volumeActual(200).status(ExecutionStatus.DONE).build()
        ));
        when(fluidOutputRepository.findByIcuDayId(1L)).thenReturn(List.of(
                FluidOutput.builder().volume(500).build(),
                FluidOutput.builder().volume(300).build()
        ));
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(
                IcuDay.builder().id(1L).icuCard(IcuCard.builder().id(1L).build()).build()
        ));
        when(icuDayRepository.findByIcuCardIdOrderByDayNumberAsc(1L)).thenReturn(List.of(
                IcuDay.builder().id(1L).build()
        ));

        var result = fluidBalanceService.getBalance(1L);

        assertEquals(200, result.getTotalIntake());
        assertEquals(800, result.getTotalOutput());
        assertEquals(-600, result.getDailyBalance());
        assertEquals(-600, result.getCumulativeBalance());
    }

    @Test
    void getBalance_cumulativeWithMultipleDays_shouldSumCorrectly() {
        IcuDay day1 = IcuDay.builder().id(1L).icuCard(IcuCard.builder().id(1L).build()).build();
        IcuDay day2 = IcuDay.builder().id(2L).icuCard(IcuCard.builder().id(1L).build()).build();

        when(icuDayRepository.findById(2L)).thenReturn(Optional.of(day2));
        when(icuDayRepository.findByIcuCardIdOrderByDayNumberAsc(1L)).thenReturn(List.of(day1, day2));

        when(fluidIntakeRepository.findByIcuDayId(2L)).thenReturn(List.of(
                FluidIntake.builder().volumeActual(500).status(ExecutionStatus.DONE).build()
        ));
        when(fluidOutputRepository.findByIcuDayId(2L)).thenReturn(List.of(
                FluidOutput.builder().volume(200).build()
        ));

        when(fluidBalanceRepository.findByIcuDayId(1L)).thenReturn(Optional.of(
                FluidBalance.builder().dailyBalance(200).build()
        ));

        var result = fluidBalanceService.getBalance(2L);

        assertEquals(500, result.getTotalIntake());
        assertEquals(200, result.getTotalOutput());
        assertEquals(300, result.getDailyBalance());
        assertEquals(500, result.getCumulativeBalance());
    }

    @Test
    void calculateAndSave_shouldUpdateExistingBalance() {
        when(fluidIntakeRepository.findByIcuDayId(1L)).thenReturn(List.of());
        when(fluidOutputRepository.findByIcuDayId(1L)).thenReturn(List.of());
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(
                IcuDay.builder().id(1L).icuCard(IcuCard.builder().id(1L).build()).build()
        ));
        when(icuDayRepository.findByIcuCardIdOrderByDayNumberAsc(1L)).thenReturn(List.of(
                IcuDay.builder().id(1L).build()
        ));
        FluidBalance existing = FluidBalance.builder().id(10L).build();
        when(fluidBalanceRepository.findByIcuDayId(1L)).thenReturn(Optional.of(existing));

        IcuDay day = IcuDay.builder().id(1L).build();
        fluidBalanceService.calculateAndSave(day);

        verify(fluidBalanceRepository).save(existing);
    }
}
