package com.superhumans.service;

import com.superhumans.dto.VentilationCreateRequest;
import com.superhumans.dto.VentilationPatchRequest;
import com.superhumans.dto.VentilationResponse;
import com.superhumans.entity.ClinicalDay;
import com.superhumans.entity.ClinicalDayStatus;
import com.superhumans.entity.VentilationSettings;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.VentilationMapper;
import com.superhumans.repository.icu.ClinicalDayRepository;
import com.superhumans.repository.icu.VentilationSettingsRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class VentilationSettingsService {

    VentilationSettingsRepository ventilationRepository;
    ClinicalDayRepository clinicalDayRepository;
    AuditService auditService;
    VentilationMapper ventilationMapper;

    public List<VentilationResponse> getByClinicalDay(UUID clinicalDayId) {
        return ventilationRepository.findByClinicalDayIdOrderByRecordHourAsc(clinicalDayId)
                .stream().map(ventilationMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public VentilationResponse create(UUID clinicalDayId, VentilationCreateRequest request, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(clinicalDayId)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + clinicalDayId));
        assertNotLocked(day);

        VentilationSettings entity = VentilationSettings.builder()
                .clinicalDay(day)
                .recordHour(request.getRecordHour())
                .mode(request.getMode())
                .fio2(request.getFio2())
                .peep(request.getPeep())
                .tidalVolume(request.getTidalVolume())
                .minuteVolume(request.getMinuteVolume())
                .pinsp(request.getPinsp())
                .psupport(request.getPsupport())
                .triggerType(request.getTriggerType())
                .ieRatio(request.getIeRatio())
                .respiratoryRate(request.getRespiratoryRate())
                .plateauPressure(request.getPlateauPressure())
                .meanAirwayPressure(request.getMeanAirwayPressure())
                .build();
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        entity = ventilationRepository.save(entity);
        auditService.logCreate("VentilationSettings", entity.getId(), userId);
        return ventilationMapper.toResponse(entity);
    }

    @Transactional
    public VentilationResponse update(UUID id, VentilationPatchRequest request, Long userId) {
        VentilationSettings entity = ventilationRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Ventilation settings not found: " + id));

        if (!entity.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Ventilation settings were modified by another user");
        }
        assertNotLocked(entity.getClinicalDay());

        if (request.getMode() != null) entity.setMode(request.getMode());
        if (request.getFio2() != null) entity.setFio2(request.getFio2());
        if (request.getPeep() != null) entity.setPeep(request.getPeep());
        if (request.getTidalVolume() != null) entity.setTidalVolume(request.getTidalVolume());
        if (request.getMinuteVolume() != null) entity.setMinuteVolume(request.getMinuteVolume());
        if (request.getPinsp() != null) entity.setPinsp(request.getPinsp());
        if (request.getPsupport() != null) entity.setPsupport(request.getPsupport());
        if (request.getTriggerType() != null) entity.setTriggerType(request.getTriggerType());
        if (request.getIeRatio() != null) entity.setIeRatio(request.getIeRatio());
        if (request.getRespiratoryRate() != null) entity.setRespiratoryRate(request.getRespiratoryRate());
        if (request.getPlateauPressure() != null) entity.setPlateauPressure(request.getPlateauPressure());
        if (request.getMeanAirwayPressure() != null) entity.setMeanAirwayPressure(request.getMeanAirwayPressure());
        entity.setUpdatedBy(userId);
        entity = ventilationRepository.save(entity);
        auditService.logUpdate("VentilationSettings", id, userId, null, "Updated settings");
        return ventilationMapper.toResponse(entity);
    }

    private void assertNotLocked(ClinicalDay day) {
        if (day.getStatus() == ClinicalDayStatus.NURSE_SIGNED
                || day.getStatus() == ClinicalDayStatus.DOCTOR_SIGNED
                || day.getStatus() == ClinicalDayStatus.CLOSED) {
            throw new DocumentLockedException("Clinical day is signed and cannot be modified");
        }
    }
}
