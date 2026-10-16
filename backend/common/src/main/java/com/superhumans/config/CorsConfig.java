package com.superhumans.config;

import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.cors.CorsConfiguration;
import org.springframework.web.cors.CorsConfigurationSource;
import org.springframework.web.cors.UrlBasedCorsConfigurationSource;

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
        // Startup guard (#370 item 5): with allowCredentials=true a "*" or
        // suffix-pattern origin would silently turn the API into
        // any-origin-credentialed. Fail the boot instead of serving it.
        for (String origin : origins) {
            if (origin.contains("*")) {
                throw new IllegalStateException(
                        "CORS misconfiguration: origin pattern '" + origin
                                + "' must never combine with allowCredentials=true. "
                                + "Set APP_CORS_ALLOWED_ORIGINS to an exact allowlist.");
            }
        }
        config.setAllowedOriginPatterns(origins);
        config.setAllowedMethods(List.of("GET", "POST", "PUT", "DELETE", "PATCH", "OPTIONS"));
        config.setAllowedHeaders(List.of("*"));
        config.setAllowCredentials(true);
        config.setMaxAge(3600L);

        var source = new UrlBasedCorsConfigurationSource();
        source.registerCorsConfiguration("/**", config);
        return source;
    }
}