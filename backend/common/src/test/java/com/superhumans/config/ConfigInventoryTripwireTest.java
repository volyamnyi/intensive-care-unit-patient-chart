package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Deque;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Discoverability contract (#430 item 5): every code-consumed {@code app.*}
 * key is (a) present in {@code application.yml} with an {@code ${ENV:default}}
 * placeholder so an operator can find it, and (b) named in the deployment
 * runbook env table. A key that exists only as a code-side {@code @Value}
 * default is exactly the drift this issue documents.
 */
class ConfigInventoryTripwireTest {

    private static final Pattern VALUE_KEY = Pattern.compile(
            "@Value\\(\"\\$\\{(app\\.[A-Za-z0-9._\\-]+)(?::([^}]*))?}\"\\)");
    private static final Pattern YAML_ENTRY =
            Pattern.compile("^([ ]*)([a-z][A-Za-z0-9._\\-]*):[ ]*(.*)$");
    private static final Pattern PLACEHOLDER = Pattern.compile("\\$\\{([A-Z][A-Z0-9_]*)(?::[^}]*)?}");

    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (current != null && !Files.exists(current.resolve("common/pom.xml"))) {
            current = current.getParent();
        }
        return current == null ? Path.of(".").toAbsolutePath() : current;
    }

    private static List<Path> javaSources() throws IOException {
        try (Stream<Path> files = Files.walk(repoRoot().resolve("common/src/main/java"))) {
            return files.filter(path -> path.toString().endsWith(".java")).toList();
        }
    }

    private static Set<String> appKeysUsedInCode() throws IOException {
        Set<String> keys = new LinkedHashSet<>();
        for (Path source : javaSources()) {
            Matcher matcher = VALUE_KEY.matcher(Files.readString(source, StandardCharsets.UTF_8));
            while (matcher.find()) {
                keys.add(matcher.group(1));
            }
        }
        return keys;
    }

    /** Indexes {@code application.yml} by dotted path, e.g. {@code app.audit.retention.cron}. */
    private static Map<String, String> yamlIndex(String yamlText) {
        Map<String, String> index = new LinkedHashMap<>();
        Deque<String> segments = new ArrayDeque<>();
        Deque<Integer> indents = new ArrayDeque<>();
        for (String line : yamlText.split("\\R")) {
            if (line.isBlank() || line.stripLeading().startsWith("#") || line.stripLeading().startsWith("-")) {
                continue;
            }
            Matcher entry = YAML_ENTRY.matcher(line);
            if (!entry.matches()) {
                continue;
            }
            int indent = entry.group(1).length();
            while (!indents.isEmpty() && indents.peek() >= indent) {
                indents.pop();
                segments.pop();
            }
            segments.push(entry.group(2));
            indents.push(indent);
            List<String> path = new ArrayList<>(segments);
            Collections.reverse(path);
            // First occurrence wins: the base block, not a later profile override.
            index.putIfAbsent(String.join(".", path), entry.group(3));
        }
        return index;
    }

    @Test
    void everyCodeConsumedAppKeyIsDocumentedInYamlAndRunbook() throws IOException {
        Map<String, String> yamlEntries = yamlIndex(Files.readString(
                repoRoot().resolve("common/src/main/resources/application.yml"), StandardCharsets.UTF_8));
        String runbook = Files.readString(
                repoRoot().resolve("../docs/Production-Deployment-Runbook.md"), StandardCharsets.UTF_8);

        List<String> undocumented = new ArrayList<>();
        for (String key : appKeysUsedInCode()) {
            // app.api.* is OpenAPI metadata, not an operational tunable.
            if (key.startsWith("app.api.")) {
                continue;
            }
            String value = yamlEntries.get(key);
            if (value == null) {
                undocumented.add(key + " (no application.yml entry)");
                continue;
            }
            Matcher placeholder = PLACEHOLDER.matcher(value);
            if (!placeholder.find()) {
                undocumented.add(key + " (application.yml has no ${ENV:default} placeholder)");
                continue;
            }
            if (!runbook.contains(placeholder.group(1))) {
                undocumented.add(key + " (env " + placeholder.group(1)
                        + " absent from the runbook env table)");
            }
        }
        assertThat(undocumented)
                .describedAs("app.* keys consumed by code without a documented env placeholder")
                .isEmpty();
    }

    @Test
    void yamlPlaceholderNamesAreUpperSnakeCaseAndUnique() throws IOException {
        String yamlText = Files.readString(
                repoRoot().resolve("common/src/main/resources/application.yml"), StandardCharsets.UTF_8);
        List<String> names = new ArrayList<>();
        Matcher placeholder = PLACEHOLDER.matcher(yamlText);
        while (placeholder.find()) {
            names.add(placeholder.group(1));
        }
        assertThat(names)
                .describedAs("relaxed binding requires upper snake case env names")
                .isNotEmpty()
                .allSatisfy(name -> assertThat(name).matches("[A-Z][A-Z0-9_]*"))
                .doesNotHaveDuplicates();
    }
}