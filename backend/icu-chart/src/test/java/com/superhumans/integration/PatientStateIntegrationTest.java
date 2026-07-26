package com.superhumans.integration;

import com.superhumans.dto.PatientStateCreateRequest;
import com.superhumans.dto.PatientStatePatchRequest;
import com.superhumans.dto.PatientStateResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PatientStateIntegrationTest extends AbstractIntegrationTest {

    private static final UUID OPEN_DAY_ID =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");
    private static final UUID NURSE_SIGNED_DAY_ID =
            UUID.fromString("b1111112-1111-1111-1111-111111111112");

    @Test
    @Sql(statements = "DELETE FROM patient_state_assessments WHERE clinical_day_id = 'b1111111-1111-1111-1111-111111111111'")
    void getPatientState_returnsEmptyListInitially() {
        var entity = authGet(getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/patient-state", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<PatientStateResponse>>() {},
                OPEN_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody()).isEmpty();
    }

    @Test
    void createPatientState_createsSuccessfully() {
        PatientStateCreateRequest req = new PatientStateCreateRequest(
                8, "Clear", "Normal", "None", "Moist",
                "Normal", "Present", "Stable", null);

        var entity = authEntity(req, getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/patient-state", HttpMethod.POST, entity,
                PatientStateResponse.class, OPEN_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getRecordHour()).isEqualTo(8);
        assertThat(res.getBody().getConsciousness()).isEqualTo("Clear");
        assertThat(res.getBody().getSkin()).isEqualTo("Normal");
    }

    @Test
    void createPatientState_onNurseSignedDay_returnsUnprocessable() {
        PatientStateCreateRequest req = new PatientStateCreateRequest(
                8, "Clear", null, null, null, null, null, null, null);

        var entity = authEntity(req, getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/patient-state", HttpMethod.POST, entity,
                String.class, NURSE_SIGNED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void createThenListPatientState_returnsCreatedRecord() {
        PatientStateCreateRequest req = new PatientStateCreateRequest(
                9, "Drowsy", "Warm", "Mild", null, null, null, null, null);

        var createEntity = authEntity(req, getNurseToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/patient-state", HttpMethod.POST, createEntity,
                PatientStateResponse.class, OPEN_DAY_ID);

        UUID newId = createRes.getBody().getId();

        var listEntity = authGet(getDoctorToken());
        var listRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/patient-state", HttpMethod.GET, listEntity,
                new ParameterizedTypeReference<List<PatientStateResponse>>() {},
                OPEN_DAY_ID);

        assertThat(listRes.getBody()).isNotEmpty();
        assertThat(listRes.getBody()).anyMatch(r -> r.getId().equals(newId));
    }

    @Test
    void updatePatientState_updatesFields() {
        PatientStateCreateRequest createReq = new PatientStateCreateRequest(
                10, "Clear", "Normal", null, null, null, null, null, null);

        var createEntity = authEntity(createReq, getNurseToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/patient-state", HttpMethod.POST, createEntity,
                PatientStateResponse.class, OPEN_DAY_ID);

        UUID recordId = createRes.getBody().getId();
        Integer version = createRes.getBody().getVersion();

        PatientStatePatchRequest patchReq = new PatientStatePatchRequest(
                "Drowsy", null, null, null, null, null, null, null, version);

        var patchEntity = authEntity(patchReq, getNurseToken());
        var patchRes = restTemplate.exchange(
                "/api/patient-state/{id}", HttpMethod.PATCH, patchEntity,
                Void.class, recordId);

        assertThat(patchRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void updatePatientState_withVersionMismatch_returnsConflict() {
        PatientStateCreateRequest createReq = new PatientStateCreateRequest(
                11, "Clear", null, null, null, null, null, null, null);

        var createEntity = authEntity(createReq, getNurseToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/patient-state", HttpMethod.POST, createEntity,
                PatientStateResponse.class, OPEN_DAY_ID);

        UUID recordId = createRes.getBody().getId();

        PatientStatePatchRequest patchReq = new PatientStatePatchRequest(
                "Drowsy", null, null, null, null, null, null, null, 999);

        var patchEntity = authEntity(patchReq, getNurseToken());
        var patchRes = restTemplate.exchange(
                "/api/patient-state/{id}", HttpMethod.PATCH, patchEntity,
                String.class, recordId);

        assertThat(patchRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
