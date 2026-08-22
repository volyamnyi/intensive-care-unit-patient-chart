package com.superhumans.config;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;

import org.junit.jupiter.api.Test;
import org.springframework.core.io.ClassPathResource;

/**
 * Static tripwire guarding audit finding A2 (CWE-798): the core seed script must
 * never revert a user password on upsert. If {@code data-core.sql} ever contains
 * {@code EXCLUDED.password_hash} inside a {@code DO UPDATE SET} clause, seeding
 * would silently reset operator-rotated passwords on the next boot.
 */
class SeedDataSqlTripwireTest {

    @Test
    void coreSeed_neverOverwritesPasswordHashOnConflict() throws Exception {
        String sql = sqlText();

        assertThat(sql).describedAs("data-core.sql must not reference EXCLUDED.password_hash")
                .doesNotContain("password_hash = EXCLUDED.password_hash")
                .doesNotContain("EXCLUDED.password_hash");
    }

    @Test
    void coreSeed_userInsertsAreIdempotent() throws Exception {
        String sql = sqlText();

        assertThat(sql).contains("ON CONFLICT (login) DO NOTHING");
    }

    private static String sqlText() throws Exception {
        return new String(new ClassPathResource("data-core.sql").getInputStream().readAllBytes(),
                StandardCharsets.UTF_8);
    }
}
