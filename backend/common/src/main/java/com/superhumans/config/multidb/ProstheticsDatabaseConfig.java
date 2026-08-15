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
        basePackages = "com.superhumans.prosthesismanufacturing.repository",
        entityManagerFactoryRef = "prosthEntityManagerFactory",
        transactionManagerRef = "prosthTransactionManager")
public class ProstheticsDatabaseConfig {

    @Bean
    @ConfigurationProperties("app.datasource.prosth")
    public DataSourceProperties prosthDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource prosthDataSource(@Qualifier("prosthDataSourceProperties") DataSourceProperties properties) {
        return MultiDatabaseSupport.moduleDataSource(properties);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean prosthEntityManagerFactory(
            @Qualifier("prosthDataSource") DataSource dataSource) {
        return MultiDatabaseSupport.entityManagerFactory(dataSource,
                "com.superhumans.prosthesismanufacturing.entity", "com.superhumans.entity.base");
    }

    @Bean
    public PlatformTransactionManager prosthTransactionManager(
            @Qualifier("prosthEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return MultiDatabaseSupport.transactionManager(entityManagerFactory);
    }

    @Bean
    public SpringLiquibase prosthLiquibase(@Qualifier("prosthDataSource") DataSource dataSource) {
        return MultiDatabaseSupport.liquibase(dataSource,
                "classpath:/db/changelog/db.changelog-master-prosth.yaml");
    }
}