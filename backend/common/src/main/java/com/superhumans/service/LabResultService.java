package com.superhumans.service;

import com.superhumans.dto.LabResultCreateRequest;
import com.superhumans.dto.LabResultPatchRequest;
import com.superhumans.dto.LabResultResponse;
import com.superhumans.icu.entity.ClinicalDay;
import com.superhumans.icu.entity.ClinicalDayStatus;
import com.superhumans.icu.entity.LabResult;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.LabResultMapper;
import com.superhumans.icu.repository.ClinicalDayRepository;
import com.superhumans.icu.repository.LabResultRepository;
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
public class LabResultService {

    LabResultRepository labResultRepository;
    ClinicalDayRepository clinicalDayRepository;
    AuditService auditService;
    LabResultMapper labResultMapper;

    public List<LabResultResponse> getLabResultsByClinicalDay(UUID clinicalDayId) {
        return labResultRepository.findByClinicalDayIdOrderByMeasuredAtAsc(clinicalDayId)
                .stream().map(labResultMapper::toResponse).collect(Collectors.toList());
    }

    @Transactional
    public LabResultResponse createLabResult(UUID clinicalDayId, LabResultCreateRequest request, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(clinicalDayId)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + clinicalDayId));
        assertNotLocked(day);

        boolean isAbnormal = computeIsAbnormal(request.getResult(), request.getReferenceMin(), request.getReferenceMax());

        LabResult labResult = LabResult.builder()
                .clinicalDay(day)
                .testCode(request.getTestCode())
                .testName(request.getTestName())
                .result(request.getResult())
                .unit(request.getUnit())
                .referenceMin(request.getReferenceMin())
                .referenceMax(request.getReferenceMax())
                .isAbnormal(isAbnormal)
                .measuredAt(request.getMeasuredAt())
                .build();
        labResult.setCreatedBy(userId);
        labResult.setUpdatedBy(userId);
        labResult = labResultRepository.save(labResult);
        auditService.logCreate("LabResult", labResult.getId(), userId);
        return labResultMapper.toResponse(labResult);
    }

    @Transactional
    public LabResultResponse updateLabResult(UUID id, LabResultPatchRequest request, Long userId) {
        LabResult labResult = labResultRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Lab result not found: " + id));

        if (!labResult.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Lab result was modified by another user");
        }
        assertNotLocked(labResult.getClinicalDay());

        if (request.getResult() != null) {
            labResult.setResult(request.getResult());
            labResult.setIsAbnormal(computeIsAbnormal(
                    request.getResult(),
                    labResult.getReferenceMin(),
                    labResult.getReferenceMax()));
        }
        labResult.setUpdatedBy(userId);
        labResult = labResultRepository.save(labResult);
        auditService.logUpdate("LabResult", id, userId, null, "Updated result");
        return labResultMapper.toResponse(labResult);
    }

    private void assertNotLocked(ClinicalDay day) {
        if (day.getStatus() == ClinicalDayStatus.NURSE_SIGNED
                || day.getStatus() == ClinicalDayStatus.DOCTOR_SIGNED
                || day.getStatus() == ClinicalDayStatus.CLOSED) {
            throw new DocumentLockedException("Clinical day is signed and cannot be modified");
        }
    }

    private boolean computeIsAbnormal(String resultStr, Double refMin, Double refMax) {
        if (refMin == null || refMax == null || resultStr == null) return false;
        try {
            double val = Double.parseDouble(resultStr);
            return val < refMin || val > refMax;
        } catch (NumberFormatException e) {
            return false;
        }
    }
}
