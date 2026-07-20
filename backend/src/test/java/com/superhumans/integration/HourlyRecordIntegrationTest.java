package com.superhumans.integration;

import com.superhumans.dto.HourlyRecordCreateRequest;
import com.superhumans.dto.HourlyRecordPatchRequest;
import com.superhumans.dto.HourlyRecordResponse;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class HourlyRecordIntegrationTest extends AbstractIntegrationTest {

    private static final UUID SEED_DAY_ID =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");
    private static final UUID OTHER_OPEN_DAY_ID =
            UUID.fromString("b2222222-2222-2222-2222-222222222222");

    @Test
    void getHourlyRecords_returnsEmptyListInitially() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/hourly-records", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<HourlyRecordResponse>>() {},
                OTHER_OPEN_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody()).isEmpty();
    }

    @Test
    void createHourlyRecord_createsSuccessfully() {
        HourlyRecordCreateRequest req = new HourlyRecordCreateRequest();
        req.setRecordTime(LocalDateTime.now().withHour(8));
        req.setHeartRate(80);
        req.setSystolicBP(120);
        req.setDiastolicBP(80);
        req.setTemperature(36.6);
        req.setSpo2(98.0);

        var entity = authEntity(req, getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/hourly-records", HttpMethod.POST, entity,
                HourlyRecordResponse.class, SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getHeartRate()).isEqualTo(80);
        assertThat(res.getBody().getSystolicBP()).isEqualTo(120);
        assertThat(res.getBody().getDiastolicBP()).isEqualTo(80);
    }

    @Test
    void createThenListHourlyRecords_returnsCreatedRecord() {
        HourlyRecordCreateRequest req = new HourlyRecordCreateRequest();
        req.setRecordTime(LocalDateTime.now().withHour(9));
        req.setHeartRate(75);
        req.setTemperature(37.0);

        var createEntity = authEntity(req, getNurseToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/hourly-records", HttpMethod.POST, createEntity,
                HourlyRecordResponse.class, SEED_DAY_ID);

        UUID newId = createRes.getBody().getId();

        var listEntity = authGet(getDoctorToken());
        var listRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/hourly-records", HttpMethod.GET, listEntity,
                new ParameterizedTypeReference<List<HourlyRecordResponse>>() {},
                SEED_DAY_ID);

        assertThat(listRes.getBody()).isNotEmpty();
        assertThat(listRes.getBody()).anyMatch(r -> r.getId().equals(newId));
    }

    @Test
    void updateHourlyRecord_updatesFields() {
        HourlyRecordCreateRequest createReq = new HourlyRecordCreateRequest();
        createReq.setRecordTime(LocalDateTime.now().withHour(10));
        createReq.setHeartRate(70);

        var createEntity = authEntity(createReq, getNurseToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/hourly-records", HttpMethod.POST, createEntity,
                HourlyRecordResponse.class, SEED_DAY_ID);

        UUID recordId = createRes.getBody().getId();
        Integer version = createRes.getBody().getVersion();

        HourlyRecordPatchRequest patchReq = new HourlyRecordPatchRequest();
        patchReq.setHeartRate(90);
        patchReq.setVersion(version);

        var patchEntity = authEntity(patchReq, getNurseToken());
        var patchRes = restTemplate.exchange(
                "/api/hourly-records/{id}", HttpMethod.PATCH, patchEntity,
                Void.class, recordId);

        assertThat(patchRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void updateHourlyRecord_onSignedDay_returnsConflictOrLocked() {
        UUID signedDayId = UUID.fromString("b1111112-1111-1111-1111-111111111112");

        HourlyRecordCreateRequest createReq = new HourlyRecordCreateRequest();
        createReq.setRecordTime(LocalDateTime.now().withHour(12));
        createReq.setHeartRate(72);

        var createEntity = authEntity(createReq, getNurseToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/hourly-records", HttpMethod.POST, createEntity,
                HourlyRecordResponse.class, signedDayId);

        UUID recordId = createRes.getBody().getId();
        Integer version = createRes.getBody().getVersion();

        HourlyRecordPatchRequest patchReq = new HourlyRecordPatchRequest();
        patchReq.setHeartRate(95);
        patchReq.setVersion(version);

        var patchEntity = authEntity(patchReq, getNurseToken());
        var patchRes = restTemplate.exchange(
                "/api/hourly-records/{id}", HttpMethod.PATCH, patchEntity,
                String.class, recordId);

        assertThat(patchRes.getStatusCode()).isIn(
                java.util.Set.of(HttpStatus.LOCKED, HttpStatus.UNPROCESSABLE_ENTITY, HttpStatus.CONFLICT));
    }

    @Test
    void updateHourlyRecord_withVersionMismatch_returnsConflict() {
        HourlyRecordCreateRequest createReq = new HourlyRecordCreateRequest();
        createReq.setRecordTime(LocalDateTime.now().withHour(11));
        createReq.setHeartRate(70);

        var createEntity = authEntity(createReq, getNurseToken());
        var createRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/hourly-records", HttpMethod.POST, createEntity,
                HourlyRecordResponse.class, SEED_DAY_ID);

        UUID recordId = createRes.getBody().getId();

        HourlyRecordPatchRequest patchReq = new HourlyRecordPatchRequest();
        patchReq.setHeartRate(100);
        patchReq.setVersion(999);

        var patchEntity = authEntity(patchReq, getNurseToken());
        var patchRes = restTemplate.exchange(
                "/api/hourly-records/{id}", HttpMethod.PATCH, patchEntity,
                String.class, recordId);

        assertThat(patchRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
