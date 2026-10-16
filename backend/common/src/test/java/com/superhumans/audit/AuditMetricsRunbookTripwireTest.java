package com.superhumans.audit;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Set;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Doc tripwire (#397 item 1): every {@code audit.*} meter registered in
 * production sources must have a row in the runbook F.5 alert table, so a new
 * signal (e.g. a fresh skip reason) cannot land without operator guidance.
 * IO failures fail loudly (never vacuous-pass).
 */
class AuditMetricsRunbookTripwireTest {

    private static final Pattern METER_LITERAL =
            Pattern.compile("\"(audit\\.[a-z0-9._-]+)\"");

    @Test
    void everyRegisteredMeter_hasRunbookRow() throws Exception {
        Path base = Path.of("").toAbsolutePath();
        Path main = Files.isDirectory(base.resolve("src/main/java"))
                ? base.resolve("src/main/java")
                : base.resolve("backend/common/src/main/java");
        assertThat(main).describedAs("expected production sources at " + main).exists();

        Set<String> meters = new TreeSet<>();
        try (Stream<Path> walk = Files.walk(main)) {
            walk.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                Matcher matcher = METER_LITERAL.matcher(read(p));
                while (matcher.find()) {
                    meters.add(matcher.group(1));
                }
            });
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(main.toString(), e);
        }
        assertThat(meters).describedAs("expected audit meters in sources").isNotEmpty();

        // Surefire CWD is the module dir (backend/common); repo docs live two
        // levels up. Probe likely locations, fail loudly if none exists.
        Path runbook = null;
        for (String candidate : new String[]{
                "docs/Production-Deployment-Runbook.md",
                "../docs/Production-Deployment-Runbook.md",
                "../../docs/Production-Deployment-Runbook.md"}) {
            if (Files.exists(base.resolve(candidate))) {
                runbook = base.resolve(candidate);
                break;
            }
        }
        assertThat(runbook).describedAs("expected runbook under " + base).isNotNull();
        String runbookText = read(runbook);

        Set<String> missing = new TreeSet<>();
        for (String meter : meters) {
            if (!runbookText.contains("`" + meter)) {
                missing.add(meter);
            }
        }
        assertThat(missing)
                .describedAs("audit meters without an F.5 runbook row")
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
