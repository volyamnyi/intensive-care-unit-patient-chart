package com.superhumans.mis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Static tripwire (issue #265) guarding the Phase 11 MIS Data Policy: no
 * production source may reference the legacy {@code spzIB*} stored-procedure
 * family (e.g. {@code spzIBMedicineDictionary}, {@code spzIBPatientAllergy}).
 * The real-MIS cutover (#263/#264) replaced every {@code spzIB} call with the
 * three live {@code spi*} read procedures; reintroducing a {@code spzIB} token
 * in production source breaks the build here, before any test boots Spring.
 *
 * <p>Scope: every {@code backend/&lt;module&gt;/src/main} Java source plus
 * {@code frontend/src} TypeScript sources. Historical session-log entries in
 * docs are intentionally out of scope (they are not production code).
 */
class MisSpzIBTripwireTest {

    private static final String[] SOURCE_EXTENSIONS = {".java", ".ts", ".tsx"};

    @Test
    void mainSources_neverReferenceSpzIB() throws IOException {
        Path root = repoRoot();
        List<String> offenders = new java.util.ArrayList<>();
        scanDirectory(root.resolve("backend"), offenders, true);
        scanDirectory(root.resolve("frontend"), offenders, true);

        offenders.sort(String::compareTo);
        assertThat(offenders)
                .describedAs("spzIB* procedures are forbidden in production sources (MIS Data Policy #264/#265)")
                .isEmpty();
    }

    /**
     * Finds the repository root by walking up from the surefire working directory
     * (which is a module directory such as {@code backend/common} or {@code app})
     * until a directory containing {@code .git} is found.
     */
    private static Path repoRoot() {
        Path dir = Path.of("").toAbsolutePath();
        while (dir != null) {
            if (Files.isDirectory(dir.resolve(".git")) || Files.isRegularFile(dir.resolve("AGENTS.md"))) {
                return dir;
            }
            dir = dir.getParent();
        }
        return Path.of("").toAbsolutePath();
    }

    private static void scanDirectory(Path dir, List<String> offenders, boolean mainOnly) throws IOException {
        if (!Files.isDirectory(dir)) {
            return;
        }
        try (Stream<Path> walk = Files.walk(dir)) {
            walk.filter(Files::isRegularFile)
                    .filter(p -> endsWithAny(p, SOURCE_EXTENSIONS))
                    .filter(p -> isSourceInScope(p, mainOnly))
                    .filter(p -> containsSpzIb(p))
                    .forEach(p -> offenders.add(p.toString().replace('\\', '/')));
        }
    }

    /**
     * Restricts the scan to production sources: only files under a
     * {@code /src/main/} segment (Java) or the {@code frontend/src} tree
     * (TypeScript). Test, resources, and tooling files are excluded.
     */
    private static boolean isSourceInScope(Path p, boolean mainOnly) {
        String normalized = p.toString().replace('\\', '/');
        if (mainOnly && normalized.contains("/src/main/")) {
            return true;
        }
        // frontend production sources live directly under frontend/src (no src/main).
        if (mainOnly && normalized.contains("/frontend/src/")) {
            return !normalized.contains("/node_modules/") && !normalized.contains(".test.");
        }
        return false;
    }

    private static boolean containsSpzIb(Path p) {
        try {
            String content = Files.readString(p, StandardCharsets.UTF_8);
            return content.toLowerCase(java.util.Locale.ROOT).contains("spzib");
        } catch (Exception e) {
            return false;
        }
    }

    private static boolean endsWithAny(Path p, String[] extensions) {
        String name = p.toString().toLowerCase(java.util.Locale.ROOT);
        for (String extension : extensions) {
            if (name.endsWith(extension)) {
                return true;
            }
        }
        return false;
    }
}
