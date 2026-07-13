package com.superhumans.mapper;

import com.superhumans.dto.HourlyRecordCreateRequest;
import com.superhumans.dto.HourlyRecordResponse;
import com.superhumans.entity.HourlyRecord;

public class HourlyRecordMapper {

    public static HourlyRecordResponse toResponse(HourlyRecord entity) {
        return HourlyRecordResponse.builder()
                .id(entity.getId())
                .clinicalDayId(entity.getClinicalDay().getId())
                .recordTime(entity.getRecordTime())
                .consciousness(entity.getConsciousness())
                .temperature(entity.getTemperature())
                .heartRate(entity.getHeartRate())
                .respiratoryRate(entity.getRespiratoryRate())
                .systolicBP(entity.getSystolicBP())
                .diastolicBP(entity.getDiastolicBP())
                .meanArterialPressure(entity.getMeanArterialPressure())
                .spo2(entity.getSpo2())
                .etco2(entity.getEtco2())
                .fio2(entity.getFio2())
                .cvp(entity.getCvp())
                .urineOutput(entity.getUrineOutput())
                .drainOutput(entity.getDrainOutput())
                .stool(entity.getStool())
                .vomit(entity.getVomit())
                .painScore(entity.getPainScore())
                .notes(entity.getNotes())
                .createdBy(entity.getCreatedBy())
                .createdAt(entity.getCreatedAt())
                .updatedBy(entity.getUpdatedBy())
                .updatedAt(entity.getUpdatedAt())
                .version(entity.getVersion())
                .build();
    }

    public static HourlyRecord toEntity(HourlyRecordCreateRequest request) {
        return HourlyRecord.builder()
                .recordTime(request.getRecordTime())
                .consciousness(request.getConsciousness())
                .temperature(request.getTemperature())
                .heartRate(request.getHeartRate())
                .respiratoryRate(request.getRespiratoryRate())
                .systolicBP(request.getSystolicBP())
                .diastolicBP(request.getDiastolicBP())
                .meanArterialPressure(request.getMeanArterialPressure())
                .spo2(request.getSpo2())
                .etco2(request.getEtco2())
                .fio2(request.getFio2())
                .cvp(request.getCvp())
                .urineOutput(request.getUrineOutput())
                .drainOutput(request.getDrainOutput())
                .stool(request.getStool())
                .vomit(request.getVomit())
                .painScore(request.getPainScore())
                .notes(request.getNotes())
                .build();
    }
}
