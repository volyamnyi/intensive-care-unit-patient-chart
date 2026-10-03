--liquibase formatted sql

--changeset split-core:13
-- Audit v2 retention archive (epic #329, issue #338, D1): cold copies of
-- expired canonical rows. Same shapes, no check constraints, no foreign
-- keys (archive must never block canonical deletes); provenance via
-- archived_at. Archive tables are append-only by convention and excluded
-- from the console read API — restores are an operator runbook procedure.
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';
CREATE TABLE audit_events_archive (
    audit_id UUID PRIMARY KEY,
    schema_version SMALLINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL,
    event_class VARCHAR(32) NOT NULL,
    module VARCHAR(24) NOT NULL,
    functional_area VARCHAR(100) NOT NULL,
    action VARCHAR(160) NOT NULL,
    action_type VARCHAR(40) NOT NULL,
    criticality VARCHAR(16) NOT NULL,
    actor_type VARCHAR(24) NOT NULL,
    actor_id VARCHAR(128),
    actor_login VARCHAR(100),
    actor_display_name VARCHAR(200),
    actor_roles JSONB NOT NULL,
    target_type VARCHAR(100),
    target_id VARCHAR(255),
    business_key VARCHAR(255),
    outcome VARCHAR(24) NOT NULL,
    request_id UUID,
    user_action_id UUID,
    correlation_id UUID,
    parent_audit_id UUID,
    retention_class VARCHAR(32) NOT NULL,
    retention_until TIMESTAMPTZ NOT NULL,
    integrity_hash CHAR(64),
    event_payload JSONB NOT NULL,
    archived_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE audit_event_targets_archive (
    audit_id UUID NOT NULL,
    relation_type VARCHAR(16) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    business_key VARCHAR(255),
    occurred_at TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    PRIMARY KEY (audit_id, relation_type, entity_type, entity_id)
);

CREATE TABLE audit_legacy_events_archive (
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
    legacy_is_deleted BOOLEAN NOT NULL,
    schema_version INTEGER NOT NULL,
    context_completeness VARCHAR(32) NOT NULL,
    timestamp_precision VARCHAR(32) NOT NULL,
    outcome VARCHAR(32) NOT NULL,
    checksum CHAR(64) NOT NULL,
    retention_until TIMESTAMPTZ NOT NULL,
    backfilled_at TIMESTAMPTZ NOT NULL,
    archived_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_audit_events_archive_retention
    ON audit_events_archive (retention_until);
CREATE INDEX idx_audit_legacy_archive_retention
    ON audit_legacy_events_archive (retention_until);

--rollback DROP INDEX IF EXISTS idx_audit_legacy_archive_retention;
--rollback DROP INDEX IF EXISTS idx_audit_events_archive_retention;
--rollback DROP TABLE IF EXISTS audit_legacy_events_archive;
--rollback DROP TABLE IF EXISTS audit_event_targets_archive;
--rollback DROP TABLE IF EXISTS audit_events_archive;
