package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.TreeSet;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Stream;
import org.junit.jupiter.api.Test;

/**
 * Access-matrix artifact (#374).
 *
 * <p>The review artifact for the CR-01 access matrix was never produced, so the
 * rules lived only in {@code SecurityConfig}/{@code ClinicalSecurityRules} and
 * drifted silently. {@code docs/Access-Matrix.md} is now generated from those
 * two files plus every controller mapping, and this test regenerates the same
 * content in-memory and fails when the checked-in artifact is stale вЂ” the matrix
 * can no longer rot.
 */
class AccessMatrixArtifactTest {

    private static final Pattern MAPPING = Pattern.compile(
            "@(Get|Post|Put|Patch|Delete)Mapping(?:\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\")?");
    private static final Pattern CLASS_MAPPING = Pattern.compile(
            "@RequestMapping\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\"");
private static final Pattern RULE = Pattern.compile(
            "requestMatchers\\(([^)]*)\\)(?:\\.access\\((spEl\\([A-Z_]+\\))\\))?\\.([a-zA-Z]+)\\(([^)]*)\\)");

    private static final List<String> MODULES = List.of(
            "common", "icu-chart", "medication-sheet", "prosthesis-manufacturing");

/** Repository root (the directory that holds {@code backend/}, {@code docs/}, {@code frontend/}). */
    private static Path repoRoot() {
        Path current = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        Path candidate = current;
        while (candidate != null && !Files.exists(candidate.resolve("common/pom.xml"))) {
            candidate = candidate.getParent();
        }
        if (candidate == null) {
            return current;
        }
        return Files.isDirectory(candidate.resolve("frontend")) ? candidate : candidate.getParent();
    }

    private static Path backendRoot() {
        return repoRoot().resolve("backend");
    }

    private static List<Path> javaSources(String module) throws IOException {
        try (Stream<Path> files = Files.walk(backendRoot().resolve(module + "/src/main/java"))) {
            return files.filter(p -> p.toString().endsWith(".java")).sorted().toList();
        }
    }

    private static String read(Path path) throws IOException {
        return Files.readString(path, StandardCharsets.UTF_8);
}

    private static String join(String base, String sub) {
        String prefix = base == null ? "" : base;
        if (prefix.endsWith("/") && sub.startsWith("/")) {
            return prefix + sub.substring(1);
        }
        if (!prefix.endsWith("/") && !sub.isEmpty() && !sub.startsWith("/")) {
            return prefix + "/" + sub;
        }
        return prefix + sub;
    }

/** URL-level rules in declaration order, exactly as SecurityConfig applies them. */
    private static List<String> urlRules() throws IOException {
        List<String> rules = new ArrayList<>();
        for (String source : List.of(
                "common/src/main/java/com/superhumans/config/SecurityConfig.java",
                "common/src/main/java/com/superhumans/config/ClinicalSecurityRules.java")) {
            // The whole authorizeHttpRequests block is one statement, so split on
            // the rule calls themselves rather than on ';'.
            String text = read(backendRoot().resolve(source))
                    .replaceAll("//[^\\r\\n]*", "")
                    .replaceAll("\\s+", " ");
            List<Integer> starts = new ArrayList<>();
            for (int at = text.indexOf("requestMatchers("); at >= 0;
                    at = text.indexOf("requestMatchers(", at + 1)) {
                starts.add(at);
            }
            for (int i = 0; i < starts.size(); i++) {
                int from = starts.get(i);
                int to = i + 1 < starts.size() ? starts.get(i + 1) : text.length();
                String segment = text.substring(from, Math.min(to, text.length()));
                int close = segment.indexOf(')');
                if (close < 0) {
                    continue;
                }
                String matchers = segment.substring("requestMatchers(".length(), close).trim();
                String tail = segment.substring(close + 1).trim();
                String rule;
                if (tail.startsWith(".access(")) {
                    rule = trimArguments(tail.substring(".access(".length()).trim());
                } else if (tail.startsWith(".")) {
                    int open = tail.indexOf('(');
                    rule = trimArguments(open < 0
                            ? tail.substring(1)
                            : tail.substring(1, open + 1) + tail.substring(open + 1));
                } else {
                    rule = "-";
                }
                rules.add(rule + "   <- " + matchers);
            }
        }
        return rules;
    }

