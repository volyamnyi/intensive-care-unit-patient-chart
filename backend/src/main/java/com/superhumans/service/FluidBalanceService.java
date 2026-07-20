package com.superhumans.service;

import com.superhumans.dto.FluidBalanceResponse;
import com.superhumans.entity.*;
import com.superhumans.mapper.FluidBalanceMapper;
import com.superhumans.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class FluidBalanceService {

    FluidBalanceRepository fluidBalanceRepository;
    HourlyRecordRepository hourlyRecordRepository;
    MedicalOrderRepository medicalOrderRepository;
    OrderExecutionRepository orderExecutionRepository;
    ClinicalDayRepository clinicalDayRepository;
    AuditService auditService;
    FluidBalanceMapper fluidBalanceMapper;

    public List<FluidBalanceResponse> getBalances(UUID clinicalDayId) {
        List<FluidBalanceResponse> responses = fluidBalanceRepository
                .findByClinicalDayIdOrderByHourAsc(clinicalDayId)
                .stream().map(fluidBalanceMapper::toResponse)
                .collect(Collectors.toList());
        enrichWithCategoryBreakdowns(clinicalDayId, responses);
        return responses;
    }

    @Transactional
    public List<FluidBalanceResponse> recalculate(UUID clinicalDayId, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(clinicalDayId)
                .orElseThrow(() -> new RuntimeException("Clinical day not found: " + clinicalDayId));

        fluidBalanceRepository.deleteByClinicalDayId(clinicalDayId);

        List<HourlyRecord> hourlyRecords = hourlyRecordRepository
                .findByClinicalDayIdOrderByRecordTimeAsc(clinicalDayId);
        List<MedicalOrder> orders = medicalOrderRepository
                .findByClinicalDayIdOrderByStartTimeAsc(clinicalDayId);

        Map<Integer, Double> intakeByHour = new HashMap<>();
        for (MedicalOrder order : orders) {
            UUID orderId = order.getId();
            List<OrderExecution> executions = orderExecutionRepository.findByOrderId(orderId);
            for (OrderExecution exec : executions) {
                if (exec.getExecutedAt() == null) continue;
                int hour = exec.getExecutedAt().getHour();
                double dose = parseDose(exec.getActualDose());
                intakeByHour.merge(hour, dose, Double::sum);
            }
        }

        Map<Integer, Double> outputByHour = new HashMap<>();
        for (HourlyRecord rec : hourlyRecords) {
            int hour = rec.getRecordTime().getHour();
            double output = 0.0;
            if (rec.getUrineOutput() != null) output += rec.getUrineOutput();
            if (rec.getDrainOutput() != null) output += rec.getDrainOutput();
            if (rec.getStool() != null && !rec.getStool().isBlank()) output += 200.0;
            if (rec.getVomit() != null && !rec.getVomit().isBlank()) output += 100.0;
            outputByHour.merge(hour, output, Double::sum);
        }

        Set<Integer> allHours = new TreeSet<>();
        allHours.addAll(intakeByHour.keySet());
        allHours.addAll(outputByHour.keySet());

        double cumulative = 0.0;
        List<FluidBalance> results = new ArrayList<>();
        for (Integer hour : allHours) {
            double intake = intakeByHour.getOrDefault(hour, 0.0);
            double output = outputByHour.getOrDefault(hour, 0.0);
            double balance = intake - output;
            cumulative += balance;

            FluidBalance fb = FluidBalance.builder()
                    .clinicalDay(day)
                    .hour(hour)
                    .intake(intake)
                    .output(output)
                    .balance(balance)
                    .cumulativeBalance(cumulative)
                    .build();
            if (userId != null) {
                fb.setCreatedBy(userId);
                fb.setUpdatedBy(userId);
            }
            results.add(fb);
        }

        results = fluidBalanceRepository.saveAll(results);
        if (userId != null) {
            auditService.logAction("FluidBalance", clinicalDayId, "RECALCULATE", userId);
        }

        List<FluidBalanceResponse> responses = results.stream()
                .map(fluidBalanceMapper::toResponse).collect(Collectors.toList());
        enrichWithCategoryBreakdowns(clinicalDayId, responses);
        return responses;
    }

    private void enrichWithCategoryBreakdowns(UUID clinicalDayId, List<FluidBalanceResponse> responses) {
        List<HourlyRecord> hourlyRecords = hourlyRecordRepository
                .findByClinicalDayIdOrderByRecordTimeAsc(clinicalDayId);
        List<MedicalOrder> orders = medicalOrderRepository
                .findByClinicalDayIdOrderByStartTimeAsc(clinicalDayId);

        Map<String, Double> intakeByCategory = new LinkedHashMap<>();
        intakeByCategory.put("crystalloids", 0.0);
        intakeByCategory.put("colloids", 0.0);
        intakeByCategory.put("blood", 0.0);
        intakeByCategory.put("plasma", 0.0);
        intakeByCategory.put("nutrition", 0.0);
        intakeByCategory.put("oral", 0.0);
        intakeByCategory.put("other", 0.0);

        for (MedicalOrder order : orders) {
            String category = order.getCategory();
            List<OrderExecution> executions = orderExecutionRepository.findByOrderId(order.getId());
            double totalDose = 0.0;
            for (OrderExecution exec : executions) {
                if (exec.getExecutedAt() == null) continue;
                totalDose += parseDose(exec.getActualDose());
            }
            if (totalDose == 0.0) continue;

            String intakeKey = mapOrderCategoryToIntake(category);
            intakeByCategory.merge(intakeKey, totalDose, Double::sum);
        }

        Map<String, Double> outputByCategory = new LinkedHashMap<>();
        outputByCategory.put("diuresis", 0.0);
        outputByCategory.put("drainage", 0.0);
        outputByCategory.put("vomiting", 0.0);
        outputByCategory.put("stool", 0.0);
        outputByCategory.put("bloodLoss", 0.0);
        outputByCategory.put("other", 0.0);

        for (HourlyRecord rec : hourlyRecords) {
            if (rec.getUrineOutput() != null)
                outputByCategory.merge("diuresis", rec.getUrineOutput(), Double::sum);
            if (rec.getDrainOutput() != null)
                outputByCategory.merge("drainage", rec.getDrainOutput(), Double::sum);
            if (rec.getVomit() != null && !rec.getVomit().isBlank())
                outputByCategory.merge("vomiting", 100.0, Double::sum);
            if (rec.getStool() != null && !rec.getStool().isBlank())
                outputByCategory.merge("stool", 200.0, Double::sum);
        }

        for (FluidBalanceResponse response : responses) {
            response.setIntakeByCategory(new LinkedHashMap<>(intakeByCategory));
            response.setOutputByCategory(new LinkedHashMap<>(outputByCategory));
        }
    }

    private String mapOrderCategoryToIntake(String category) {
        if (category == null) return "other";
        return switch (category) {
            case "INFUSION" -> "crystalloids";
            case "NUTRITION" -> "nutrition";
            default -> "other";
        };
    }

    private double parseDose(String actualDose) {
        if (actualDose == null || actualDose.isBlank()) return 0.0;
        String numeric = actualDose.replaceAll("[^0-9.,]", "").replace(",", ".");
        try {
            return Double.parseDouble(numeric);
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }
}
