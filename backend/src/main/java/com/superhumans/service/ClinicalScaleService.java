package com.superhumans.service;

import com.superhumans.dto.ScaleResultCreateRequest;
import com.superhumans.dto.ScaleResultPatchRequest;
import com.superhumans.dto.ScaleResultResponse;
import com.superhumans.entity.*;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.ScaleResultMapper;
import com.superhumans.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;

@Service
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class ClinicalScaleService {

    ScaleResultRepository scaleResultRepository;
    ClinicalScaleRepository clinicalScaleRepository;
    ClinicalDayRepository clinicalDayRepository;
    HourlyRecordRepository hourlyRecordRepository;
    AuditService auditService;
    ScaleResultMapper scaleResultMapper;

    public ScaleResultResponse getScaleResult(UUID id) {
        ScaleResult result = scaleResultRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Scale result not found: " + id));
        return scaleResultMapper.toResponse(result);
    }

    public List<ScaleResultResponse> getScaleResultsByClinicalDay(UUID clinicalDayId) {
        return scaleResultRepository.findByClinicalDayId(clinicalDayId)
                .stream().map(scaleResultMapper::toResponse).collect(Collectors.toList());
    }

    public List<ClinicalScale> getAvailableScales() {
        return clinicalScaleRepository.findByStatus("ACTIVE");
    }

    @Transactional
    public ScaleResultResponse createScaleResult(UUID clinicalDayId, ScaleResultCreateRequest request, Long userId) {
        ClinicalDay day = clinicalDayRepository.findById(clinicalDayId)
                .orElseThrow(() -> new NotFoundException("Clinical day not found: " + clinicalDayId));
        assertNotLocked(day);

        ClinicalScale scale = clinicalScaleRepository.findById(request.getScaleId())
                .orElseThrow(() -> new NotFoundException("Clinical scale not found: " + request.getScaleId()));

        String result = scale.getIsAutomatic() != null && scale.getIsAutomatic()
                ? calculateAutomatic(scale, clinicalDayId, request.getResult())
                : request.getResult();

        ScaleResult sr = ScaleResult.builder()
                .clinicalDay(day)
                .scale(scale)
                .result(result)
                .calculatedAt(LocalDateTime.now())
                .calculatedBy(userId)
                .build();
        sr.setCreatedBy(userId);
        sr.setUpdatedBy(userId);
        sr = scaleResultRepository.save(sr);
        auditService.logCreate("ScaleResult", sr.getId(), userId);
        return scaleResultMapper.toResponse(sr);
    }

    @Transactional
    public ScaleResultResponse updateScaleResult(UUID id, ScaleResultPatchRequest request, Long userId) {
        ScaleResult result = scaleResultRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Scale result not found: " + id));

        if (!result.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Scale result was modified by another user");
        }
        assertNotLocked(result.getClinicalDay());

        if (request.getResult() != null) result.setResult(request.getResult());
        result.setUpdatedBy(userId);
        result = scaleResultRepository.save(result);
        auditService.logUpdate("ScaleResult", id, userId, null, "Updated result");
        return scaleResultMapper.toResponse(result);
    }

    private String calculateAutomatic(ClinicalScale scale, UUID clinicalDayId, String fallback) {
        String name = scale.getName().toLowerCase();
        if (name.contains("glasgow") || name.contains("gcs") || name.contains("шкала глазго")) {
            return calculateGcs(clinicalDayId);
        }
        if (name.contains("rass") || name.contains("ричмонд")) {
            return calculateRass(clinicalDayId);
        }
        return fallback;
    }

    private String calculateGcs(UUID clinicalDayId) {
        List<HourlyRecord> records = hourlyRecordRepository
                .findByClinicalDayIdOrderByRecordTimeAsc(clinicalDayId);
        if (records.isEmpty()) return "N/A";

        HourlyRecord latest = records.get(records.size() - 1);
        String consciousness = latest.getConsciousness();
        if (consciousness == null) return "N/A";

        int gcs;
        switch (consciousness.toUpperCase()) {
            case "CLEAR":
            case "ЯСНА":
                gcs = 15;
                break;
            case "STUPOR":
            case "СТУПОР":
                gcs = 10;
                break;
            case "SOPOR":
            case "СОПОР":
                gcs = 7;
                break;
            case "COMA":
            case "КОМА":
                gcs = 3;
                break;
            case "SEDATED":
            case "СЕДАЦІЯ":
                gcs = 5;
                break;
            default:
                gcs = 15;
        }
        return String.valueOf(gcs);
    }

    private String calculateRass(UUID clinicalDayId) {
        List<HourlyRecord> records = hourlyRecordRepository
                .findByClinicalDayIdOrderByRecordTimeAsc(clinicalDayId);
        if (records.isEmpty()) return "N/A";

        HourlyRecord latest = records.get(records.size() - 1);
        String consciousness = latest.getConsciousness();
        if (consciousness == null) return "N/A";

        int rass;
        switch (consciousness.toUpperCase()) {
            case "CLEAR":
            case "ЯСНА":
                rass = 0;
                break;
            case "STUPOR":
            case "СТУПОР":
                rass = -2;
                break;
            case "SOPOR":
            case "СОПОР":
                rass = -3;
                break;
            case "COMA":
            case "КОМА":
                rass = -5;
                break;
            case "SEDATED":
            case "СЕДАЦІЯ":
                rass = -4;
                break;
            default:
                rass = 0;
        }
        return String.valueOf(rass);
    }

    private void assertNotLocked(ClinicalDay day) {
        if (day.getStatus() == ClinicalDayStatus.NURSE_SIGNED
                || day.getStatus() == ClinicalDayStatus.DOCTOR_SIGNED
                || day.getStatus() == ClinicalDayStatus.CLOSED) {
            throw new DocumentLockedException("Clinical day is signed and cannot be modified");
        }
    }
}
