package com.superhumans.integration;

import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import com.superhumans.audit.AuditActionDefinition;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventSerializer;
import com.superhumans.entity.core.AuditLegacyEvent;
import com.superhumans.repository.core.AuditLegacyEventRepository;
import com.superhumans.service.AuditEventPersistenceService;
import com.superhumans.service.AuditRetentionService;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.time.temporal.ChronoUnit;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

class AuditRetentionIntegrationTest extends AbstractIntegrationTest {

    private final ObjectMapper mapper = new ObjectMapper();

    @Autowired
    AuditEventPersistenceService persistenceService;

    @Autowired
    AuditEventSerializer serializer;

    @Autowired
    AuditLegacyEventRepository legacyEventRepository;

    @Autowired
    MeterRegistry meterRegistry;

    @Autowired
    @Qualifier("coreDataSource")
    DataSource coreDataSource;

    @Autowired
    @Qualifier("icuDataSource")
    DataSource icuDataSource;

    @Autowired
    @Qualifier("medDataSource")
    DataSource medDataSource;

    @Autowired
    @Qualifier("prosthDataSource")
    DataSource prosthDataSource;

    @Autowired
    @Qualifier("coreTransactionManager")
    PlatformTransactionManager coreTransactionManager;

    private AuditRetentionService enabledService() {
        return new AuditRetentionService(coreDataSource, icuDataSource, medDataSource,
                prosthDataSource, meterRegistry, true, 30);
    }

    private UUID persistExpiredCanonicalEvent() {
        AuditEvent event = AuditEvent.builder()
                .auditId(UUID.randomUUID())
                .actor(new AuditEvent.AuditActor(AuditEvent.ActorType.UNKNOWN,
                        null, null, null, Set.of(), null, null))
                .eventClass(AuditActionDefinition.EventClass.SECURITY)
                .module("platform")
                .functionalArea("token")
                .action("platform.auth.token.rejected")
                .actionType(AuditActionDefinition.ActionType.ACCESS_DENIED)
                .occurredAt(Instant.now().minus(3 * 365L, ChronoUnit.DAYS))
                .outcome(AuditEvent.AuditOutcome.FAILURE)
                .source(AuditEvent.AuditSource.API)
                .build();
        persistenceService.persist(event, serializer.sha256(event));
        return event.auditId();
    }

    @Test
    void retention_disabledByDefault_reportsSkipped() {
        var res = restTemplate.exchange("/api/admin/audit/retention/run", HttpMethod.POST,
                authGet(getAdminToken()), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.OK);
        try {
            JsonNode report = mapper.readTree(res.getBody());
            assertThat(report.get("enabled").asBoolean()).isFalse();
            assertThat(report.get("archivedEvents").asInt()).isZero();
        } catch (Exception exception) {
            throw new IllegalStateException("Response is not JSON", exception);
        }
    }

    @Test
    void retention_movesExpiredCanonicalAndLegacyRowsToArchive() {
        UUID expiredId = persistExpiredCanonicalEvent();
        UUID legacyId = UUID.randomUUID();
        new TransactionTemplate(coreTransactionManager).execute(status -> {
            legacyEventRepository.save(AuditLegacyEvent.builder()
                    .auditId(legacyId)
                    .sourceTable("audit_logs")
                    .sourceId(UUID.randomUUID())
                    .occurredAt(Instant.now().minus(3 * 365L, ChronoUnit.DAYS))
                    .legacyKind("legacy.audit.action")
                    .legacyEntity("RetentionProbe")
                    .legacyAction("CREATE")
                    .legacyIsDeleted(false)
                    .schemaVersion(0)
                    .contextCompleteness("LEGACY")
                    .timestampPrecision("LEGACY_NAIVE")
                    .outcome("UNKNOWN_LEGACY")
                    .checksum("probe")
                    .retentionUntil(Instant.now().minus(365L, ChronoUnit.DAYS))
                    .build());
            return null;
        });
        JdbcTemplate core = new JdbcTemplate(coreDataSource);
        int archiveEventsBefore = core.queryForObject(
                "SELECT COUNT(*) FROM audit_events_archive", Integer.class);
        int archiveLegacyBefore = core.queryForObject(
                "SELECT COUNT(*) FROM audit_legacy_events_archive", Integer.class);

        var report = enabledService().runOnce(Instant.now());

        assertThat(report.isEnabled()).isTrue();
        assertThat(report.getArchivedEvents()).isGreaterThanOrEqualTo(1);
        assertThat(report.getArchivedLegacy()).isGreaterThanOrEqualTo(1);
        assertThat(core.queryForObject(
                "SELECT COUNT(*) FROM audit_events_archive", Integer.class))
                .isEqualTo(archiveEventsBefore + report.getArchivedEvents());
        assertThat(core.queryForObject(
                "SELECT COUNT(*) FROM audit_legacy_events_archive", Integer.class))
                .isEqualTo(archiveLegacyBefore + report.getArchivedLegacy());
        assertThat(core.queryForObject(
                "SELECT COUNT(*) FROM audit_events WHERE audit_id = ?", Integer.class, expiredId))
                .isZero();
        assertThat(core.queryForObject(
                "SELECT COUNT(*) FROM audit_legacy_events WHERE audit_id = ?", Integer.class, legacyId))
                .isZero();
        assertThat(core.queryForObject(
                "SELECT COUNT(*) FROM audit_event_targets WHERE audit_id = ?", Integer.class, expiredId))
                .isZero();
    }

    @Test
    void retention_prunesDeliveredOutboxCopiesAndKeepsDead() {
        UUID deliveredId = UUID.randomUUID();
        UUID deadId = UUID.randomUUID();
        JdbcTemplate icu = new JdbcTemplate(icuDataSource);
        java.sql.Timestamp old = java.sql.Timestamp.from(Instant.now().minus(60L, ChronoUnit.DAYS));
        icu.update("INSERT INTO audit_outbox (audit_id, payload, payload_hash, relay_status, delivered_at) "
                        + "VALUES (?, CAST('{}' AS jsonb), ?, 'DELIVERED', ?)",
                deliveredId, "0".repeat(64), old);
        icu.update("INSERT INTO audit_outbox (audit_id, payload, payload_hash, relay_status, delivered_at) "
                        + "VALUES (?, CAST('{}' AS jsonb), ?, 'DEAD', ?)",
                deadId, "1".repeat(64), old);

        var report = enabledService().runOnce(Instant.now());

        assertThat(report.getDeletedOutboxByModule().get("icu")).isGreaterThanOrEqualTo(1);
        assertThat(icu.queryForObject(
                "SELECT COUNT(*) FROM audit_outbox WHERE audit_id = ?", Integer.class, deliveredId))
                .isZero();
        assertThat(icu.queryForObject(
                "SELECT COUNT(*) FROM audit_outbox WHERE audit_id = ?", Integer.class, deadId))
                .isEqualTo(1);
    }

    @Test
    void retention_triggerRequiresAdmin() {
        var res = restTemplate.exchange("/api/admin/audit/retention/run", HttpMethod.POST,
                authGet(getNurseToken()), String.class);
        assertThat(res.getStatusCode()).isEqualTo(HttpStatus.FORBIDDEN);
    }
}
