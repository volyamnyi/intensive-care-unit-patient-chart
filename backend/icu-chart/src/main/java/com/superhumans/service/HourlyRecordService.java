package com.superhumans.service;

import com.superhumans.dto.HourlyRecordCreateRequest;
import com.superhumans.dto.HourlyRecordPatchRequest;
import com.superhumans.dto.HourlyRecordResponse;
import com.superhumans.entity.ClinicalDay;
import com.superhumans.entity.ClinicalDayStatus;
import com.superhumans.entity.HourlyRecord;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.DuplicateHourlyRecordException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.HourlyRecordMapper;
import com.superhumans.repository.ClinicalDayRepository;
import com.superhumans.repository.HourlyRecordRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;

@Slf4j

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class HourlyRecordService {

    HourlyRecordRepository hourlyRecordRepository;
    ClinicalDayRepository clinicalDayRepository;
    AuditService auditService;
    FluidBalanceService fluidBalanceService;
    HourlyRecordMapper hourlyRecordMapper;

    public HourlyRecordResponse getHourlyRecord(UUID id) {
        HourlyRecord record = hourlyRecordRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Hourly record not found: " + id));
        return hourlyRecordMapper.toResponse(record);
    }

    public List<HourlyRecordResponse> getHourlyRecordsByClinicalDay(UUID clinicalDayId) {
        return hourlyRecordRepository.findByClinicalDayIdOrderByRecordTimeAsc(clinicalDayId)
                .stream().map(hourlyRecordMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public HourlyRecordResponse createHourlyRecord(UUID clinicalDayId, HourlyRecordCreateRequest request, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(clinicalDayId)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + clinicalDayId));
        assertNotLocked(day);

        int recordHour = request.getRecordTime().getHour();
        if (hourlyRecordRepository.findByClinicalDayIdAndRecordHour(clinicalDayId, recordHour).isPresent()) {
            throw new DuplicateHourlyRecordException(clinicalDayId, recordHour);
        }

        HourlyRecord record = hourlyRecordMapper.toEntity(request);
        record.setClinicalDay(day);
        record.setCreatedBy(userId);
        record.setUpdatedBy(userId);
        record = hourlyRecordRepository.save(record);

        if (record.getRecordTime() != null && record.getRecordTime().getHour() < LocalDateTime.now().getHour()) {
            log.info("BACK_ENTRY: HourlyRecord {} created for past hour {}", record.getId(), record.getRecordTime());
            auditService.logAction("HourlyRecord", record.getId(), "BACK_ENTRY", userId);
        }

        auditService.logCreate("HourlyRecord", record.getId(), userId);
        fluidBalanceService.recalculate(clinicalDayId, userId);
        return hourlyRecordMapper.toResponse(record);
    }

    @Transactional
    public HourlyRecordResponse updateHourlyRecord(UUID id, HourlyRecordPatchRequest request, Long userId) {
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
        fluidBalanceService.recalculate(record.getClinicalDay().getId(), userId);
        return hourlyRecordMapper.toResponse(record);
    }

    private void assertNotLocked(ClinicalDay day) {
        if (day.getStatus() == ClinicalDayStatus.NURSE_SIGNED
                || day.getStatus() == ClinicalDayStatus.DOCTOR_SIGNED
                || day.getStatus() == ClinicalDayStatus.CLOSED) {
            throw new DocumentLockedException("Clinical day is signed and cannot be modified");
        }
    }
}
