package com.superhumans.integration;

import com.superhumans.dto.VentilationCreateRequest;
import com.superhumans.dto.VentilationPatchRequest;
import com.superhumans.dto.VentilationResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class VentilationIntegrationTest extends AbstractIntegrationTest {

    private static final UUID OPEN_DAY_ID =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");
    private static final UUID NURSE_SIGNED_DAY_ID =
            UUID.fromString("b1111112-1111-1111-1111-111111111112");

    @Test
    @Sql(statements = "DELETE FROM ventilation_settings WHERE clinical_day_id = 'b1111111-1111-1111-1111-111111111111'")
    void getVentilationSettings_returnsEmptyListInitially() {
        var entity = authGet(getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/ventilation", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<VentilationResponse>>() {},
                OPEN_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody()).isEmpty();
    }

    @Test
    void createVentilation_createsSuccessfully() {
        VentilationCreateRequest req = new VentilationCreateRequest();
        req.setRecordHour(8);
        req.setMode("SIMV");
        req.setFio2(0.5);
        req.setPeep(5.0);

        var entity = authEntity(req, getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/ventilation", HttpMethod.POST, entity,
                VentilationResponse.class, OPEN_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getRecordHour()).isEqualTo(8);
        assertThat(res.getBody().getMode()).isEqualTo("SIMV");
        assertThat(res.getBody().getFio2()).isEqualTo(0.5);
        assertThat(res.getBody().getPeep()).isEqualTo(5.0);
    }

    @Test
    void createVentilation_onNurseSignedDay_returnsUnprocessable() {
        VentilationCreateRequest req = new VentilationCreateRequest();
        req.setRecordHour(8);

        var entity = authEntity(req, getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/ventilation", HttpMethod.POST, entity,
                String.class, NURSE_SIGNED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void createThenListVentilation_returnsCreatedRecord() {
        VentilationCreateRequest req = new VentilationCreateRequest();
        req.setRecordHour(9);
        req.setMode("CPAP");
        req.setFio2(0.4);

        var createEntity = authEntity(req, getNurseToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/ventilation", HttpMethod.POST, createEntity,
                VentilationResponse.class, OPEN_DAY_ID);

        UUID newId = createRes.getBody().getId();

        var listEntity = authGet(getDoctorToken());
        var listRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/ventilation", HttpMethod.GET, listEntity,
                new ParameterizedTypeReference<List<VentilationResponse>>() {},
                OPEN_DAY_ID);

        assertThat(listRes.getBody()).isNotEmpty();
        assertThat(listRes.getBody()).anyMatch(r -> r.getId().equals(newId));
    }

    @Test
    void updateVentilation_updatesFields() {
        VentilationCreateRequest createReq = new VentilationCreateRequest();
        createReq.setRecordHour(10);
        createReq.setMode("SIMV");

        var createEntity = authEntity(createReq, getNurseToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/ventilation", HttpMethod.POST, createEntity,
                VentilationResponse.class, OPEN_DAY_ID);

        UUID recordId = createRes.getBody().getId();
        Integer version = createRes.getBody().getVersion();

        VentilationPatchRequest patchReq = new VentilationPatchRequest();
        patchReq.setMode("PSIMV");
        patchReq.setVersion(version);

        var patchEntity = authEntity(patchReq, getNurseToken());
        var patchRes = restTemplate.exchange(
                "/api/ventilation/{id}", HttpMethod.PATCH, patchEntity,
                Void.class, recordId);

        assertThat(patchRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void updateVentilation_withVersionMismatch_returnsConflict() {
        VentilationCreateRequest createReq = new VentilationCreateRequest();
        createReq.setRecordHour(11);
        createReq.setMode("SIMV");

        var createEntity = authEntity(createReq, getNurseToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/ventilation", HttpMethod.POST, createEntity,
                VentilationResponse.class, OPEN_DAY_ID);

        UUID recordId = createRes.getBody().getId();

        VentilationPatchRequest patchReq = new VentilationPatchRequest();
        patchReq.setMode("PSIMV");
        patchReq.setVersion(999);

        var patchEntity = authEntity(patchReq, getNurseToken());
        var patchRes = restTemplate.exchange(
                "/api/ventilation/{id}", HttpMethod.PATCH, patchEntity,
                String.class, recordId);

        assertThat(patchRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
