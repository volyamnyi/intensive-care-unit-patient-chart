package com.superhumans.integration;

import com.superhumans.dto.*;
import com.superhumans.entity.ClinicalDayStatus;
import org.junit.jupiter.api.Test;
import org.springframework.http.*;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;

class SignatureIntegrationTest extends AbstractIntegrationTest {

    private static final UUID SEED_EPISODE_ID =
            UUID.fromString("a1111111-1111-1111-1111-111111111111");
    private static final UUID SEED_DAY_ID =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");
    private static final UUID NURSE_SIGNED_DAY_ID =
            UUID.fromString("b1111112-1111-1111-1111-111111111111");

    @Test
    void nurseSignedDay_hasCorrectFields() {
        var entity = authGet(getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{id}", HttpMethod.GET, entity,
                ClinicalDayResponse.class, NURSE_SIGNED_DAY_ID);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getNurseSigned()).isTrue();
        assertThat(res.getBody().getDoctorSigned()).isFalse();
    }

    @Test
    void openDay_hasNoSignatures() {
        var entity = authGet(getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{id}", HttpMethod.GET, entity,
                ClinicalDayResponse.class, SEED_DAY_ID);

        assertThat(res.getBody()).isNotNull();
        assertThat(res.getBody().getNurseSigned()).isFalse();
        assertThat(res.getBody().getDoctorSigned()).isFalse();
    }

    @Test
    void signNurse_returnsNoContent() {
        SignRequest req = new SignRequest(13L, "nurse-hash-001");
        var entity = authEntity(req, getNurseToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{id}/sign/nurse", HttpMethod.POST, entity,
                Void.class, SEED_DAY_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);
    }

    @Test
    void signDoctor_afterNurse_updatesDayStatus() {
        UUID dayId = UUID.fromString("b3333333-3333-3333-3333-333333333333");
        UUID episodeId = UUID.fromString("a3333333-3333-3333-3333-333333333333");

        SignRequest nurseReq = new SignRequest(13L, "nurse-hash-002");
        var nurseEntity = authEntity(nurseReq, getNurseToken());
        restTemplate.exchange(
                "/api/clinical-days/{id}/sign/nurse", HttpMethod.POST, nurseEntity,
                Void.class, dayId);

        SignRequest doctorReq = new SignRequest(15L, "doctor-hash-002");
        var doctorEntity = authEntity(doctorReq, getHodToken());

        var res = restTemplate.exchange(
                "/api/clinical-days/{id}/sign/doctor", HttpMethod.POST, doctorEntity,
                Void.class, dayId);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        var getEntity = authGet(getDoctorToken());
        var getRes = restTemplate.exchange(
                "/api/clinical-days/{id}", HttpMethod.GET, getEntity,
                ClinicalDayResponse.class, dayId);

        assertThat(getRes.getBody().getStatus()).isEqualTo(ClinicalDayStatus.DOCTOR_SIGNED);
    }

    @Test
    void reopenClearsSignatures() {
        UUID dayId = UUID.fromString("b2222222-2222-2222-2222-222222222222");
        UUID episodeId = UUID.fromString("a2222222-2222-2222-2222-222222222222");

        SignRequest nurseReq = new SignRequest(13L, "hash-n-reopen");
        var nurseEntity = authEntity(nurseReq, getNurseToken());
        restTemplate.exchange(
                "/api/clinical-days/{id}/sign/nurse", HttpMethod.POST, nurseEntity,
                Void.class, dayId);

        SignRequest doctorReq = new SignRequest(15L, "hash-d-reopen");
        var doctorEntity = authEntity(doctorReq, getHodToken());
        restTemplate.exchange(
                "/api/clinical-days/{id}/sign/doctor", HttpMethod.POST, doctorEntity,
                Void.class, dayId);

        var getBefore = authGet(getDoctorToken());
        var beforeRes = restTemplate.exchange(
                "/api/clinical-days/{id}", HttpMethod.GET, getBefore,
                ClinicalDayResponse.class, dayId);

        int version = beforeRes.getBody().getVersion();

        ReopenRequest reopenReq = new ReopenRequest("Потрібні виправлення", version);
        var reopenEntity = authEntity(reopenReq, getHodToken());

        var reopenRes = restTemplate.exchange(
                "/api/clinical-days/{id}/reopen", HttpMethod.POST, reopenEntity,
                Void.class, dayId);

        assertThat(reopenRes.getStatusCode()).isEqualTo(HttpStatus.NO_CONTENT);

        // Verify via GET that the day was reopened and signatures cleared
        var getAfter = authGet(getDoctorToken());
        var afterRes = restTemplate.exchange(
                "/api/clinical-days/{id}", HttpMethod.GET, getAfter,
                ClinicalDayResponse.class, dayId);
        assertThat(afterRes.getBody().getStatus()).isEqualTo(ClinicalDayStatus.REOPENED);
        assertThat(afterRes.getBody().getNurseSigned()).isFalse();
        assertThat(afterRes.getBody().getDoctorSigned()).isFalse();
    }
}

