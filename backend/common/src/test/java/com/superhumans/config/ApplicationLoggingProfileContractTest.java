package com.superhumans.config;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Static contract for the logging profile structure (CR-08 #422): the BASE
 * document must stay lean (INFO/WARN) because a mis-deployed environment —
 * one missing {@code SPRING_PROFILES_ACTIVE=prod} — boots exactly that base
 * document. Verbose diagnostics (TRACE/DEBUG incl. SQL parameter binds) may
 * live only behind the explicit {@code dev} profile, and prod pins ANSI off.
 */
class ApplicationLoggingProfileContractTest {

    private static final Pattern TRACE_OR_DEBUG_LEVEL =
            Pattern.compile("(?m)^\\s*[\\w.]+:\\s*(TRACE|DEBUG)\\s*$");

    private static String readApplicationYml() throws IOException {
        try (InputStream in = ApplicationLoggingProfileContractTest.class
                .getResourceAsStream("/application.yml")) {
            assertThat(in).as("application.yml on the classpath").isNotNull();
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    @Test
    void baseDocument_carriesNoTraceOrDebugLevels() throws IOException {
        String base = documents()[0];

        assertThat(TRACE_OR_DEBUG_LEVEL.matcher(base).find())
                .as("the base document is what a mis-deployed environment boots; "
                        + "verbose levels belong to the dev profile")
                .isFalse();
        assertThat(base).contains("root: INFO");
        assertThat(base).contains("com.superhumans: INFO");
    }

    @Test
    void devProfile_carriesTheVerboseDiagnosticsBlock() throws IOException {
        String dev = documents()[1];

        assertThat(dev).contains("on-profile: dev");
        assertThat(dev).contains("root: DEBUG");
        assertThat(dev).contains("com.superhumans: TRACE");
        assertThat(dev).contains("org.hibernate.orm.jdbc.bind: TRACE");
        assertThat(dev).contains("liquibase.executor: TRACE");
    }

    @Test
    void prodProfile_isSilent_andPinsAnsiOff() throws IOException {
        String prod = documents()[2];

        assertThat(prod).contains("on-profile: prod");
        assertThat(TRACE_OR_DEBUG_LEVEL.matcher(prod).find())
                .as("prod overrides may only silence, never enable verbose levels")
                .isFalse();
        assertThat(prod).contains("org.hibernate.SQL: WARN");
        assertThat(prod).contains("enabled: never");
    }

    private String[] documents() throws IOException {
        String[] documents = readApplicationYml().split("(?m)^---\\s*$");
        assertThat(documents).hasSize(3);
        return documents;
    }
}