    /** Cuts a rule at the first statement/chaining boundary, then drops stray parens. */
    private static String trimArguments(String call) {
        String v = call.trim();
        // Chained calls (.access(...), .authenticated(), .build();) follow the rule.
        int cut = indexOfAny(v, ";}", " .", ") .");
        if (cut > 0) {
            v = v.substring(0, cut).trim();
        } else if (cut == 0) {
            return "-";
        }
        while (v.endsWith(")") && count(v, ')') > count(v, '(')) {
            v = v.substring(0, v.length() - 1).trim();
        }
        // The cut may land inside the argument list - close what we opened.
        while (count(v, '(') > count(v, ')')) {
            v = v + ")";
        }
        return v.isEmpty() ? "-" : v;
    }

    private static int indexOfAny(String value, String... needles) {
        int best = -1;
        for (String needle : needles) {
            int at = value.indexOf(needle);
            if (at >= 0 && (best < 0 || at < best)) {
                best = at;
            }
        }
        return best;
    }

private static int count(String value, char c) {
        int n = 0;
        for (int i = 0; i < value.length(); i++) {
            if (value.charAt(i) == c) {
                n++;
            }
        }
        return n;
    }

    /** One row of the review matrix: everything the issue asks for, read from code. */
    private record Endpoint(String module, String controller, String method, String path,
                            String ceiling, String gate, String ownership) {
    }

    private static final Pattern PRE_AUTHORIZE = Pattern.compile(
            "@PreAuthorize\\(\\s*(?:value\\s*=\\s*)?\"([^\"]*)\"");
    private static final Pattern QUOTED = Pattern.compile("\"([^\"]*)\"");
    private static final Pattern METHOD_CONSTRAINT = Pattern.compile("HttpMethod\\.([A-Z]+)");
    /** Ownership guards: assigned user or allow-all, strangers get 404 (anti-enumeration). */
    private static final List<String> OWNERSHIP_GUARDS = List.of(
            "requireOwner", "requireAssignee", "checkOwner", "verifyOwner", "isOwner");

    /**
     * Parses the URL rules into (method constraint, patterns, expression) so every
     * endpoint can be matched against the first rule that wins, exactly as the
     * filter chain evaluates it.
     */
    private static List<String[]> ruleTriples() throws IOException {
        List<String[]> triples = new ArrayList<>();
        for (String rule : urlRules()) {
            int arrow = rule.indexOf("   <- ");
            String expression = normalize(arrow < 0 ? rule : rule.substring(0, arrow).trim());
            String matchers = arrow < 0 ? "" : rule.substring(arrow + 5).trim();
            String method = null;
            Matcher constraint = METHOD_CONSTRAINT.matcher(matchers);
            if (constraint.find()) {
                method = constraint.group(1);
            }
            List<String> patterns = new ArrayList<>();
            Matcher quoted = QUOTED.matcher(matchers);
            while (quoted.find()) {
                patterns.add(quoted.group(1));
            }
            triples.add(new String[] {method, String.join("|", patterns), expression});
        }
        return triples;
    }

    /**
     * One row = one terminal call. The parser sees whole chained blocks, so drop
     * whatever it dragged in from the next statement and rebalance the parentheses
     * the cut opened.
     */
    private static String normalize(String expression) {
        String value = expression.split(";")[0].trim();
        while (count(value, '(') > count(value, ')')) {
            value = value + ")";
        }
        while (value.endsWith(")") && count(value, ')') > count(value, '(')) {
            value = value.substring(0, value.length() - 1).trim();
        }
        return value.isEmpty() ? "-" : value;
    }

    /** Ant-style matcher: {@code **} any depth, {@code *} one segment, {@code {id}} one segment. */
    private static boolean matches(String pattern, String path) {
        StringBuilder regex = new StringBuilder();
        for (int i = 0; i < pattern.length(); i++) {
            char c = pattern.charAt(i);
            if (c == '*' && i + 1 < pattern.length() && pattern.charAt(i + 1) == '*') {
                regex.append(".*");
                i++;
            } else if (c == '*') {
                regex.append("[^/]*");
            } else if (c == '{') {
                int close = pattern.indexOf('}', i);
                if (close < 0) {
                    regex.append("\\Q").append(c).append("\\E");
                } else {
                    regex.append("[^/]+");
                    i = close;
                }
            } else if ("\\.[]{}()+-?^$|".indexOf(c) >= 0) {
                regex.append('\\').append(c);
            } else {
                regex.append(c);
            }
        }
        return path.matches(regex.toString());
    }

    /** First matching rule wins; {@code null} means the chain falls through to no match. */
    private static String ceilingFor(String method, String path, List<String[]> rules) {
        for (String[] rule : rules) {
            if (rule[0] != null && !rule[0].equals(method)) {
                continue;
            }
            for (String pattern : rule[1].split("\\|")) {
                if (!pattern.isEmpty() && matches(pattern, path)) {
                    return rule[2];
                }
            }
        }
        return "(no rule)";
    }

