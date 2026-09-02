--liquibase formatted sql

--changeset split-prosth:20
ALTER TABLE prosthetics_step_executions ADD COLUMN IF NOT EXISTS note TEXT;
--rollback ALTER TABLE prosthetics_step_executions DROP COLUMN IF EXISTS note;
