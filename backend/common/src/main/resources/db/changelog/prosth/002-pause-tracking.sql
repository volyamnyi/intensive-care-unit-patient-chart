--liquibase formatted sql

--changeset split-prosth:16
ALTER TABLE prosthetics_flow_instances
    ADD COLUMN IF NOT EXISTS paused_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS resumed_at TIMESTAMP,
    ADD COLUMN IF NOT EXISTS pause_category VARCHAR(16);

--rollback ALTER TABLE prosthetics_flow_instances DROP COLUMN IF EXISTS paused_at, DROP COLUMN IF EXISTS resumed_at, DROP COLUMN IF EXISTS pause_category;