    /**
     * {@code FlowInstanceService flowInstanceService;} - the modifiers are optional
     * because Lombok {@code @FieldDefaults(makeFinal = true, level = PRIVATE)}
     * supplies them at compile time and the source has none.
     */
    private static final Pattern SERVICE_FIELD = Pattern.compile(
            "(?:private\\s+|public\\s+|protected\\s+)?(?:final\\s+)?(\\w*Service)\\s+(\\w+)\\s*;");

    /** Endpoints with the gate and ownership check resolved from the same file. */
    private static List<Endpoint> endpointRows(List<String[]> rules) throws IOException {
        List<Endpoint> rows = new ArrayList<>();
        for (String module : MODULES) {
            Map<String, Path> serviceSources = serviceSourcesByClass(module);
            for (Path file : javaSources(module)) {
                String text = read(file);
                if (!text.contains("@RestController")) {
                    continue;
                }
                Matcher classMapping = CLASS_MAPPING.matcher(text);
                String base = classMapping.find() ? classMapping.group(1) : "";
                Matcher className = Pattern.compile("(?:class|record)\\s+(\\w+)").matcher(text);
                String controller = className.find() ? className.group(1) : file.getFileName().toString();

                // Injected services, so a guard inside one of them can be attributed.
                Map<String, String> fieldToService = new LinkedHashMap<>();
                Matcher field = SERVICE_FIELD.matcher(text);
                while (field.find()) {
                    fieldToService.put(field.group(2), field.group(1));
                }

                Matcher mapping = MAPPING.matcher(text);
                while (mapping.find()) {
                    String method = mapping.group(1).toUpperCase(java.util.Locale.ROOT);
                    String sub = mapping.group(2) == null ? "" : mapping.group(2);
                    String path = join(base, sub);

                    // The method slice runs from its mapping annotation to the next
                    // one - that is where @PreAuthorize and the body live.
                    int from = mapping.start();
                    int to = text.length();
                    for (String nextMapping : List.of("@GetMapping", "@PostMapping", "@PutMapping",
                            "@PatchMapping", "@DeleteMapping")) {
                        int next = text.indexOf(nextMapping, from + 1);
                        if (next >= 0 && next < to) {
                            to = next;
                        }
                    }
                    String slice = text.substring(from, to);

                    String gate = "-";
                    Matcher preAuthorize = PRE_AUTHORIZE.matcher(slice);
                    if (preAuthorize.find()) {
                        gate = preAuthorize.group(1);
                    } else if (PRE_AUTHORIZE.matcher(
                            text.substring(0, from)).find()) {
                        gate = "(class-level)";
                    }

                    rows.add(new Endpoint(module, controller, method, path,
                            ceilingFor(method, path, rules), gate,
                            ownershipOf(slice, fieldToService, serviceSources)));
                }
            }
        }
        rows.sort(java.util.Comparator.comparing(Endpoint::path).thenComparing(Endpoint::method));
        return rows;
    }

    /** {@code ServiceName} -> its source file, for the module currently being scanned. */
    private static Map<String, Path> serviceSourcesByClass(String module) throws IOException {
        Map<String, Path> byClass = new LinkedHashMap<>();
        for (Path file : javaSources(module)) {
            Matcher declared = Pattern.compile("(?:class|interface)\\s+(\\w+Service)\\b").matcher(read(file));
            if (declared.find()) {
                byClass.put(declared.group(1), file);
            }
        }
        return byClass;
    }

    /**
     * IOC when the controller calls a service method that guards ownership, or when
     * the controller calls a guard itself. A guard inside a service is attributed to
     * the endpoint that reaches it - reading only the controller body would have
     * reported zero, which is what the first version of this generator did.
     */
    private static String ownershipOf(String slice, Map<String, String> fieldToService,
            Map<String, Path> serviceSources) throws IOException {
        for (String guard : OWNERSHIP_GUARDS) {
            if (slice.contains(guard + "(")) {
                return "IOC";
            }
        }
        Matcher call = Pattern.compile("(\\w+)\\.(\\w+)\\(").matcher(slice);
        while (call.find()) {
            String serviceClass = fieldToService.get(call.group(1));
            if (serviceClass == null) {
                continue;
            }
            Path source = serviceSources.get(serviceClass);
            if (source == null) {
                continue;
            }
            String serviceText = read(source);
            Matcher declaration = Pattern.compile(
                            "(?:public|protected|private)?\\s*[\\w<>,\\[\\]?. ]*\\s+" + Pattern.quote(call.group(2))
                            + "\\s*\\(").matcher(serviceText);
            if (!declaration.find()) {
                continue;
            }
            int body = serviceText.indexOf('{', declaration.end());
            if (body < 0) {
                continue;
            }
            int end = matchingBrace(serviceText, body);
            String methodBody = serviceText.substring(body, end);
            for (String guard : OWNERSHIP_GUARDS) {
                if (methodBody.contains(guard + "(")) {
                    return "IOC";
                }
            }
        }
        return "-";
    }

