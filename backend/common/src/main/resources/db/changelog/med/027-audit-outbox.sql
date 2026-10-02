--liquibase formatted sql

--changeset split-med:27
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';
CREATE TABLE audit_outbox (
    audit_id UUID PRIMARY KEY,
    payload JSONB NOT NULL,
    payload_hash CHAR(64) NOT NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    relay_status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
    relay_attempts INTEGER NOT NULL DEFAULT 0,
    next_attempt_at TIMESTAMPTZ NOT NULL DEFAULT CURRENT_TIMESTAMP,
    lease_until TIMESTAMPTZ,
    delivered_at TIMESTAMPTZ,
    last_error_code VARCHAR(100),
    CONSTRAINT ck_med_audit_outbox_status CHECK
        (relay_status IN ('PENDING', 'PROCESSING', 'DELIVERED', 'DEAD')),
    CONSTRAINT ck_med_audit_outbox_attempts CHECK (relay_attempts >= 0)
);

CREATE INDEX idx_med_audit_outbox_poll
    ON audit_outbox (relay_status, next_attempt_at, created_at);

--rollback DROP INDEX IF EXISTS idx_med_audit_outbox_poll;
--rollback DROP TABLE IF EXISTS audit_outbox;
