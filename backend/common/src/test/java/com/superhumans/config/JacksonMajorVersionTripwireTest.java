package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Jackson discipline (#430 item 3).
 *
 * <p>The serialization stack is Jackson 3 ({@code tools.jackson}, Boot 4.1
 * default). Jackson 3 still depends on the Jackson 2 <em>annotations</em>
 * artifact, so {@code com.fasterxml.jackson.annotation} is legitimate. Jackson
 * 2 <em>databind/core/datatype</em> must not appear in {@code src/main}: it is
 * only present transitively through springdoc, and a feature importing it would
 * silently bind a second, incompatible mapper.
 */
class JacksonMajorVersionTripwireTest {

    private static final Pattern BANNED = Pattern.compile(
            "com\\.fasterxml\\.jackson\\.(databind|core|datatype|dataformat)\\b");

    private static Path backendRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path root = current;
        while (root != null && !Files.exists(root.resolve("common/pom.xml"))) {
            root = root.getParent();
        }
        return root == null ? current : root;
    }

    @Test
    void mainSourcesNeverImportJackson2Databind() throws IOException {
        Path backend = backendRoot();
        List<String> offenders = new ArrayList<>();
        try (Stream<Path> modules = Files.list(backend)) {
            for (Path module : modules.filter(p -> Files.isDirectory(p.resolve("src/main/java")))
                    .toList()) {
                try (Stream<Path> files = Files.walk(module.resolve("src/main/java"))) {
                    for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                        Matcher matcher = BANNED.matcher(read(file));
                        if (matcher.find()) {
                            offenders.add(backend.relativize(file) + " imports " + matcher.group());
                        }
                    }
                }
            }
        }
        assertThat(offenders)
                .describedAs("Jackson 2 databind in production sources (use tools.jackson)")
                .isEmpty();
    }

    @Test
    void theOnlyJackson2PackageInUseIsTheSharedAnnotationsArtifact() throws IOException {
        Path backend = backendRoot();
        List<String> annotationsUsers = new ArrayList<>();
        try (Stream<Path> modules = Files.list(backend)) {
            for (Path module : modules.filter(p -> Files.isDirectory(p.resolve("src/main/java")))
                    .toList()) {
                try (Stream<Path> files = Files.walk(module.resolve("src/main/java"))) {
                    for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                        if (read(file).contains("com.fasterxml.jackson.annotation")) {
                            annotationsUsers.add(backend.relativize(file).toString());
                        }
                    }
                }
            }
        }
        // Not an empty-set assertion on purpose: it documents that the v2 surface
        // is exactly the annotations package and nothing else.
        assertThat(annotationsUsers).allSatisfy(path ->
                assertThat(path).doesNotContain("databind"));
    }

    private static Matcher matcher(String text) {
        return BANNED.matcher(text);
    }

    private static String read(Path file) {
        try {
            return Files.readString(file, StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new UncheckedIOException(file.toString(), e);
        }
    }
}