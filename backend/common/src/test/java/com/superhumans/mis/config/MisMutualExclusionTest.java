package com.superhumans.mis.config;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;
import org.springframework.web.client.RestTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Fail-fast validation for MIS configuration (#193/#194).
 * WireMock must be enabled; embedded mode requires wiremock.
 */
class MisMutualExclusionTest {

    private final ApplicationContextRunner runner = new ApplicationContextRunner()
            .withUserConfiguration(MisServiceConfig.class);

    @Test
    void wiremockEnabled_bootsSuccessfully() {
        runner.withPropertyValues("app.mis.wiremock-enabled=true")
                .run(context -> {
                    assertThat(context).hasSingleBean(RestTemplate.class);
                    assertThat(context.getStartupFailure()).isNull();
                });
    }

    @Test
    void wiremockDisabled_contextFails() {
        runner.withPropertyValues("app.mis.wiremock-enabled=false")
                .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }

    @Test
    void embeddedWithoutWiremock_contextFails() {
        runner.withPropertyValues(
                        "app.mis.wiremock-enabled=false",
                        "app.mis.embedded-wiremock-enabled=true")
                .run(context -> assertThat(context.getStartupFailure()).isNotNull());
    }
}
