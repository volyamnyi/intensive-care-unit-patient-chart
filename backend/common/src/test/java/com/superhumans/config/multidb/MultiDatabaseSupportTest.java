package com.superhumans.config.multidb;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Map;
import java.util.Properties;

import org.junit.jupiter.api.Test;

class MultiDatabaseSupportTest {

    @Test
    void resolvePoolMax_defaultsWhenUnset() {
        assertEquals(MultiDatabaseSupport.DEFAULT_POOL_MAX,
                MultiDatabaseSupport.resolvePoolMax(key -> null));
    }

    @Test
    void resolvePoolMax_defaultsWhenBlank() {
        assertEquals(MultiDatabaseSupport.DEFAULT_POOL_MAX,
                MultiDatabaseSupport.resolvePoolMax(key -> "   "));
    }

    @Test
    void resolvePoolMax_defaultsWhenInvalid() {
        assertEquals(MultiDatabaseSupport.DEFAULT_POOL_MAX,
                MultiDatabaseSupport.resolvePoolMax(key -> "lots"));
    }

    @Test
    void resolvePoolMax_defaultsWhenNonPositive() {
        assertEquals(MultiDatabaseSupport.DEFAULT_POOL_MAX,
                MultiDatabaseSupport.resolvePoolMax(key -> "0"));
        assertEquals(MultiDatabaseSupport.DEFAULT_POOL_MAX,
                MultiDatabaseSupport.resolvePoolMax(key -> "-3"));
    }

    @Test
    void resolvePoolMax_honoursOverride() {
        Map<String, String> env = Map.of("APP_DATASOURCE_POOL_MAX", " 2 ");
        assertEquals(2, MultiDatabaseSupport.resolvePoolMax(env::get));
    }

    @Test
    void resolvePoolMinIdle_defaultsToOneAndNeverExceedsMax() {
        assertEquals(1, MultiDatabaseSupport.resolvePoolMinIdle(key -> null, 10));
        assertEquals(1, MultiDatabaseSupport.resolvePoolMinIdle(key -> null, 1));
        assertEquals(1, MultiDatabaseSupport.resolvePoolMinIdle(key -> "invalid", 10));
        assertEquals(3, MultiDatabaseSupport.resolvePoolMinIdle(key -> "3", 10));
        assertEquals(5, MultiDatabaseSupport.resolvePoolMinIdle(key -> "8", 5));
        assertEquals(0, MultiDatabaseSupport.resolvePoolMinIdle(key -> "0", 5));
    }

    @Test
    void resolveLogSql_defaultsWhenUnset() {
        assertFalse(MultiDatabaseSupport.resolveLogSql(key -> null));
    }

    @Test
    void resolveLogSql_defaultsWhenBlank() {
        assertFalse(MultiDatabaseSupport.resolveLogSql(key -> "   "));
    }

    @Test
    void resolveLogSql_enabledByEnv() {
        assertTrue(MultiDatabaseSupport.resolveLogSql(key -> " true "));
    }

    @Test
    void jpaProperties_sqlEchoOffByDefault() {
        Properties properties = MultiDatabaseSupport.jpaProperties(false);
        assertNull(properties.getProperty("hibernate.show_sql"));
        assertNull(properties.getProperty("hibernate.format_sql"));
        assertNull(properties.getProperty("hibernate.use_sql_comments"));
        assertNull(properties.getProperty("hibernate.generate_statistics"));
        assertEquals("30000", properties.getProperty("jakarta.persistence.query.timeout"));
    }

    @Test
    void jpaProperties_sqlEchoOnlyWhenAsked() {
        Properties properties = MultiDatabaseSupport.jpaProperties(true);
        assertEquals("true", properties.getProperty("hibernate.show_sql"));
        assertEquals("true", properties.getProperty("hibernate.format_sql"));
        assertEquals("true", properties.getProperty("hibernate.use_sql_comments"));
        assertNull(properties.getProperty("hibernate.generate_statistics"));
    }
}
