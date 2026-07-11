package com.superhumans.service;

import com.superhumans.entity.CareMeasure;
import com.superhumans.entity.IcuDay;
import com.superhumans.repository.CareMeasureRepository;
import com.superhumans.repository.IcuDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class CareMeasureService {

    private final CareMeasureRepository careMeasureRepository;
    private final IcuDayRepository icuDayRepository;

    public CareMeasure saveCareMeasure(Long dayId, Integer hour, String procedure, Boolean performed, String performedBy) {
        IcuDay day = icuDayRepository.findById(dayId)
                .orElseThrow(() -> new RuntimeException("IcuDay not found: " + dayId));
        CareMeasure cm = CareMeasure.builder()
                .icuDay(day)
                .hour(hour)
                .procedure(procedure)
                .performed(performed)
                .performedBy(performedBy)
                .createdAt(LocalDateTime.now())
                .build();
        return careMeasureRepository.save(cm);
    }

    public List<CareMeasure> getCareMeasuresByDay(Long dayId) {
        return careMeasureRepository.findByIcuDayIdOrderByHourAsc(dayId);
    }
}
