package com.superhumans.integration;

import static org.assertj.core.api.Assertions.assertThat;

import com.superhumans.dto.ReopenRequest;
import com.superhumans.dto.SignRequest;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;

/**
 * Phase B clinical-integrity and abuse pins (issue #171): document locking,
 * optimistic concurrency, validation ranges, duplicate-hour conflicts, the
 * login abuse baseline (including the #185 rate limiter), and PDF response
 * hardening headers.
 *
 * <p>Each mutating scenario owns a distinct seeded day so JUnit method order
 * never changes outcomes: b1111112 (signed, read-only asserts), b4444444
 * (reopen flow), b2222222 (full sign+PDF flow), b3333333 (failed-sign +
 * hourly writes), b1111111 (inline sign flow for the note boundary pin).
 */
class ClinicalIntegrityPinsIntegrationTest extends AbstractIntegrationTest {

    private static final UUID NURSE_SIGNED_DAY_MELNYK =
            UUID.fromString("b1111112-1111-1111-1111-111111111111");
    private static final UUID NURSE_SIGNED_DAY_BOYKO =
            UUID.fromString("b4444444-4444-4444-4444-444444444444");
    private static final UUID OPEN_DAY_PDF_FLOW =
            UUID.fromString("b2222222-2222-2222-2222-222222222222");
    private static final UUID OPEN_DAY_HOURLY_WRITES =
            UUID.fromString("b3333333-3333-3333-3333-333333333333");
    private static final UUID OPEN_DAY_SIGN_FLOW =
            UUID.fromString("b1111111-1111-1111-1111-111111111111");
    private static final UUID EPISODE_ID =
            UUID.fromString("a1111111-1111-1111-1111-111111111111");

    // ---- Signed-day immutability ----

