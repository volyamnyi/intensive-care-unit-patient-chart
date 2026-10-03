--liquibase formatted sql

--changeset split-core:12
-- Audit v2 append-only hardening (epic #329, issue #338): belt-and-braces
-- REVOKE of UPDATE/DELETE on the canonical event store from PUBLIC. The
-- application connects with a privileged role, so the primary enforcement
-- is JPA-level (@Immutable entities + delete-blocking repositories,
-- proven by AuditAppendOnlyIntegrationTest); this changeset documents the
-- intent in schema and protects least-privilege deployments. Module outbox
-- tables are intentionally excluded — the relay updates claim/lease/retry
-- state on them by design. Dedicated prod roles live in the runbook.
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';
REVOKE UPDATE, DELETE ON audit_events FROM PUBLIC;
REVOKE UPDATE, DELETE ON audit_event_targets FROM PUBLIC;
REVOKE UPDATE, DELETE ON audit_legacy_events FROM PUBLIC;

--rollback GRANT UPDATE, DELETE ON audit_legacy_events TO PUBLIC;
--rollback GRANT UPDATE, DELETE ON audit_event_targets TO PUBLIC;
--rollback GRANT UPDATE, DELETE ON audit_events TO PUBLIC;
