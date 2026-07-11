package com.superhumans.service;

import com.superhumans.entity.*;
import com.superhumans.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class FluidBalanceService {

    private final FluidIntakeRepository fluidIntakeRepository;
    private final FluidOutputRepository fluidOutputRepository;
    private final FluidBalanceRepository fluidBalanceRepository;
    private final IcuDayRepository icuDayRepository;

    public FluidBalanceResponse getBalance(Long dayId) {
        List<FluidIntake> intakes = fluidIntakeRepository.findByIcuDayId(dayId);
        List<FluidOutput> outputs = fluidOutputRepository.findByIcuDayId(dayId);

        int totalIntake = intakes.stream()
                .filter(i -> i.getStatus() == ExecutionStatus.DONE)
                .mapToInt(i -> i.getVolumeActual() != null ? i.getVolumeActual() : 0)
                .sum();

        int totalOutput = outputs.stream()
                .filter(o -> o.getVolume() != null)
                .mapToInt(FluidOutput::getVolume)
                .sum();

        int dailyBalance = totalIntake - totalOutput;
        int cumulativeBalance = calculateCumulativeBalance(dayId, dailyBalance);

        return new FluidBalanceResponse(dayId, totalIntake, totalOutput, dailyBalance, cumulativeBalance);
    }

    @Transactional
    public FluidBalanceResponse calculateAndSave(IcuDay day) {
        var balance = getBalance(day.getId());

        FluidBalance fb = fluidBalanceRepository.findByIcuDayId(day.getId())
                .orElse(new FluidBalance());
        fb.setIcuDayId(day.getId());
        fb.setTotalIntake(balance.getTotalIntake());
        fb.setTotalOutput(balance.getTotalOutput());
        fb.setDailyBalance(balance.getDailyBalance());
        fb.setCumulativeBalance(balance.getCumulativeBalance());
        fluidBalanceRepository.save(fb);

        return balance;
    }

    private int calculateCumulativeBalance(Long currentDayId, int currentDailyBalance) {
        IcuDay currentDay = icuDayRepository.findById(currentDayId)
                .orElseThrow(() -> new RuntimeException("Day not found"));
        Long cardId = currentDay.getIcuCard().getId();
        List<IcuDay> allDays = icuDayRepository.findByIcuCardIdOrderByDayNumberAsc(cardId);

        int cumulative = 0;
        for (IcuDay day : allDays) {
            if (day.getId().equals(currentDayId)) {
                cumulative += currentDailyBalance;
                break;
            }
            Optional<FluidBalance> fb = fluidBalanceRepository.findByIcuDayId(day.getId());
            cumulative += fb.map(FluidBalance::getDailyBalance).orElse(0);
        }
        return cumulative;
    }

    @lombok.AllArgsConstructor
    @lombok.Getter
    public static class FluidBalanceResponse {
        private Long icuDayId;
        private Integer totalIntake;
        private Integer totalOutput;
        private Integer dailyBalance;
        private Integer cumulativeBalance;
    }
}
