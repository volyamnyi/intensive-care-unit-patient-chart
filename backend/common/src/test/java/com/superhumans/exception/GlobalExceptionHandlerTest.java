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
}
