package com.superhumans.integration;

import com.superhumans.dto.*;
import com.superhumans.entity.ClinicalDayStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class ClinicalDayIntegrationTest extends AbstractIntegrationTest {

    private static final UUID SEED_EPISODE_ID =
            UUID.fromString("a1111111-1111-1111-1111-111111111111");
    private static final UUID SEED_DAY_ID =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");

    @Test
    void getExistingClinicalDay_returnsDay() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{id}", HttpMethod.GET, entity,
                ClinicalDayResponse.class, SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getId()).isEqualTo(SEED_DAY_ID);
        assertThat(res.getBody().getDayNumber()).isEqualTo(1);
        assertThat(res.getBody().getStatus()).isEqualTo(ClinicalDayStatus.OPEN);
    }

    @Test
    void getNonExistentClinicalDay_returnsNotFound() {
        var entity = authGet(getDoctorToken());
        UUID fakeId = UUID.randomUUID();

        var res = restTemplate.exchange(
                "/api/clinical-days/{id}", HttpMethod.GET, entity,
                String.class, fakeId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void createClinicalDay_createsSuccessfully() {
        ClinicalDayCreateRequest req = new ClinicalDayCreateRequest(
                SEED_EPISODE_ID, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        var entity = authEntity(req, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days", HttpMethod.POST, entity,
                ClinicalDayResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getEpisodeId()).isEqualTo(SEED_EPISODE_ID);
    }

    @Test
    void createClinicalDay_whenOpenDayExists_returnsConflict() {
        ClinicalDayCreateRequest req = new ClinicalDayCreateRequest(
                SEED_EPISODE_ID, LocalDateTime.now(), LocalDateTime.now().plusDays(1));

        var entity = authEntity(req, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days", HttpMethod.POST, entity,
                String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void updateClinicalDay_updatesEndTime() {
        ClinicalDayPatchRequest req = new ClinicalDayPatchRequest(
                LocalDateTime.now().plusHours(36), 0);

        var entity = authEntity(req, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{id}", HttpMethod.PATCH, entity,
                ClinicalDayResponse.class, SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
    }

    @Test
    void signNurse_marksDayAsNurseSigned() {
        SignRequest req = new SignRequest(UUID.randomUUID(), "nurse-hash");

        var entity = authEntity(req, getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{id}/sign/nurse", HttpMethod.POST, entity,
                SignResponse.class, SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getRole()).isEqualTo("NURSE");
    }

    @Test
    void signDoctor_whenNurseNotSigned_returnsBadRequest() {
        // Use a fresh day from seed that hasn't been nurse-signed yet
        UUID freshDayId = UUID.fromString("b2222222-2222-2222-2222-222222222222");
        SignRequest req = new SignRequest(UUID.randomUUID(), "doctor-hash");

        var entity = authEntity(req, getHodToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{id}/sign/doctor", HttpMethod.POST, entity,
                String.class, freshDayId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void fullSignWorkflow_nurseThenDoctor_succeeds() {
        UUID freshEpisodeId = UUID.fromString("a3333333-3333-3333-3333-333333333333");
        UUID freshDayId = UUID.fromString("b3333333-3333-3333-3333-333333333333");

        SignRequest nurseReq = new SignRequest(UUID.randomUUID(), "nurse-hash");
        var nurseEntity = authEntity(nurseReq, getNurseToken());
        restTemplate.exchange(
                "/api/clinical-days/{id}/sign/nurse", HttpMethod.POST, nurseEntity,
                SignResponse.class, freshDayId);

        SignRequest doctorReq = new SignRequest(UUID.randomUUID(), "doctor-hash");
        var doctorEntity = authEntity(doctorReq, getHodToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{id}/sign/doctor", HttpMethod.POST, doctorEntity,
                SignResponse.class, freshDayId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getRole()).isEqualTo("DOCTOR");
    }

    @Test
    void reopenClinicalDay_reopensDoctorSignedDay() {
        // First sign nurse, then doctor, then reopen
        UUID episodeId = UUID.fromString("a2222222-2222-2222-2222-222222222222");
        UUID dayId = UUID.fromString("b2222222-2222-2222-2222-222222222222");

        SignRequest nurseReq = new SignRequest(UUID.randomUUID(), "hash-n");
        var nurseEntity = authEntity(nurseReq, getNurseToken());
        restTemplate.exchange(
                "/api/clinical-days/{id}/sign/nurse", HttpMethod.POST, nurseEntity,
                SignResponse.class, dayId);

        SignRequest doctorReq = new SignRequest(UUID.randomUUID(), "hash-d");
        var doctorEntity = authEntity(doctorReq, getHodToken());
        restTemplate.exchange(
                "/api/clinical-days/{id}/sign/doctor", HttpMethod.POST, doctorEntity,
                SignResponse.class, dayId);

        ReopenRequest reopenReq = new ReopenRequest("Need corrections", 0);
        var reopenEntity = authEntity(reopenReq, getHodToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{id}/reopen", HttpMethod.POST, reopenEntity,
                ClinicalDayResponse.class, dayId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getStatus()).isEqualTo(ClinicalDayStatus.REOPENED);
    }
}
