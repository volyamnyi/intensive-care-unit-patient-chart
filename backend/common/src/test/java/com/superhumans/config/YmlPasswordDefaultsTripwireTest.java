package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import org.junit.jupiter.api.Test;

/**
 * Static tripwire (#372 item 4): no {@code *password*} key in
 * {@code application.yml} may carry a non-empty default — credentials are
 * env-only, and a literal default would silently authenticate (or fail) with
 * a well-known value in every environment that forgets to set the variable.
 */
class YmlPasswordDefaultsTripwireTest {

    // Matches `password: ${VAR:default}` and captures the default part.
    private static final Pattern PASSWORD_DEFAULT =
            Pattern.compile("(?m)^\\s*[\\w.-]*password[\\w.-]*:\\s*\\$\\{[^:}]+:([^}]*)\\}");

    @Test
    void passwordKeys_carryNoLiteralDefaults() throws Exception {
        String yml;
        try (var in = getClass().getResourceAsStream("/application.yml")) {
            assertThat(in).as("application.yml on the classpath").isNotNull();
            yml = new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }

        List<String> offenders = new ArrayList<>();
        Matcher matcher = PASSWORD_DEFAULT.matcher(yml);
        while (matcher.find()) {
            if (!matcher.group(1).isBlank()) {
                offenders.add(matcher.group(0).trim());
            }
        }
        assertThat(offenders)
                .describedAs("password keys with literal defaults in application.yml")
                .isEmpty();
    }
}
