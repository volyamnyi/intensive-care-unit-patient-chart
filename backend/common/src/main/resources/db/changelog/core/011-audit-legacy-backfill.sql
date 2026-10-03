--liquibase formatted sql

--changeset split-core:11
-- Audit v2 legacy backfill store (epic #329, issue #338, §H.4): frozen,
-- clearly-marked copies of audit_logs rows. Nothing is reconstructed: the
-- original entity/action/values are stored verbatim, outcome is always
-- UNKNOWN_LEGACY, schema_version is 0, and naive timestamps are stored as
-- UTC-assumed instants flagged LEGACY_NAIVE (exact server-local offset is
-- unknown — legacy period queries carry offset uncertainty, see runbook).
-- Idempotency guard: UNIQUE(source_table, source_id); re-runs converge.
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';
CREATE TABLE audit_legacy_events (
    audit_id UUID PRIMARY KEY,
    source_table VARCHAR(64) NOT NULL,
    source_id UUID NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    legacy_kind VARCHAR(32) NOT NULL,
    legacy_entity VARCHAR(100) NOT NULL,
    legacy_entity_id UUID,
    legacy_action VARCHAR(100) NOT NULL,
    legacy_user_id BIGINT,
    legacy_user_role VARCHAR(255),
    legacy_ip_address VARCHAR(255),
    legacy_old_value TEXT,
    legacy_new_value TEXT,
    legacy_details TEXT,
    legacy_correlation_id VARCHAR(100),
    legacy_is_deleted BOOLEAN NOT NULL DEFAULT FALSE,
    schema_version INTEGER NOT NULL DEFAULT 0,
    context_completeness VARCHAR(32) NOT NULL DEFAULT 'LEGACY',
    timestamp_precision VARCHAR(32) NOT NULL DEFAULT 'LEGACY_NAIVE',
    outcome VARCHAR(32) NOT NULL DEFAULT 'UNKNOWN_LEGACY',
    checksum CHAR(64) NOT NULL,
    retention_until TIMESTAMPTZ NOT NULL,
    backfilled_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT ck_audit_legacy_kind CHECK
        (legacy_kind IN ('legacy.http.request', 'legacy.audit.action')),
    CONSTRAINT uq_audit_legacy_source UNIQUE (source_table, source_id)
);

CREATE INDEX idx_audit_legacy_history
    ON audit_legacy_events (legacy_entity, legacy_entity_id, occurred_at ASC);
CREATE INDEX idx_audit_legacy_action_time
    ON audit_legacy_events (legacy_action, occurred_at DESC);

--rollback DROP INDEX IF EXISTS idx_audit_legacy_action_time;
--rollback DROP INDEX IF EXISTS idx_audit_legacy_history;
--rollback DROP TABLE IF EXISTS audit_legacy_events;
