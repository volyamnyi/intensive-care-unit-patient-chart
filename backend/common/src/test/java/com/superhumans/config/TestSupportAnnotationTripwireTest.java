package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Static tripwire for the test-support annotation (#371 item 5):
 * {@code EnableTestExceptionHandler} exists only to import the production
 * advice into MVC slices, so no production source except the annotation
 * itself may reference it. IO failures fail loudly (never vacuous-pass).
 */
class TestSupportAnnotationTripwireTest {

    @Test
    void mainSources_neverReferenceTestSupportAnnotation() throws Exception {
        Path base = Path.of("").toAbsolutePath();
        Path main = Files.isDirectory(base.resolve("src/main/java"))
                ? base.resolve("src/main/java")
                : base.resolve("backend/common/src/main/java");
        assertThat(main).describedAs("expected production sources at " + main).exists();

        List<String> offenders;
        try (Stream<Path> walk = Files.walk(main)) {
            offenders = walk.filter(p -> p.toString().endsWith(".java"))
                    .filter(p -> !p.getFileName().toString().equals("EnableTestExceptionHandler.java"))
                    .filter(p -> read(p).contains("EnableTestExceptionHandler"))
                    .map(p -> base.relativize(p).toString())
                    .sorted()
                    .toList();
        }
        assertThat(offenders)
                .describedAs("test-support annotation must not leak into src/main")
                .isEmpty();
    }

    private static String read(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(p.toString(), e);
        }
    }
}
