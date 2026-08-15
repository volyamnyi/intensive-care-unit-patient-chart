--liquibase formatted sql

--changeset split-icu:74
-- Add hourly planning fields to order_executions for the medication plan/execute workflow (form № 003-15/о)
ALTER TABLE order_executions ADD COLUMN hour INTEGER;
ALTER TABLE order_executions ADD COLUMN planned BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE order_executions ADD COLUMN planned_by BIGINT;
ALTER TABLE order_executions ADD COLUMN planned_at TIMESTAMP;
ALTER TABLE order_executions ADD COLUMN planned_dose VARCHAR(100);
ALTER TABLE order_executions ADD COLUMN planned_finished BOOLEAN NOT NULL DEFAULT FALSE;
ALTER TABLE order_executions ADD COLUMN completed_finished BOOLEAN NOT NULL DEFAULT FALSE;

--changeset split-icu:75
-- Backfill hour from executed_at for existing rows
UPDATE order_executions SET hour = EXTRACT(HOUR FROM executed_at) WHERE hour IS NULL;

--changeset split-icu:76
-- One execution record per (order, hour)
ALTER TABLE order_executions ADD CONSTRAINT uk_order_executions_order_hour UNIQUE (order_id, hour);

--changeset split-icu:77
-- Allow null executed_by/executed_at for planned-only records (execution data arrives on execute)
ALTER TABLE order_executions ALTER COLUMN executed_by DROP NOT NULL;
ALTER TABLE order_executions ALTER COLUMN executed_at DROP NOT NULL;

