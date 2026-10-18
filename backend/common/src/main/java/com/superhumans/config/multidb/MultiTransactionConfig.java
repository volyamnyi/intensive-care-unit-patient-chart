package com.superhumans.config.multidb;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Primary;
import org.springframework.data.transaction.ChainedTransactionManager;
import org.springframework.transaction.PlatformTransactionManager;

/**
 * The default transaction manager for all four databases.
 *
 * <p><b>This is not 2PC.</b> {@link ChainedTransactionManager} begins the legs
 * core → icu → med → prosth and commits them in reverse. A runtime exception in a
 * method body rolls every leg back, but a failure <em>during</em> the commit phase
 * of a later leg leaves the earlier legs already committed — a cross-database
 * partial state between business data and its audit trail. The window is narrow
 * (deferred constraints, serialization failures, a dropped connection) and was hit
 * in practice as "mixed outcome" in issue #321.
 *
 * <p>Why the deprecated manager is still here: replacing it properly means 2PC
 * (JTA/Atomikos), which is disproportionate for this deployment. The residual is
 * accepted and documented — see {@code docs/Production-Deployment-Runbook.md} §F.9
 * and the {@code CrossDatabaseWriteInventoryTest} that lists every service writing
 * to a feature database and the core audit table inside one transaction.
 *
 * <p><b>Trap: do not "fix" an audit call with {@code REQUIRES_NEW}.</b>
 * {@code MultiTransactionStatus.registerTransactionManager} forwards the
 * {@code TransactionDefinition} to <em>every</em> leg, so a {@code REQUIRES_NEW}
 * definition would suspend the four open legs and begin four <em>new</em> ones —
 * eight pooled connections for a single audit write (CI bounds each pool to 2) and a
 * write that commits even when the business transaction rolls back. The safe lever
 * is the {@code app.audit.legacy-write-enabled} kill switch in {@code AuditService},
 * which is the single choke point for the core-side legacy audit leg.
 */
@Configuration
public class MultiTransactionConfig {

    private static final Logger log = LoggerFactory.getLogger(MultiTransactionConfig.class);

    @Bean
    @Primary
    public PlatformTransactionManager transactionManager(
            @Qualifier("coreTransactionManager") PlatformTransactionManager coreTransactionManager,
            @Qualifier("icuTransactionManager") PlatformTransactionManager icuTransactionManager,
            @Qualifier("medTransactionManager") PlatformTransactionManager medTransactionManager,
            @Qualifier("prosthTransactionManager") PlatformTransactionManager prosthTransactionManager) {
        log.info("Multi-DB transaction manager: chained (core -> icu -> med -> prosth), not 2PC. "
                + "A commit-phase failure on a later leg leaves earlier legs committed - "
                + "see Production-Deployment-Runbook section F.9. To shrink the window, disable the "
                + "core-side legacy audit leg with app.audit.legacy-write-enabled=false.");
        return new ChainedTransactionManager(
                coreTransactionManager, icuTransactionManager, medTransactionManager, prosthTransactionManager);
    }
}