    private static int matchingBrace(String text, int open) {
        int depth = 0;
        for (int i = open; i < text.length(); i++) {
            char c = text.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}' && --depth == 0) {
                return i;
            }
        }
        return text.length();
    }

    @Test
    void checkedInArtifactMatchesTheCode() throws IOException {
        Path artifact = repoRoot().resolve("docs/Access-Matrix.md");
        assertThat(artifact).describedAs("run 'mvn -q -pl common -Dtest=AccessMatrixArtifactTest' "
                + "with -Daccess.matrix.write=true to regenerate").exists();

        Map<String, Integer> byModule = new LinkedHashMap<>();
        for (String module : MODULES) {
            byModule.put(module, javaSources(module).size());
        }

StringBuilder expected = new StringBuilder();
        List<String[]> rules = ruleTriples();
        List<Endpoint> rows = endpointRows(rules);
        expected.append("# Access matrix\n\n");
        expected.append("<!-- GENERATED FILE - do not edit by hand. Source of truth: ")
                .append("`SecurityConfig`, `ClinicalSecurityRules` and the controller mappings. ")
                .append("`AccessMatrixArtifactTest` fails when it goes stale. -->\n\n");
        expected.append("Endpoint count (code): **").append(rows.size()).append("**\n\n");
        expected.append("## URL rules (first match wins)\n\n");
        expected.append("| # | Matchers | Rule |\n|---|---|---|\n");
List<String> urlRules = urlRules();
        for (int i = 0; i < urlRules.size(); i++) {
            String rule = urlRules.get(i);
            int arrow = rule.indexOf("   <- ");
            String expression = arrow < 0 ? rule : rule.substring(0, arrow).trim();
            String matchers = arrow < 0 ? "-" : rule.substring(arrow + 5).trim();
            expected.append("| ").append(i + 1).append(" | `").append(matchers).append("` | `")
                    .append(normalize(expression)).append("` |\n");
        }
expected.append("\n## Endpoints by module\n\n| Module | Java sources | Endpoints |\n|---|---|---|\n");
        Map<String, Long> endpointsByModule = rows.stream()
                .collect(java.util.stream.Collectors.groupingBy(Endpoint::module,
                        java.util.stream.Collectors.counting()));
        for (String module : MODULES) {
            expected.append("| `").append(module).append("` | ").append(byModule.get(module))
                    .append(" | ").append(endpointsByModule.getOrDefault(module, 0L)).append(" |\n");
        }

        expected.append("\n## Endpoint matrix\n\n")
                .append("`IOC` = object-level ownership check (assigned user or allow-all; ")
                .append("strangers get 404 instead of 403 so ids cannot be enumerated). ")
                .append("`-` = permission/ceiling only, no caller-scope check. ")
                .append("`(class-level)` = inherited from the controller.\n\n")
                .append("| Module | Controller | HTTP | Path | URL ceiling | @PreAuthorize | Object-level |\n")
                .append("|---|---|---|---|---|---|---|\n");
        int n = 0;
        for (Endpoint row : rows) {
            expected.append("| `").append(row.module()).append("` | `").append(row.controller())
                    .append("` | ").append(row.method()).append(" | `").append(row.path())
                    .append("` | `").append(row.ceiling()).append("` | `").append(row.gate())
                    .append("` | ").append(row.ownership()).append(" |\n");
            n++;
        }

        expected.append("\nEndpoints: **").append(n).append("** (with an ownership check: **")
                .append(rows.stream().filter(r -> "IOC".equals(r.ownership())).count())
                .append("**).\n");

        String actual = read(artifact);
        if (Boolean.getBoolean("access.matrix.write")) {
            Files.writeString(artifact, expected.toString(), StandardCharsets.UTF_8);
            return;
        }
        assertThat(actual)
                .describedAs("docs/Access-Matrix.md is stale - regenerate it "
                        + "(mvn -pl common -Dtest=AccessMatrixArtifactTest -Daccess.matrix.write=true test)")
                .isEqualTo(expected.toString());
    }
}
