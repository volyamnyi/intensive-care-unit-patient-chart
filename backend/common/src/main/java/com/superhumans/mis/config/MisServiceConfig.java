package com.superhumans.mis.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * MIS implementation wiring: WireMock is the single active implementation.
 * <p>
 * Validates that {@code app.mis.wiremock-enabled} is active at startup
 * and that embedded WireMock is only enabled alongside the WireMock
 * implementation.
 */
@Slf4j
@Configuration
public class MisServiceConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public MisWireMockEnabledGuard misWireMockEnabledGuard(
            @Value("${app.mis.wiremock-enabled:true}") boolean wiremockEnabled,
            @Value("${app.mis.embedded-wiremock-enabled:false}") boolean embeddedWiremockEnabled) {
        return new MisWireMockEnabledGuard(wiremockEnabled, embeddedWiremockEnabled);
    }

    /** Fail-fast guard: refuses startup when MIS is not properly configured. */
    @Slf4j
    public static class MisWireMockEnabledGuard implements InitializingBean {

        private final boolean wiremockEnabled;
        private final boolean embeddedWiremockEnabled;

        MisWireMockEnabledGuard(boolean wiremockEnabled, boolean embeddedWiremockEnabled) {
            this.wiremockEnabled = wiremockEnabled;
            this.embeddedWiremockEnabled = embeddedWiremockEnabled;
        }

        @Override
        public void afterPropertiesSet() {
            if (!wiremockEnabled) {
                throw new IllegalStateException(
                        "MIS не увімкнена (app.mis.wiremock-enabled=false). "
                                + "WireMock — єдина реалізація, увімкніть її.");
            }
            if (embeddedWiremockEnabled) {
                log.info("MIS implementation: WireMock (embedded server)");
            } else {
                log.info("MIS implementation: WireMock (external server)");
            }
        }
    }
}
