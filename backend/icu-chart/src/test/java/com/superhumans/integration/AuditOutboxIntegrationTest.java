package com.superhumans.integration;

import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventWriter;
import com.superhumans.audit.AuditOutboxStore;
import com.superhumans.repository.core.AuditEventRepository;
import com.superhumans.repository.core.AuditEventTargetRepository;
import java.util.Set;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.TestPropertySource;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@TestPropertySource(properties = "app.audit.relay.poll-ms=60000")
class AuditOutboxIntegrationTest extends AbstractIntegrationTest {

    @Autowired
    @Qualifier("icuAuditEventWriter")
    private AuditEventWriter icuAuditEventWriter;

    @Autowired
    @Qualifier("icuAuditOutboxStore")
    private AuditOutboxStore icuAuditOutboxStore;

    @Autowired
    @Qualifier("icuTransactionManager")
    private PlatformTransactionManager icuTransactionManager;

    @Autowired
    @Qualifier("icuDataSource")
    private javax.sql.DataSource icuDataSource;

    @Autowired
    private AuditEventRepository auditEventRepository;

    @Autowired
    private AuditEventTargetRepository auditEventTargetRepository;

    @Autowired
    private com.superhumans.service.AuditEventRelay auditEventRelay;

    @Test
    void businessTransactionOutboxRelayAndCrossEntityHistoryAreAtomicAndIdempotent() {
        UUID auditId = UUID.randomUUID();
        UUID episodeId = UUID.randomUUID();
        UUID dayId = UUID.randomUUID();
        AuditEvent event = event(auditId, episodeId, dayId);
        JdbcTemplate icuJdbc = new JdbcTemplate(icuDataSource);
        TransactionTemplate transaction = new TransactionTemplate(icuTransactionManager);

        transaction.executeWithoutResult(status -> icuAuditEventWriter.append(event));
        assertThat(icuJdbc.queryForObject(
                "SELECT relay_status FROM audit_outbox WHERE audit_id = ?", String.class, auditId))
                .isEqualTo("PENDING");

        auditEventRelay.poll();
        assertThat(auditEventRepository.findById(auditId)).isPresent();
        assertThat(auditEventTargetRepository
                .findByEntityTypeAndEntityIdOrderByOccurredAtDesc("ClinicalDay", dayId.toString()))
                .hasSize(1);
        assertThat(icuJdbc.queryForObject(
                "SELECT relay_status FROM audit_outbox WHERE audit_id = ?", String.class, auditId))
                .isEqualTo("DELIVERED");

        // Simulate a relay crash after the core commit but before its local acknowledgement.
        // At-least-once replay must remain idempotent in the canonical store.
        icuJdbc.update("UPDATE audit_outbox SET relay_status = 'PENDING', delivered_at = NULL "
                + "WHERE audit_id = ?", auditId);
        auditEventRelay.poll();
        assertThat(auditEventRepository.findAllById(Set.of(auditId))).hasSize(1);
        assertThat(auditEventTargetRepository
                .findByEntityTypeAndEntityIdOrderByOccurredAtDesc("ClinicalDay", dayId.toString()))
                .hasSize(1);
    }

    @Test
    void rollbackOfBusinessTransactionRollsBackItsAuditOutboxRow() {
        UUID auditId = UUID.randomUUID();
        JdbcTemplate icuJdbc = new JdbcTemplate(icuDataSource);
        TransactionTemplate transaction = new TransactionTemplate(icuTransactionManager);

        assertThatThrownBy(() -> transaction.executeWithoutResult(status -> {
            icuAuditEventWriter.append(event(auditId, UUID.randomUUID(), UUID.randomUUID()));
            throw new IllegalStateException("test rollback");
        })).isInstanceOf(IllegalStateException.class).hasMessage("test rollback");

        assertThat(icuJdbc.queryForObject(
                "SELECT count(*) FROM audit_outbox WHERE audit_id = ?", Long.class, auditId)).isZero();
        assertThat(auditEventRepository.findById(auditId)).isEmpty();
    }

    @Test
    void outboxAppendWithoutModuleTransactionFailsClosed() {
        UUID auditId = UUID.randomUUID();
        JdbcTemplate icuJdbc = new JdbcTemplate(icuDataSource);

        assertThatThrownBy(() -> icuAuditEventWriter.append(
                event(auditId, UUID.randomUUID(), UUID.randomUUID())))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("active business transaction");
        assertThat(icuJdbc.queryForObject(
                "SELECT count(*) FROM audit_outbox WHERE audit_id = ?", Long.class, auditId)).isZero();
    }

    @Test
    void outboxRetriesThenMovesToDeadAfterConfiguredAttempts() {
        UUID auditId = UUID.randomUUID();
        TransactionTemplate transaction = new TransactionTemplate(icuTransactionManager);
        JdbcTemplate icuJdbc = new JdbcTemplate(icuDataSource);
        transaction.executeWithoutResult(status ->
                icuAuditEventWriter.append(event(auditId, UUID.randomUUID(), UUID.randomUUID())));

        var firstClaim = icuAuditOutboxStore.claimBatch(10, 60);
        assertThat(firstClaim).hasSize(1);
        assertThat(firstClaim.get(0).attempts()).isEqualTo(1);
        icuAuditOutboxStore.scheduleRetry(auditId, 1, 2, 0, "TEST_RETRY");

        var secondClaim = icuAuditOutboxStore.claimBatch(10, 60);
        assertThat(secondClaim).hasSize(1);
        assertThat(secondClaim.get(0).attempts()).isEqualTo(2);
        icuAuditOutboxStore.scheduleRetry(auditId, 2, 2, 0, "TEST_EXHAUSTED");

        assertThat(icuJdbc.queryForObject(
                "SELECT relay_status FROM audit_outbox WHERE audit_id = ?", String.class, auditId))
                .isEqualTo("DEAD");
        assertThat(icuJdbc.queryForObject(
                "SELECT last_error_code FROM audit_outbox WHERE audit_id = ?", String.class, auditId))
                .isEqualTo("TEST_EXHAUSTED");
    }

    private static AuditEvent event(UUID auditId, UUID episodeId, UUID clinicalDayId) {
        return AuditEvent.builder()
                .auditId(auditId)
                .actor(new AuditEvent.AuditActor(
                        AuditEvent.ActorType.USER, "11", "doctor1", "Test Doctor", Set.of("DOCTOR"), null, null))
                .eventClass(com.superhumans.audit.AuditActionDefinition.EventClass.BUSINESS)
                .module("icu")
                .functionalArea("episode")
                .action("icu.episode.create")
                .actionType(com.superhumans.audit.AuditActionDefinition.ActionType.CREATE)
                .target(new AuditEvent.AuditTarget("Episode", episodeId.toString(), null))
                .relatedEntities(java.util.List.of(
                        new AuditEvent.AuditTarget("ClinicalDay", clinicalDayId.toString(), null)))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .source(AuditEvent.AuditSource.API)
                .build();
    }
}
