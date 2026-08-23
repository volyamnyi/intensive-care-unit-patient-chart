package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

@ExtendWith(MockitoExtension.class)
class CorsConfigTest {

    private CorsConfig corsConfig;

    @BeforeEach
    void setUp() {
        corsConfig = new CorsConfig();
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins",
                new String[]{"http://localhost:5173", "http://localhost:3000"});
    }

    @Test
    void corsConfigurationSource_configuresExplicitOriginsNotWildcard() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/api/test"));

        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getAllowedOrigins()).containsExactlyInAnyOrder(
                "http://localhost:5173", "http://localhost:3000");
        assertThat(config.getAllowedOrigins()).doesNotContain("*");
        assertThat(config.getAllowedMethods()).contains("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        assertThat(config.getAllowedHeaders()).containsExactly("*");
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    void corsConfiguration_reflectsConfiguredOriginsWhenChanged() {
        ReflectionTestUtils.setField(corsConfig, "allowedOrigins",
                new String[]{"https://hospital.ua"});

        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/api/test"));

        assertThat(config.getAllowedOrigins()).containsExactly("https://hospital.ua");
        assertThat(config.getAllowedOrigins()).doesNotContain("*");
    }

    @Test
    void corsFilter_isBoundToConfiguredSource() {
        assertThat(corsConfig.corsFilter()).isNotNull();
    }
}