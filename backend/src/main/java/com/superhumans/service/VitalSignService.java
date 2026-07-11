package com.superhumans.service;

import com.superhumans.dto.VitalSignsRequest;
import com.superhumans.entity.HourlyVital;
import com.superhumans.entity.IcuDay;
import com.superhumans.exception.BadRequestException;
import com.superhumans.repository.HourlyVitalRepository;
import com.superhumans.repository.IcuDayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
public class VitalSignService {

    private final HourlyVitalRepository hourlyVitalRepository;
    private final IcuDayRepository icuDayRepository;

    @Transactional
    public HourlyVital saveVitals(Long dayId, Integer hour, VitalSignsRequest req) {
        if (hour < 0 || hour > 23) {
            throw new BadRequestException("Hour must be between 0 and 23");
        }
        IcuDay day = icuDayRepository.findById(dayId)
                .orElseThrow(() -> new RuntimeException("Day not found"));

        HourlyVital vital = hourlyVitalRepository.findByIcuDayIdAndHour(dayId, hour)
                .orElse(HourlyVital.builder()
                        .icuDay(day)
                        .hour(hour)
                        .build());

        vital.setSystolicBp(req.getSystolicBp());
        vital.setDiastolicBp(req.getDiastolicBp());
        vital.setHeartRate(req.getHeartRate());
        vital.setSpo2(req.getSpo2());
        vital.setTemperature(req.getTemperature());
        vital.setCvp(req.getCvp());
        vital.setRespiratoryRate(req.getRespiratoryRate());
        vital.setVentilatorMode(req.getVentilatorMode());
        vital.setTidalVolume(req.getTidalVolume());
        vital.setMinuteVentilation(req.getMinuteVentilation());
        vital.setPeep(req.getPeep());
        vital.setFio2(req.getFio2());
        vital.setVentFrequency(req.getVentFrequency());

        return hourlyVitalRepository.save(vital);
    }

    public List<HourlyVital> getVitalsByDay(Long dayId) {
        return hourlyVitalRepository.findByIcuDayIdOrderByHourAsc(dayId);
    }

    public HourlyVital getVitalByHour(Long dayId, Integer hour) {
        return hourlyVitalRepository.findByIcuDayIdAndHour(dayId, hour).orElse(null);
    }
}
