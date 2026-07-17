package com.superhumans.integration;

import com.superhumans.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class EpisodeIntegrationTest extends AbstractIntegrationTest {

    private static final UUID SEED_EPISODE_ID =
            UUID.fromString("a1111111-1111-1111-1111-111111111111");
    private static final long SEED_PATIENT_ID = 1001L;

    @Test
    void getExistingEpisode_returnsEpisode() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/episodes/{id}", HttpMethod.GET, entity,
                EpisodeResponse.class, SEED_EPISODE_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getId()).isEqualTo(SEED_EPISODE_ID);
        assertThat(res.getBody().getPatientId()).isEqualTo(SEED_PATIENT_ID);
        assertThat(res.getBody().getStatus()).isNotNull();
    }

    @Test
    void getNonExistentEpisode_returnsNotFound() {
        var entity = authGet(getDoctorToken());
        UUID fakeId = UUID.randomUUID();

        var res = restTemplate.exchange(
                "/api/episodes/{id}", HttpMethod.GET, entity,
                String.class, fakeId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void searchEpisodes_byPatientId_returnsResults() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/episodes?patientId={patientId}", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<EpisodeResponse>>() {},
                SEED_PATIENT_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
    }

    @Test
    void searchEpisodes_all_returnsAllSeedData() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/episodes", HttpMethod.GET, entity,
                new ParameterizedTypeReference<List<EpisodeResponse>>() {});

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotEmpty();
    }

    @Test
    void createEpisode_asDoctor_succeeds() {
        EpisodeCreateRequest req = new EpisodeCreateRequest(
                1006L, null, null, LocalDateTime.now());

        var entity = authEntity(req, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/episodes", HttpMethod.POST, entity,
                EpisodeResponse.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getPatientId()).isEqualTo(req.getPatientId());
        assertThat(res.getBody().getStatus().name()).isEqualTo("ACTIVE");
    }

    @Test
    void createEpisode_asNurse_returnsForbidden() {
        EpisodeCreateRequest req = new EpisodeCreateRequest(
                1007L, null, null, LocalDateTime.now());

        var entity = authEntity(req, getNurseToken());

        var res = restTemplate.exchange(
                "/api/episodes", HttpMethod.POST, entity,
                String.class);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }

    @Test
    void updateEpisode_updatesSuccessfully() {
        var getEntity = authGet(getDoctorToken());
        var getRes = restTemplate.exchange(
                "/api/episodes/{id}", HttpMethod.GET, getEntity,
                EpisodeResponse.class, SEED_EPISODE_ID);
        int currentVersion = getRes.getBody().getVersion();

        EpisodePatchRequest req = new EpisodePatchRequest(
                null, null, LocalDateTime.now().plusDays(5), currentVersion);

        var entity = authEntity(req, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/episodes/{id}", HttpMethod.PATCH, entity,
                Void.class, SEED_EPISODE_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void updateEpisode_withVersionMismatch_returnsConflict() {
        EpisodePatchRequest req = new EpisodePatchRequest(
                null, null, null, 999);

        var entity = authEntity(req, getDoctorToken());

        var res = restTemplate.exchange(
                "/api/episodes/{id}", HttpMethod.PATCH, entity,
                String.class, SEED_EPISODE_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    @Test
    void closeEpisode_closesSuccessfully() {
        EpisodeCreateRequest createReq = new EpisodeCreateRequest(
                1008L, null, null, LocalDateTime.now());

        var createEntity = authEntity(createReq, getDoctorToken());

        var createRes = restTemplate.exchange(
                "/api/episodes", HttpMethod.POST, createEntity,
                EpisodeResponse.class);

        UUID newId = createRes.getBody().getId();

        EpisodeCloseRequest closeReq = new EpisodeCloseRequest(
                LocalDateTime.now(), 0);

        var closeEntity = authEntity(closeReq, getDoctorToken());

        var closeRes = restTemplate.exchange(
                "/api/episodes/{id}/close", HttpMethod.POST, closeEntity,
                Void.class, newId);

        assertThat(closeRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void closeEpisode_withWrongVersion_returnsConflict() {
        EpisodeCreateRequest createReq = new EpisodeCreateRequest(
                1009L, null, null, LocalDateTime.now());

        var createEntity = authEntity(createReq, getDoctorToken());
        var createRes = restTemplate.exchange(
                "/api/episodes", HttpMethod.POST, createEntity,
                EpisodeResponse.class);
        UUID newId = createRes.getBody().getId();

        EpisodeCloseRequest closeReq = new EpisodeCloseRequest(
                LocalDateTime.now(), 999);

        var closeEntity = authEntity(closeReq, getDoctorToken());
        var closeRes = restTemplate.exchange(
                "/api/episodes/{id}/close", HttpMethod.POST, closeEntity,
                String.class, newId);

        assertThat(closeRes.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }
}
