package com.superhumans.service;

import com.superhumans.dto.AuditRetentionReportResponse;
import io.micrometer.core.instrument.Counter;
import io.micrometer.core.instrument.MeterRegistry;
import java.time.Instant;
import java.util.LinkedHashMap;
import java.util.Map;
import javax.sql.DataSource;
import lombok.AccessLevel;
import lombok.experimental.FieldDefaults;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

/**
 * Retention enforcement for the audit store (F8, D1: temporary two-year
 * retention). Moves expired canonical rows to the archive tables and deletes
 * the originals (targets before events — the live FK is RESTRICT), and
 * prunes delivered transport copies from the module outboxes (DEAD rows are
 * kept for operator review). Disabled by default; the scheduled cycle and
 * the admin trigger share {@link #runOnce(Instant)}.
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
    boolean retentionEnabled;
    int outboxDeliveredDays;

    public AuditRetentionService(
            @Qualifier("coreDataSource") DataSource coreDataSource,
            @Qualifier("icuDataSource") DataSource icuDataSource,
            @Qualifier("medDataSource") DataSource medDataSource,
            @Qualifier("prosthDataSource") DataSource prosthDataSource,
            MeterRegistry meterRegistry,
            @Value("${app.audit.retention.enabled:false}") boolean retentionEnabled,
            @Value("${app.audit.retention.outbox-delivered-days:30}") int outboxDeliveredDays) {
        this.coreDataSource = coreDataSource;
        this.icuDataSource = icuDataSource;
        this.medDataSource = medDataSource;
        this.prosthDataSource = prosthDataSource;
        this.meterRegistry = meterRegistry;
        this.retentionEnabled = retentionEnabled;
        this.outboxDeliveredDays = outboxDeliveredDays;
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
        JdbcTemplate core = new JdbcTemplate(coreDataSource);
        int archivedTargets = core.update(
                "INSERT INTO audit_event_targets_archive "
                        + "(audit_id, relation_type, entity_type, entity_id, business_key, occurred_at) "
                        + "SELECT t.audit_id, t.relation_type, t.entity_type, t.entity_id, "
                        + "t.business_key, t.occurred_at FROM audit_event_targets t "
                        + "JOIN audit_events e ON e.audit_id = t.audit_id "
                        + "WHERE e.retention_until < ? "
                        + "ON CONFLICT DO NOTHING",
                cutoff);
        int archivedEvents = core.update(
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
                        + "FROM audit_events WHERE retention_until < ? "
                        + "ON CONFLICT DO NOTHING",
                cutoff);
        int archivedLegacy = core.update(
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
                        + "FROM audit_legacy_events WHERE retention_until < ? "
                        + "ON CONFLICT DO NOTHING",
                cutoff);
        int deletedTargets = core.update(
                "DELETE FROM audit_event_targets t USING audit_events_archive a "
                        + "WHERE t.audit_id = a.audit_id");
        int deletedEvents = core.update(
                "DELETE FROM audit_events e USING audit_events_archive a "
                        + "WHERE e.audit_id = a.audit_id");
        int deletedLegacy = core.update(
                "DELETE FROM audit_legacy_events e USING audit_legacy_events_archive a "
                        + "WHERE e.audit_id = a.audit_id");
        Map<String, Integer> deletedOutbox = pruneOutboxes(now);

        count("audit.retention.archived.events", archivedEvents);
        count("audit.retention.archived.targets", archivedTargets);
        count("audit.retention.archived.legacy", archivedLegacy);
        deletedOutbox.forEach((module, deleted) -> meterRegistry
                .counter("audit.retention.outbox.deleted", "module", module).increment(deleted));
        if (deletedTargets != archivedTargets || deletedEvents != archivedEvents
                || deletedLegacy != archivedLegacy) {
            log.warn("Audit retention archive/delete drift targets={}/{} events={}/{} legacy={}/{}",
                    deletedTargets, archivedTargets, deletedEvents, archivedEvents,
                    deletedLegacy, archivedLegacy);
        }
        return AuditRetentionReportResponse.builder()
                .enabled(true)
                .cutoff(now.toString())
                .archivedEvents(archivedEvents)
                .archivedTargets(archivedTargets)
                .archivedLegacy(archivedLegacy)
                .deletedOutboxByModule(deletedOutbox)
                .build();
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

    private void count(String name, int value) {
        Counter.builder(name).register(meterRegistry).increment(value);
    }
}
