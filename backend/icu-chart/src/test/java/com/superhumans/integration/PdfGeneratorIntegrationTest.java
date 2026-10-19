package com.superhumans.integration;

import com.superhumans.dto.*;
import com.superhumans.icu.entity.ClinicalDayStatus;
import com.superhumans.icu.entity.ScaleResult;
import com.superhumans.icu.repository.ClinicalDayRepository;
import com.superhumans.icu.repository.ClinicalScaleRepository;
import com.superhumans.icu.repository.ScaleResultRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.*;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PdfGeneratorIntegrationTest extends AbstractIntegrationTest {

    private static final UUID SEED_EPISODE_ID =
            UUID.fromString("a1111111-1111-1111-1111-111111111111");
    private static final UUID SEED_DAY_ID =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");
    private static final UUID NURSE_SIGNED_DAY_ID =
            UUID.fromString("b1111112-1111-1111-1111-111111111111");

    /** A seeded scale (GCS) the regression below attaches a result to. */
    private static final UUID SEED_SCALE_ID =
            UUID.fromString("c1111111-1111-1111-1111-111111111101");

    @Autowired
    private ClinicalScaleRepository scaleRepository;

    @Autowired
    private ScaleResultRepository scaleResultRepository;

    @Autowired
    private ClinicalDayRepository clinicalDayRepository;

    @Test
    void generatePdf_onOpenDay_returnsLocked() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/pdf", HttpMethod.POST, entity,
                String.class, SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void getLatestPdf_whenNoneExists_returnsNotFound() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/pdf", HttpMethod.GET, entity,
                String.class, SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NOT_FOUND);
    }

    @Test
    void generatePdf_afterDoctorSign_succeeds() {
        UUID dayId = NURSE_SIGNED_DAY_ID;

        SignRequest signReq = new SignRequest(15L, "pdf-doctor-hash");
        var signEntity = authEntity(signReq, getHodToken());

        var signRes = restTemplate.exchange(
                "/api/clinical-days/{id}/sign/doctor", HttpMethod.POST, signEntity,
                Void.class, dayId);

        assertThat(signRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var pdfEntity = authGet(getDoctorToken());

        var pdfRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/pdf", HttpMethod.POST, pdfEntity,
                PdfResponse.class, dayId);

        assertThat(pdfRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(pdfRes.getBody()).isNotNull();
        assertThat(pdfRes.getBody().getClinicalDayId()).isEqualTo(dayId);
        assertThat(pdfRes.getBody().getFileName()).contains(dayId.toString());
        assertThat(pdfRes.getBody().getFileVersion()).isEqualTo(2);
        assertThat(pdfRes.getBody().getGeneratedAt()).isNotNull();
    }

    @Test
    void getLatestPdf_afterGeneration_returnsPdf() {
        UUID dayId = UUID.fromString("b2222222-2222-2222-2222-222222222222");

        SignRequest nurseReq = new SignRequest(13L, "pdf-nurse-hash");
        var nurseEntity = authEntity(nurseReq, getNurseToken());
        restTemplate.exchange(
                "/api/clinical-days/{id}/sign/nurse", HttpMethod.POST, nurseEntity,
                Void.class, dayId);

        SignRequest docReq = new SignRequest(15L, "pdf-doctor-hash-2");
        var docEntity = authEntity(docReq, getHodToken());
        restTemplate.exchange(
                "/api/clinical-days/{id}/sign/doctor", HttpMethod.POST, docEntity,
                Void.class, dayId);

        var genEntity = authGet(getDoctorToken());
        restTemplate.exchange(
                "/api/clinical-days/{dayId}/pdf", HttpMethod.POST, genEntity,
                PdfResponse.class, dayId);

        var getEntity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/pdf", HttpMethod.GET, getEntity,
                PdfResponse.class, dayId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getFileVersion()).isPositive();
    }

    /**
     * The PDF render runs outside a transaction, so the lazy {@code scale}
     * association of a scale result had to be fetched with the result itself.
     * Reading it off the detached proxy threw {@code LazyInitializationException}
     * and the whole generation answered 500.
     *
     * <p>The seeded days carry no scale results, so only an environment whose
     * seed does (the E2E production seed) ever reached that code path - the
     * case is pinned here instead.
     */
    @Test
    void generatePdf_withScaleResults_rendersInsteadOfFailing() {
        // A fresh episode gives an isolated day: creating a day on a seeded
        // episode is rejected (next-day gating) and the seeded days are shared
        // with other integration tests.
        var epRes = restTemplate.exchange(
                "/api/episodes", HttpMethod.POST,
                authEntity(new EpisodeCreateRequest(
                        1021L, null, null, LocalDateTime.now(), null, null, null, null, null),
                        getDoctorToken()),
                EpisodeResponse.class);
        assertThat(epRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        UUID episodeId = epRes.getBody().getId();

        var day = clinicalDayRepository
                .findFirstByEpisodeIdOrderByDayNumberDesc(episodeId).orElseThrow();
        day.setStatus(ClinicalDayStatus.DOCTOR_SIGNED);
        day.setNurseSigned(true);
        day.setDoctorSigned(true);
        clinicalDayRepository.save(day);

        var scale = scaleRepository.findById(SEED_SCALE_ID).orElseThrow();
        scaleResultRepository.save(ScaleResult.builder()
                .clinicalDay(day)
                .scale(scale)
                .episodeId(episodeId)
                .result("15")
                .calculatedAt(LocalDateTime.now())
                .calculatedBy(15L)
                .build());

        var pdfRes = restTemplate.exchange(
                "/api/clinical-days/{dayId}/pdf", HttpMethod.POST, authGet(getDoctorToken()),
                PdfResponse.class, day.getId());

        assertThat(pdfRes.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        assertThat(pdfRes.getBody()).isNotNull();
        assertThat(pdfRes.getBody().getFileName()).contains(day.getId().toString());
    }

    @Test
    void generatePdf_withoutAuth_returnsUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<Void> entity = new HttpEntity<>(null, headers);

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/pdf", HttpMethod.POST, entity,
                String.class, SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
    }
}
