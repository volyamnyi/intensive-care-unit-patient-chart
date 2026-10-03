package com.superhumans.integration;

import com.superhumans.audit.AuditActionDefinition;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventFilter;
import com.superhumans.audit.AuditEventSerializer;
import com.superhumans.dto.LegacyBackfillReportResponse;
import com.superhumans.entity.core.AuditLog;
import com.superhumans.repository.core.AuditLogRepository;
import com.superhumans.service.AuditEventPersistenceService;
import com.superhumans.service.AuditEventQueryService;
import com.superhumans.service.AuditEventRelay;
import com.superhumans.service.LegacyAuditBackfillService;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.UUID;
import javax.sql.DataSource;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfSystemProperty;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.domain.PageRequest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Audit load measurements (F8, §B7). Local-only: enabled exclusively with
 * {@code -Daudit.local.perf=true} (never in CI), against scratch databases.
 * Thresholds are generous regression tripwires, not SLOs — the measured
 * numbers are recorded in the F8 session note and issue #338, and the SLO
 * targets are approved by the owner from those measurements.
 */
@EnabledIfSystemProperty(named = "audit.local.perf", matches = "true")
class AuditLoadPerfIntegrationTest extends AbstractIntegrationTest {

    private static final int CANONICAL_VOLUME = 10_000;
    private static final int BACKFILL_VOLUME = 5_000;
    private static final int RELAY_VOLUME = 500;

    @Autowired
    AuditEventPersistenceService persistenceService;

    @Autowired
    AuditEventSerializer serializer;

    @Autowired
    AuditEventQueryService queryService;

    @Autowired
    AuditEventRelay relay;

    @Autowired
    LegacyAuditBackfillService backfillService;

    @Autowired
    AuditLogRepository auditLogRepository;

    @Autowired
    @Qualifier("coreTransactionManager")
    PlatformTransactionManager coreTransactionManager;

    @Autowired
    @Qualifier("icuDataSource")
    DataSource icuDataSource;

    private AuditEvent probeEvent() {
        return AuditEvent.builder()
                .auditId(UUID.randomUUID())
                .actor(new AuditEvent.AuditActor(AuditEvent.ActorType.UNKNOWN,
                        null, null, null, Set.of(), null, null))
                .eventClass(AuditActionDefinition.EventClass.SECURITY)
                .module("platform")
                .functionalArea("token")
                .action("platform.auth.token.rejected")
                .actionType(AuditActionDefinition.ActionType.ACCESS_DENIED)
                .outcome(AuditEvent.AuditOutcome.FAILURE)
                .source(AuditEvent.AuditSource.API)
                .build();
    }

    @Test
    void measure_canonicalWrite_search_detail() {
        List<AuditEvent> batch = new ArrayList<>(CANONICAL_VOLUME);
        for (int i = 0; i < CANONICAL_VOLUME; i++) {
            batch.add(probeEvent());
        }
        long writeStart = System.nanoTime();
        new TransactionTemplate(coreTransactionManager).execute(status -> {
            for (AuditEvent event : batch) {
                persistenceService.persist(event, serializer.sha256(event));
            }
            return null;
        });
        double writeSeconds = (System.nanoTime() - writeStart) / 1_000_000_000.0;
        double writeRate = CANONICAL_VOLUME / writeSeconds;

        AuditEventFilter filter = new AuditEventFilter(null, "platform", null,
                "platform.auth.token.rejected", null, null, "FAILURE", null, null,
                null, null, null, null, null);
        long searchStart = System.nanoTime();
        var page = queryService.search(filter, PageRequest.of(0, 20));
        double searchMs = (System.nanoTime() - searchStart) / 1_000_000.0;

        UUID sampleId = page.getContent().get(0).getAuditId();
        long detailStart = System.nanoTime();
        var detail = queryService.detail(sampleId);
        double detailMs = (System.nanoTime() - detailStart) / 1_000_000.0;

        System.out.printf("AUDIT-PERF canonical_write rows=%d seconds=%.1f rate=%.0f rows/s%n",
                CANONICAL_VOLUME, writeSeconds, writeRate);
        System.out.printf("AUDIT-PERF search matched=%d ms=%.1f%n",
                page.getTotalElements(), searchMs);
        System.out.printf("AUDIT-PERF detail integrity=%s ms=%.1f%n",
                detail.isIntegrityVerified(), detailMs);

        assertThat(writeRate).isGreaterThan(50.0);
        assertThat(searchMs).isLessThan(5_000.0);
        assertThat(detailMs).isLessThan(2_000.0);
        assertThat(detail.isIntegrityVerified()).isTrue();
    }

