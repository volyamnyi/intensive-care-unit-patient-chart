package com.superhumans.config.multidb;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.Map;

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
}
