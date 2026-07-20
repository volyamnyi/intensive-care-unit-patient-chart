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
        assertThat(res.getBody().getEpisodeId()).isEqualTo(SEED_EPISODE_ID);
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
        EpisodeCreateRequest epReq = new EpisodeCreateRequest(
                1020L, null, null, LocalDateTime.now(), null, null, null, null);
        var epEntity = authEntity(epReq, getDoctorToken());
        var epRes = restTemplate.exchange("/api/episodes", HttpMethod.POST, epEntity, EpisodeResponse.class);
        UUID newEpisodeId = epRes.getBody().getId();

        ClinicalDayCreateRequest req = new ClinicalDayCreateRequest(
                newEpisodeId, LocalDateTime.now(), LocalDateTime.now().plusDays(1), null);

        var entity = authEntity(req, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days", HttpMethod.POST, entity,
                ClinicalDayResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getEpisodeId()).isEqualTo(newEpisodeId);
    }

    @Test
    void createClinicalDay_whenOpenDayExists_returnsConflict() {
        EpisodeCreateRequest epReq = new EpisodeCreateRequest(
                1021L, null, null, LocalDateTime.now(), null, null, null, null);
        var epEntity = authEntity(epReq, getDoctorToken());
        var epRes = restTemplate.exchange("/api/episodes", HttpMethod.POST, epEntity, EpisodeResponse.class);
        UUID newEpisodeId = epRes.getBody().getId();

        ClinicalDayCreateRequest firstDay = new ClinicalDayCreateRequest(
                newEpisodeId, LocalDateTime.now(), LocalDateTime.now().plusDays(1), null);
        var firstEntity = authEntity(firstDay, getDoctorToken());
        restTemplate.exchange("/api/clinical-days", HttpMethod.POST, firstEntity, ClinicalDayResponse.class);

        ClinicalDayCreateRequest secondDay = new ClinicalDayCreateRequest(
                newEpisodeId, LocalDateTime.now(), LocalDateTime.now().plusDays(1), null);

        var entity = authEntity(secondDay, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days", HttpMethod.POST, entity,
                String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void updateClinicalDay_updatesEndTime() {
        UUID dayId = UUID.fromString("b2222222-2222-2222-2222-222222222222");
        var getEntity = authGet(getDoctorToken());
        var getRes = restTemplate.exchange(
                "/api/clinical-days/{id}", HttpMethod.GET, getEntity,
                ClinicalDayResponse.class, dayId);
        int currentVersion = getRes.getBody().getVersion();

        ClinicalDayPatchRequest req = new ClinicalDayPatchRequest(
                LocalDateTime.now().plusHours(36), null, currentVersion);

        var entity = authEntity(req, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{id}", HttpMethod.PATCH, entity,
                Void.class, dayId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void signNurse_marksDayAsNurseSigned() {
        SignRequest req = new SignRequest(13L, "nurse-hash");

        var entity = authEntity(req, getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{id}/sign/nurse", HttpMethod.POST, entity,
                Void.class, SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void signDoctor_whenNurseNotSigned_returnsBadRequest() {
        // Use a fresh day from seed that hasn't been nurse-signed yet
        UUID freshDayId = UUID.fromString("b2222222-2222-2222-2222-222222222222");
        SignRequest req = new SignRequest(15L, "doctor-hash");

        var entity = authEntity(req, getHodToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{id}/sign/doctor", HttpMethod.POST, entity,
                String.class, freshDayId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
    }

    @Test
    void fullSignWorkflow_nurseThenDoctor_succeeds() {
        UUID freshEpisodeId = UUID.fromString("a3333333-3333-3333-3333-333333333333");
        UUID freshDayId = UUID.fromString("b3333333-3333-3333-3333-333333333333");

        SignRequest nurseReq = new SignRequest(13L, "nurse-hash");
        var nurseEntity = authEntity(nurseReq, getNurseToken());
        restTemplate.exchange(
                "/api/clinical-days/{id}/sign/nurse", HttpMethod.POST, nurseEntity,
                Void.class, freshDayId);

        SignRequest doctorReq = new SignRequest(15L, "doctor-hash");
        var doctorEntity = authEntity(doctorReq, getHodToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{id}/sign/doctor", HttpMethod.POST, doctorEntity,
                Void.class, freshDayId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void reopenClinicalDay_reopensDoctorSignedDay() {
        // First sign nurse, then doctor, then reopen
        UUID episodeId = UUID.fromString("a2222222-2222-2222-2222-222222222222");
        UUID dayId = UUID.fromString("b2222222-2222-2222-2222-222222222222");

        SignRequest nurseReq = new SignRequest(13L, "hash-n");
        var nurseEntity = authEntity(nurseReq, getNurseToken());
        restTemplate.exchange(
                "/api/clinical-days/{id}/sign/nurse", HttpMethod.POST, nurseEntity,
                Void.class, dayId);

        SignRequest doctorReq = new SignRequest(15L, "hash-d");
        var doctorEntity = authEntity(doctorReq, getHodToken());
        restTemplate.exchange(
                "/api/clinical-days/{id}/sign/doctor", HttpMethod.POST, doctorEntity,
                Void.class, dayId);

        // Fetch the current day to get the latest version
        var getEntity = authGet(getHodToken());
        var getRes = restTemplate.exchange(
                "/api/clinical-days/{id}", HttpMethod.GET, getEntity,
                ClinicalDayResponse.class, dayId);
        int currentVersion = getRes.getBody().getVersion();

        ReopenRequest reopenReq = new ReopenRequest("Need corrections", currentVersion);
        var reopenEntity = authEntity(reopenReq, getHodToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{id}/reopen", HttpMethod.POST, reopenEntity,
                Void.class, dayId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify via GET that the day was reopened
        var verifyEntity = authGet(getHodToken());
        var verifyRes = restTemplate.exchange(
                "/api/clinical-days/{id}", HttpMethod.GET, verifyEntity,
                ClinicalDayResponse.class, dayId);
        assertThat(verifyRes.getBody().getStatus()).isEqualTo(ClinicalDayStatus.REOPENED);
    }
}
