package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

    private CorsConfig corsConfig;

    @BeforeEach
    void setUp() {
        corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins",
                "https://supercare.superhumans.com,http://localhost:5173");
    }

    @Test
    void corsConfigurationSource_usesConfiguredAllowlistWithCredentials() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/api/test"));

        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getAllowedOrigins()).isNull();
        assertThat(config.getAllowedOriginPatterns())
                .containsExactly("https://supercare.superhumans.com", "http://localhost:5173");
        assertThat(config.getAllowedMethods())
                .contains("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        assertThat(config.getAllowedHeaders()).containsExactly("*");
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    void corsConfiguration_allowsListedOriginsAndRejectsOthers() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/api/test"));

        assertThat(config.checkOrigin("https://supercare.superhumans.com"))
                .isEqualTo("https://supercare.superhumans.com");
        assertThat(config.checkOrigin("http://localhost:5173"))
                .isEqualTo("http://localhost:5173");
        assertThat(config.checkOrigin("https://evil.example.com")).isNull();
    }

    @Test
    void corsConfiguration_trimsWhitespaceAndDropsBlanks() {
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins",
                " https://supercare.superhumans.com ,, http://localhost:5173 ");

        CorsConfiguration config = corsConfig.corsConfigurationSource().getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/api/test"));

        assertThat(config.getAllowedOriginPatterns())
                .containsExactly("https://supercare.superhumans.com", "http://localhost:5173");
    }

    @Test
    void corsFilter_isBoundToConfiguredSource() {
        assertThat(corsConfig.corsFilter()).isNotNull();
    }
}
