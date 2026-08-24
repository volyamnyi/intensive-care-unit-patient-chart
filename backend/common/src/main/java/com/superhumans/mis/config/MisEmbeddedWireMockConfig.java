package com.superhumans.mis.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Embedded WireMock server for local dev and CI E2E. Serves the production
 * fixture files ({@code mis-wiremock/mappings} + {@code __files}) from the
 * application classpath so a single JVM process provides both the backend
 * and the MIS data source — no external WireMock process needed.
 *
 * <p>Active when {@code app.mis.embedded-wiremock-enabled=true}. In
 * production, leave this off and point {@code app.mis.wiremock-url} at an
 * external WireMock/MIS endpoint instead.
 */
@Slf4j
@Configuration
@ConditionalOnProperty(name = "app.mis.embedded-wiremock-enabled", havingValue = "true")
public class MisEmbeddedWireMockConfig {

    @Bean(destroyMethod = "stop")
    public WireMockServer embeddedWireMockServer(
            @Value("${app.mis.wiremock-port:9090}") int port) {
        var server = new WireMockServer(WireMockConfiguration.options()
                .port(port)
                .usingFilesUnderClasspath("mis-wiremock"));
        server.start();
        log.info("Embedded WireMock server started on port {} serving classpath mis-wiremock/", port);
        return server;
    }
}
