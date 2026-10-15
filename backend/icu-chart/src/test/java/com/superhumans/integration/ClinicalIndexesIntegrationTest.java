package com.superhumans.integration;

import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.sql.Statement;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.ConnectionCallback;
import org.springframework.jdbc.core.JdbcTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * EXPLAIN-based guard for the ICU secondary indexes (CR-07 #419): the
 * patient-day screen queries must have an index path. {@code enable_seqscan =
 * off} forces the planner to prefer any usable index — if an index is missing
 * or INVALID the plan still (and only) shows a Seq Scan and the assertion
 * fails, so this pins both existence and usability.
 */
class ClinicalIndexesIntegrationTest extends AbstractIntegrationTest {

    private static final String U = "'11111111-1111-1111-1111-111111111111'::uuid";

    @Autowired
    @Qualifier("icuDataSource")
    private DataSource icuDataSource;

    @Test
    void dayPanelQueries_haveIndexPlans() {
        Map<String, String> queries = new LinkedHashMap<>();
        queries.put("episodes by patient", "SELECT id FROM episodes WHERE patient_id = 900001");
        queries.put("episodes by status+patient",
                "SELECT id FROM episodes WHERE status = 'ACTIVE' AND patient_id = 900001");
        queries.put("clinical days by episode order dayNumber",
                "SELECT id FROM clinical_days WHERE episode_id = " + U + " ORDER BY day_number");
        queries.put("medical orders by day",
                "SELECT id FROM medical_orders WHERE clinical_day_id = " + U);
        queries.put("medical notes by day",
                "SELECT id FROM medical_notes WHERE clinical_day_id = " + U);
        queries.put("scale results by day",
                "SELECT id FROM scale_results WHERE clinical_day_id = " + U);
        queries.put("scale results by episode",
                "SELECT id FROM scale_results WHERE episode_id = " + U);
        queries.put("fluid balances by day order hour",
                "SELECT id FROM fluid_balances WHERE clinical_day_id = " + U + " ORDER BY hour");
        queries.put("signatures by day",
                "SELECT id FROM signatures WHERE clinical_day_id = " + U);
        queries.put("generated pdfs latest version",
                "SELECT id FROM generated_pdfs WHERE clinical_day_id = " + U
                        + " ORDER BY file_version DESC");
        queries.put("lab results by day",
                "SELECT id FROM lab_results WHERE clinical_day_id = " + U);
        queries.put("ventilation by day+hour",
                "SELECT id FROM ventilation_settings WHERE clinical_day_id = " + U
                        + " AND record_hour = 7");
        queries.put("patient state by day+hour",
                "SELECT id FROM patient_state_assessments WHERE clinical_day_id = " + U
                        + " AND record_hour = 7");

        Map<String, String> violations = new LinkedHashMap<>();
        for (Map.Entry<String, String> check : queries.entrySet()) {
            String plan = explain(icuDataSource, check.getValue());
            if (plan.contains("Seq Scan")) {
                violations.put(check.getKey(), plan);
            }
        }
        assertThat(violations)
                .as("queries without a usable index (Seq Scan under enable_seqscan=off)")
                .isEmpty();
    }

    @Test
    void autoCloseRows_keepTheirUniqueIndex() {
        String plan = explain(icuDataSource,
                "SELECT id FROM hourly_records WHERE clinical_day_id = " + U + " AND record_hour = 7");
        assertThat(plan).doesNotContain("Seq Scan");
    }

    private String explain(DataSource dataSource, String sql) {
        return new JdbcTemplate(dataSource).execute((ConnectionCallback<String>) con -> {
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
