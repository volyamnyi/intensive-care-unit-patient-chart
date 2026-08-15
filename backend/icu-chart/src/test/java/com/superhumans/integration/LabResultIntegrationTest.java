package com.superhumans.integration;

import com.superhumans.dto.LabResultCreateRequest;
import com.superhumans.dto.LabResultPatchRequest;
import com.superhumans.dto.LabResultResponse;
import org.junit.jupiter.api.Test;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class LabResultIntegrationTest extends AbstractIntegrationTest {

    private static final UUID OPEN_DAY_ID =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");
    private static final UUID NURSE_SIGNED_DAY_ID =
            UUID.fromString("b1111112-1111-1111-1111-111111111112");

    @Test
    @Sql(statements = "DELETE FROM lab_results WHERE clinical_day_id = 'b1111111-1111-1111-1111-111111111111'",
         config = @SqlConfig(dataSource = "icuDataSource"))
    void getLabResults_returnsEmptyListInitially() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/lab-results", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<LabResultResponse>>() {},
                OPEN_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody()).isEmpty();
    }

    @Test
    void createLabResult_createsSuccessfully() {
        LabResultCreateRequest req = new LabResultCreateRequest(
                "HGB", "Hemoglobin", "14.5", "g/dL", 12.0, 16.0, LocalDateTime.now());

        var entity = authEntity(req, getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/lab-results", HttpMethod.POST, entity,
                LabResultResponse.class, OPEN_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getTestCode()).isEqualTo("HGB");
        assertThat(res.getBody().getTestName()).isEqualTo("Hemoglobin");
        assertThat(res.getBody().getResult()).isEqualTo("14.5");
        assertThat(res.getBody().getIsAbnormal()).isFalse();
    }

    @Test
    void createLabResult_onNurseSignedDay_returnsUnprocessable() {
        LabResultCreateRequest req = new LabResultCreateRequest(
                "HGB", "Hemoglobin", "14.5", "g/dL", 12.0, 16.0, LocalDateTime.now());

        var entity = authEntity(req, getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/lab-results", HttpMethod.POST, entity,
                String.class, NURSE_SIGNED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void createThenListLabResults_returnsCreatedRecord() {
        LabResultCreateRequest req = new LabResultCreateRequest(
                "WBC", "White Blood Cells", "8.5", "K/uL", 4.0, 11.0, LocalDateTime.now());

        var createEntity = authEntity(req, getNurseToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/lab-results", HttpMethod.POST, createEntity,
                LabResultResponse.class, OPEN_DAY_ID);

        UUID newId = createRes.getBody().getId();

        var listEntity = authGet(getDoctorToken());
        var listRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/lab-results", HttpMethod.GET, listEntity,
                new ParameterizedTypeReference<List<LabResultResponse>>() {},
                OPEN_DAY_ID);

        assertThat(listRes.getBody()).isNotEmpty();
        assertThat(listRes.getBody()).anyMatch(r -> r.getId().equals(newId));
    }

    @Test
    void updateLabResult_updatesFields() {
        LabResultCreateRequest createReq = new LabResultCreateRequest(
                "NA", "Sodium", "140", "mmol/L", 135.0, 145.0, LocalDateTime.now());

        var createEntity = authEntity(createReq, getNurseToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/lab-results", HttpMethod.POST, createEntity,
                LabResultResponse.class, OPEN_DAY_ID);

        UUID recordId = createRes.getBody().getId();
        Integer version = createRes.getBody().getVersion();

        LabResultPatchRequest patchReq = new LabResultPatchRequest("138", version);

        var patchEntity = authEntity(patchReq, getNurseToken());
        var patchRes = restTemplate.exchange(
                "/api/lab-results/{id}", HttpMethod.PATCH, patchEntity,
                Void.class, recordId);

        assertThat(patchRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void updateLabResult_withVersionMismatch_returnsConflict() {
        LabResultCreateRequest createReq = new LabResultCreateRequest(
                "K", "Potassium", "4.0", "mmol/L", 3.5, 5.0, LocalDateTime.now());

        var createEntity = authEntity(createReq, getNurseToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/lab-results", HttpMethod.POST, createEntity,
                LabResultResponse.class, OPEN_DAY_ID);

        UUID recordId = createRes.getBody().getId();

        LabResultPatchRequest patchReq = new LabResultPatchRequest("4.5", 999);

        var patchEntity = authEntity(patchReq, getNurseToken());
        var patchRes = restTemplate.exchange(
                "/api/lab-results/{id}", HttpMethod.PATCH, patchEntity,
                String.class, recordId);

        assertThat(patchRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void createLabResult_marksAsAbnormalWhenOutOfRange() {
        LabResultCreateRequest req = new LabResultCreateRequest(
                "HGB", "Hemoglobin", "8.0", "g/dL", 12.0, 16.0, LocalDateTime.now());

        var entity = authEntity(req, getNurseToken());
        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/lab-results", HttpMethod.POST, entity,
                LabResultResponse.class, OPEN_DAY_ID);

        assertThat(res.getBody().getIsAbnormal()).isTrue();
    }
}
