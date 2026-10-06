package com.superhumans.config;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;
import org.springframework.web.filter.CorsFilter;

import java.util.Arrays;
import java.util.List;

@Configuration
@FieldDefaults(level = AccessLevel.PRIVATE)
public class CorsConfig {

    /**
     * Exact allowlist for credentialed CORS, comma-separated.
     * Same-origin deployments (nginx serving {@code dist/} + proxying {@code /api})
     * never trigger CORS; this list is defence-in-depth only. Production value comes
     * from {@code APP_CORS_ALLOWED_ORIGINS} (e.g. {@code https://supercare.superhumans.com}).
     * A wildcard must never return here: with {@code allowCredentials=true} browsers
     * would accept any origin carrying the {@code jwt} cookie.
     */
    @Value("${app.cors.allowed-origins:http://localhost:5173,http://localhost:3000}")
    String allowedOrigins;

    @Bean
    public CorsConfigurationSource corsConfigurationSource() {
        var config = new CorsConfiguration();
        List<String> origins = Arrays.stream(allowedOrigins.split(","))
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .toList();
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }

    @Bean
    public CorsFilter corsFilter() {
        return new CorsFilter(corsConfigurationSource());
    }
}