package com.superhumans.mis.config;

import com.github.tomakehurst.wiremock.WireMockServer;
import com.github.tomakehurst.wiremock.core.WireMockConfiguration;
import jakarta.annotation.PostConstruct;
import jakarta.annotation.PreDestroy;
import java.io.IOException;
import java.io.InputStream;
import java.net.JarURLConnection;
import java.net.URISyntaxException;
import java.net.URL;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Enumeration;
import java.util.jar.JarEntry;
import java.util.jar.JarFile;
import lombok.AccessLevel;
import lombok.Getter;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnProperty(name = "app.mis.wiremock-enabled", havingValue = "true", matchIfMissing = true)
@FieldDefaults(level = AccessLevel.PRIVATE)
public class WireMockServerConfig {

    @Getter
    private WireMockServer wireMockServer;

    @Value("${app.mis.wiremock-port:9090}")
    private int port;

    @PostConstruct
    public void start() {
        log.info("Starting embedded WireMock server on port {}", port);
        Path stubDir = resolveStubDirectory();
        wireMockServer = new WireMockServer(
                WireMockConfiguration.options()
                        .port(port)
                        .usingFilesUnderDirectory(stubDir.toString())
        );
        wireMockServer.start();
        log.info("WireMock server started on port {} (stubs from {})", port, stubDir);
    }

    @PreDestroy
    public void stop() {
        if (wireMockServer != null && wireMockServer.isRunning()) {
            wireMockServer.stop();
            log.info("WireMock server stopped");
        }
    }

    /**
     * Resolves the {@code mis-wiremock} stub directory (mappings + __files) from the classpath.
     * Works for exploded classpath layouts (dev, tests) and fat JARs on Windows and Linux;
     * WireMock's own {@code usingFilesUnderClasspath} builds nested: URLs that break on Windows
     * drive letters, so stubs are materialized to a real directory instead.
     */
    private Path resolveStubDirectory() {
        try {
            URL resource = getClass().getClassLoader().getResource("mis-wiremock");
            if (resource == null) {
                throw new IllegalStateException("mis-wiremock resources not found on classpath");
            }
            if ("file".equals(resource.getProtocol())) {
                return Paths.get(resource.toURI());
            }
            if ("jar".equals(resource.getProtocol())) {
                return extractStubsToTempDir(resource);
            }
            throw new IllegalStateException("Unsupported resource protocol: " + resource.getProtocol());
        } catch (IOException | URISyntaxException e) {
            throw new IllegalStateException("Failed to resolve mis-wiremock stub directory", e);
        }
    }

    private Path extractStubsToTempDir(URL resource) throws IOException {
        Path tempDir = Files.createTempDirectory("mis-wiremock");
        JarURLConnection connection = (JarURLConnection) resource.openConnection();
        try (JarFile jarFile = connection.getJarFile()) {
            Enumeration<JarEntry> entries = jarFile.entries();
            while (entries.hasMoreElements()) {
                JarEntry entry = entries.nextElement();
                if (!entry.getName().startsWith("mis-wiremock/") || entry.isDirectory()) {
                    continue;
                }
                String relativeName = entry.getName().substring("mis-wiremock/".length());
                Path target = tempDir.resolve(relativeName);
                Files.createDirectories(target.getParent());
                try (InputStream in = jarFile.getInputStream(entry)) {
                    Files.copy(in, target, StandardCopyOption.REPLACE_EXISTING);
                }
            }
        }
        log.info("Extracted WireMock stubs from JAR to {}", tempDir);
        return tempDir;
    }
}
