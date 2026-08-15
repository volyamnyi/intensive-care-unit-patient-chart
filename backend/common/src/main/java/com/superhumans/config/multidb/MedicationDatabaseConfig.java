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
        basePackages = "com.superhumans.medicationsheet.repository",
        entityManagerFactoryRef = "medEntityManagerFactory",
        transactionManagerRef = "medTransactionManager")
public class MedicationDatabaseConfig {

    @Bean
    @ConfigurationProperties("app.datasource.med")
    public DataSourceProperties medDataSourceProperties() {
        return new DataSourceProperties();
    }

    @Bean
    public DataSource medDataSource(@Qualifier("medDataSourceProperties") DataSourceProperties properties) {
        return MultiDatabaseSupport.moduleDataSource(properties);
    }

    @Bean
    public LocalContainerEntityManagerFactoryBean medEntityManagerFactory(
            @Qualifier("medDataSource") DataSource dataSource) {
        return MultiDatabaseSupport.entityManagerFactory(dataSource,
                "com.superhumans.medicationsheet.entity", "com.superhumans.entity.base");
    }

    @Bean
    public PlatformTransactionManager medTransactionManager(
            @Qualifier("medEntityManagerFactory") EntityManagerFactory entityManagerFactory) {
        return MultiDatabaseSupport.transactionManager(entityManagerFactory);
    }

    @Bean
    public SpringLiquibase medLiquibase(@Qualifier("medDataSource") DataSource dataSource) {
        return MultiDatabaseSupport.liquibase(dataSource, "classpath:/db/changelog/db.changelog-master-med.yaml");
    }
}