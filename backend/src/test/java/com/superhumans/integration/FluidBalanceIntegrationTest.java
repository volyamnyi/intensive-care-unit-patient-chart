package com.superhumans.integration;

import com.superhumans.entity.*;
import com.superhumans.repository.*;
import com.superhumans.service.FluidBalanceService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.time.LocalDateTime;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
class FluidBalanceIntegrationTest {

    @Autowired private IcuCardRepository icuCardRepository;
    @Autowired private IcuDayRepository icuDayRepository;
    @Autowired private FluidIntakeRepository fluidIntakeRepository;
    @Autowired private FluidOutputRepository fluidOutputRepository;
    @Autowired private FluidBalanceService fluidBalanceService;

    private IcuDay setupDay() {
        IcuCard card = IcuCard.builder()
                .patientId(600L).patientName("Balance Test")
                .medicalCardNumber("BL-001")
                .admissionDate(LocalDateTime.now())
                .diagnosis("Balance integration")
                .status(CardStatus.ACTIVE)
                .createdBy("doctor1").createdAt(LocalDateTime.now())
                .build();
        icuCardRepository.save(card);

        IcuDay day = IcuDay.builder()
                .icuCard(card)
                .dayNumber(1).date(LocalDate.now())
                .status(DayStatus.ACTIVE).doctorId(1L)
                .build();
        return icuDayRepository.save(day);
    }

    @Test
    void balance_calculatesCorrectlyFromIntakeAndOutput() {
        IcuDay day = setupDay();

        FluidIntake intake = FluidIntake.builder()
                .icuDay(day).hour(10)
                .medicationName("Saline").volumeOrdered(500).volumeActual(500)
                .status(ExecutionStatus.DONE)
                .build();
        fluidIntakeRepository.save(intake);

        FluidOutput output = FluidOutput.builder()
                .icuDay(day).hour(12)
                .type(OutputType.URINE).volume(300)
                .build();
        fluidOutputRepository.save(output);

        var balance = fluidBalanceService.getBalance(day.getId());

        assertEquals(500, balance.getTotalIntake());
        assertEquals(300, balance.getTotalOutput());
        assertEquals(200, balance.getDailyBalance());
    }

    @Test
    void balance_excludesStoolFromOutput() {
        IcuDay day = setupDay();

        FluidIntake intake = FluidIntake.builder()
                .icuDay(day).hour(10)
                .medicationName("Saline").volumeOrdered(400).volumeActual(400)
                .status(ExecutionStatus.DONE)
                .build();
        fluidIntakeRepository.save(intake);

        FluidOutput urine = FluidOutput.builder()
                .icuDay(day).hour(12)
                .type(OutputType.URINE).volume(200)
                .build();
        fluidOutputRepository.save(urine);

        FluidOutput stool = FluidOutput.builder()
                .icuDay(day).hour(14)
                .type(OutputType.STOOL).volume(100)
                .build();
        fluidOutputRepository.save(stool);

        var balance = fluidBalanceService.getBalance(day.getId());

        assertEquals(400, balance.getTotalIntake());
        assertEquals(200, balance.getTotalOutput());
        assertEquals(200, balance.getDailyBalance());
    }

    @Test
    void balance_returnsZeros_whenNoData() {
        IcuDay day = setupDay();

        var balance = fluidBalanceService.getBalance(day.getId());

        assertEquals(0, balance.getTotalIntake());
        assertEquals(0, balance.getTotalOutput());
        assertEquals(0, balance.getDailyBalance());
    }
}
