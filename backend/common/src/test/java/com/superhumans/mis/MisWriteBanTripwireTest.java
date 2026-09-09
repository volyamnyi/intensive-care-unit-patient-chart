package com.superhumans.mis;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Static tripwire guarding the Phase 16 MIS Data Policy (absolute read-only,
 * no exceptions): no production source may reference {@code sendPdf} — PDF
 * transfer to MIS is banned, PDFs stay local for download/print in-module.
 * Reintroducing the call breaks the build here, before any test boots Spring.
 */
class MisWriteBanTripwireTest {

    @Test
    void mainSources_neverReferenceSendPdf() throws Exception {
        Path base = Path.of("").toAbsolutePath();
        Path main = Files.isDirectory(base.resolve("src/main/java"))
                ? base.resolve("src/main/java")
                : base.resolve("backend/common/src/main/java");
        assertThat(main).describedAs("expected production sources at " + main).exists();

        List<String> offenders;
        try (Stream<Path> walk = Files.walk(main)) {
            offenders = walk.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> {
                        try {
                            return Files.readString(p, StandardCharsets.UTF_8).contains("sendPdf");
                        } catch (Exception e) {
                            return false;
                        }
                    })
                    .map(p -> base.relativize(p).toString())
                    .sorted()
                    .toList();
        }
        assertThat(offenders)
                .describedAs("MIS PDF transfer is banned — sendPdf must not exist in src/main")
                .isEmpty();
    }
}
