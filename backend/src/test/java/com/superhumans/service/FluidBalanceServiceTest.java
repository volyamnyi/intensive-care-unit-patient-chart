package com.superhumans.service;

import com.superhumans.entity.*;
import com.superhumans.repository.*;
import org.junit.jupiter.api.BeforeEach;
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

    @Mock private FluidIntakeRepository fluidIntakeRepository;
    @Mock private FluidOutputRepository fluidOutputRepository;
    @Mock private FluidBalanceRepository fluidBalanceRepository;
    @Mock private IcuDayRepository icuDayRepository;
    @InjectMocks private FluidBalanceService fluidBalanceService;

    private IcuCard card;
    private IcuDay day;

    @BeforeEach
    void setUp() {
        card = IcuCard.builder().id(1L).build();
        day = IcuDay.builder().id(1L).icuCard(card).dayNumber(1).build();
    }

    private void mockCumulativeBase() {
        when(icuDayRepository.findById(1L)).thenReturn(Optional.of(day));
        when(icuDayRepository.findByIcuCardIdOrderByDayNumberAsc(1L)).thenReturn(List.of(day));
    }

    @Test
    void getBalance_shouldCalculateCorrectly() {
        mockCumulativeBase();
        List<FluidIntake> intakes = List.of(
                FluidIntake.builder().id(1L).volumeActual(500).status(ExecutionStatus.DONE).build(),
                FluidIntake.builder().id(2L).volumeActual(300).status(ExecutionStatus.DONE).build()
        );
        List<FluidOutput> outputs = List.of(
                FluidOutput.builder().id(1L).type(OutputType.URINE).volume(400).build(),
                FluidOutput.builder().id(2L).type(OutputType.TUBE).volume(100).build(),
                FluidOutput.builder().id(3L).type(OutputType.DRAINAGE).volume(50).build()
        );

        when(fluidIntakeRepository.findByIcuDayId(1L)).thenReturn(intakes);
        when(fluidOutputRepository.findByIcuDayId(1L)).thenReturn(outputs);

        var balance = fluidBalanceService.getBalance(1L);
        assertEquals(800, balance.getTotalIntake().intValue());
        assertEquals(550, balance.getTotalOutput().intValue());
        assertEquals(250, balance.getDailyBalance().intValue());
    }

    @Test
    void getBalance_shouldExcludeStoolFromOutput() {
        mockCumulativeBase();
        List<FluidIntake> intakes = List.of(
                FluidIntake.builder().id(1L).volumeActual(1000).status(ExecutionStatus.DONE).build()
        );
        List<FluidOutput> outputs = List.of(
                FluidOutput.builder().id(1L).type(OutputType.URINE).volume(400).build(),
                FluidOutput.builder().id(2L).type(OutputType.TUBE).volume(100).build(),
                FluidOutput.builder().id(3L).type(OutputType.DRAINAGE).volume(50).build(),
                FluidOutput.builder().id(4L).type(OutputType.STOOL).isPresent(true).build()
        );

        when(fluidIntakeRepository.findByIcuDayId(1L)).thenReturn(intakes);
        when(fluidOutputRepository.findByIcuDayId(1L)).thenReturn(outputs);

        var balance = fluidBalanceService.getBalance(1L);
        assertEquals(1000, balance.getTotalIntake().intValue());
        assertEquals(550, balance.getTotalOutput().intValue());
        assertEquals(450, balance.getDailyBalance().intValue());
    }

    @Test
    void getBalance_shouldOnlyCountDoneIntakes() {
        mockCumulativeBase();
        List<FluidIntake> intakes = List.of(
                FluidIntake.builder().id(1L).volumeActual(500).status(ExecutionStatus.DONE).build(),
                FluidIntake.builder().id(2L).volumeActual(300).status(ExecutionStatus.PENDING).build()
        );
        when(fluidIntakeRepository.findByIcuDayId(1L)).thenReturn(intakes);
        when(fluidOutputRepository.findByIcuDayId(1L)).thenReturn(List.of());

        var balance = fluidBalanceService.getBalance(1L);
        assertEquals(500, balance.getTotalIntake().intValue());
    }

    @Test
    void getBalance_shouldReturnZeros_whenNoData() {
        mockCumulativeBase();
        when(fluidIntakeRepository.findByIcuDayId(1L)).thenReturn(List.of());
        when(fluidOutputRepository.findByIcuDayId(1L)).thenReturn(List.of());

        var balance = fluidBalanceService.getBalance(1L);
        assertEquals(0, balance.getTotalIntake().intValue());
        assertEquals(0, balance.getTotalOutput().intValue());
        assertEquals(0, balance.getDailyBalance().intValue());
    }

    @Test
    void getBalance_shouldReturnNegativeBalance() {
        mockCumulativeBase();
        List<FluidIntake> intakes = List.of(
                FluidIntake.builder().id(1L).volumeActual(200).status(ExecutionStatus.DONE).build()
        );
        List<FluidOutput> outputs = List.of(
                FluidOutput.builder().id(1L).type(OutputType.URINE).volume(500).build()
        );
        when(fluidIntakeRepository.findByIcuDayId(1L)).thenReturn(intakes);
        when(fluidOutputRepository.findByIcuDayId(1L)).thenReturn(outputs);

        var balance = fluidBalanceService.getBalance(1L);
        assertEquals(-300, balance.getDailyBalance().intValue());
    }

    @Test
    void calculateAndSave_shouldPersistBalance() {
        mockCumulativeBase();
        IcuDay dayParam = IcuDay.builder().id(1L).build();
        when(fluidIntakeRepository.findByIcuDayId(1L)).thenReturn(List.of());
        when(fluidOutputRepository.findByIcuDayId(1L)).thenReturn(List.of());
        when(fluidBalanceRepository.findByIcuDayId(1L)).thenReturn(Optional.empty());
        when(fluidBalanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        fluidBalanceService.calculateAndSave(dayParam);
        verify(fluidBalanceRepository).save(any());
    }

    @Test
    void calculateAndSave_shouldUpdateExistingBalance() {
        mockCumulativeBase();
        IcuDay dayParam = IcuDay.builder().id(1L).build();
        FluidBalance existing = new FluidBalance();
        when(fluidIntakeRepository.findByIcuDayId(1L)).thenReturn(List.of());
        when(fluidOutputRepository.findByIcuDayId(1L)).thenReturn(List.of());
        when(fluidBalanceRepository.findByIcuDayId(1L)).thenReturn(Optional.of(existing));
        when(fluidBalanceRepository.save(any())).thenAnswer(i -> i.getArgument(0));

        fluidBalanceService.calculateAndSave(dayParam);
        verify(fluidBalanceRepository).save(existing);
    }
}
