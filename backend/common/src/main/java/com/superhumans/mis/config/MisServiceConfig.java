package com.superhumans.mis.config;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.ApplicationRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.client.RestTemplate;

/**
 * MIS implementation wiring: mutual-exclusion fail-fast validation.
 * <p>
 * Ensures that exactly one of {@code app.mis.mock-enabled} or
 * {@code app.mis.wiremock-enabled} is active at startup. Both {@code true}
 * would produce an ambiguous {@code MisService} bean; both {@code false}
 * leaves the application without patient/user data. Also validates that
 * embedded WireMock is only enabled alongside the WireMock implementation.
 */
@Slf4j
@Configuration
public class MisServiceConfig {

    @Bean
    public RestTemplate restTemplate() {
        return new RestTemplate();
    }

    @Bean
    public ApplicationRunner misMutualExclusionValidator(
            @Value("${app.mis.mock-enabled:false}") boolean mockEnabled,
            @Value("${app.mis.wiremock-enabled:true}") boolean wiremockEnabled,
            @Value("${app.mis.embedded-wiremock-enabled:false}") boolean embeddedWiremockEnabled) {
        return args -> {
            if (mockEnabled && wiremockEnabled) {
                throw new IllegalStateException(
                        "Обидві реалізації MIS увімкнені одночасно (app.mis.mock-enabled=true "
                                + "та app.mis.wiremock-enabled=true). Оберіть одну.");
            }
            if (!mockEnabled && !wiremockEnabled) {
                throw new IllegalStateException(
                        "Жодна реалізація MIS не увімкнена (app.mis.mock-enabled=false "
                                + "та app.mis.wiremock-enabled=false). Увімкніть одну.");
            }
            if (embeddedWiremockEnabled && !wiremockEnabled) {
                throw new IllegalStateException(
                        "app.mis.embedded-wiremock-enabled=true вимагає "
                                + "app.mis.wiremock-enabled=true.");
            }
            log.info("MIS implementation: {}{}",
                    mockEnabled ? "Mock" : "WireMock",
                    embeddedWiremockEnabled ? " (embedded server)" : "");
        };
    }
}
