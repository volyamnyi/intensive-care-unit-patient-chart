package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
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
 * #383: blocking external I/O must not run inside a transaction.
 *
 * <p>The {@code @Primary} transaction manager is the chained
 * {@code ChainedTransactionManager}, so any {@code @Transactional} method —
 * read-only or not — opens a connection in <em>every</em> pool at
 * {@code begin} and holds it until commit. A MIS HTTP call made inside such a
 * method therefore pins four connections for the whole round trip. Under MIS
 * latency that exhausts {@code APP_DATASOURCE_POOL_MAX} and cascades timeouts
 * into unrelated requests, so the rule is structural, not stylistic: fetch MIS
 * data before the transaction, after the commit, or in a method that is not
 * transactional at all.
 *
 * <p>This tripwire scans every production source of all four modules and fails
 * the build when a method annotated {@code @Transactional} calls a MIS client
 * (directly, or through the MIS-backed {@code ProstheticsPatientService}
 * facade). Keeping it source-level makes the guard cheap and deterministic —
 * a runtime pool-headroom harness would be slow and timing-dependent.
 *
 * <p>{@link #APPROVED_EXCEPTIONS} records the sites that still violate the rule
 * and are tracked outside this issue; each entry must name a reason so a new
 * violation is visibly different from a knowingly accepted one.
 */
class MisIoInsideTransactionTripwireTest {

    /**
     * Deliberately accepted residual offenders, each awaiting its own issue.
     * Do not extend this list to silence a new finding — fix the call site.
     */
    /**
     * Residual offenders that still violate the rule. Each is listed on
     * purpose rather than fixed here, because fixing a controller-level
     * {@code @Transactional} means first deciding whether a read audit event
     * may be emitted without an ambient transaction (today the controllers
     * carry the transaction precisely so the outbox INSERT has one). That is
     * an audit-atomicity decision, not a mechanical refactor, so it is
     * tracked separately instead of being smuggled into #383.
     *
     * <p>Do not add an entry to silence a new finding — move the MIS call out
     * of the transaction first.
     */
    private static final List<String[]> APPROVED_EXCEPTIONS = List.of(
            new String[] {"PatientController.java",
                    "patient search/record/pool endpoints; controller tx supplies the read audit event"},
            new String[] {"ProstheticsPatientController.java",
                    "same controller-tx-for-read-audit pattern as PatientController"},
            new String[] {"ProstheticsOrderService.java",
                    "MIS document lookup + per-document URL probes, incl. the provision write"},
            new String[] {"ProstheticsEligibilityService.java",
                    "MIS candidate roster scan"});

    private static final Pattern TRANSACTIONAL = Pattern.compile(
            "@(?:org\\.springframework\\.transaction\\.annotation\\.)?Transactional");

    /** Start of the next class member, used to bound a method-sized block. */
    private static final Pattern MEMBER_START = Pattern.compile(
            "(?m)^\\s{4}(?:@|public\\s|private\\s|protected\\s|static\\s|final\\s)");

    /** MIS client invocations, direct and through the patient facade. */
    private static final List<String> MIS_CALLS = List.of(
            "misService.", "misClient.", "misApiClient.",
            "patientService.get(", "patientService.search(", "patientService.getPibs(");

    private static Path backendRoot() {
        Path root = Path.of(System.getProperty("user.dir")).toAbsolutePath();
        while (root != null && !Files.exists(root.resolve("common/pom.xml"))) {
            root = root.getParent();
        }
        return Files.isDirectory(root.resolve("common")) ? root : root.getParent();
    }

    @Test
    void noTransactionalMethodCallsMis() throws IOException {
        java.util.Set<String> violations = new java.util.LinkedHashSet<>();
        int scannedTransactionalMethods = 0;

        for (String module : List.of("common", "icu-chart", "medication-sheet",
                "prosthesis-manufacturing")) {
            Path sources = backendRoot().resolve(module + "/src/main/java");
            try (Stream<Path> files = Files.walk(sources)) {
                for (Path file : files.filter(p -> p.toString().endsWith(".java")).toList()) {
                    String source = stripCommentsAndLiterals(
                            Files.readString(file, StandardCharsets.UTF_8));
                    String relative = module + "/" + sources.relativize(file).toString()
                            .replace('\\', '/');

                    Matcher transactional = TRANSACTIONAL.matcher(source);
                    while (transactional.find()) {
                        int from = transactional.start();
                        int to = blockEnd(source, from);
                        scannedTransactionalMethods++;
                        String body = source.substring(from, to);
                        for (String call : MIS_CALLS) {
                            if (body.contains(call)) {
                                String offender = relative + " -> " + call;
                                if (isApproved(offender)) {
                                    continue;
                                }
                                violations.add(offender);
                            }
                        }
                    }
                }
            }
        }

        assertThat(scannedTransactionalMethods)
                .as("tripwire found no @Transactional methods to scan - the pattern is broken")
                .isPositive();
        assertThat(violations)
                .as("#383: MIS I/O inside a transaction pins all four pool connections")
                .isEmpty();
    }

    /**
     * Blanks out comments and string/char literal contents, preserving offsets
     * and line breaks so block boundaries stay valid.
     *
     * <p>A regex strip is not safe here: this codebase is full of SQL and
     * absolute-URL literals whose trailing slashes would silently delete the
     * rest of the line and hide a real finding, and Javadoc that legitimately
     * names {@code @Transactional} must not be read as an annotation. Blanking
     * literals <em>and</em> comments keeps both cases honest.
     */
private static String stripCommentsAndLiterals(String source) {
        StringBuilder out = new StringBuilder(source.length());
        int i = 0;
        int n = source.length();
        while (i < n) {
            char c = source.charAt(i);
            char next = i + 1 < n ? source.charAt(i + 1) : '\0';
            if (c == '/' && next == '/') {
                while (i < n && source.charAt(i) != '\n') {
                    i++;
                }
            } else if (c == '/' && next == '*') {
                i += 2;
                while (i < n && !(source.charAt(i) == '*' && i + 1 < n
                        && source.charAt(i + 1) == '/')) {
                    if (source.charAt(i) == '\n') {
                        out.append('\n');
                    }
                    i++;
                }
                i = Math.min(n, i + 2);
            } else if (c == '"' || c == '\'') {
                char quote = c;
                i++;
                while (i < n) {
                    char lit = source.charAt(i);
                    if (lit == '\\') {
                        i += 2;
                        continue;
                    }
                    i++;
                    if (lit == quote || lit == '\n') {
                        break;
                    }
                }
            } else {
                out.append(c);
                i++;
            }
        }
        return out.toString();
    }

    /**
     * A {@code @Transactional} block runs from the annotation to the closing
     * brace of the annotated method body.
     *
     * <p>Bounding by "next member start" is wrong: a method signature line
     * follows its own annotations, so that rule truncates every block before
     * the body and silently hides the finding. Brace matching is exact and
     * survives nested blocks, lambdas and anonymous classes.
     */
    private static int blockEnd(String source, int from) {
        int open = source.indexOf('{', from);
        if (open < 0) {
            return source.length();
        }
        int depth = 0;
        for (int i = open; i < source.length(); i++) {
            char c = source.charAt(i);
            if (c == '{') {
                depth++;
            } else if (c == '}') {
                depth--;
                if (depth == 0) {
                    return i;
                }
            }
        }
        return source.length();
    }

    /** Exceptions are keyed by source file name so package moves cannot silently drop them. */
    private static boolean isApproved(String offender) {
        String file = offender.substring(0, offender.indexOf(" -> "));
        return APPROVED_EXCEPTIONS.stream().anyMatch(entry -> file.endsWith("/" + entry[0]));
    }
}