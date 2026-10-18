package com.superhumans.config.multidb;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import liquibase.integration.spring.SpringLiquibase;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.superhumans.repository.core",
        entityManagerFactoryRef = "coreEntityManagerFactory",
        transactionManagerRef = "coreTransactionManager")
public class CoreDatabaseConfig {

    @Bean
    @ConfigurationProperties("app.datasource.core")
    public DataSourceProperties coreDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    @Primary
    public DataSource coreDataSource(@Qualifier("coreDataSourceProperties") DataSourceProperties properties) {
        return MultiDatabaseSupport.moduleDataSource(properties);
    }

    /**
     * Transaction/JPA view of the core pool: lazily acquires a connection
     * (#423 item 2) so a request that touches one module DB no longer reserves
     * a connection in all four pools. Liquibase and seeding keep the raw pool.
     */
    @Bean
    public DataSource coreLazyDataSource(@Qualifier("coreDataSource") DataSource dataSource) {
        return MultiDatabaseSupport.lazyTransactionDataSource(dataSource);
    }

    @Bean
    @Primary
    public LocalContainerEntityManagerFactoryBean coreEntityManagerFactory(
            @Qualifier("coreLazyDataSource") DataSource dataSource) {
        return MultiDatabaseSupport.entityManagerFactory(dataSource,
                "com.superhumans.entity.core", "com.superhumans.entity.base");
    }

    @Bean
    public PlatformTransactionManager coreTransactionManager(
            @Qualifier("coreEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return MultiDatabaseSupport.transactionManager(entityManagerFactory);
    }

    @Bean
    public SpringLiquibase coreLiquibase(@Qualifier("coreDataSource") DataSource dataSource) {
        return MultiDatabaseSupport.liquibase(dataSource, "classpath:/db/changelog/db.changelog-master-core.yaml");
    }
}