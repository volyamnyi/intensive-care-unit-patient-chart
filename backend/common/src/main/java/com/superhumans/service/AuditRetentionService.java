package com.superhumans.service;

import com.superhumans.audit.AuditActionDefinition.ActionType;
import com.superhumans.audit.AuditActorResolver;
import com.superhumans.audit.AuditEvent;
import com.superhumans.audit.AuditEventRecorder;
import com.superhumans.dto.AuditRetentionReportResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.core.namedparam.MapSqlParameterSource;
import org.springframework.jdbc.core.namedparam.NamedParameterJdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Retention enforcement for the audit store (F8, D1: temporary two-year
 * retention). Moves expired canonical rows to the archive tables and deletes
 * the originals (targets before events — the live FK is RESTRICT), and
 * prunes delivered transport copies from the module outboxes (DEAD rows are
 * kept for operator review). Disabled by default; the scheduled cycle and
 * the admin trigger share {@link #runOnce(Instant)}.
 *
 * <p>Every enabled run emits one {@code platform.audit.retention.run} event
 * (actor: the admin on the manual endpoint, {@code retention-job} on the cron)
 * — audit-data lifecycle operations must not be unauditable (#393).
 */
@Service
@Slf4j
@FieldDefaults(level = AccessLevel.PRIVATE, makeFinal = true)
public class AuditRetentionService {

    DataSource coreDataSource;
    DataSource icuDataSource;
    DataSource medDataSource;
    DataSource prosthDataSource;
    MeterRegistry meterRegistry;
    AuditEventRecorder auditEventRecorder;
    boolean retentionEnabled;
    int outboxDeliveredDays;
    int chunkSize;
    TransactionTemplate coreTransactionTemplate;

    public AuditRetentionService(
            @Qualifier("coreDataSource") DataSource coreDataSource,
            @Qualifier("icuDataSource") DataSource icuDataSource,
            @Qualifier("medDataSource") DataSource medDataSource,
            @Qualifier("prosthDataSource") DataSource prosthDataSource,
            MeterRegistry meterRegistry,
            AuditEventRecorder auditEventRecorder,
            @Value("${app.audit.retention.enabled:false}") boolean retentionEnabled,
            @Value("${app.audit.retention.outbox-delivered-days:30}") int outboxDeliveredDays,
            @Qualifier("coreTransactionManager") PlatformTransactionManager coreTransactionManager,
            @Value("${app.audit.retention.chunk-size:500}") int chunkSize) {
        this.coreDataSource = coreDataSource;
        this.icuDataSource = icuDataSource;
        this.medDataSource = medDataSource;
        this.prosthDataSource = prosthDataSource;
        this.meterRegistry = meterRegistry;
        this.auditEventRecorder = auditEventRecorder;
        this.retentionEnabled = retentionEnabled;
        this.outboxDeliveredDays = outboxDeliveredDays;
        this.chunkSize = Math.max(1, chunkSize);
        this.coreTransactionTemplate = new TransactionTemplate(coreTransactionManager);
    }

    @Scheduled(cron = "${app.audit.retention.cron:0 0 3 * * *}")
    public void scheduledCycle() {
        if (!retentionEnabled) {
            return;
        }
        AuditRetentionReportResponse report = runOnce(Instant.now());
        log.info("Audit retention cycle archivedEvents={} archivedLegacy={} deletedOutbox={}",
                report.getArchivedEvents(), report.getArchivedLegacy(), report.getDeletedOutboxByModule());
    }

    public AuditRetentionReportResponse runOnce(Instant now) {
        if (!retentionEnabled) {
            return AuditRetentionReportResponse.builder()
                    .enabled(false)
                    .cutoff(now.toString())
                    .archivedEvents(0)
                    .archivedTargets(0)
                    .archivedLegacy(0)
                    .deletedOutboxByModule(Map.of())
                    .build();
        }
        // PostgreSQL JDBC cannot infer a type for Instant parameters —
        // bind java.sql.Timestamp explicitly.
        java.sql.Timestamp cutoff = java.sql.Timestamp.from(now);
        int archivedTargets = 0;
        int archivedEvents = 0;
        ChunkResult eventChunk;
        do {
            eventChunk = archiveCanonicalChunk(cutoff);
            archivedTargets += eventChunk.targets();
            archivedEvents += eventChunk.events();
        } while (eventChunk.events() > 0);

        int archivedLegacy = 0;
        ChunkResult legacyChunk;
        do {
            legacyChunk = archiveLegacyChunk(cutoff);
            archivedLegacy += legacyChunk.events();
        } while (legacyChunk.events() > 0);
        Map<String, Integer> deletedOutbox = pruneOutboxes(now);

        count("audit.retention.archived.events", archivedEvents);
        count("audit.retention.archived.targets", archivedTargets);
        count("audit.retention.archived.legacy", archivedLegacy);
        deletedOutbox.forEach((module, deleted) -> meterRegistry
                .counter("audit.retention.outbox.deleted", "module", module).increment(deleted));
        AuditRetentionReportResponse report = AuditRetentionReportResponse.builder()
                .enabled(true)
                .cutoff(now.toString())
                .archivedEvents(archivedEvents)
                .archivedTargets(archivedTargets)
                .archivedLegacy(archivedLegacy)
                .deletedOutboxByModule(deletedOutbox)
                .build();
        emitRetentionRun(now, report);
        return report;
    }

    /** One event per enabled run — actor is the admin on the manual endpoint, retention-job on the cron. */
    private void emitRetentionRun(Instant now, AuditRetentionReportResponse report) {
        AuditEvent.AuditActor actor = AuditActorResolver.fromCurrentContextOrSystem("retention-job");
        int deletedOutboxTotal = report.getDeletedOutboxByModule().values()
                .stream().mapToInt(Integer::intValue).sum();
        Map<String, Object> metadata = new LinkedHashMap<>();
        metadata.put("cutoff", now.toString());
        metadata.put("archivedEvents", report.getArchivedEvents());
        metadata.put("archivedTargets", report.getArchivedTargets());
        metadata.put("archivedLegacy", report.getArchivedLegacy());
        metadata.put("deletedOutbox", deletedOutboxTotal);
        auditEventRecorder.record(AuditEvent.builder()
                .actor(actor)
                .eventClass(com.superhumans.audit.AuditActionDefinition.EventClass.SECURITY)
                .module("platform")
                .functionalArea("audit")
                .action("platform.audit.retention.run")
                .actionType(ActionType.DELETE)
                .target(new AuditEvent.AuditTarget("AuditStore", "retention", null))
                .outcome(AuditEvent.AuditOutcome.SUCCESS)
                .affectedRecords(report.getArchivedEvents() + report.getArchivedTargets()
                        + report.getArchivedLegacy() + deletedOutboxTotal)
                .metadata(metadata)
                .source(actor.type() == AuditEvent.ActorType.USER
                        ? AuditEvent.AuditSource.ADMIN_TOOL
                        : AuditEvent.AuditSource.SCHEDULED_JOB)
                .build());
    }

    private Map<String, Integer> pruneOutboxes(Instant now) {
        java.sql.Timestamp cutoff = java.sql.Timestamp.from(now);
        Map<String, JdbcTemplate> templates = Map.of(
                "icu", new JdbcTemplate(icuDataSource),
                "medication", new JdbcTemplate(medDataSource),
                "prosthetics", new JdbcTemplate(prosthDataSource));
        Map<String, Integer> deleted = new LinkedHashMap<>();
        templates.forEach((module, template) -> deleted.put(module, template.update(
                "DELETE FROM audit_outbox WHERE relay_status = 'DELIVERED' "
                        + "AND delivered_at < CAST(? AS timestamptz) - (? * INTERVAL '1 day')",
                cutoff, outboxDeliveredDays)));
        return deleted;
    }

    private ChunkResult archiveCanonicalChunk(java.sql.Timestamp cutoff) {
        return coreTransactionTemplate.execute(status -> {
            NamedParameterJdbcTemplate core = new NamedParameterJdbcTemplate(coreDataSource);
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("cutoff", cutoff)
                    .addValue("limit", chunkSize);
            List<UUID> ids = core.query(
                    "SELECT audit_id FROM audit_events "
                            + "WHERE retention_until < :cutoff "
                            + "ORDER BY retention_until, audit_id LIMIT :limit",
                    params, (rs, rowNum) -> rs.getObject("audit_id", UUID.class));
            if (ids.isEmpty()) {
                return new ChunkResult(0, 0);
            }
            params.addValue("ids", ids);
            int targets = core.update(
                    "INSERT INTO audit_event_targets_archive "
                            + "(audit_id, relation_type, entity_type, entity_id, business_key, occurred_at) "
                            + "SELECT audit_id, relation_type, entity_type, entity_id, business_key, occurred_at "
                            + "FROM audit_event_targets WHERE audit_id IN (:ids) ON CONFLICT DO NOTHING",
                    params);
            core.update(
                    "INSERT INTO audit_events_archive "
                            + "(audit_id, schema_version, occurred_at, recorded_at, event_class, module, "
                            + "functional_area, action, action_type, criticality, actor_type, actor_id, "
                            + "actor_login, actor_display_name, actor_roles, target_type, target_id, "
                            + "business_key, outcome, request_id, user_action_id, correlation_id, "
                            + "parent_audit_id, retention_class, retention_until, integrity_hash, event_payload) "
                            + "SELECT audit_id, schema_version, occurred_at, recorded_at, event_class, module, "
                            + "functional_area, action, action_type, criticality, actor_type, actor_id, "
                            + "actor_login, actor_display_name, actor_roles, target_type, target_id, "
                            + "business_key, outcome, request_id, user_action_id, correlation_id, "
                            + "parent_audit_id, retention_class, retention_until, integrity_hash, event_payload "
                            + "FROM audit_events WHERE audit_id IN (:ids) ON CONFLICT DO NOTHING",
                    params);
            core.update("DELETE FROM audit_event_targets WHERE audit_id IN (:ids)", params);
            int events = core.update("DELETE FROM audit_events WHERE audit_id IN (:ids)", params);
            return new ChunkResult(targets, events);
        });
    }

    private ChunkResult archiveLegacyChunk(java.sql.Timestamp cutoff) {
        return coreTransactionTemplate.execute(status -> {
            NamedParameterJdbcTemplate core = new NamedParameterJdbcTemplate(coreDataSource);
            MapSqlParameterSource params = new MapSqlParameterSource()
                    .addValue("cutoff", cutoff)
                    .addValue("limit", chunkSize);
            List<UUID> ids = core.query(
                    "SELECT audit_id FROM audit_legacy_events "
                            + "WHERE retention_until < :cutoff "
                            + "ORDER BY retention_until, audit_id LIMIT :limit",
                    params, (rs, rowNum) -> rs.getObject("audit_id", UUID.class));
            if (ids.isEmpty()) {
                return new ChunkResult(0, 0);
            }
            params.addValue("ids", ids);
            core.update(
                    "INSERT INTO audit_legacy_events_archive "
                            + "(audit_id, source_table, source_id, occurred_at, legacy_kind, legacy_entity, "
                            + "legacy_entity_id, legacy_action, legacy_user_id, legacy_user_role, "
                            + "legacy_ip_address, legacy_old_value, legacy_new_value, legacy_details, "
                            + "legacy_correlation_id, legacy_is_deleted, schema_version, context_completeness, "
                            + "timestamp_precision, outcome, checksum, retention_until, backfilled_at) "
                            + "SELECT audit_id, source_table, source_id, occurred_at, legacy_kind, legacy_entity, "
                            + "legacy_entity_id, legacy_action, legacy_user_id, legacy_user_role, "
                            + "legacy_ip_address, legacy_old_value, legacy_new_value, legacy_details, "
                            + "legacy_correlation_id, legacy_is_deleted, schema_version, context_completeness, "
                            + "timestamp_precision, outcome, checksum, retention_until, backfilled_at "
                            + "FROM audit_legacy_events WHERE audit_id IN (:ids) ON CONFLICT DO NOTHING",
                    params);
            int events = core.update("DELETE FROM audit_legacy_events WHERE audit_id IN (:ids)", params);
            return new ChunkResult(0, events);
        });
    }

    private record ChunkResult(int targets, int events) {
    }

    private void count(String name, int value) {
        Counter.builder(name).register(meterRegistry).increment(value);
    }
}
