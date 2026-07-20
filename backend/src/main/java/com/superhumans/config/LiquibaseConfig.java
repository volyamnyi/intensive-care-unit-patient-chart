package com.superhumans.config;

import liquibase.integration.spring.SpringLiquibase;
import org.springframework.beans.BeansException;
import org.springframework.beans.factory.config.BeanDefinition;
import org.springframework.beans.factory.config.BeanFactoryPostProcessor;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.beans.factory.InitializingBean;

import javax.sql.DataSource;

@Configuration
public class LiquibaseConfig {

    @Bean
    public InitializingBean liquibaseMigrator(DataSource dataSource) {
        return () -> {
            SpringLiquibase liquibase = new SpringLiquibase();
            liquibase.setDataSource(dataSource);
            liquibase.setChangeLog("classpath:db/changelog/db.changelog-master.yaml");
            liquibase.afterPropertiesSet();
        };
    }

    @Bean
    public static BeanFactoryPostProcessor liquibaseDependsOnPostProcessor() {
        return (ConfigurableListableBeanFactory beanFactory) -> {
            BeanDefinition initializer = beanFactory.getBeanDefinition("dataSourceScriptDatabaseInitializer");
            initializer.setDependsOn("liquibaseMigrator");
        };
    }
}
