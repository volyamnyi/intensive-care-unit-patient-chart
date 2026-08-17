package com.superhumans.integration;

import com.superhumans.dto.ScaleResultCreateRequest;
import com.superhumans.dto.ScaleResultResponse;
import com.superhumans.icu.entity.ClinicalScale;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

import java.util.List;
import java.util.Map;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:data-test-core.sql",
     config = @SqlConfig(dataSource = "coreDataSource"))
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:data-test-icu.sql",
     config = @SqlConfig(dataSource = "icuDataSource"))
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:data-test-med.sql",
     config = @SqlConfig(dataSource = "medDataSource"))
class ClinicalScaleIntegrationTest extends AbstractIntegrationTest {

    private static final UUID SEED_DAY_ID =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");
    private static final UUID SEED_DAY_ID_2 =
            UUID.fromString("b3333333-3333-3333-3333-333333333333");
    private static final UUID SEED_EPISODE_ID =
            UUID.fromString("a3333333-3333-3333-3333-333333333333");
    private static final UUID SEED_APACHE_SCALE_ID =
            UUID.fromString("c1111111-1111-1111-1111-111111111104");
    private static final UUID SEED_SOFA_SCALE_ID =
            UUID.fromString("c1111111-1111-1111-1111-111111111103");
    private static final UUID SEED_CAM_SCALE_ID =
            UUID.fromString("c1111111-1111-1111-1111-111111111105");
    private static final UUID SEED_BRADEN_SCALE_ID =
            UUID.fromString("c1111111-1111-1111-1111-111111111106");

