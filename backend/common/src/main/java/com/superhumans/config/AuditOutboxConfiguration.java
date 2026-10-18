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

/**
 * Creates one local-outbox adapter per feature database without cross-module dependencies.
 *
 * <p>Each store MUST see the same DataSource view as its module's transaction
 * manager (the lazy view from #423). Pointing a store at the raw pool while the
 * EMF/tx manager runs on the lazy proxy makes the outbox INSERT take a second
 * connection outside the business transaction - the row then survives a
 * rollback (phantom event for a write that never happened) and the F8 invariant
 * "the outbox joins the feature-DB leg" is gone. Caught by
 * {@code AuditOutboxIntegrationTest.rollbackOfBusinessTransactionRollsBackItsAuditOutboxRow}.
 */
@Configuration
@RequiredArgsConstructor
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditOutboxConfiguration {

    AuditEventSerializer serializer;

    @Bean(name = {"icuAuditOutboxStore", "icuAuditEventWriter"})
    public AuditOutboxStore icuAuditOutboxStore(
            @Qualifier("icuLazyDataSource") DataSource dataSource,
            @Qualifier("icuTransactionManager") PlatformTransactionManager transactionManager) {
        return new AuditOutboxJdbcStore("icu", dataSource, transactionManager, serializer);
    }

    @Bean(name = {"medicationAuditOutboxStore", "medicationAuditEventWriter"})
    public AuditOutboxStore medicationAuditOutboxStore(
            @Qualifier("medLazyDataSource") DataSource dataSource,
            @Qualifier("medTransactionManager") PlatformTransactionManager transactionManager) {
        return new AuditOutboxJdbcStore("medication", dataSource, transactionManager, serializer);
    }

    @Bean(name = {"prostheticsAuditOutboxStore", "prostheticsAuditEventWriter"})
    public AuditOutboxStore prostheticsAuditOutboxStore(
            @Qualifier("prosthLazyDataSource") DataSource dataSource,
            @Qualifier("prosthTransactionManager") PlatformTransactionManager transactionManager) {
        return new AuditOutboxJdbcStore("prosthetics", dataSource, transactionManager, serializer);
    }
}