    @Test
    void nurseSignedDay_blocksHourlyCreate_422() {
        var res = restTemplate.exchange("/api/clinical-days/{id}/hourly-records",
                HttpMethod.POST,
                authEntity(hourlyAt(LocalDate.now().minusDays(1).atTime(13, 0)), getNurseToken()),
                String.class, NURSE_SIGNED_DAY_MELNYK);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void nurseSignedDay_blocksOrderCreate_422() {
        String body = "{\"category\":\"infusion\",\"drugName\":\"NaCl\",\"dose\":\"100\","
                + "\"unit\":\"ml\",\"route\":\"IV\",\"frequency\":\"once\","
                + "\"startTime\":\"" + LocalDate.now().minusDays(1).atTime(10, 0) + "\"}";

        var res = restTemplate.exchange("/api/clinical-days/{id}/orders", HttpMethod.POST,
                authEntity(body, getDoctorToken()), String.class, NURSE_SIGNED_DAY_MELNYK);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void doubleNurseSign_throws422() {
        var res = restTemplate.exchange("/api/clinical-days/{id}/sign/nurse", HttpMethod.POST,
                authEntity(new SignRequest(13L, "pin"), getNurseToken()), Void.class,
                NURSE_SIGNED_DAY_MELNYK);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void doctorSignedDay_blocksNoteCreate_422() {
        // Notes stay writable on NURSE_SIGNED days by design (doctors document
        // before their sign-off); the hard boundary is DOCTOR_SIGNED.
        restTemplate.exchange("/api/clinical-days/{id}/sign/nurse", HttpMethod.POST,
                authEntity(new SignRequest(13L, "pin"), getNurseToken()), Void.class,
                OPEN_DAY_SIGN_FLOW);
        restTemplate.exchange("/api/clinical-days/{id}/sign/doctor", HttpMethod.POST,
                authEntity(new SignRequest(15L, "pin"), getHodToken()), Void.class,
                OPEN_DAY_SIGN_FLOW);

        var res = restTemplate.exchange("/api/clinical-days/{id}/notes", HttpMethod.POST,
                authEntity("{\"noteType\":\"exam\",\"text\":\"pin\"}", getDoctorToken()),
                String.class, OPEN_DAY_SIGN_FLOW);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void doctorSign_beforeNurse_throws422() {
        var res = restTemplate.exchange("/api/clinical-days/{id}/sign/doctor", HttpMethod.POST,
                authEntity(new SignRequest(15L, "pin"), getHodToken()), Void.class,
                OPEN_DAY_HOURLY_WRITES);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
    }

    @Test
    void reopen_restoresWritability() {
        var reopen = restTemplate.exchange("/api/clinical-days/{id}/reopen", HttpMethod.POST,
                authEntity(new ReopenRequest("pin", 0), getHodToken()), Void.class,
                NURSE_SIGNED_DAY_BOYKO);
        assertThat(reopen.getStatusCode().is2xxSuccessful()).isTrue();

        var write = restTemplate.exchange("/api/clinical-days/{id}/hourly-records",
                HttpMethod.POST,
                authEntity(hourlyAt(LocalDate.now().minusDays(1).atTime(13, 0)), getNurseToken()),
                String.class, NURSE_SIGNED_DAY_BOYKO);

        assertThat(write.getStatusCode()).isEqualTo(HttpStatus.CREATED);
    }

    // ---- Optimistic concurrency ----

    @Test
    void staleVersion_episodePatch_returns409() {
        var res = restTemplate.exchange("/api/episodes/{id}", HttpMethod.PATCH,
                authEntity("{\"version\":999,\"ward\":\"A\"}", getDoctorToken()),
                String.class, EPISODE_ID);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.CONFLICT);
    }

    // ---- Clinical validation ranges & duplicates ----

    @Test
    void hourly_temperatureOutOfRange_returns400() {
        String body = "{\"recordTime\":\"" + LocalDate.now().atTime(9, 0)
                + "\",\"temperature\":43.5,\"heartRate\":80}";

        var res = restTemplate.exchange("/api/clinical-days/{id}/hourly-records",
                HttpMethod.POST, authEntity(body, getNurseToken()), String.class,
                OPEN_DAY_HOURLY_WRITES);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.BAD_REQUEST);
    }

    @Test
    void duplicateRecordHour_returnsDuplicateError() {
        String body = "{\"recordTime\":\"" + LocalDate.now().atTime(9, 30)
                + "\",\"temperature\":36.6}";
        var first = restTemplate.exchange("/api/clinical-days/{id}/hourly-records",
                HttpMethod.POST, authEntity(body, getNurseToken()), String.class,
                OPEN_DAY_HOURLY_WRITES);
        var second = restTemplate.exchange("/api/clinical-days/{id}/hourly-records",
                HttpMethod.POST, authEntity(body, getNurseToken()), String.class,
                OPEN_DAY_HOURLY_WRITES);

        assertThat(first.getStatusCode()).isEqualTo(HttpStatus.CREATED);
        // DuplicateHourlyRecordException (extends BusinessException) carries the
        // DUPLICATE_HOURLY_RECORD code; the advice layer surfaces it as 422.
        assertThat(second.getStatusCode()).isEqualTo(HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(second.getBody()).contains("DUPLICATE_HOURLY_RECORD");
    }

    // ---- Login abuse characterization ----

    @Test
    void loginCharacterization_twentyFiveUnknownLogins_all401_andAudited() {
        for (int i = 1; i <= 25; i++) {
            var res = restTemplate.postForEntity("/api/auth/login",
                    new com.superhumans.dto.LoginRequest("sec-probe-" + i, "wrong"), String.class);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        String auditor = loginAs("auditor1", "doctor123");
        var audit = restTemplate.exchange("/api/audit?action=LOGIN_FAILED", HttpMethod.GET,
                authGet(auditor), String.class);

        assertThat(audit.getStatusCode()).isEqualTo(HttpStatus.OK);
        // Audit rows record the action + IP, not the attempted login string.
        // Exactly the probes from this class fail login: 25 here, plus up to 6
        // from rateLimiter_locksAfterFiveFailures_sameLogin if it ran first.
        assertThat(audit.getBody()).contains("\"action\":\"LOGIN_FAILED\"");
        assertThat(audit.getBody()).containsPattern("\"totalElements\":(2[5-9]|3[01])");
    }

    @Test
    void rateLimiter_locksAfterFiveFailures_sameLogin() {
        String probe = "sec-probe-lock";
        for (int i = 0; i < 5; i++) {
            var res = restTemplate.postForEntity("/api/auth/login",
                    new com.superhumans.dto.LoginRequest(probe, "wrong"), String.class);
            assertThat(res.getStatusCode()).isEqualTo(HttpStatus.UNAUTHORIZED);
        }

        var sixth = restTemplate.postForEntity("/api/auth/login",
                new com.superhumans.dto.LoginRequest(probe, "wrong"), String.class);

        assertThat(sixth.getStatusCode()).isEqualTo(HttpStatus.TOO_MANY_REQUESTS);
    }

    // ---- PDF response hardening headers ----

    @Test
    void pdfResponse_setsNoStoreAndNosniffHeaders() {
        restTemplate.exchange("/api/clinical-days/{id}/sign/nurse", HttpMethod.POST,
                authEntity(new SignRequest(13L, "pin"), getNurseToken()), Void.class,
                OPEN_DAY_PDF_FLOW);
        restTemplate.exchange("/api/clinical-days/{id}/sign/doctor", HttpMethod.POST,
                authEntity(new SignRequest(15L, "pin"), getHodToken()), Void.class,
                OPEN_DAY_PDF_FLOW);
        var generated = restTemplate.exchange("/api/clinical-days/{id}/pdf", HttpMethod.POST,
                authGet(getDoctorToken()), Void.class, OPEN_DAY_PDF_FLOW);
        assertThat(generated.getStatusCode()).isEqualTo(HttpStatus.CREATED);

        HttpHeaders headers = new HttpHeaders();
        headers.set("Authorization", "Bearer " + loginAs("doctor1", "doctor123"));
        HttpEntity<Void> entity = new HttpEntity<>(headers);
        var res = restTemplate.exchange("/api/clinical-days/{id}/pdf", HttpMethod.GET,
                entity, String.class, OPEN_DAY_PDF_FLOW);

        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        assertThat(res.getHeaders().getCacheControl()).contains("no-store");
        assertThat(res.getHeaders().getFirst("X-Content-Type-Options")).isEqualTo("nosniff");
    }

    // ---- helpers ----

    private static String hourlyAt(LocalDateTime time) {
        return "{\"recordTime\":\"" + time + "\",\"temperature\":36.6,\"heartRate\":80}";
    }
}