    @Test
    void measure_backfillThroughput() {
        String entity = "PerfProbe-" + UUID.randomUUID().toString().substring(0, 8);
        List<AuditLog> rows = new ArrayList<>(BACKFILL_VOLUME);
        for (int i = 0; i < BACKFILL_VOLUME; i++) {
            rows.add(AuditLog.builder()
                    .entity(entity)
                    .entityId(UUID.randomUUID())
                    .action(i % 2 == 0 ? "CREATE" : "API_PERF_SEARCH")
                    .userId(11L)
                    .timestamp(LocalDateTime.now().minusDays(10))
                    .build());
        }
        new TransactionTemplate(coreTransactionManager).execute(status -> {
            auditLogRepository.saveAll(rows);
            return null;
        });

        long start = System.nanoTime();
        LegacyBackfillReportResponse report = backfillService.backfill();
        double seconds = (System.nanoTime() - start) / 1_000_000_000.0;
        double rate = report.getInserted() / Math.max(seconds, 0.001);

        System.out.printf("AUDIT-PERF backfill inserted=%d seconds=%.1f rate=%.0f rows/s verified=%s%n",
                report.getInserted(), seconds, rate, report.isVerified());

        assertThat(report.getInserted()).isGreaterThanOrEqualTo(BACKFILL_VOLUME);
        assertThat(report.isVerified()).isTrue();
        assertThat(rate).isGreaterThan(20.0);
    }

    @Test
    void measure_relayDrainThroughput() {
        JdbcTemplate icu = new JdbcTemplate(icuDataSource);
        List<Object[]> batch = new ArrayList<>(RELAY_VOLUME);
        for (int i = 0; i < RELAY_VOLUME; i++) {
            AuditEvent event = AuditEvent.builder()
                    .auditId(UUID.randomUUID())
                    .actor(new AuditEvent.AuditActor(AuditEvent.ActorType.SYSTEM,
                            "perf", null, null, Set.of(), null, null))
                    .eventClass(AuditActionDefinition.EventClass.BUSINESS)
                    .module("icu")
                    .functionalArea("fluid-balance")
                    .action("icu.fluid_balance.recalculate")
                    .actionType(AuditActionDefinition.ActionType.RECALCULATE)
                    .outcome(AuditEvent.AuditOutcome.SUCCESS)
                    .source(AuditEvent.AuditSource.SCHEDULED_JOB)
                    .build();
            batch.add(new Object[]{event.auditId(), serializer.serialize(event),
                    serializer.sha256(event)});
        }
        icu.batchUpdate(
                "INSERT INTO audit_outbox (audit_id, payload, payload_hash) VALUES (?, CAST(? AS jsonb), ?)",
                batch);
        long deliveredBefore = icu.queryForObject(
                "SELECT COUNT(*) FROM audit_outbox WHERE relay_status = 'DELIVERED'", Long.class);
        long deadBefore = icu.queryForObject(
                "SELECT COUNT(*) FROM audit_outbox WHERE relay_status = 'DEAD'", Long.class);

        long start = System.nanoTime();
        int polls = 0;
        while (icu.queryForObject(
                "SELECT COUNT(*) FROM audit_outbox WHERE relay_status IN ('PENDING', 'PROCESSING')",
                Long.class) > 0 && polls < 500) {
            relay.poll();
            polls++;
        }
        double seconds = (System.nanoTime() - start) / 1_000_000_000.0;

        long remaining = icu.queryForObject(
                "SELECT COUNT(*) FROM audit_outbox WHERE relay_status IN ('PENDING', 'PROCESSING')",
                Long.class);
        long deliveredDelta = icu.queryForObject(
                "SELECT COUNT(*) FROM audit_outbox WHERE relay_status = 'DELIVERED'", Long.class)
                - deliveredBefore;
        long deadDelta = icu.queryForObject(
                "SELECT COUNT(*) FROM audit_outbox WHERE relay_status = 'DEAD'", Long.class)
                - deadBefore;
        System.out.printf("AUDIT-PERF relay drained=%d polls=%d seconds=%.1f rate=%.0f events/s remaining=%d deadDelta=%d%n",
                RELAY_VOLUME - remaining, polls, seconds,
                (RELAY_VOLUME - remaining) / Math.max(seconds, 0.001), remaining, deadDelta);

        assertThat(remaining).isZero();
        assertThat(deadDelta).isZero();
        assertThat(deliveredDelta).isGreaterThanOrEqualTo(RELAY_VOLUME);
        assertThat((double) RELAY_VOLUME / Math.max(seconds, 0.001)).isGreaterThan(5.0);
    }
}
