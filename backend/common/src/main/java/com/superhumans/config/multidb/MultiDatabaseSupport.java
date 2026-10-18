package com.superhumans.config.multidb;

import java.util.Properties;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.jdbc.datasource.LazyConnectionDataSourceProxy;
import org.springframework.orm.jpa.JpaTransactionManager;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.orm.jpa.vendor.HibernateJpaVendorAdapter;

import com.zaxxer.hikari.HikariDataSource;

import liquibase.integration.spring.SpringLiquibase;

final class MultiDatabaseSupport {

    private MultiDatabaseSupport() {
    }

    /**
     * Hikari default pool size, preserved when no override is configured.
     * Every module pool (core/icu/med/prosth) opens up to this many connections
     * per Spring context, so the full integration suite needs
     * contexts × 4 × poolMax backend connections.
     */
    static final int DEFAULT_POOL_MAX = 10;
    static final int DEFAULT_POOL_MIN_IDLE = 1;

    /**
     * Bounds every module Hikari pool to {@code APP_DATASOURCE_POOL_MAX}
     * connections (default {@value #DEFAULT_POOL_MAX}, i.e. current behaviour).
     * The CI backend-integration job runs against stock PostgreSQL
     * ({@code max_connections=100}) where the growing suite of Spring test
     * contexts otherwise exhausts the server ("sorry, too many clients
     * already"); it sets {@code APP_DATASOURCE_POOL_MAX=2}. Production and
     * the E2E backend keep the default.
     */
    static DataSource moduleDataSource(DataSourceProperties properties) {
        int poolMax = resolvePoolMax(System::getenv);
        return moduleDataSource(properties, poolMax, resolvePoolMinIdle(System::getenv, poolMax));
    }

    /**
     * Explicit pool shape, so the {@code APP_DATASOURCE_POOL_MAX} contract can be
     * asserted for all four pools without touching the process environment
     * (#423 proposed test). The env-only entry point above is the production one.
     */
    static DataSource moduleDataSource(DataSourceProperties properties, int poolMax, int minIdle) {
        DataSource dataSource = properties.initializeDataSourceBuilder().build();
        if (dataSource instanceof HikariDataSource hikari) {
            hikari.setConnectionInitSql("SET client_encoding = 'UTF8'");
            hikari.setMaximumPoolSize(poolMax);
            hikari.setMinimumIdle(minIdle);
        }
        return dataSource;
    }

    /**
     * JPA/tx view of a module pool: a {@link LazyConnectionDataSourceProxy}.
     *
     * <p>The chained transaction manager begins a transaction on all four module
     * transaction managers, and {@code DataSourceTransactionManager} acquires a
     * physical connection at {@code doBegin} — so a request touching a single
     * module DB used to reserve one connection in every pool (#423 item 2: the
     * pools saturated at ~10 concurrent requests). With the proxy, a
     * participating manager only opens a connection when a statement is really
     * executed, which removes the ×4 multiplier for every read path that does
     * not touch all four databases.
     *
     * <p>Liquibase and the seed initializer keep the raw pool: they manage DDL
     * and connection lifecycle themselves and must not go through a proxy.
     */
    static DataSource lazyTransactionDataSource(DataSource dataSource) {
        return new LazyConnectionDataSourceProxy(dataSource);
    }

    static int resolvePoolMinIdle(java.util.function.Function<String, String> env, int poolMax) {
        String raw = env.apply("APP_DATASOURCE_POOL_MIN_IDLE");
        if (raw == null || raw.isBlank()) {
            return Math.min(DEFAULT_POOL_MIN_IDLE, poolMax);
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value < 0 ? Math.min(DEFAULT_POOL_MIN_IDLE, poolMax) : Math.min(value, poolMax);
        } catch (NumberFormatException e) {
            return Math.min(DEFAULT_POOL_MIN_IDLE, poolMax);
        }
    }

    static int resolvePoolMax(java.util.function.Function<String, String> env) {
        String raw = env.apply("APP_DATASOURCE_POOL_MAX");
        if (raw == null || raw.isBlank()) {
            return DEFAULT_POOL_MAX;
        }
        try {
            int value = Integer.parseInt(raw.trim());
            return value < 1 ? DEFAULT_POOL_MAX : value;
        } catch (NumberFormatException e) {
            return DEFAULT_POOL_MAX;
        }
    }

    /**
     * SQL echo for local debugging, enabled only via {@code APP_JPA_LOG_SQL}.
     * {@code hibernate.show_sql} writes straight to {@code System.out} — it
     * bypasses the logging system entirely, so the prod profile's
     * {@code org.hibernate.SQL: WARN} override cannot silence it. It must
     * therefore never default to on.
     */
    static boolean resolveLogSql(java.util.function.Function<String, String> env) {
        String raw = env.apply("APP_JPA_LOG_SQL");
        return raw != null && Boolean.parseBoolean(raw.trim());
    }

    static SpringLiquibase liquibase(DataSource dataSource, String changeLog) {
        SpringLiquibase liquibase = new SpringLiquibase();
        liquibase.setDataSource(dataSource);
        liquibase.setChangeLog(changeLog);
        return liquibase;
    }

    static LocalContainerEntityManagerFactoryBean entityManagerFactory(DataSource dataSource,
            String... packagesToScan) {
        LocalContainerEntityManagerFactoryBean entityManagerFactory = new LocalContainerEntityManagerFactoryBean();
        entityManagerFactory.setDataSource(dataSource);
        entityManagerFactory.setPackagesToScan(packagesToScan);
        entityManagerFactory.setPersistenceProviderClass(HibernatePersistenceProvider.class);
        entityManagerFactory.setJpaVendorAdapter(new HibernateJpaVendorAdapter());
        entityManagerFactory.setJpaProperties(jpaProperties(resolveLogSql(System::getenv)));
        return entityManagerFactory;
    }

    static JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    static Properties jpaProperties(boolean logSql) {
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "none");
        properties.setProperty("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        properties.setProperty("hibernate.implicit_naming_strategy",
                "org.springframework.boot.hibernate.SpringImplicitNamingStrategy");
        // Upper bound for JPA-issued queries so a stuck statement can no longer
        // hold a pooled (chained-transaction) connection forever. JdbcTemplate
        // paths (audit retention/backfill) are intentionally unaffected.
        properties.setProperty("jakarta.persistence.query.timeout", "30000");
        if (logSql) {
            properties.setProperty("hibernate.show_sql", "true");
            properties.setProperty("hibernate.format_sql", "true");
            properties.setProperty("hibernate.use_sql_comments", "true");
        }
        return properties;
    }
}
