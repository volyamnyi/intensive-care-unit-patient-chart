package com.superhumans.integration;

import com.zaxxer.hikari.HikariDataSource;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Guards the runbook pool contract (#423): the four module pools are built
 * programmatically, so {@code spring.datasource.hikari.*} does NOT apply to
 * them — {@code APP_DATASOURCE_POOL_MAX} is the only supported knob and it
 * must reach every pool. CI runs with {@code APP_DATASOURCE_POOL_MAX=2};
 * local/dev without the variable must resolve the default of 10.
 */
class MultiDatabasePoolWiringIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    @Qualifier("coreDataSource")
    private DataSource coreDataSource;

    @Autowired
    @Qualifier("icuDataSource")
    private DataSource icuDataSource;

    @Autowired
    @Qualifier("medDataSource")
    private DataSource medDataSource;

    @Autowired
    @Qualifier("prosthDataSource")
    private DataSource prosthDataSource;

    @Test
    void allFourModulePools_honourAppDatasourcePoolMax() {
        // Mirrors MultiDatabaseSupport.resolvePoolMax (package-private): the
        // default is 10, a non-positive or malformed override falls back to it.
        String raw = System.getenv("APP_DATASOURCE_POOL_MAX");
        int expected = 10;
        if (raw != null && !raw.isBlank()) {
            try {
                int parsed = Integer.parseInt(raw.trim());
                expected = parsed < 1 ? 10 : parsed;
            } catch (NumberFormatException ignored) {
                // malformed override keeps the default
            }
        }

        assertThat(((HikariDataSource) coreDataSource).getMaximumPoolSize())
                .as("core pool must match APP_DATASOURCE_POOL_MAX").isEqualTo(expected);
        assertThat(((HikariDataSource) icuDataSource).getMaximumPoolSize())
                .as("icu pool must match APP_DATASOURCE_POOL_MAX").isEqualTo(expected);
        assertThat(((HikariDataSource) medDataSource).getMaximumPoolSize())
                .as("med pool must match APP_DATASOURCE_POOL_MAX").isEqualTo(expected);
        assertThat(((HikariDataSource) prosthDataSource).getMaximumPoolSize())
                .as("prosth pool must match APP_DATASOURCE_POOL_MAX").isEqualTo(expected);
    }
}
