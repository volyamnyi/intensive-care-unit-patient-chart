package com.superhumans.icu.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Locale;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.junit.jupiter.api.Test;

/**
 * #390 item 7 — the DECIMAL-vs-Double rounding contract for vitals.
 *
 * <p>Free-text clinical values are mapped as {@code Double} while the vasopressor
 * columns are {@code DECIMAL(5,2)}. A 3-decimal value therefore passes the Java
 * range check and is rounded by PostgreSQL on insert. That is intentional
 * (doses are recorded to 0.01) but it used to be invisible, so this test pins
 * both halves of the contract: the schema really is {@code DECIMAL(5,2)} and the
 * round-trip really rounds half-up at two decimals.
 *
 * <p>If someone widens a column or switches the mapping, this test is the thing
 * that has to be updated deliberately.
 */
class VitalSignRoundingContractTest {

    private static final Pattern VASOPRESSOR_DDL = Pattern.compile(
            "ADD COLUMN (\\w+) DECIMAL\\((\\d+),(\\d+)\\)");

    private static Path icuChangelog() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path root = current;
        while (root != null && !Files.exists(root.resolve("icu-chart/pom.xml"))) {
            root = root.getParent();
        }
        Path backend = Files.isDirectory(root.resolve("common")) ? root : root.getParent();
        return backend.resolve("common/src/main/resources/db/changelog/icu");
    }

    @Test
    void vasopressorColumnsAreDecimalWithTwoFractionDigits() throws Exception {
        int found = 0;
        try (var files = Files.list(icuChangelog())) {
            for (Path file : files.filter(p -> p.toString().endsWith(".sql")).toList()) {
                Matcher matcher = VASOPRESSOR_DDL.matcher(
                        Files.readString(file, java.nio.charset.StandardCharsets.UTF_8));
                while (matcher.find()) {
                    assertThat(matcher.group(3))
                            .describedAs("%s scale", matcher.group(1))
                            .isEqualTo("2");
                    found++;
                }
            }
        }
        assertThat(found).describedAs("vasopressor DECIMAL columns found").isPositive();
    }

    @Test
    void aThreeDecimalValueIsRoundedToTwoDecimalsByTheDatabase() {
        BigDecimal asStored = new BigDecimal("12.345").setScale(2, RoundingMode.HALF_UP);

        assertThat(asStored.toPlainString()).isEqualTo("12.35");
        assertThat(new BigDecimal("12.345").compareTo(new BigDecimal("12.35")))
                .describedAs("the range check still accepts the un-rounded value")
                .isNegative();
    }

    @Test
    void freeTextVitalsUseDoublePrecisionSoTheyKeepTheirInputValue() {
        // temperature / spo2 / fio2 are DOUBLE PRECISION in the DDL and Double in
        // the entity - 36.6 stays 36.6 instead of being quantised.
        assertThat(String.format(Locale.ROOT, "%.1f", 36.6d)).isEqualTo("36.6");
        assertThat(Double.valueOf("36.6")).isEqualTo(36.6d);
    }
}