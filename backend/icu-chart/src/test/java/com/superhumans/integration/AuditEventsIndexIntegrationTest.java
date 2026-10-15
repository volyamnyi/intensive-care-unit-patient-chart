package com.superhumans.integration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.List;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EXPLAIN-based guards for the audit-events index family (CR-07 #420):
 * the stable pagination sort must be served by
 * {@code idx_audit_events_occurred_at (occurred_at DESC, audit_id ASC)}
 * without a Sort node on every page, and the parent-children lookup must
 * use {@code idx_audit_events_parent}.
 */
class AuditEventsIndexIntegrationTest extends AbstractIntegrationTest {

    private static final String U = "'11111111-1111-1111-1111-111111111111'::uuid";

    @Autowired
    @Qualifier("coreDataSource")
    private DataSource coreDataSource;

    @Test
    void stablePagination_walksTheIndexWithoutSorting() {
        String plan = explain("SELECT audit_id FROM audit_events "
                + "ORDER BY occurred_at DESC, audit_id ASC LIMIT 20");

        assertThat(plan).doesNotContain("Seq Scan");
        assertThat(plan).doesNotContain("Sort");
    }

    @Test
    void childrenOfAnEvent_useTheParentIndex() {
        String plan = explain("SELECT audit_id FROM audit_events "
                + "WHERE parent_audit_id = " + U + " ORDER BY occurred_at ASC");

        assertThat(plan).doesNotContain("Seq Scan");
    }

    @Test
    void actorLoginSearch_usesTheActorLoginIndex() {
        String plan = explain("SELECT audit_id FROM audit_events "
                + "WHERE actor_login = 'doctor1' ORDER BY occurred_at DESC, audit_id ASC LIMIT 20");

        assertThat(plan).doesNotContain("Seq Scan");
    }

    private String explain(String sql) {
        return new JdbcTemplate(coreDataSource).execute((ConnectionCallback<String>) con -> {
            try (Statement setup = con.createStatement()) {
                setup.execute("SET enable_seqscan = off");
            }
            try (PreparedStatement statement = con.prepareStatement("EXPLAIN (COSTS OFF) " + sql);
                 ResultSet rows = statement.executeQuery()) {
                List<String> lines = new ArrayList<>();
                while (rows.next()) {
                    lines.add(rows.getString(1));
                }
                return String.join("\n", lines);
            }
        });
    }
}
