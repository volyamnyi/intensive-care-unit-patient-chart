package com.superhumans.config.multidb;

import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

@Configuration
public class MultiTransactionConfig {

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("coreTransactionManager") PlatformTransactionManager coreTransactionManager,
            @Qualifier("icuTransactionManager") PlatformTransactionManager icuTransactionManager,
            @Qualifier("medTransactionManager") PlatformTransactionManager medTransactionManager,
            @Qualifier("prosthTransactionManager") PlatformTransactionManager prosthTransactionManager) {
        return new ChainedTransactionManager(
                coreTransactionManager, icuTransactionManager, medTransactionManager, prosthTransactionManager);
    }
}