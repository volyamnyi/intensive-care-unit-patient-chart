package com.superhumans.mis.config;

import java.time.Duration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.web.client.RestTemplate;

/**
 * MIS implementation wiring: WireMock is the active implementation by default
 * ({@code app.mis.mode=wiremock}); {@code app.mis.mode=real} routes the shared
 * client to the real MIS API with Bearer authentication (#255).
 * <p>
 * Validates the mode selection at startup and applies the MIS HTTP timeouts.
 */
@Slf4j
@Configuration
public class MisServiceConfig {

    @Bean
    public RestTemplate restTemplate(
            @Value("${app.mis.api.connect-timeout-ms:5000}") int connectTimeoutMs,
            @Value("${app.mis.api.read-timeout-ms:15000}") int readTimeoutMs) {
        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofMillis(connectTimeoutMs));
        factory.setReadTimeout(Duration.ofMillis(readTimeoutMs));
        return new RestTemplate(factory);
    }

    @Bean
    public MisModeGuard misModeGuard(
            @Value("${app.mis.mode:wiremock}") String mode,
            @Value("${app.mis.wiremock-enabled:true}") boolean wiremockEnabled,
            @Value("${app.mis.embedded-wiremock-enabled:false}") boolean embeddedWiremockEnabled) {
        return new MisModeGuard(mode, wiremockEnabled, embeddedWiremockEnabled);
    }

    /** Fail-fast guard: refuses startup when the MIS mode is not properly configured. */
    @Slf4j
    public static class MisModeGuard implements InitializingBean {

        private final String mode;
        private final boolean wiremockEnabled;
        private final boolean embeddedWiremockEnabled;

        MisModeGuard(String mode, boolean wiremockEnabled, boolean embeddedWiremockEnabled) {
            this.mode = mode;
            this.wiremockEnabled = wiremockEnabled;
            this.embeddedWiremockEnabled = embeddedWiremockEnabled;
        }

        @Override
        public void afterPropertiesSet() {
            if (!"wiremock".equalsIgnoreCase(mode) && !"real".equalsIgnoreCase(mode)) {
                throw new IllegalStateException(
                        "Unknown app.mis.mode=" + mode + ", expected wiremock|real");
            }
            if ("real".equalsIgnoreCase(mode)) {
                if (embeddedWiremockEnabled) {
                    throw new IllegalStateException(
                            "Embedded WireMock cannot be combined with app.mis.mode=real");
                }
                log.info("MIS implementation: Real API (Bearer authentication)");
                return;
            }
            if (!wiremockEnabled) {
                throw new IllegalStateException(
                        "MIS не увімкнена (app.mis.wiremock-enabled=false). "
                                + "WireMock — активна реалізація, увімкніть її.");
            }
            if (embeddedWiremockEnabled) {
                log.info("MIS implementation: WireMock (embedded server)");
            } else {
                log.info("MIS implementation: WireMock (external server)");
            }
        }
    }
}
