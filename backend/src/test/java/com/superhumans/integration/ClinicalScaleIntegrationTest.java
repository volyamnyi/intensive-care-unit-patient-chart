package com.superhumans.integration;

import com.superhumans.dto.ScaleResultCreateRequest;
import com.superhumans.dto.ScaleResultResponse;
import com.superhumans.entity.ClinicalScale;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicalScaleIntegrationTest extends AbstractIntegrationTest {

    private static final UUID SEED_DAY_ID =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");
    private static final UUID SEED_DAY_ID_2 =
            UUID.fromString("b3333333-3333-3333-3333-333333333333");

    @Test
    void getAvailableScales_returnsOk() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/scales", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<ClinicalScale>>() {});

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
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
}
