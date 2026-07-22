package com.superhumans.mis.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.mis.wiremock-enabled", havingValue = "true", matchIfMissing = true)
public class WireMockServerConfig {

    @Getter
    private WireMockServer wireMockServer;

    @Value("${app.mis.wiremock-port:9090}")
    private int port;

    @Value("${app.mis.wiremock-mappings:classpath:mis-wiremock/mappings}")
    private String mappingsPath;

    @PostConstruct
    public void start() {
        log.info("Starting embedded WireMock server on port {}", port);
        wireMockServer = new WireMockServer(
                WireMockConfiguration.options()
                        .port(port)
                        .usingFilesUnderDirectory("src/main/resources/mis-wiremock")
        );
        wireMockServer.start();
        log.info("WireMock server started on port {}", port);
    }

    @PreDestroy
    public void stop() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
            log.info("WireMock server stopped");
        }
    }
}
