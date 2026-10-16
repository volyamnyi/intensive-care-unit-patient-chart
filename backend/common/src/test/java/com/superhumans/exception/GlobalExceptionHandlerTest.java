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
    void illegalArgument_usesFixedTemplateWithoutEcho() {
        var handler = new GlobalExceptionHandler();

        var res = handler.handleIllegalArgument(
                new IllegalArgumentException("Invalid UUID string: " + "x".repeat(100)));

        assertThat(res.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(res.getBody().getMessage()).isEqualTo("Invalid request value.");
    }

    @Test
    void missingParameter_namesParameterWithoutEcho() {
        var handler = new GlobalExceptionHandler();

        var res = handler.handleMissingParameter(
                new org.springframework.web.bind.MissingServletRequestParameterException(
                        "dayId", "UUID"));

        assertThat(res.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.BAD_REQUEST);
        assertThat(res.getBody().getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
        assertThat(res.getBody().getMessage()).contains("dayId");
    }

    @Test
    void methodNotSupported_mapsTo405() {
        var handler = new GlobalExceptionHandler();

        var res = handler.handleMethodNotSupported(
                new org.springframework.web.HttpRequestMethodNotSupportedException("DELETE"));

        assertThat(res.getStatusCode())
                .isEqualTo(org.springframework.http.HttpStatus.METHOD_NOT_ALLOWED);
        assertThat(res.getBody().getCode()).isEqualTo(ErrorCode.VALIDATION_ERROR);
    }

    @Test
    void noResourceFound_mapsTo404WithoutPathEcho() {
        var handler = new GlobalExceptionHandler();

        var res = handler.handleNoResourceFound(
                new org.springframework.web.servlet.resource.NoResourceFoundException(
                        org.springframework.http.HttpMethod.GET, "/secret-path", "nope"));

        assertThat(res.getStatusCode()).isEqualTo(org.springframework.http.HttpStatus.NOT_FOUND);
        assertThat(res.getBody().getCode()).isEqualTo(ErrorCode.NOT_FOUND);
        assertThat(res.getBody().getMessage()).doesNotContain("secret-path");
    }

    @Test
    void ioException_mapsTo500WithoutEcho() {
        var handler = new GlobalExceptionHandler();

        var res = handler.handleIOException(new java.io.IOException("disk gone"));

        assertThat(res.getStatusCode())
                .isEqualTo(org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR);
        assertThat(res.getBody().getCode()).isEqualTo(ErrorCode.INTERNAL_ERROR);
        assertThat(res.getBody().getMessage()).doesNotContain("disk gone");
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
