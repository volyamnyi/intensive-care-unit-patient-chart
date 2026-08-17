package com.superhumans.config.multidb;

import javax.sql.DataSource;

import jakarta.persistence.EntityManagerFactory;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.boot.jdbc.autoconfigure.DataSourceProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.data.jpa.repository.config.EnableJpaRepositories;
import org.springframework.orm.jpa.LocalContainerEntityManagerFactoryBean;
import org.springframework.transaction.PlatformTransactionManager;

import liquibase.integration.spring.SpringLiquibase;

@Configuration
@EnableJpaRepositories(
        basePackages = "com.superhumans.icu.repository",
        entityManagerFactoryRef = "icuEntityManagerFactory",
        transactionManagerRef = "icuTransactionManager")
public class IcuDatabaseConfig {

    @Bean
    @ConfigurationProperties("app.datasource.icu")
    public DataSourceProperties icuDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource icuDataSource(@Qualifier("icuDataSourceProperties") DataSourceProperties properties) {
        return MultiDatabaseSupport.moduleDataSource(properties);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean icuEntityManagerFactory(
            @Qualifier("icuDataSource") DataSource dataSource) {
        return MultiDatabaseSupport.entityManagerFactory(dataSource,
                "com.superhumans.icu.entity", "com.superhumans.entity.base");
    }

    @Bean
    public PlatformTransactionManager icuTransactionManager(
            @Qualifier("icuEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return MultiDatabaseSupport.transactionManager(entityManagerFactory);
    }

    @Bean
    public SpringLiquibase icuLiquibase(@Qualifier("icuDataSource") DataSource dataSource) {
        return MultiDatabaseSupport.liquibase(dataSource, "classpath:/db/changelog/db.changelog-master-icu.yaml");
    }
}