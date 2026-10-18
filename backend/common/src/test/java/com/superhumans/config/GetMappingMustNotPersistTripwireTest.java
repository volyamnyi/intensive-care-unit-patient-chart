package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * #365 verb discipline: a {@code @GetMapping} handler must never persist.
 *
 * <p>The vitals GETs used to self-provision 107 rows, which made a safe HTTP
 * method mutate state - and with a {@code SameSite=Lax} cookie that is a CSRF
 * surface: a top-level navigation carries the cookie, so an attacker link could
 * force the inserts as the victim. This tripwire scans every controller source
 * in every module and fails the build if a GET handler body calls a repository
 * save/persist or a getOrCreate-style provisioning method.
 */
class GetMappingMustNotPersistTripwireTest {

    private static final Pattern GET_MAPPING = Pattern.compile("@GetMapping");
    private static final String[] FORBIDDEN = {
            ".save(", ".saveAll(", ".persist(", ".getOrCreate(", ".merge("
    };

    private static Path backendRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path root = current;
        while (root != null && !Files.exists(root.resolve("common/pom.xml"))) {
            root = root.getParent();
        }
        return Files.isDirectory(root.resolve("common")) ? root : root.getParent();
    }

    @Test
    void noGetMappingHandlerPersistAnything() throws IOException {
        List<String> violations = new ArrayList<>();
        int scannedGetHandlers = 0;

        for (String module : List.of("common", "icu-chart", "medication-sheet",
                "prosthesis-manufacturing")) {
            Path controllers = backendRoot().resolve(module + "/src/main/java");
            try (Stream<Path> files = Files.walk(controllers)) {
                for (Path file : files.filter(p -> p.toString().endsWith("Controller.java")).toList()) {
                    String source = Files.readString(file, StandardCharsets.UTF_8);
                    String relative = module + "/" + controllers.relativize(file);

                    // Split into method-sized blocks: every @GetMapping annotation
                    // starts one; the block ends at the next mapping annotation.
                    List<int[]> getBlocks = new ArrayList<>();
                    List<int[]> allMappings = new ArrayList<>();
                    java.util.regex.Matcher mapping = Pattern.compile(
                            "@(Get|Post|Put|Patch|Delete)Mapping").matcher(source);
                    while (mapping.find()) {
                        allMappings.add(new int[] {mapping.start(), mapping.start()});
                        if (source.charAt(mapping.start() + 1) == 'G') {
                            getBlocks.add(new int[] {mapping.start(), mapping.start()});
                        }
                    }
                    for (int[] get : getBlocks) {
                        int from = get[0];
                        int to = source.length();
                        for (int[] other : allMappings) {
                            if (other[0] > from && other[0] < to) {
                                to = other[0];
                            }
                        }
                        scannedGetHandlers++;
                        String body = source.substring(from, to);
                        for (String forbidden : FORBIDDEN) {
                            if (body.contains(forbidden)) {
                                violations.add(relative + " -> " + forbidden);
                            }
                        }
                    }
                }
            }
        }

        assertThat(scannedGetHandlers)
                .describedAs("GET handlers scanned - a zero here means the parser broke")
                .isGreaterThan(50);
        assertThat(violations)
                .describedAs("GET handlers must not persist; move provisioning to a POST")
                .isEmpty();
    }
}