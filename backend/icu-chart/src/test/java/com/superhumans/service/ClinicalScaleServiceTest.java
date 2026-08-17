package com.superhumans.service;

import tools.jackson.databind.ObjectMapper;
import com.superhumans.dto.ScaleResultCreateRequest;
import com.superhumans.dto.ScaleResultPatchRequest;
import com.superhumans.dto.ScaleResultResponse;
import com.superhumans.icu.entity.*;
import com.superhumans.exception.DocumentLockedException;
import com.superhumans.exception.NotFoundException;
import com.superhumans.exception.VersionConflictException;
import com.superhumans.mapper.ScaleResultMapper;
import com.superhumans.icu.repository.ClinicalDayRepository;
import com.superhumans.icu.repository.ClinicalScaleRepository;
import com.superhumans.icu.repository.HourlyRecordRepository;
import com.superhumans.icu.repository.ScaleResultRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ClinicalScaleServiceTest {

    @Mock
    private ScaleResultRepository scaleResultRepository;

    @Mock
    private ClinicalScaleRepository clinicalScaleRepository;

    @Mock
    private ClinicalDayRepository clinicalDayRepository;

    @Mock
    private HourlyRecordRepository hourlyRecordRepository;

    @Mock
    private AuditService auditService;

    @Mock
    private ScaleResultMapper scaleResultMapper;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private ClinicalScaleService clinicalScaleService;

    @Captor
    private ArgumentCaptor<ScaleResult> scaleResultCaptor;

    private UUID resultId;
    private UUID clinicalDayId;
    private UUID episodeId;
    private UUID scaleId;
    private Long userId;
    private ClinicalDay clinicalDay;
    private ClinicalScale clinicalScale;
    private ClinicalScale apacheScale;
    private ClinicalScale sofaScale;

    @BeforeEach
    void setUp() {
        resultId = UUID.randomUUID();
        clinicalDayId = UUID.randomUUID();
        episodeId = UUID.randomUUID();
        scaleId = UUID.randomUUID();
        userId = 11L;
        clinicalDay = ClinicalDay.builder()
                .status(ClinicalDayStatus.OPEN)
                .build();
        clinicalDay.setId(clinicalDayId);
        clinicalDay.setVersion(0);
        clinicalScale = ClinicalScale.builder()
                .name("GCS")
                .isAutomatic(false)
                .status("ACTIVE")
                .build();
        clinicalScale.setId(scaleId);
        apacheScale = ClinicalScale.builder()
                .name("APACHE II")
                .isAutomatic(false)
                .status("ACTIVE")
                .build();
        apacheScale.setId(UUID.randomUUID());
        sofaScale = ClinicalScale.builder()
                .name("SOFA")
                .isAutomatic(false)
                .status("ACTIVE")
                .build();
        sofaScale.setId(UUID.randomUUID());
    }

    @Test
    void getAvailableScales_returnsList() {
        when(clinicalScaleRepository.findByStatus("ACTIVE")).thenReturn(List.of(clinicalScale));

        var result = clinicalScaleService.getAvailableScales();

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getName()).isEqualTo("GCS");
    }

    @Test
    void getScaleResult_whenFound_returnsResponse() {
        ScaleResult sr = ScaleResult.builder()
                .result("15")
                .build();
        sr.setId(resultId);
        sr.setClinicalDay(clinicalDay);
        sr.setScale(clinicalScale);
        when(scaleResultRepository.findById(resultId)).thenReturn(Optional.of(sr));

        ScaleResultResponse expected = ScaleResultResponse.builder()
                .id(resultId)
                .result("15")
                .build();
        when(scaleResultMapper.toResponse(sr)).thenReturn(expected);

        ScaleResultResponse res = clinicalScaleService.getScaleResult(resultId);

        assertThat(res.getId()).isEqualTo(resultId);
        assertThat(res.getResult()).isEqualTo("15");
    }

    @Test
    void getScaleResult_whenNotFound_throws() {
        when(scaleResultRepository.findById(any())).thenReturn(Optional.empty());

        assertThatThrownBy(() -> clinicalScaleService.getScaleResult(resultId))
                .isInstanceOf(NotFoundException.class);
    }

    @Test
    void getScaleResultsByClinicalDay_returnsList() {
        ScaleResult sr = ScaleResult.builder().build();
        sr.setId(resultId);
        sr.setClinicalDay(clinicalDay);
        sr.setScale(clinicalScale);
        when(scaleResultRepository.findByClinicalDayId(clinicalDayId)).thenReturn(List.of(sr));

        var results = clinicalScaleService.getScaleResultsByClinicalDay(clinicalDayId);

        assertThat(results).hasSize(1);
    }

    @Test
    void createScaleResult_createsSuccessfully() {
        ScaleResultCreateRequest req = new ScaleResultCreateRequest(scaleId, "15", null);

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        when(clinicalScaleRepository.findById(scaleId)).thenReturn(Optional.of(clinicalScale));
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setClinicalDay(clinicalDay);
        saved.setScale(clinicalScale);
        saved.setVersion(0);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);

        ScaleResultResponse res = clinicalScaleService.createScaleResult(clinicalDayId, req, userId);

        verify(scaleResultRepository).save(scaleResultCaptor.capture());
        assertThat(scaleResultCaptor.getValue().getResult()).isEqualTo("15");
        assertThat(scaleResultCaptor.getValue().getScale()).isEqualTo(clinicalScale);
        verify(auditService).logCreate("ScaleResult", resultId, userId);
    }

    @Test
    void createScaleResult_withAutomaticGCSCalculates() {
        clinicalScale.setIsAutomatic(true);
        clinicalScale.setName("Glasgow Coma Scale");

        HourlyRecord rec = HourlyRecord.builder()
                .consciousness("CLEAR")
                .recordTime(LocalDateTime.now())
                .build();

        ScaleResultCreateRequest req = new ScaleResultCreateRequest(scaleId, "10", null);

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        when(clinicalScaleRepository.findById(scaleId)).thenReturn(Optional.of(clinicalScale));
        when(hourlyRecordRepository.findByClinicalDayIdOrderByRecordTimeAsc(clinicalDayId))
                .thenReturn(List.of(rec));
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setClinicalDay(clinicalDay);
        saved.setScale(clinicalScale);
        saved.setVersion(0);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);

        clinicalScaleService.createScaleResult(clinicalDayId, req, userId);

        verify(scaleResultRepository).save(scaleResultCaptor.capture());
        assertThat(scaleResultCaptor.getValue().getResult()).isEqualTo("15");
    }

    @Test
    void createScaleResult_withAutomaticGCSUsesExplicitGcsFieldWhenPresent() {
        clinicalScale.setIsAutomatic(true);
        clinicalScale.setName("Glasgow Coma Scale");

        HourlyRecord rec = HourlyRecord.builder()
                .consciousness("COMA")
                .gcs(13)
                .recordTime(LocalDateTime.now())
                .build();

        ScaleResultCreateRequest req = new ScaleResultCreateRequest(scaleId, "10", null);

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        when(clinicalScaleRepository.findById(scaleId)).thenReturn(Optional.of(clinicalScale));
        when(hourlyRecordRepository.findByClinicalDayIdOrderByRecordTimeAsc(clinicalDayId))
                .thenReturn(List.of(rec));
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setClinicalDay(clinicalDay);
        saved.setScale(clinicalScale);
        saved.setVersion(0);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);

        clinicalScaleService.createScaleResult(clinicalDayId, req, userId);

        verify(scaleResultRepository).save(scaleResultCaptor.capture());
        assertThat(scaleResultCaptor.getValue().getResult()).isEqualTo("13");
    }

    @Test
    void createScaleResult_withAutomaticRASSCalculates() {
        clinicalScale.setIsAutomatic(true);
        clinicalScale.setName("RASS");

        HourlyRecord rec = HourlyRecord.builder()
                .consciousness("STUPOR")
                .recordTime(LocalDateTime.now())
                .build();

        ScaleResultCreateRequest req = new ScaleResultCreateRequest(scaleId, "0", null);

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));
        when(clinicalScaleRepository.findById(scaleId)).thenReturn(Optional.of(clinicalScale));
        when(hourlyRecordRepository.findByClinicalDayIdOrderByRecordTimeAsc(clinicalDayId))
                .thenReturn(List.of(rec));
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setClinicalDay(clinicalDay);
        saved.setScale(clinicalScale);
        saved.setVersion(0);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);

        clinicalScaleService.createScaleResult(clinicalDayId, req, userId);

        verify(scaleResultRepository).save(scaleResultCaptor.capture());
        assertThat(scaleResultCaptor.getValue().getResult()).isEqualTo("-2");
    }

    @Test
    void createScaleResult_whenDaySigned_throws() {
        clinicalDay.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        ScaleResultCreateRequest req = new ScaleResultCreateRequest(scaleId, "15", null);

        when(clinicalDayRepository.findById(clinicalDayId)).thenReturn(Optional.of(clinicalDay));

        assertThatThrownBy(() -> clinicalScaleService.createScaleResult(clinicalDayId, req, userId))
                .isInstanceOf(DocumentLockedException.class);
    }

    @Test
    void updateScaleResult_updatesSuccessfully() {
        ScaleResult existing = ScaleResult.builder()
                .result("10")
                .build();
        existing.setId(resultId);
        existing.setClinicalDay(clinicalDay);
        existing.setScale(clinicalScale);
        existing.setVersion(0);

        ScaleResultPatchRequest req = new ScaleResultPatchRequest("15", 0);

        when(scaleResultRepository.findById(resultId)).thenReturn(Optional.of(existing));
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setClinicalDay(clinicalDay);
        saved.setScale(clinicalScale);
        saved.setVersion(1);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);

        ScaleResultResponse res = clinicalScaleService.updateScaleResult(resultId, req, userId);

        verify(auditService).logUpdate("ScaleResult", resultId, userId, null, "Updated result");
    }

    @Test
    void updateScaleResult_withVersionMismatch_throws() {
        ScaleResult existing = ScaleResult.builder().build();
        existing.setId(resultId);
        existing.setClinicalDay(clinicalDay);
        existing.setScale(clinicalScale);
        existing.setVersion(0);
        ScaleResultPatchRequest req = new ScaleResultPatchRequest("15", 999);

        when(scaleResultRepository.findById(resultId)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> clinicalScaleService.updateScaleResult(resultId, req, userId))
                .isInstanceOf(VersionConflictException.class);
    }

    @Test
    void getScaleResultsByEpisode_returnsList() {
        ScaleResult sr = ScaleResult.builder().build();
        sr.setId(resultId);
        sr.setEpisodeId(episodeId);
        sr.setScale(apacheScale);
        when(scaleResultRepository.findByEpisodeId(episodeId)).thenReturn(List.of(sr));

        var results = clinicalScaleService.getScaleResultsByEpisode(episodeId);

        assertThat(results).hasSize(1);
        verify(scaleResultRepository).findByEpisodeId(episodeId);
    }

    @Test
    void getScaleResultsByEpisode_emptyEpisode_returnsEmptyList() {
        when(scaleResultRepository.findByEpisodeId(episodeId)).thenReturn(List.of());

        var results = clinicalScaleService.getScaleResultsByEpisode(episodeId);

        assertThat(results).isEmpty();
    }

    @Test
    void createEpisodeScaleResult_createsSuccessfully() {
        ScaleResultCreateRequest req = new ScaleResultCreateRequest(apacheScale.getId(), "25", null);

        when(clinicalScaleRepository.findById(apacheScale.getId())).thenReturn(Optional.of(apacheScale));
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setEpisodeId(episodeId);
        saved.setScale(apacheScale);
        saved.setResult("25");
        saved.setVersion(0);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);

        ScaleResultResponse res = clinicalScaleService.createEpisodeScaleResult(episodeId, req, userId);

        verify(scaleResultRepository).save(scaleResultCaptor.capture());
        assertThat(scaleResultCaptor.getValue().getEpisodeId()).isEqualTo(episodeId);
        assertThat(scaleResultCaptor.getValue().getResult()).isEqualTo("25");
        assertThat(scaleResultCaptor.getValue().getScale()).isEqualTo(apacheScale);
        verify(auditService).logCreate("ScaleResult", resultId, userId);
    }

    @Test
    void createEpisodeScaleResult_nullResult_setsN_A() {
        ScaleResultCreateRequest req = new ScaleResultCreateRequest(apacheScale.getId(), "", null);

        when(clinicalScaleRepository.findById(apacheScale.getId())).thenReturn(Optional.of(apacheScale));
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setEpisodeId(episodeId);
        saved.setScale(apacheScale);
        saved.setResult("N/A");
        saved.setVersion(0);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);

        clinicalScaleService.createEpisodeScaleResult(episodeId, req, userId);

        verify(scaleResultRepository).save(scaleResultCaptor.capture());
        assertThat(scaleResultCaptor.getValue().getResult()).isEqualTo("N/A");
    }

    @Test
    void calculateAndSaveScale_apacheIi_calculatesAndReturnsResult() throws Exception {
        UUID apacheScaleId = apacheScale.getId();
        Map<String, Object> rawData = Map.of(
                "temperatureC", 37.0,
                "heartRate", 80.0,
                "age", 35
        );

        when(clinicalScaleRepository.findById(apacheScaleId)).thenReturn(Optional.of(apacheScale));
        when(objectMapper.writeValueAsString(rawData)).thenReturn("{\"temperatureC\":37.0}");
        when(clinicalDayRepository.getReferenceById(clinicalDayId)).thenReturn(clinicalDay);
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setEpisodeId(episodeId);
        saved.setClinicalDay(clinicalDay);
        saved.setScale(apacheScale);
        saved.setResult("0");
        saved.setRawData("{\"temperatureC\":37.0}");
        saved.setVersion(0);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);
        ScaleResultResponse expected = ScaleResultResponse.builder().id(resultId).result("0").build();
        when(scaleResultMapper.toResponse(saved)).thenReturn(expected);

        ScaleResultResponse res = clinicalScaleService.calculateAndSaveScale(
                episodeId, clinicalDayId, apacheScaleId, rawData, userId);

        assertThat(res.getResult()).isEqualTo("0");
        verify(scaleResultRepository).save(scaleResultCaptor.capture());
        assertThat(scaleResultCaptor.getValue().getEpisodeId()).isEqualTo(episodeId);
        assertThat(scaleResultCaptor.getValue().getClinicalDay()).isEqualTo(clinicalDay);
        verify(auditService).logCreate("ScaleResult", resultId, userId);
    }

    @Test
    void calculateAndSaveScale_sofa_calculatesAndReturnsResult() throws Exception {
        UUID sofaScaleId = sofaScale.getId();
        Map<String, Object> rawData = Map.of(
                "paO2", 80.0,
                "fio2", 0.5,
                "onVentilator", true
        );

        when(clinicalScaleRepository.findById(sofaScaleId)).thenReturn(Optional.of(sofaScale));
        when(objectMapper.writeValueAsString(rawData)).thenReturn("{\"paO2\":80.0}");
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setEpisodeId(episodeId);
        saved.setScale(sofaScale);
        saved.setResult("3");
        saved.setVersion(0);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);
        ScaleResultResponse expected = ScaleResultResponse.builder().id(resultId).result("3").build();
        when(scaleResultMapper.toResponse(saved)).thenReturn(expected);

        ScaleResultResponse res = clinicalScaleService.calculateAndSaveScale(
                episodeId, null, sofaScaleId, rawData, userId);

        assertThat(res.getResult()).isEqualTo("3");
        verify(scaleResultRepository).save(scaleResultCaptor.capture());
        assertThat(scaleResultCaptor.getValue().getClinicalDay()).isNull();
    }

    @Test
    void calculateAndSaveScale_camIcu_positiveDelirium() throws Exception {
        UUID camScaleId = UUID.randomUUID();
        ClinicalScale camScale = ClinicalScale.builder()
                .name("CAM-ICU")
                .isAutomatic(false)
                .status("ACTIVE")
                .build();
        camScale.setId(camScaleId);

        Map<String, Object> rawData = Map.of(
                "acuteOnset", true,
                "inattention", true,
                "disorganizedThinking", true,
                "alteredConsciousness", false
        );

        when(clinicalScaleRepository.findById(camScaleId)).thenReturn(Optional.of(camScale));
        when(objectMapper.writeValueAsString(rawData)).thenReturn("{\"cam\":true}");
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setEpisodeId(episodeId);
        saved.setScale(camScale);
        saved.setResult("Позитивний");
        saved.setVersion(0);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);
        ScaleResultResponse expected = ScaleResultResponse.builder().id(resultId).result("Позитивний").build();
        when(scaleResultMapper.toResponse(saved)).thenReturn(expected);

        ScaleResultResponse res = clinicalScaleService.calculateAndSaveScale(
                episodeId, null, camScaleId, rawData, userId);

        assertThat(res.getResult()).isEqualTo("Позитивний");
    }

    @Test
    void calculateAndSaveScale_camIcu_negativeDelirium() throws Exception {
        UUID camScaleId = UUID.randomUUID();
        ClinicalScale camScale = ClinicalScale.builder()
                .name("CAM-ICU")
                .isAutomatic(false)
                .status("ACTIVE")
                .build();
        camScale.setId(camScaleId);

        Map<String, Object> rawData = Map.of(
                "acuteOnset", true,
                "inattention", false,
                "disorganizedThinking", false,
                "alteredConsciousness", false
        );

        when(clinicalScaleRepository.findById(camScaleId)).thenReturn(Optional.of(camScale));
        when(objectMapper.writeValueAsString(rawData)).thenReturn("{\"cam\":false}");
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setEpisodeId(episodeId);
        saved.setScale(camScale);
        saved.setResult("Негативний");
        saved.setVersion(0);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);
        ScaleResultResponse expected = ScaleResultResponse.builder().id(resultId).result("Негативний").build();
        when(scaleResultMapper.toResponse(saved)).thenReturn(expected);

        ScaleResultResponse res = clinicalScaleService.calculateAndSaveScale(
                episodeId, null, camScaleId, rawData, userId);

        assertThat(res.getResult()).isEqualTo("Негативний");
    }

    @Test
    void calculateAndSaveScale_braden_calculatesTotal() throws Exception {
        UUID bradenScaleId = UUID.randomUUID();
        ClinicalScale bradenScale = ClinicalScale.builder()
                .name("Браден")
                .isAutomatic(false)
                .status("ACTIVE")
                .build();
        bradenScale.setId(bradenScaleId);

        Map<String, Object> rawData = Map.of(
                "sensoryPerception", 4,
                "moisture", 4,
                "activity", 4,
                "mobility", 4,
                "nutrition", 4,
                "frictionShear", 3
        );

        when(clinicalScaleRepository.findById(bradenScaleId)).thenReturn(Optional.of(bradenScale));
        when(objectMapper.writeValueAsString(rawData)).thenReturn("{\"braden\":true}");
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setEpisodeId(episodeId);
        saved.setScale(bradenScale);
        saved.setResult("23");
        saved.setVersion(0);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);
        ScaleResultResponse expected = ScaleResultResponse.builder().id(resultId).result("23").build();
        when(scaleResultMapper.toResponse(saved)).thenReturn(expected);

        ScaleResultResponse res = clinicalScaleService.calculateAndSaveScale(
                episodeId, null, bradenScaleId, rawData, userId);

        assertThat(res.getResult()).isEqualTo("23");
    }

    @Test
    void updateScaleResult_withEpisodeLevelResult_doesNotCheckDayLock() {
        ScaleResult existing = ScaleResult.builder()
                .result("10")
                .build();
        existing.setId(resultId);
        existing.setEpisodeId(episodeId);
        existing.setClinicalDay(null);
        existing.setScale(apacheScale);
        existing.setVersion(0);

        ScaleResultPatchRequest req = new ScaleResultPatchRequest("20", 0);

        when(scaleResultRepository.findById(resultId)).thenReturn(Optional.of(existing));
        ScaleResult saved = new ScaleResult();
        saved.setId(resultId);
        saved.setEpisodeId(episodeId);
        saved.setScale(apacheScale);
        saved.setResult("20");
        saved.setVersion(1);
        when(scaleResultRepository.save(any(ScaleResult.class))).thenReturn(saved);

        clinicalScaleService.updateScaleResult(resultId, req, userId);

        verify(scaleResultRepository).save(scaleResultCaptor.capture());
        assertThat(scaleResultCaptor.getValue().getResult()).isEqualTo("20");
    }
}
