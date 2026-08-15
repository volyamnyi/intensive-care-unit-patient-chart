--liquibase formatted sql

--changeset split-core:1
--comment Add permissions column to users table for RBAC permission flags (e.g. PRESCRIBER)
ALTER TABLE users ADD COLUMN IF NOT EXISTS permissions VARCHAR(500);
--rollback ALTER TABLE users DROP COLUMN IF EXISTS permissions;

