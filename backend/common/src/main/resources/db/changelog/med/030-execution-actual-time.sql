--liquibase formatted sql

--changeset split-med:34
--comment Actual administration/dispensing time on prescription_executions (#443, phase 2
--comment of the actual-time epic, docs/Actual-Time-Design.md section 3). administered_at
--comment is the medical fact as recorded/adjusted; executed_at stays the technical time of
--comment the API operation - the two coincide only for legacy rows and for executions
--comment recorded without a manual time. The planned model (prescription_item_days.day_date
--comment + prescription_day_parts.period) and the status model are untouched. Naive local
--comment time (TIMESTAMP WITHOUT TIME ZONE) - the same contract as the rest of the schema
--comment (design section 6). Legacy rows are backfilled: the fact = the time the operation
--comment was recorded.
ALTER TABLE prescription_executions ADD COLUMN IF NOT EXISTS administered_at TIMESTAMP;
ALTER TABLE prescription_executions ADD COLUMN IF NOT EXISTS dispensed_at TIMESTAMP;
ALTER TABLE prescription_executions ADD COLUMN IF NOT EXISTS administered_at_manual BOOLEAN NOT NULL DEFAULT FALSE;

UPDATE prescription_executions SET administered_at = executed_at
 WHERE administered_at IS NULL AND executed_at IS NOT NULL;

--rollback ALTER TABLE prescription_executions DROP COLUMN IF EXISTS administered_at_manual;
--rollback ALTER TABLE prescription_executions DROP COLUMN IF EXISTS dispensed_at;
--rollback ALTER TABLE prescription_executions DROP COLUMN IF EXISTS administered_at;
