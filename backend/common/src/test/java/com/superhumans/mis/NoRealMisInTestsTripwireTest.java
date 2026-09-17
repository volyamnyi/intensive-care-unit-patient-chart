package com.superhumans.mis;

import static org.assertj.core.api.Assertions.assertThat;

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
 * Static tripwire guarding test isolation from the real MIS and LDAP
 * (issue #298): no test source in the reactor may reference MIS API
 * environment names, dial non-loopback HTTP hosts, or use directory URLs
 * outside the documented local-only shapes.
 *
 * <p>Allowed exceptions (each verified by grep to never leave the JVM):
 * <ul>
 *   <li>loopback hosts ({@code localhost}, {@code 127.0.0.1}) — unit doubles,
 *       embedded servers and dummy base URLs;</li>
 *   <li>bare {@code mis} / {@code mis.local} hosts and {@code *.example} /
 *       {@code *.invalid} names — Mockito / MockRestServiceServer doubles and
 *       RFC-2606 example hosts, never passed to a live HTTP client;</li>
 *   <li>{@code hospital.ua} and {@code www.apache.org} — CORS origin and
 *       licence string constants (never fetched);</li>
 *   <li>{@code www.w3.org} — the XML namespace constant inside SVG payloads;</li>
 *   <li>{@code ldap} URLs pointing at loopback, the documented unroutable
 *       probe ({@code 10.255.255.1}) or {@code *.example} — the hermetic
 *       directory double and the gated local-only suites.</li>
 * </ul>
 *
 * <p>This file itself is excluded from the scan (it necessarily spells out
 * the forbidden patterns); the environment-name pattern is additionally
 * assembled by concatenation so the literal can never self-match.
 */
class NoRealMisInTestsTripwireTest {

    private static final List<String> MODULES = List.of(
            "common", "icu-chart", "medication-sheet", "prosthesis-manufacturing", "app");

    private static final Pattern MIS_ENV_NAMES = Pattern.compile("APP_MIS_API_" + "[A-Z_]+");
    private static final Pattern HTTP_URL = Pattern.compile("https?://([^\\s\"'<>]+)");
    private static final Pattern LDAP_URL = Pattern.compile("ldaps?://([^\\s\"'<>]+)");

    private static final Pattern LOOPBACK_HOST =
            Pattern.compile("^(localhost|127\\.0\\.0\\.1|0\\.0\\.0\\.0)$");

    private static final List<String> NEVER_DIALLED_HOSTS = List.of(
            "mis", "mis.local", "hospital.ua", "www.apache.org", "www.w3.org");

    private static final String UNROUTABLE_LDAP_PROBE = "10.255.255.1";

    private record SourceLine(Path path, int number, String text) {
    }

    @Test
    void testSources_referenceNoRealMisEnvNames() throws Exception {
        List<SourceLine> lines = allTestLines();
        List<String> offenders = lines.stream()
                .filter(line -> MIS_ENV_NAMES.matcher(line.text()).find())
                .map(line -> describe(line, "MIS env name"))
                .sorted()
                .toList();
        assertThat(offenders)
                .describedAs("Test sources must not reference APP_MIS_API_* "
                        + "environment names — CI tests run against the stub, never real MIS")
                .isEmpty();
    }

    @Test
    void testSources_dialNoExternalHttpHosts() throws Exception {
        List<String> offenders = new ArrayList<>();
        for (SourceLine line : allTestLines()) {
            Matcher matcher = HTTP_URL.matcher(line.text());
            while (matcher.find()) {
                String host = hostOf(matcher.group(1));
                if (!isLoopbackOrFake(host)) {
                    offenders.add(describe(line, "external host " + host));
                }
            }
        }
        assertThat(offenders.stream().sorted().toList())
                .describedAs("Test sources must not contain external HTTP(S) hosts — "
                        + "loopback doubles and the documented never-dialled fakes only")
                .isEmpty();
    }

    @Test
    void testSources_useLdapUrlsOnlyInLocalOnlyShapes() throws Exception {
        List<String> offenders = new ArrayList<>();
        for (SourceLine line : allTestLines()) {
            Matcher matcher = LDAP_URL.matcher(line.text());
            while (matcher.find()) {
                String host = hostOf(matcher.group(1));
                if (!isLocalOnlyLdapHost(host)) {
                    offenders.add(describe(line, "directory host " + host));
                }
            }
        }
        assertThat(offenders.stream().sorted().toList())
                .describedAs("Test sources must use directory URLs only against loopback, "
                        + "the documented unroutable probe, or example hosts")
                .isEmpty();
    }

    private static boolean isLoopbackOrFake(String host) {
        return LOOPBACK_HOST.matcher(host).matches()
                || NEVER_DIALLED_HOSTS.contains(host)
                || host.contains("example")
                || host.endsWith(".invalid");
    }

    private static boolean isLocalOnlyLdapHost(String host) {
        return LOOPBACK_HOST.matcher(host).matches()
                || host.equals(UNROUTABLE_LDAP_PROBE)
                || host.contains("example");
    }

    private static String hostOf(String authorityAndPath) {
        String host = authorityAndPath.split("[/?#\"'\\s]", 2)[0].toLowerCase();
        return host.replaceAll(":\\d*$", "");
    }

    private static String describe(SourceLine line, String detail) {
        return line.path() + ":" + line.number() + ": " + detail;
    }

    private static List<SourceLine> allTestLines() throws Exception {
        List<Path> roots = testRoots();
        assertThat(roots)
                .describedAs("expected at least one backend test tree")
                .isNotEmpty();
        List<SourceLine> lines = new ArrayList<>();
        for (Path root : roots) {
            try (Stream<Path> walk = Files.walk(root)) {
                for (Path file : walk.filter(p -> p.toString().endsWith(".java"))
                        .filter(p -> !p.getFileName().toString()
                                .equals("NoRealMisInTestsTripwireTest.java"))
                        .sorted()
                        .toList()) {
                    List<String> content = Files.readAllLines(file, StandardCharsets.UTF_8);
                    for (int i = 0; i < content.size(); i++) {
                        lines.add(new SourceLine(root.relativize(file), i + 1, content.get(i)));
                    }
                }
            }
        }
        assertThat(lines)
                .describedAs("expected test sources to scan (vacuous pass is a failure)")
                .isNotEmpty();
        return lines;
    }

    private static List<Path> testRoots() {
        Path cwd = Path.of("").toAbsolutePath();
        Path backend = backendRoot(cwd);
        return MODULES.stream()
                .map(module -> backend.resolve(module + "/src/test/java"))
                .filter(Files::isDirectory)
                .toList();
    }

    private static Path backendRoot(Path cwd) {
        if (cwd.getFileName() != null && cwd.getFileName().toString().equals("backend")) {
            return cwd;
        }
        Path parent = cwd.getParent();
        if (parent != null && parent.getFileName() != null
                && parent.getFileName().toString().equals("backend")) {
            return parent;
        }
        Path nested = cwd.resolve("backend");
        if (Files.isDirectory(nested)) {
            return nested;
        }
        return cwd;
    }
}
