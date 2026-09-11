package com.superhumans.config.multidb;

import java.util.Properties;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import org.hibernate.jpa.HibernatePersistenceProvider;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
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
        DataSource dataSource = properties.initializeDataSourceBuilder().build();
        if (dataSource instanceof HikariDataSource hikari) {
            hikari.setConnectionInitSql("SET client_encoding = 'UTF8'");
            int poolMax = resolvePoolMax(System::getenv);
            hikari.setMaximumPoolSize(poolMax);
            hikari.setMinimumIdle(poolMax);
        }
        return dataSource;
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
        entityManagerFactory.setJpaProperties(jpaProperties());
        return entityManagerFactory;
    }

    static JpaTransactionManager transactionManager(EntityManagerFactory entityManagerFactory) {
        return new JpaTransactionManager(entityManagerFactory);
    }

    private static Properties jpaProperties() {
        Properties properties = new Properties();
        properties.setProperty("hibernate.dialect", "org.hibernate.dialect.PostgreSQLDialect");
        properties.setProperty("hibernate.hbm2ddl.auto", "none");
        properties.setProperty("hibernate.physical_naming_strategy",
                "org.hibernate.boot.model.naming.CamelCaseToUnderscoresNamingStrategy");
        properties.setProperty("hibernate.implicit_naming_strategy",
                "org.springframework.boot.hibernate.SpringImplicitNamingStrategy");
        properties.setProperty("hibernate.show_sql", "true");
        properties.setProperty("hibernate.format_sql", "true");
        properties.setProperty("hibernate.use_sql_comments", "true");
        properties.setProperty("hibernate.generate_statistics", "true");
        return properties;
    }
}