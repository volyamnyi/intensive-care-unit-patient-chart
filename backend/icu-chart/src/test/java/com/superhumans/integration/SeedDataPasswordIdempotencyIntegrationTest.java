package com.superhumans.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import javax.sql.DataSource;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.core.io.ClassPathResource;
import org.springframework.core.io.support.EncodedResource;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.init.ScriptUtils;
import org.springframework.test.context.jdbc.Sql;
import org.springframework.test.context.jdbc.SqlConfig;

/**
 * Integration pin for audit finding A2 (CWE-798): re-running the core seeding
 * path must never revert an operator-rotated password. The core seed script is
 * idempotent ({@code ON CONFLICT (login) DO NOTHING}), so a DB restart against
 * existing data keeps the current password hash unchanged. This executes
 * {@code data-core.sql} the same way {@code SeedDataInitializer} does (core
 * module only) and asserts the mutated hash survives.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.sql.init.mode=never",
    "app.seed-data.enabled=false",
    "app.scheduling.signing-window-start=0",
    "app.scheduling.signing-window-end=23",
    "app.scheduling.signing-window-enabled=false",
    "server.ssl.enabled=false"
})
@Sql(executionPhase = Sql.ExecutionPhase.BEFORE_TEST_METHOD, scripts = "classpath:data-test-core.sql",
     config = @SqlConfig(dataSource = "coreDataSource"))
class SeedDataPasswordIdempotencyIntegrationTest {

    private static final String ROTATED_HASH =
            "$2a$10$ROTATEDBYOPERATOR_NOT_THE_DEMO_HASH_0000000000000";

    @Autowired
    @Qualifier("coreDataSource")
    private DataSource coreDataSource;

    @Test
    void reseeding_doesNotRevertRotatedPassword() throws Exception {
        JdbcTemplate jdbc = new JdbcTemplate(coreDataSource);

        assertThat(jdbc.queryForObject("SELECT COUNT(*) FROM users WHERE login = 'doctor1'", Integer.class))
                .as("seeded user must exist before the mutation")
                .isEqualTo(1);

        jdbc.update("UPDATE users SET password_hash = ? WHERE login = 'doctor1'", ROTATED_HASH);

        rerunCoreSeed();

        String hash = jdbc.queryForObject("SELECT password_hash FROM users WHERE login = 'doctor1'", String.class);
        assertThat(hash).as("re-running the core seed must not overwrite the rotated password")
                .isEqualTo(ROTATED_HASH);
    }

    private void rerunCoreSeed() throws Exception {
        try (var connection = coreDataSource.getConnection()) {
            ScriptUtils.executeSqlScript(connection,
                    new EncodedResource(new ClassPathResource("data-core.sql"), StandardCharsets.UTF_8));
        }
    }
}
