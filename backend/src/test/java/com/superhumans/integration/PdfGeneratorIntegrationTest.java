package com.superhumans.integration;

import com.superhumans.dto.*;
import com.superhumans.entity.ClinicalDayStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class PdfGeneratorIntegrationTest extends AbstractIntegrationTest {

    private static final UUID SEED_EPISODE_ID =
            UUID.fromString("a1111111-1111-1111-1111-111111111111");
    private static final UUID SEED_DAY_ID =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");
    private static final UUID NURSE_SIGNED_DAY_ID =
            UUID.fromString("b1111112-1111-1111-1111-111111111111");

    @Test
    void generatePdf_onOpenDay_returnsLocked() {
        var entity = authGet(getDoctorToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/pdf", HttpMethod.POST, entity,
                String.class, SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_ENTITY);
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

        SignRequest signReq = new SignRequest(UUID.randomUUID(), "pdf-doctor-hash");
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
        assertThat(pdfRes.getBody().getFileVersion()).isEqualTo(1);
        assertThat(pdfRes.getBody().getGeneratedAt()).isNotNull();
    }

    @Test
    void getLatestPdf_afterGeneration_returnsPdf() {
        UUID dayId = NURSE_SIGNED_DAY_ID;

        SignRequest signReq = new SignRequest(UUID.randomUUID(), "pdf-doctor-hash-2");
        var signEntity = authEntity(signReq, getHodToken());
        restTemplate.exchange(
                "/api/clinical-days/{id}/sign/doctor", HttpMethod.POST, signEntity,
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

    @Test
    void generatePdf_withoutAuth_returnsUnauthorized() {
        HttpHeaders headers = new HttpHeaders();
        headers.set("Content-Type", "application/json");
        HttpEntity<Void> entity = new HttpEntity<>(null, headers);

        var res = restTemplate.exchange(
                "/api/clinical-days/{dayId}/pdf", HttpMethod.POST, entity,
                String.class, SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
