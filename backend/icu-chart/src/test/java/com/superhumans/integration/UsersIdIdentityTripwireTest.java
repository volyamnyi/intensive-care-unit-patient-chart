package com.superhumans.integration;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import java.util.Map;
import javax.sql.DataSource;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;

/**
 * Schema pin for the {@code users.id} identity defect that broke every restart
 * of a seeded database with
 * {@code duplicate key value violates unique constraint "users_pkey"}.
 *
 * <p>{@code core/001-initial.sql} created the column as {@code BIGSERIAL} (a
 * bare {@code nextval} default, {@code is_identity = NO}) while
 * {@code User.id} declares {@code @GeneratedValue(strategy = IDENTITY)}.
 * Hibernate then hands ids to such a column from an in-memory block without
 * advancing the sequence, so {@code users_id_seq} could lag behind
 * {@code MAX(id)} and the next {@code nextval()} returned an id that was already
 * taken — {@code UserSeedService} then aborted the whole application context.
 *
 * <p>Written as a general rule rather than a single-column assertion: <b>no</b>
 * table may carry a {@code nextval} default without being a real identity
 * column — that shape is what let Hibernate and PostgreSQL disagree about who
 * owns the id. With a real identity column PostgreSQL allocates every id it
 * hands out, so the sequence can no longer drift behind its own table.
 */
@SpringBootTest(webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT, properties = {
    "spring.sql.init.mode=never",
    "app.seed-data.enabled=false",
    "app.scheduling.signing-window-start=0",
    "app.scheduling.signing-window-end=23",
    "app.scheduling.signing-window-enabled=false",
    "server.ssl.enabled=false",
    "app.mis.api.base-url=http://localhost:9090"
})
class UsersIdIdentityTripwireTest {

    private static final String PROBE_LOGIN_PREFIX = "tripwire-explicit-";

    @Autowired
    @Qualifier("coreDataSource")
    private DataSource coreDataSource;

    private JdbcTemplate jdbc() {
        return new JdbcTemplate(coreDataSource);
    }

    /**
     * The probe rows are removed explicitly instead of relying on test-managed
     * transactions: this class runs against a shared development database, and
     * the multi-DB chained transaction manager does not give test methods a
     * rollback. {@code nextval} is non-transactional anyway, so a rollback
     * would leave the sequence ahead while the row vanished.
     */
    @AfterEach
    void removeProbeRows() {
        jdbc().update("DELETE FROM users WHERE login LIKE ?", PROBE_LOGIN_PREFIX + "%");
    }

    @Test
    void usersId_isARealIdentityColumn() {
        Boolean identity = jdbc().queryForObject(
                "SELECT is_identity FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND table_name = 'users' AND column_name = 'id'",
                Boolean.class);

        assertThat(identity)
                .describedAs("users.id must be a real identity column (core/015-users-id-identity.sql)")
                .isTrue();
    }

    @Test
    void noTableUsesABareSequenceDefaultWithoutBeingAnIdentityColumn() {
        List<Map<String, Object>> offenders = jdbc().queryForList(
                "SELECT table_name, column_default FROM information_schema.columns "
                        + "WHERE table_schema = 'public' AND is_identity = 'NO' "
                        + "AND column_default LIKE 'nextval(%'");

        assertThat(offenders)
                .describedAs("a bare nextval default without an identity column lets Hibernate "
                        + "and PostgreSQL disagree about id allocation (users.id was this bug)")
                .isEmpty();
    }

    /**
     * The reported failure, end to end: rows with explicit ids exist (the
     * fixture/seed pattern), the sequence is synchronised the way the migration
     * does it, and a generated insert must still succeed and land on a fresh id.
     */
    @Test
    void generatedInsertSucceedsNextToExplicitIds() {
        Long maxBefore = jdbc().queryForObject("SELECT COALESCE(MAX(id), 0) FROM users", Long.class);
        long max = maxBefore == null ? 0L : maxBefore;

        jdbc().update("INSERT INTO users (id, login, full_name, role, auth_provider, created_at, version) "
                        + "VALUES (?, ?, ?, 'GUEST', 'LOCAL', NOW(), 0)",
                max + 1, PROBE_LOGIN_PREFIX + (max + 1), "Tripwire " + (max + 1));

        // Exactly what core/015 does at deploy time.
        jdbc().execute("SELECT setval(pg_get_serial_sequence('users', 'id'), "
                + "GREATEST(COALESCE((SELECT MAX(id) FROM users), 0), 1), true)");

        // No explicit id: PostgreSQL must allocate a fresh, unused one.
        jdbc().update("INSERT INTO users (login, full_name, role, auth_provider, created_at, version) "
                + "VALUES (?, ?, 'GUEST', 'LOCAL', NOW(), 0)",
                PROBE_LOGIN_PREFIX + "generated", "Tripwire generated");

        Long maxAfter = jdbc().queryForObject("SELECT COALESCE(MAX(id), 0) FROM users", Long.class);
        assertThat(maxAfter).isEqualTo(max + 2);
        assertThat(jdbc().queryForObject(
                "SELECT COUNT(*) FROM users WHERE login LIKE ?", Integer.class, PROBE_LOGIN_PREFIX + "%"))
                .isEqualTo(2);
    }
}