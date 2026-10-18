package com.superhumans.support;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import org.springframework.core.io.ClassPathResource;

/**
 * Shared text reader for SQL seed/resources in tests (#428 item 10).
 *
 * <p>Seed files are committed with LF, but Windows checkouts materialize CRLF
 * ({@code core.autocrlf}), which broke the multi-line assertions of
 * {@code TpLl02SeedValidationTest} on 2026-09-17. Normalizing on read keeps
 * those assertions OS-independent; the seed files themselves must never be
 * reformatted (their bytes are Liquibase checksum inputs).
 */
public final class SeedText {

    private SeedText() {
    }

    /** Reads a classpath resource as UTF-8 with CRLF normalized to LF. */
    public static String sqlText(String resource) throws IOException {
        try (InputStream stream = new ClassPathResource(resource).getInputStream()) {
            return normalize(new String(stream.readAllBytes(), StandardCharsets.UTF_8));
        }
    }

    public static String normalize(String raw) {
        return raw.replace("\r\n", "\n").replace('\r', '\n');
    }
}
