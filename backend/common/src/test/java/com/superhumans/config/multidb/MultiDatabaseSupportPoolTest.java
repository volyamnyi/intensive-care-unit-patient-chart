package com.superhumans.config.multidb;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.zaxxer.hikari.HikariDataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.function.Function;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;

/**
 * #423: the runbook documents {@code APP_DATASOURCE_POOL_MAX}, so the knob must
 * reach all four hand-built pools — and the ×4 connection multiplier of the
 * chained transaction manager must be gone.
 */
class MultiDatabaseSupportPoolTest {

    private static final Map<String, String> URLS = Map.of(
            "core", "jdbc:postgresql://localhost:5432/my_fullstack_core",
            "icu", "jdbc:postgresql://localhost:5432/my_fullstack_icu",
            "med", "jdbc:postgresql://localhost:5432/my_fullstack_med",
            "prosth", "jdbc:postgresql://localhost:5432/my_fullstack_prosth");

    private static DataSourceProperties properties(String module) {
        DataSourceProperties properties = new DataSourceProperties();
        properties.setUrl(URLS.get(module));
        properties.setUsername("postgres");
        properties.setPassword("postgres");
        return properties;
    }

    @Test
    void everyModulePoolHonoursTheSameMaximumPoolSize() {
        // Guards against a future re-introduction of the wrong
        // spring.datasource.hikari.* prefix: these pools are hand-built.
        for (String module : URLS.keySet()) {
            DataSource dataSource = MultiDatabaseSupport.moduleDataSource(properties(module), 3, 1);
            assertThat(dataSource).isInstanceOf(HikariDataSource.class);
            HikariDataSource hikari = (HikariDataSource) dataSource;
            assertThat(hikari.getMaximumPoolSize())
                    .describedAs("%s pool must honour the configured maximum", module)
                    .isEqualTo(3);
            assertThat(hikari.getMinimumIdle()).isLessThanOrEqualTo(3);
            hikari.close();
        }
    }

    @Test
    void poolEnvKnobResolvesToTheSameValueForEveryPool() {
        Function<String, String> env = key -> "APP_DATASOURCE_POOL_MAX".equals(key) ? "7"
                : "APP_DATASOURCE_POOL_MIN_IDLE".equals(key) ? "2" : null;
        int max = MultiDatabaseSupport.resolvePoolMax(env);

        for (String module : URLS.keySet()) {
            DataSource dataSource = MultiDatabaseSupport.moduleDataSource(
                    properties(module), max, MultiDatabaseSupport.resolvePoolMinIdle(env, max));
            HikariDataSource hikari = (HikariDataSource) dataSource;
            assertThat(hikari.getMaximumPoolSize()).isEqualTo(7);
            assertThat(hikari.getMinimumIdle()).isEqualTo(2);
            hikari.close();
        }
    }

    /**
     * The multiplier fix (#423 item 2): the handle a participating transaction
     * manager receives is backed by a lazily obtained pool connection, so a
     * transaction that never executes a statement never checks one out.
     */
    @Test
    void lazyProxyDefersThePoolCheckoutUntilAStatementRuns() throws SQLException {
        DataSource target = mock(DataSource.class);
        // Spring probes default connection properties with one checkout, then the
        // handle uses the next one — two distinct mocks prove which is which.
        Connection probe = mock(Connection.class);
        Connection handle = mock(Connection.class);
        when(target.getConnection()).thenReturn(probe, handle);

        DataSource lazy = MultiDatabaseSupport.lazyTransactionDataSource(target);
        assertThat(lazy).isInstanceOf(LazyConnectionDataSourceProxy.class);

        lazy.getConnection().close();

        // An unused handle runs no SQL: only the defaults probe touched the pool.
        verify(target, times(1)).getConnection();
        verify(probe, never()).prepareStatement("select 1");
        verify(handle, never()).prepareStatement("select 1");
    }

    @Test
    void lazyProxyRunsStatementsOnTheConnectionItObtained() throws SQLException {
        DataSource target = mock(DataSource.class);
        Connection probe = mock(Connection.class);
        Connection handle = mock(Connection.class);
        when(target.getConnection()).thenReturn(probe, handle);

        DataSource lazy = MultiDatabaseSupport.lazyTransactionDataSource(target);
        lazy.getConnection().prepareStatement("select 1");

        verify(handle).prepareStatement("select 1");
        assertThat(target).isNotSameAs(lazy);
    }
}