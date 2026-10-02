package com.superhumans.config;

import com.superhumans.audit.AuditEventSerializer;
import com.superhumans.audit.AuditOutboxJdbcStore;
import com.superhumans.audit.AuditOutboxStore;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.RequiredArgsConstructor;
import lombok.experimental.FieldDefaults;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.transaction.PlatformTransactionManager;

/** Creates one local-outbox adapter per feature database without cross-module dependencies. */
@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditOutboxConfiguration {

    AuditEventSerializer serializer;

    @Bean(name = {"icuAuditOutboxStore", "icuAuditEventWriter"})
    public AuditOutboxStore icuAuditOutboxStore(
            @Qualifier("icuDataSource") DataSource dataSource,
            @Qualifier("icuTransactionManager") PlatformTransactionManager transactionManager) {
        return new AuditOutboxJdbcStore("icu", dataSource, transactionManager, serializer);
    }

    @Bean(name = {"medicationAuditOutboxStore", "medicationAuditEventWriter"})
    public AuditOutboxStore medicationAuditOutboxStore(
            @Qualifier("medDataSource") DataSource dataSource,
            @Qualifier("medTransactionManager") PlatformTransactionManager transactionManager) {
        return new AuditOutboxJdbcStore("medication", dataSource, transactionManager, serializer);
    }

    @Bean(name = {"prostheticsAuditOutboxStore", "prostheticsAuditEventWriter"})
    public AuditOutboxStore prostheticsAuditOutboxStore(
            @Qualifier("prosthDataSource") DataSource dataSource,
            @Qualifier("prosthTransactionManager") PlatformTransactionManager transactionManager) {
        return new AuditOutboxJdbcStore("prosthetics", dataSource, transactionManager, serializer);
    }
}
