--liquibase formatted sql

--changeset split-core:9
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';
CREATE TABLE audit_events (
    audit_id UUID PRIMARY KEY,
    schema_version SMALLINT NOT NULL,
    occurred_at TIMESTAMPTZ NOT NULL,
    recorded_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
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
    actor_roles JSONB NOT NULL DEFAULT '[]'::jsonb,
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
    CONSTRAINT ck_audit_events_schema_version CHECK (schema_version > 0),
    CONSTRAINT ck_audit_events_event_class CHECK
        (event_class IN ('BUSINESS', 'USER_ACTIVITY', 'SECURITY', 'TECHNICAL')),
    CONSTRAINT ck_audit_events_module CHECK
        (module IN ('platform', 'icu', 'medication', 'prosthetics')),
    CONSTRAINT ck_audit_events_action CHECK
        (action ~ '^[a-z][a-z0-9_]*(\.[a-z][a-z0-9_]*){2,5}$'),
    CONSTRAINT ck_audit_events_actor_type CHECK
        (actor_type IN ('USER', 'SYSTEM', 'SERVICE', 'INTEGRATION', 'UNKNOWN')),
    CONSTRAINT ck_audit_events_criticality CHECK
        (criticality IN ('LOW', 'MEDIUM', 'HIGH', 'CRITICAL')),
    CONSTRAINT ck_audit_events_outcome CHECK
        (outcome IN ('SUCCESS', 'FAILURE', 'DENIED', 'PARTIAL', 'CANCELLED', 'UNKNOWN_LEGACY')),
    CONSTRAINT ck_audit_events_retention_class CHECK
        (retention_class IN ('STANDARD', 'SECURITY', 'SENSITIVE_ACCESS', 'RESTRICTED_DETAIL'))
);

CREATE INDEX idx_audit_events_occurred_at
    ON audit_events (occurred_at DESC, audit_id);
CREATE INDEX idx_audit_events_actor_time
    ON audit_events (actor_id, occurred_at DESC);
CREATE INDEX idx_audit_events_module_action_time
    ON audit_events (module, action, occurred_at DESC);
CREATE INDEX idx_audit_events_target_time
    ON audit_events (target_type, target_id, occurred_at DESC);
CREATE INDEX idx_audit_events_correlation
    ON audit_events (correlation_id, occurred_at);
CREATE INDEX idx_audit_events_user_action
    ON audit_events (user_action_id, occurred_at);

CREATE TABLE audit_event_targets (
    audit_id UUID NOT NULL REFERENCES audit_events(audit_id) ON DELETE RESTRICT,
    relation_type VARCHAR(16) NOT NULL,
    entity_type VARCHAR(100) NOT NULL,
    entity_id VARCHAR(255) NOT NULL,
    business_key VARCHAR(255),
    occurred_at TIMESTAMPTZ NOT NULL,
    PRIMARY KEY (audit_id, relation_type, entity_type, entity_id),
    CONSTRAINT ck_audit_event_target_relation CHECK
        (relation_type IN ('PRIMARY', 'PARENT', 'RELATED'))
);

CREATE INDEX idx_audit_event_targets_history
    ON audit_event_targets (entity_type, entity_id, occurred_at DESC, audit_id);

--rollback DROP INDEX IF EXISTS idx_audit_event_targets_history;
--rollback DROP TABLE IF EXISTS audit_event_targets;
--rollback DROP INDEX IF EXISTS idx_audit_events_user_action;
--rollback DROP INDEX IF EXISTS idx_audit_events_correlation;
--rollback DROP INDEX IF EXISTS idx_audit_events_target_time;
--rollback DROP INDEX IF EXISTS idx_audit_events_module_action_time;
--rollback DROP INDEX IF EXISTS idx_audit_events_actor_time;
--rollback DROP INDEX IF EXISTS idx_audit_events_occurred_at;
--rollback DROP TABLE IF EXISTS audit_events;