    @Test
    void getAvailableScales_returnsAllSix() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/scales", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<ClinicalScale>>() {});

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody()).hasSize(6);
    }

    @Test
    void getScaleResults_returnsEmptyListInitially() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/scales", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<ScaleResultResponse>>() {},
                SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEmpty();
    }

    @Test
    void createScaleResult_withNonExistentScale_returnsNotFound() {
        ScaleResultCreateRequest req = new ScaleResultCreateRequest();
        req.setScaleId(UUID.randomUUID());
        req.setResult("15");

        var entity = authEntity(req, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/scales", HttpMethod.POST, entity,
                String.class, SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createScaleResult_onDifferentDay_alsoEmpty() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/scales", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<ScaleResultResponse>>() {},
                SEED_DAY_ID_2);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEmpty();
    }

    @Test
    void getScaleResultsByEpisode_returnsExistingResults() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/episodes/{episodeId}/scales", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<ScaleResultResponse>>() {},
                SEED_EPISODE_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody()).hasSize(1);
        assertThat(res.getBody().get(0).getResult()).isEqualTo("25");
    }

    @Test
    void getScaleResultsByEpisode_unknownEpisode_returnsEmpty() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/episodes/{episodeId}/scales", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<ScaleResultResponse>>() {},
                UUID.randomUUID());

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isEmpty();
    }

    @Test
    void createEpisodeScaleResult_doctorCreatesSuccessfully() {
        ScaleResultCreateRequest req = new ScaleResultCreateRequest();
        req.setScaleId(SEED_APACHE_SCALE_ID);
        req.setResult("30");

        var entity = authEntity(req, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/episodes/{episodeId}/scales", HttpMethod.POST, entity,
                ScaleResultResponse.class, SEED_EPISODE_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getResult()).isEqualTo("30");
        assertThat(res.getBody().getEpisodeId()).isEqualTo(SEED_EPISODE_ID);
    }

    @Test
    void createEpisodeScaleResult_nurseBlockedFromApacheIi_returnsForbidden() {
        ScaleResultCreateRequest req = new ScaleResultCreateRequest();
        req.setScaleId(SEED_APACHE_SCALE_ID);
        req.setResult("20");

        var entity = authEntity(req, getNurseToken());

        var res = restTemplate.exchange(
                "/api/episodes/{episodeId}/scales", HttpMethod.POST, entity,
                String.class, SEED_EPISODE_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void calculateAndSaveScale_apacheIi_doctorCalculatesSuccessfully() {
        Map<String, Object> rawData = Map.of(
                "temperatureC", 38.5,
                "heartRate", 110,
                "age", 65
        );

        var entity = authEntity(rawData, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/episodes/{episodeId}/scales/calculate?scaleId={scaleId}",
                HttpMethod.POST, entity, ScaleResultResponse.class,
                SEED_EPISODE_ID, SEED_APACHE_SCALE_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getEpisodeId()).isEqualTo(SEED_EPISODE_ID);
    }

    @Test
    void calculateAndSaveScale_nurseBlockedFromApacheIi_returnsForbidden() {
        Map<String, Object> rawData = Map.of(
                "temperatureC", 38.5,
                "heartRate", 110
        );

        var entity = authEntity(rawData, getNurseToken());

        var res = restTemplate.exchange(
                "/api/episodes/{episodeId}/scales/calculate?scaleId={scaleId}",
                HttpMethod.POST, entity, String.class,
                SEED_EPISODE_ID, SEED_APACHE_SCALE_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void calculateAndSaveScale_sofa_calculatesSuccessfully() {
        Map<String, Object> rawData = Map.of(
                "paO2", 80.0,
                "fio2", 50.0,
                "onVentilator", true
        );

        var entity = authEntity(rawData, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/episodes/{episodeId}/scales/calculate?scaleId={scaleId}",
                HttpMethod.POST, entity, ScaleResultResponse.class,
                SEED_EPISODE_ID, SEED_SOFA_SCALE_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getResult()).isEqualTo("3");
    }

    @Test
    void calculateAndSaveScale_camIcu_doctorCalculatesSuccessfully() {
        Map<String, Object> rawData = Map.of(
                "acuteOnset", true,
                "inattention", true,
                "disorganizedThinking", true,
                "alteredConsciousness", false
        );

        var entity = authEntity(rawData, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/episodes/{episodeId}/scales/calculate?scaleId={scaleId}",
                HttpMethod.POST, entity, ScaleResultResponse.class,
                SEED_EPISODE_ID, SEED_CAM_SCALE_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getResult()).isEqualTo("Позитивний");
    }

    @Test
    void calculateAndSaveScale_braden_doctorAllowed() {
        Map<String, Object> rawData = Map.of(
                "sensoryPerception", 4,
                "moisture", 4,
                "activity", 4,
                "mobility", 4,
                "nutrition", 4,
                "frictionShear", 3
        );

        var entity = authEntity(rawData, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/episodes/{episodeId}/scales/calculate?scaleId={scaleId}",
                HttpMethod.POST, entity, String.class,
                SEED_EPISODE_ID, SEED_BRADEN_SCALE_ID);

        // Matrix: CAM-ICU/Браден/RASS is granted to DOCTOR, NURSE and HOD.
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    @Test
    void calculateAndSaveScale_braden_adminForbidden() {
        Map<String, Object> rawData = Map.of(
                "sensoryPerception", 4,
                "moisture", 4,
                "activity", 4,
                "mobility", 4,
                "nutrition", 4,
                "frictionShear", 3
        );

        var entity = authEntity(rawData, getAdminToken());

        var res = restTemplate.exchange(
                "/api/episodes/{episodeId}/scales/calculate?scaleId={scaleId}",
                HttpMethod.POST, entity, String.class,
                SEED_EPISODE_ID, SEED_BRADEN_SCALE_ID);

        // ADMIN holds no clinical scale permissions in the matrix.
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }

    @Test
    void calculateAndSaveScale_withClinicalDayParam_succeeds() {
        Map<String, Object> rawData = Map.of(
                "paO2", 80.0,
                "fio2", 50.0,
                "onVentilator", true
        );

        var entity = authEntity(rawData, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/episodes/{episodeId}/scales/calculate?clinicalDayId={dayId}&scaleId={scaleId}",
                HttpMethod.POST, entity, ScaleResultResponse.class,
                SEED_EPISODE_ID, SEED_DAY_ID_2, SEED_SOFA_SCALE_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getEpisodeId()).isEqualTo(SEED_EPISODE_ID);
    }

    @Test
    void createScaleResult_nurseCanCreateDailyScale() {
        ScaleResultCreateRequest req = new ScaleResultCreateRequest();
        req.setScaleId(SEED_BRADEN_SCALE_ID);
        req.setResult("15");

        var entity = authEntity(req, getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/scales", HttpMethod.POST, entity,
                ScaleResultResponse.class, SEED_DAY_ID_2);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getResult()).isEqualTo("15");
    }

    @Test
    void getScaleResults_nurseCanReadEpisodeScales() {
        var entity = authGet(getNurseToken());

        var res = restTemplate.exchange(
                "/api/episodes/{episodeId}/scales", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<ScaleResultResponse>>() {},
                SEED_EPISODE_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody()).hasSize(1);
    }

    @Test
    void createEpisodeScaleResult_nurseCanCreateNonRestrictedScale() {
        ScaleResultCreateRequest req = new ScaleResultCreateRequest();
        req.setScaleId(SEED_BRADEN_SCALE_ID);
        req.setResult("15");

        var entity = authEntity(req, getNurseToken());

        var res = restTemplate.exchange(
                "/api/episodes/{episodeId}/scales", HttpMethod.POST, entity,
                ScaleResultResponse.class, SEED_EPISODE_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getResult()).isEqualTo("15");
    }
}
