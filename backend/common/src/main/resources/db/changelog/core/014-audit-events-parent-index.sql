--liquibase formatted sql

--changeset split-core:14 runInTransaction:false
--comment audit_events.parent_audit_id — children of an event (detail card) previously seq-scanned the whole store per open
DROP INDEX CONCURRENTLY IF EXISTS idx_audit_events_parent;
CREATE INDEX CONCURRENTLY idx_audit_events_parent
    ON audit_events (parent_audit_id, occurred_at);
--rollback DROP INDEX IF EXISTS idx_audit_events_parent;

--changeset split-core:15 runInTransaction:false
--comment audit_events.actor_login — console actor filter previously seq-scanned; must stay a real column (attempted-login actors carry NULL actor_id, only actor_login)
DROP INDEX CONCURRENTLY IF EXISTS idx_audit_events_actor_login;
CREATE INDEX CONCURRENTLY idx_audit_events_actor_login
    ON audit_events (actor_login, occurred_at DESC);
--rollback DROP INDEX IF EXISTS idx_audit_events_actor_login;
