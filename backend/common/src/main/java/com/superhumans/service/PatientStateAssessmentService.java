package com.superhumans.service;

import com.superhumans.dto.PatientStateCreateRequest;
import com.superhumans.dto.PatientStatePatchRequest;
import com.superhumans.dto.PatientStateResponse;
import com.superhumans.icu.entity.ClinicalDay;
import com.superhumans.icu.entity.ClinicalDayStatus;
import com.superhumans.icu.entity.PatientStateAssessment;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.PatientStateMapper;
import com.superhumans.icu.repository.ClinicalDayRepository;
import com.superhumans.icu.repository.PatientStateAssessmentRepository;
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
public class PatientStateAssessmentService {

    PatientStateAssessmentRepository patientStateRepository;
    ClinicalDayRepository clinicalDayRepository;
    AuditService auditService;
    PatientStateMapper patientStateMapper;

    public List<PatientStateResponse> getByClinicalDay(UUID clinicalDayId) {
        return patientStateRepository.findByClinicalDayIdOrderByRecordHourAsc(clinicalDayId)
                .stream().map(patientStateMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public PatientStateResponse create(UUID clinicalDayId, PatientStateCreateRequest request, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(clinicalDayId)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + clinicalDayId));
        assertNotLocked(day);

        PatientStateAssessment entity = PatientStateAssessment.builder()
                .clinicalDay(day)
                .recordHour(request.getRecordHour())
                .consciousness(request.getConsciousness())
                .skin(request.getSkin())
                .edema(request.getEdema())
                .mucousMembranes(request.getMucousMembranes())
                .peripheralCirculation(request.getPeripheralCirculation())
                .bowelSounds(request.getBowelSounds())
                .generalCondition(request.getGeneralCondition())
                .additionalNotes(request.getAdditionalNotes())
                .build();
        entity.setCreatedBy(userId);
        entity.setUpdatedBy(userId);
        entity = patientStateRepository.save(entity);
        auditService.logCreate("PatientStateAssessment", entity.getId(), userId);
        return patientStateMapper.toResponse(entity);
    }

    @Transactional
    public PatientStateResponse update(UUID id, PatientStatePatchRequest request, Long userId) {
        PatientStateAssessment entity = patientStateRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Patient state assessment not found: " + id));

        if (!entity.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Patient state assessment was modified by another user");
        }
        assertNotLocked(entity.getClinicalDay());

        if (request.getConsciousness() != null) entity.setConsciousness(request.getConsciousness());
        if (request.getSkin() != null) entity.setSkin(request.getSkin());
        if (request.getEdema() != null) entity.setEdema(request.getEdema());
        if (request.getMucousMembranes() != null) entity.setMucousMembranes(request.getMucousMembranes());
        if (request.getPeripheralCirculation() != null) entity.setPeripheralCirculation(request.getPeripheralCirculation());
        if (request.getBowelSounds() != null) entity.setBowelSounds(request.getBowelSounds());
        if (request.getGeneralCondition() != null) entity.setGeneralCondition(request.getGeneralCondition());
        if (request.getAdditionalNotes() != null) entity.setAdditionalNotes(request.getAdditionalNotes());
        entity.setUpdatedBy(userId);
        entity = patientStateRepository.save(entity);
        auditService.logUpdate("PatientStateAssessment", id, userId, null, "Updated assessment");
        return patientStateMapper.toResponse(entity);
    }

    private void assertNotLocked(ClinicalDay day) {
        if (day.getStatus() == ClinicalDayStatus.NURSE_SIGNED
                || day.getStatus() == ClinicalDayStatus.DOCTOR_SIGNED
                || day.getStatus() == ClinicalDayStatus.CLOSED) {
            throw new DocumentLockedException("Clinical day is signed and cannot be modified");
        }
    }
}
