package com.superhumans.service;

import com.superhumans.dto.HourlyRecordCreateRequest;
import com.superhumans.dto.HourlyRecordPatchRequest;
import com.superhumans.dto.HourlyRecordResponse;
import com.superhumans.entity.ClinicalDay;
import com.superhumans.entity.ClinicalDayStatus;
import com.superhumans.entity.HourlyRecord;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.HourlyRecordMapper;
import com.superhumans.repository.ClinicalDayRepository;
import com.superhumans.repository.HourlyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class HourlyRecordService {

    private final HourlyRecordRepository hourlyRecordRepository;
    private final ClinicalDayRepository clinicalDayRepository;
    private final AuditService auditService;
    private final FluidBalanceService fluidBalanceService;

    public HourlyRecordResponse getHourlyRecord(UUID id) {
        HourlyRecord record = hourlyRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hourly record not found: " + id));
        return HourlyRecordMapper.toResponse(record);
    }

    public List<HourlyRecordResponse> getHourlyRecordsByClinicalDay(UUID clinicalDayId) {
        return hourlyRecordRepository.findByClinicalDayIdOrderByRecordTimeAsc(clinicalDayId)
                .stream().map(HourlyRecordMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public HourlyRecordResponse createHourlyRecord(UUID clinicalDayId, HourlyRecordCreateRequest request, UUID userId) {
        ClinicalDay day = clinicalDayRepository.findById(clinicalDayId)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + clinicalDayId));
        assertNotLocked(day);

        HourlyRecord record = HourlyRecordMapper.toEntity(request);
        record.setClinicalDay(day);
        record.setCreatedBy(userId);
        record.setUpdatedBy(userId);
        record = hourlyRecordRepository.save(record);
        auditService.logCreate("HourlyRecord", record.getId(), userId);
        fluidBalanceService.recalculate(clinicalDayId, userId);
        return HourlyRecordMapper.toResponse(record);
    }

    @Transactional
    public HourlyRecordResponse updateHourlyRecord(UUID id, HourlyRecordPatchRequest request, UUID userId) {
        HourlyRecord record = hourlyRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hourly record not found: " + id));

        if (!record.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Hourly record was modified by another user");
        }
        assertNotLocked(record.getClinicalDay());

        if (request.getConsciousness() != null) record.setConsciousness(request.getConsciousness());
        if (request.getTemperature() != null) record.setTemperature(request.getTemperature());
        if (request.getHeartRate() != null) record.setHeartRate(request.getHeartRate());
        if (request.getRespiratoryRate() != null) record.setRespiratoryRate(request.getRespiratoryRate());
        if (request.getSystolicBP() != null) record.setSystolicBP(request.getSystolicBP());
        if (request.getDiastolicBP() != null) record.setDiastolicBP(request.getDiastolicBP());
        if (request.getMeanArterialPressure() != null) record.setMeanArterialPressure(request.getMeanArterialPressure());
        if (request.getSpo2() != null) record.setSpo2(request.getSpo2());
        if (request.getEtco2() != null) record.setEtco2(request.getEtco2());
        if (request.getFio2() != null) record.setFio2(request.getFio2());
        if (request.getCvp() != null) record.setCvp(request.getCvp());
        if (request.getUrineOutput() != null) record.setUrineOutput(request.getUrineOutput());
        if (request.getDrainOutput() != null) record.setDrainOutput(request.getDrainOutput());
        if (request.getStool() != null) record.setStool(request.getStool());
        if (request.getVomit() != null) record.setVomit(request.getVomit());
        if (request.getPainScore() != null) record.setPainScore(request.getPainScore());
        if (request.getNotes() != null) record.setNotes(request.getNotes());
        record.setUpdatedBy(userId);
        record = hourlyRecordRepository.save(record);
        auditService.logUpdate("HourlyRecord", id, userId, null, "Updated hourly record");
        return HourlyRecordMapper.toResponse(record);
    }

    private void assertNotLocked(ClinicalDay day) {
        if (day.getStatus() == ClinicalDayStatus.DOCTOR_SIGNED
                || day.getStatus() == ClinicalDayStatus.CLOSED) {
            throw new DocumentLockedException("Clinical day is signed and cannot be modified");
        }
    }
}
