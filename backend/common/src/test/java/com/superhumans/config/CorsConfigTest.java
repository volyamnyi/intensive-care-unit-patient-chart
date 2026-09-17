package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;

class CorsConfigTest {

    private CorsConfig corsConfig;

    @BeforeEach
    void setUp() {
        corsConfig = new CorsConfig();
    }

    @Test
    void corsConfigurationSource_configuresWildcardPatternsWithCredentials() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/api/test"));

        assertThat(config.getAllowCredentials()).isTrue();
        assertThat(config.getAllowedOrigins()).isNull();
        assertThat(config.getAllowedOriginPatterns()).containsExactly("*");
        assertThat(config.getAllowedMethods())
                .contains("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS");
        assertThat(config.getAllowedHeaders()).containsExactly("*");
        assertThat(config.getMaxAge()).isEqualTo(3600L);
    }

    @Test
    void corsConfiguration_echoesArbitraryOriginViaPattern() {
        CorsConfigurationSource source = corsConfig.corsConfigurationSource();
        CorsConfiguration config = source.getCorsConfiguration(
                new MockHttpServletRequest("OPTIONS", "/api/test"));

        assertThat(config.checkOrigin("https://hospital.ua")).isEqualTo("https://hospital.ua");
        assertThat(config.checkOrigin("http://localhost:5173")).isEqualTo("http://localhost:5173");
    }

    @Test
    void corsFilter_isBoundToConfiguredSource() {
        assertThat(corsConfig.corsFilter()).isNotNull();
    }
}
