package com.superhumans.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class GlobalExceptionHandlerTest {

    @Test
    void sanitizeRoute_redactsUuidAndNumericSegments() {
        assertThat(GlobalExceptionHandler.sanitizeRoute(
                "/api/audit/events/11111111-1111-1111-1111-111111111111"))
                .isEqualTo("/api/audit/events/{id}");
        assertThat(GlobalExceptionHandler.sanitizeRoute("/api/admin/users/16"))
                .isEqualTo("/api/admin/users/{id}");
        assertThat(GlobalExceptionHandler.sanitizeRoute("/api/audit/events"))
                .isEqualTo("/api/audit/events");
    }

    @Test
    void sanitizeRoute_nullOrBlank_returnsNull() {
        assertThat(GlobalExceptionHandler.sanitizeRoute(null)).isNull();
        assertThat(GlobalExceptionHandler.sanitizeRoute("  ")).isNull();
    }

    @Test
    void misTimeout_mapsTo503WithDetailFreeMessage() {
        var handler = new GlobalExceptionHandler();

        var res = handler.handleMisTimeout(
                new com.superhumans.mis.MisTimeoutException("spiPatientProsthesCheck timed out"));

        assertThat(res.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.SERVICE_UNAVAILABLE);
        assertThat(res.getBody().getCode()).isEqualTo(ErrorCode.MIS_TIMEOUT);
        assertThat(res.getBody().getMessage()).isEqualTo("MIS is temporarily unavailable.");
        assertThat(res.getBody().getCorrelationId()).isNotNull();
    }

    @Test
    void documentLocked_mapsTo422WithCode() {
        var handler = new GlobalExceptionHandler();

        var res = handler.handleDocumentLocked(
                new com.superhumans.exception.DocumentLockedException("locked"));

        assertThat(res.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.UNPROCESSABLE_CONTENT);
        assertThat(res.getBody().getCode()).isEqualTo(ErrorCode.DOCUMENT_LOCKED);
    }

    @Test
    void misUpstream_mapsTo502WithDetailFreeMessage() {
        var handler = new GlobalExceptionHandler();

        var res = handler.handleMisUpstream(
                new com.superhumans.mis.MisBadResponseException("spiPatientProsthesCheck", 500));

        assertThat(res.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_GATEWAY);
        assertThat(res.getBody().getCode()).isEqualTo(ErrorCode.MIS_UPSTREAM_ERROR);
        assertThat(res.getBody().getMessage()).isEqualTo("MIS returned an error.");
    }
}
