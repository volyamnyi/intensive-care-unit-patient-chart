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
 * EXPLAIN-based guard for the brak-chain order index (CR-07 #429): the
 * threshold counts fire on every confirmed stage-6 brak and every threshold
 * mail, and they read ALL statuses — the partial unique on non-terminal
 * statuses cannot serve them.
 */
class FlowInstancesOrderIndexIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    @Qualifier("prosthDataSource")
    private DataSource prosthDataSource;

    @Test
    void brakChainCountByOrder_usesTheOrderIndex() {
        String plan = explain("SELECT count(*) FROM prosthetics_flow_instances "
                + "WHERE order_id = '11111111-1111-1111-1111-111111111111'::uuid");

        assertThat(plan).doesNotContain("Seq Scan");
    }

    private String explain(String sql) {
        return new JdbcTemplate(prosthDataSource).execute((ConnectionCallback<String>) con -> {
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
