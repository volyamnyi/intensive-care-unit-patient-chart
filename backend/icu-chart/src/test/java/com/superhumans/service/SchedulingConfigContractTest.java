package com.superhumans.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Config-drift contract for the ICU scheduler (#387): the yml keys
 * {@code app.scheduling.*} must be the single source of truth, so every one of
 * them has to be referenced by code, and the cron expressions must read the
 * hours from the properties instead of hardcoding them.
 */
class SchedulingConfigContractTest {

    private static final Pattern SCHEDULED = Pattern.compile(
            "@Scheduled\\(cron\\s*=\\s*\"([^\"]+)\"\\)");

    private static Path source(String relative) {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path root = current;
        while (root != null && !Files.exists(root.resolve("icu-chart/pom.xml"))) {
            root = root.getParent();
        }
        return (root == null ? current : root).resolve(relative);
    }

    private static List<String> javaSourcesIn(String module) throws Exception {
        try (Stream<Path> files = Files.walk(source(module + "/src/main/java"))) {
            return files.filter(p -> p.toString().endsWith(".java")).map(Path::toString).toList();
        }
    }

    private static String read(String file) throws Exception {
        return Files.readString(Path.of(file), StandardCharsets.UTF_8);
    }

    @Test
    void everySchedulingYmlKeyIsReferencedByCode() throws Exception {
        String yaml = Files.readString(
                source("common/src/main/resources/application.yml"), StandardCharsets.UTF_8);
        Set<String> yamlKeys = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("\\$\\{APP_SCHEDULING_([A-Z_]+):").matcher(yaml);
        while (matcher.find()) {
            // APP_DAY_CLOSE_HOUR -> the relaxed-binding kebab form used in @Value.
            yamlKeys.add("app.scheduling."
                    + matcher.group(1).toLowerCase(java.util.Locale.ROOT).replace('_', '-'));
        }
        assertThat(yamlKeys).isNotEmpty();

        StringBuilder allCode = new StringBuilder();
        for (String module : List.of("common", "icu-chart", "medication-sheet",
                "prosthesis-manufacturing")) {
            for (String file : javaSourcesIn(module)) {
                allCode.append(read(file));
            }
        }
        List<String> dead = new ArrayList<>();
        for (String key : yamlKeys) {
            if (!allCode.toString().contains(key)) {
                dead.add(key);
            }
        }
        assertThat(dead)
                .describedAs("application.yml scheduling keys that no code reads (dead config)")
                .isEmpty();
    }

    @Test
    void cronExpressionsReadTheConfiguredHoursInsteadOfHardcoding() throws Exception {
        List<String> crons = new ArrayList<>();
        for (String module : List.of("icu-chart", "medication-sheet", "prosthesis-manufacturing")) {
            for (String file : javaSourcesIn(module)) {
                Matcher matcher = SCHEDULED.matcher(read(file));
                while (matcher.find()) {
                    crons.add(matcher.group(1));
                }
            }
        }
        assertThat(crons).as("scheduled crons found").isNotEmpty();
        for (String cron : crons) {
            assertThat(cron)
                    .describedAs("cron '%s' must not hardcode an hour-of-day", cron)
                    .doesNotMatch("^\\d+ \\d+ \\d+ \\* \\* \\*$");
            assertThat(cron).contains("app.");
        }
    }

    @Test
    void emailTextUsesTheConfiguredCloseHour() throws Exception {
        String email = Files.readString(source(
                "icu-chart/src/main/java/com/superhumans/service/EmailService.java"),
                StandardCharsets.UTF_8);

        assertThat(email)
                .describedAs("escalation mail must derive its time text from the property")
                .doesNotContain("о 07:00")
                .contains("dayCloseHour");
    }

}
