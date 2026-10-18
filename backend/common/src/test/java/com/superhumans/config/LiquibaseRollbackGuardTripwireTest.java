package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * #390 item 5 — a column-shrinking rollback must be guarded, and applied
 * changesets are immutable.
 *
 * <p>{@code ALTER COLUMN ... TYPE VARCHAR(100)} inside a rollback destroys data
 * when the table already holds longer values, which is exactly why
 * {@code core/004-audit-entity-length.sql} widened the column in the first place.
 * The historical changeset cannot be repaired in place: it is applied in every
 * environment and editing it changes its checksum, so every boot fails
 * "Validation Failed" (reproduced live — {@code --validCheckSum} was not
 * honoured by the bundled Liquibase). So the rule is enforced for everything that
 * comes next, and the one historical offender is listed as an accepted exception
 * that documents its own hazard.
 */
class LiquibaseRollbackGuardTripwireTest {

    /** {@code ALTER TABLE x ALTER COLUMN c TYPE VARCHAR(n)} with n < current width. */
    private static final Pattern TYPE_CHANGE = Pattern.compile(
            "--rollback\\s+ALTER\\s+TABLE\\s+(\\w+)\\s+ALTER\\s+COLUMN\\s+(\\w+)\\s+TYPE\\s+"
                + "VARCHAR\\((\\d+)\\)", Pattern.CASE_INSENSITIVE);

    private static final Pattern CHANGESET_HEADER = Pattern.compile("--changeset\\s+([^\\r\\n]+)");

    /**
     * Changesets already applied to every environment - their bytes are frozen by
     * the Liquibase checksum, so the only correct place for the caveat is this
     * list plus the runbook.
     */
    private static final Map<String, String> ACCEPTED_EXCEPTIONS = Map.of(
            "core/004-audit-entity-length.sql",
            "Widened audit_logs.entity to 255 because real request URIs exceed 100 chars. "
                + "Its unguarded shrink rollback cannot be edited in place (checksum); rolling "
                + "this back needs the rows cleaned first - see Production-Deployment-Runbook "
                + "section 1.9.",
            "prosth/007-pause-categories.sql",
            "Widened pause_category to 32 chars. The forward data cleanup (split-prosth:22) only "
                + "NULLs the three values that were too long, so free-text values written later "
                + "would be truncated on rollback. Applied everywhere - roll back only after "
                + "auditing that column.");

    private static Path changelogRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path root = current;
        while (root != null && !Files.exists(root.resolve("common/pom.xml"))) {
            root = root.getParent();
        }
        Path backend = Files.isDirectory(root.resolve("common")) ? root : root.getParent();
        return backend.resolve("common/src/main/resources/db/changelog");
    }

    @Test
    void everyColumnShrinkRollbackIsGuardedOrDocumented() throws IOException {
        List<String> unguarded = new ArrayList<>();
        int checked = 0;

        Path root = changelogRoot();
        try (Stream<Path> files = Files.walk(root)) {
            for (Path file : files.filter(p -> p.toString().endsWith(".sql")).toList()) {
                String relative = root.relativize(file).toString().replace('\\', '/');
                String sql = Files.readString(file, StandardCharsets.UTF_8);

                Map<String, String> rollbackByChangeset = new LinkedHashMap<>();
                String currentChangeset = null;
                for (String line : sql.split("\\R")) {
                    Matcher header = CHANGESET_HEADER.matcher(line);
                    if (header.find()) {
                        currentChangeset = header.group(1).trim();
                    } else if (line.trim().startsWith("--rollback")
                            && currentChangeset != null) {
                        rollbackByChangeset.putIfAbsent(currentChangeset, line);
                    }
                }

                Matcher typeChange = TYPE_CHANGE.matcher(sql);
                while (typeChange.find()) {
                    checked++;
                    String rollbackBlock = rollbackByChangeset.values().stream()
                            .filter(line -> line.contains(typeChange.group(1))
                                    && line.contains(typeChange.group(2)))
                            .findFirst().orElse("");
                    boolean guarded = rollbackBlock.toUpperCase(Locale.ROOT).contains("RAISE EXCEPTION")
                            || rollbackBlock.toUpperCase(Locale.ROOT).contains("IF EXISTS");
                    if (!guarded && !ACCEPTED_EXCEPTIONS.containsKey(relative)) {
                        unguarded.add(relative + " -> " + typeChange.group(1) + "."
                                + typeChange.group(2));
                    }
                }
            }
        }

        assertThat(checked)
                .describedAs("rollback column-type changes scanned - a zero here means the "
                        + "pattern silently stopped matching")
                .isPositive();
        assertThat(unguarded)
                .describedAs("column-shrinking rollbacks must guard on data length first")
                .isEmpty();
        assertThat(ACCEPTED_EXCEPTIONS).as("every accepted exception needs a real caveat").isNotEmpty();
    }
}