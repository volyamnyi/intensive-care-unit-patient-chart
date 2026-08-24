package com.superhumans.mis.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Mutual-exclusion fail-fast validation for MIS implementations (#193).
 * Each scenario boots a minimal context with only the relevant properties
 * and asserts the outcome: exactly one implementation bean, or a clear
 * startup failure when both/neither are enabled.
 */
class MisMutualExclusionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MisServiceConfig.class);

    @Test
    void mockOnly_bootSuccessfully() {
        runner.withPropertyValues("app.mis.mock-enabled=true", "app.mis.wiremock-enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(RestTemplate.class);
                    assertThat(context.getStartupFailure()).isNull();
                });
    }

    @Test
    void wiremockOnly_bootsSuccessfully() {
        runner.withPropertyValues(
                        "app.mis.mock-enabled=false",
                        "app.mis.wiremock-enabled=true",
                        "app.mis.embedded-wiremock-enabled=false")
                .run(context -> {
                    assertThat(context).hasSingleBean(RestTemplate.class);
                    assertThat(context.getStartupFailure()).isNull();
                });
    }

    @Test
    void bothTrue_failsWithClearMessage() {
        runner.withPropertyValues("app.mis.mock-enabled=true", "app.mis.wiremock-enabled=true")
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat((Exception) context.getStartupFailure())
                            .hasStackTraceContaining("обидві реалізації MIS увімкнені");
                });
    }

    @Test
    void bothFalse_failsWithClearMessage() {
        runner.withPropertyValues("app.mis.mock-enabled=false", "app.mis.wiremock-enabled=false")
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat((Exception) context.getStartupFailure())
                            .hasStackTraceContaining("Жодна реалізація MIS не увімкнена");
                });
    }

    @Test
    void embeddedWireMock_withoutWireMock_fails() {
        runner.withPropertyValues(
                        "app.mis.mock-enabled=true",
                        "app.mis.wiremock-enabled=false",
                        "app.mis.embedded-wiremock-enabled=true")
                .run(context -> {
                    assertThat(context.getStartupFailure()).isNotNull();
                    assertThat((Exception) context.getStartupFailure())
                            .hasStackTraceContaining("embedded-wiremock-enabled");
                });
    }
}
