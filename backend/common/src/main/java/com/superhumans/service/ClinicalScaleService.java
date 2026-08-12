package com.superhumans.service;

import com.superhumans.dto.ScaleResultCreateRequest;
import com.superhumans.dto.ScaleResultPatchRequest;
import com.superhumans.dto.ScaleResultResponse;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.ObjectMapper;
import com.superhumans.entity.*;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.ScaleResultMapper;
import com.superhumans.repository.*;
import com.superhumans.service.scale.*;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
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
    ObjectMapper objectMapper;

    public ScaleResultResponse getScaleResult(UUID id) {
        ScaleResult result = scaleResultRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Scale result not found: " + id));
        return scaleResultMapper.toResponse(result);
    }

    public List<ScaleResultResponse> getScaleResultsByClinicalDay(UUID clinicalDayId) {
        return scaleResultRepository.findByClinicalDayId(clinicalDayId)
                .stream().map(scaleResultMapper::toResponse).collect(Collectors.toList());
    }

    public List<ScaleResultResponse> getScaleResultsByEpisode(UUID episodeId) {
        return scaleResultRepository.findByEpisodeId(episodeId)
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

        String result = request.getResult();
        if (scale.getIsAutomatic() != null && scale.getIsAutomatic()) {
            result = calculateAutomatic(scale, clinicalDayId, null);
        } else {
            if (result == null || result.isBlank()) {
                result = autoFillFromPreviousDay(day, scale, clinicalDayId);
            }
        }
        if (result == null) result = "N/A";

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
    public ScaleResultResponse createEpisodeScaleResult(UUID episodeId, ScaleResultCreateRequest request, Long userId) {
        ClinicalScale scale = clinicalScaleRepository.findById(request.getScaleId())
                .orElseThrow(() -> new NotFoundException("Clinical scale not found: " + request.getScaleId()));

        String result = request.getResult();
        if (result == null || result.isBlank()) result = "N/A";

        ScaleResult sr = ScaleResult.builder()
                .episodeId(episodeId)
                .scale(scale)
                .result(result)
                .rawData(request.getResult())
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
    public ScaleResultResponse calculateAndSaveScale(
            UUID episodeId, UUID clinicalDayId, UUID scaleId,
            Map<String, Object> rawData, Long userId) {
        ClinicalScale scale = clinicalScaleRepository.findById(scaleId)
                .orElseThrow(() -> new NotFoundException("Clinical scale not found: " + scaleId));

        String scaleName = scale.getName() != null ? scale.getName().toLowerCase() : "";
        String rawJson;
        try {
            rawJson = objectMapper.writeValueAsString(rawData);
        } catch (JacksonException e) {
            rawJson = rawData.toString();
        }

        String result;
        if (scaleName.contains("apache")) {
            result = String.valueOf(calculateApacheIi(rawData));
        } else if (scaleName.contains("sofa")) {
            result = String.valueOf(calculateSofa(rawData));
        } else if (scaleName.contains("cam-icu") || scaleName.contains("cam")) {
            result = calculateCamIcu(rawData);
        } else if (scaleName.contains("браден") || scaleName.contains("braden")) {
            result = String.valueOf(calculateBraden(rawData));
        } else {
            result = (String) rawData.getOrDefault("result", "N/A");
        }

        ScaleResult sr = ScaleResult.builder()
                .episodeId(episodeId)
                .clinicalDay(clinicalDayId != null ? clinicalDayRepository.getReferenceById(clinicalDayId) : null)
                .scale(scale)
                .result(result)
                .rawData(rawJson)
                .calculatedAt(LocalDateTime.now())
                .calculatedBy(userId)
                .build();
        sr.setCreatedBy(userId);
        sr.setUpdatedBy(userId);
        sr = scaleResultRepository.save(sr);
        auditService.logCreate("ScaleResult", sr.getId(), userId);
        return scaleResultMapper.toResponse(sr);
    }

    private int calculateApacheIi(Map<String, Object> rawData) {
        Integer age = rawData.containsKey("age") ? ((Number) rawData.get("age")).intValue() : null;
        Integer gcs = rawData.containsKey("gcs") ? ((Number) rawData.get("gcs")).intValue() : null;

        ApacheIiCalculator.ApacheIiInput input = ApacheIiCalculator.ApacheIiInput.builder()
                .temperatureC(doubleOrNull(rawData.get("temperatureC")))
                .meanArterialPressure(doubleOrNull(rawData.get("meanArterialPressure")))
                .heartRate(doubleOrNull(rawData.get("heartRate")))
                .respiratoryRate(doubleOrNull(rawData.get("respiratoryRate")))
                .fio2(doubleOrNull(rawData.get("fio2")))
                .paO2(doubleOrNull(rawData.get("paO2")))
                .paCO2(doubleOrNull(rawData.get("paCO2")))
                .aaDo2(doubleOrNull(rawData.get("aaDo2")))
                .ph(doubleOrNull(rawData.get("ph")))
                .serumHco3(doubleOrNull(rawData.get("serumHco3")))
                .serumSodium(doubleOrNull(rawData.get("serumSodium")))
                .serumPotassium(doubleOrNull(rawData.get("serumPotassium")))
                .serumCreatinine(doubleOrNull(rawData.get("serumCreatinine")))
                .acuteRenalFailure(boolOrNull(rawData.get("acuteRenalFailure")))
                .hematocrit(doubleOrNull(rawData.get("hematocrit")))
                .whiteBloodCount(doubleOrNull(rawData.get("whiteBloodCount")))
                .gcs(gcs)
                .age(age)
                .chronicHealthType((String) rawData.get("chronicHealthType"))
                .isEmergencySurgical(boolOrNull(rawData.get("emergencySurgical")))
                .build();
        return ApacheIiCalculator.calculate(input).getTotal();
    }

    private int calculateSofa(Map<String, Object> rawData) {
        SofaCalculator.SofaInput input = SofaCalculator.SofaInput.builder()
                .paO2(doubleOrNull(rawData.get("paO2")))
                .fio2(doubleOrNull(rawData.get("fio2")))
                .onVentilator(boolOrNull(rawData.get("onVentilator")))
                .platelets(doubleOrNull(rawData.get("platelets")))
                .bilirubin(doubleOrNull(rawData.get("bilirubin")))
                .map(doubleOrNull(rawData.get("map")))
                .dopamine(doubleOrNull(rawData.get("dopamine")))
                .dobutamine(doubleOrNull(rawData.get("dobutamine")))
                .norepinephrine(doubleOrNull(rawData.get("norepinephrine")))
                .epinephrine(doubleOrNull(rawData.get("epinephrine")))
                .gcs(rawData.containsKey("gcs") ? ((Number) rawData.get("gcs")).intValue() : null)
                .creatinine(doubleOrNull(rawData.get("creatinine")))
                .urineOutput(doubleOrNull(rawData.get("urineOutput")))
                .build();
        return SofaCalculator.calculate(input).getTotal();
    }

    private String calculateCamIcu(Map<String, Object> rawData) {
        CamIcuCalculator.CamIcuInput input = CamIcuCalculator.CamIcuInput.builder()
                .acuteOnset(boolOrDefault(rawData.get("acuteOnset")))
                .inattention(boolOrDefault(rawData.get("inattention")))
                .disorganizedThinking(boolOrDefault(rawData.get("disorganizedThinking")))
                .alteredConsciousness(boolOrDefault(rawData.get("alteredConsciousness")))
                .build();
        CamIcuCalculator.CamIcuResult result = CamIcuCalculator.calculate(input);
        return result.isDelirium() ? "Позитивний" : "Негативний";
    }

    private int calculateBraden(Map<String, Object> rawData) {
        BradenCalculator.BradenInput input = BradenCalculator.BradenInput.builder()
                .sensoryPerception(intClamp(rawData.get("sensoryPerception"), 1, 4))
                .moisture(intClamp(rawData.get("moisture"), 1, 4))
                .activity(intClamp(rawData.get("activity"), 1, 4))
                .mobility(intClamp(rawData.get("mobility"), 1, 4))
                .nutrition(intClamp(rawData.get("nutrition"), 1, 4))
                .frictionShear(intClamp(rawData.get("frictionShear"), 1, 3))
                .build();
        return BradenCalculator.calculate(input).getTotal();
    }

    private Double doubleOrNull(Object v) {
        if (v == null) return null;
        if (v instanceof Number n) return n.doubleValue();
        return null;
    }

    private Boolean boolOrNull(Object v) {
        if (v instanceof Boolean b) return b;
        return null;
    }

    private boolean boolOrDefault(Object v) {
        return Boolean.TRUE.equals(v);
    }

    private int intClamp(Object v, int min, int max) {
        if (v instanceof Number n) return Math.max(min, Math.min(max, n.intValue()));
        return min;
    }

    private String autoFillFromPreviousDay(ClinicalDay day, ClinicalScale scale, UUID clinicalDayId) {
        String scaleName = scale.getName() != null ? scale.getName().toLowerCase() : "";
        if (!scaleName.contains("sofa")) return null;

        List<ClinicalDay> days = clinicalDayRepository
                .findByEpisodeIdOrderByDayNumberAsc(day.getEpisode().getId());
        int idx = -1;
        for (int i = 0; i < days.size(); i++) {
            if (days.get(i).getId().equals(clinicalDayId)) { idx = i; break; }
        }
        if (idx <= 0) return null;

        ClinicalDay prevDay = days.get(idx - 1);
        List<ScaleResult> prevResults = scaleResultRepository.findByClinicalDayId(prevDay.getId());
        for (ScaleResult pr : prevResults) {
            if (pr.getScale() != null && pr.getScale().getName() != null
                    && pr.getScale().getName().equalsIgnoreCase(scale.getName())) {
                return pr.getResult();
            }
        }
        return null;
    }

    @Transactional
    public ScaleResultResponse updateScaleResult(UUID id, ScaleResultPatchRequest request, Long userId) {
        ScaleResult result = scaleResultRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Scale result not found: " + id));

        if (!result.getVersion().equals(request.getVersion())) {
            throw new VersionConflictException("Scale result was modified by another user");
        }
        if (result.getClinicalDay() != null) {
            assertNotLocked(result.getClinicalDay());
        }

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
        if (latest.getGcs() != null) return String.valueOf(latest.getGcs());

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
