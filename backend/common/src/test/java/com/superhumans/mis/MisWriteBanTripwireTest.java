package com.superhumans.mis;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.UncheckedIOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import org.junit.jupiter.api.Test;

/**
 * Static tripwire guarding the MIS Data Policy (absolute read-only, no
 * exceptions — #372 item 1): no production source in any module may reference
 * {@code sendPdf}/{@code transferPdf} or any {@code spi*save/create/update/
 * delete} write-procedure name, neither in {@code com.superhumans.mis}
 * sources nor as a literal passed to {@code MisApiClient.callMethod}.
 * Reintroducing such a call breaks the build here, before any test boots
 * Spring. IO failures fail loudly (never vacuous-pass).
 */
class MisWriteBanTripwireTest {

    private static final List<String> MODULES =
            List.of("common", "icu-chart", "medication-sheet", "prosthesis-manufacturing");

    // Case-insensitive spi*write token, e.g. spiPatientSave / spiSaveAgent.
    private static final Pattern WRITE_PROCEDURE =
            Pattern.compile("(?i)spi\\w*(save|create|update|delete)");

    private static final Pattern TRANSFER =
            Pattern.compile("sendPdf|transferPdf");

    @Test
    void mainSources_neverReferenceMisWrites() {
        List<Path> mains = moduleMains();
        List<String> offenders = new ArrayList<>();
        for (Path main : mains) {
            try (Stream<Path> walk = Files.walk(main)) {
                walk.filter(p -> p.toString().endsWith(".java")).forEach(p -> {
                    String code = stripLineComments(read(p));
                    boolean inMisPackage = p.toString().contains(
                            "com" + java.io.File.separator + "superhumans"
                                    + java.io.File.separator + "mis");
                    String line = code;
                    if (TRANSFER.matcher(line).find()) {
                        offenders.add(rel(p) + " :: transfer literal");
                    } else if (inMisPackage && WRITE_PROCEDURE.matcher(line).find()) {
                        offenders.add(rel(p) + " :: spi write token in mis sources");
                    } else if (line.contains("callMethod(") && WRITE_PROCEDURE.matcher(line).find()) {
                        offenders.add(rel(p) + " :: spi write token in callMethod");
                    }
                });
            } catch (java.io.IOException e) {
                throw new UncheckedIOException(main.toString(), e);
            }
        }
        assertThat(offenders)
                .describedAs("MIS is strictly read-only — no write procedures in src/main")
                .isEmpty();
    }

    @Test
    void matcher_catchesKnownWriteLiterals() {
        // Negative self-check: the patterns above must flag a synthetic
        // violation, so the tripwire cannot pass vacuously.
        assertThat(WRITE_PROCEDURE.matcher("callMethod(\"spiPatientSave\")").find()).isTrue();
        assertThat(WRITE_PROCEDURE.matcher("spiDocumentProsthesCheck").find()).isFalse();
        assertThat(TRANSFER.matcher("sendPdf(bytes)").find()).isTrue();
    }

    private List<Path> moduleMains() {
        Path base = Path.of("").toAbsolutePath();
        boolean nested = Files.isDirectory(base.resolve("src/main/java"));
        List<Path> mains = new ArrayList<>();
        for (String module : MODULES) {
            Path main = nested
                    ? base.resolve("src/main/java")
                    : base.resolve("backend/" + module + "/src/main/java");
            assertThat(main).describedAs("expected production sources at " + main).exists();
            mains.add(main);
        }
        return mains;
    }

    private Path base() {
        return Path.of("").toAbsolutePath();
    }

    private String rel(Path p) {
        return base().relativize(p).toString();
    }

    private static String read(Path p) {
        try {
            return Files.readString(p, StandardCharsets.UTF_8);
        } catch (java.io.IOException e) {
            throw new UncheckedIOException(p.toString(), e);
        }
    }

    private static String stripLineComments(String code) {
        StringBuilder out = new StringBuilder(code.length());
        for (String line : code.split("\n", -1)) {
            int comment = line.indexOf("//");
            // Keep the code before the comment; URLs in string literals carry
            // no spi-write tokens, so naive stripping is safe here.
            out.append(comment < 0 ? line : line.substring(0, comment)).append('\n');
        }
        return out.toString();
    }
}
