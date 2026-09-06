--liquibase formatted sql

--changeset split-core:7
-- LDAP authentication source marker (Phase 2, issue #246, decision D2):
-- distinguishes LOCAL BCrypt accounts from LDAP directory-bound accounts.
-- Existing rows become LOCAL via the column default; directory-provisioned
-- accounts use LDAP with a NULL password hash (the AD password is never stored).
-- password_hash nullability is relaxed for the same reason.
ALTER TABLE users ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(10) NOT NULL DEFAULT 'LOCAL';
ALTER TABLE users ALTER COLUMN password_hash DROP NOT NULL;

--rollback guarded: restoring NOT NULL is allowed only when no LDAP rows exist.
--rollback DO $$ BEGIN IF EXISTS (SELECT 1 FROM users WHERE password_hash IS NULL) THEN RAISE EXCEPTION 'rollback blocked: users with NULL password_hash (LDAP accounts) exist'; END IF; END $$;
--rollback ALTER TABLE users ALTER COLUMN password_hash SET NOT NULL;
--rollback ALTER TABLE users DROP COLUMN IF EXISTS auth_provider;
