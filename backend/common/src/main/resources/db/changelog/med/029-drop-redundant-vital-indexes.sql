--liquibase formatted sql

-- Drop the non-unique indexes that split-med:30/31 superseded (#382).
-- A unique index on the same column list serves every access pattern the
-- plain one did (equal columns, or the unique index has the plain one as a
-- prefix), so keeping both only doubles the write amplification of every
-- grid/entry insert and gives the planner a duplicate to choose between.
-- The applied med/001 changeset is left untouched (checksum safety): the
-- supersession is expressed here as a forward fix, with the original index
-- recreated by the rollback.

--changeset split-med:32
--comment vital_sign_lists.prescription_list_id - now covered by the unique uq_vital_sign_lists_prescription (split-med:30)
DROP INDEX IF EXISTS idx_vital_sign_lists_prescription;
--rollback CREATE INDEX IF NOT EXISTS idx_vital_sign_lists_prescription ON vital_sign_lists(prescription_list_id);

--changeset split-med:33
--comment vital_sign_entries.day_id - covered by the unique uq_vital_entries_day_period (split-med:31), day_id is its leading column
DROP INDEX IF EXISTS idx_vital_sign_entries_day;
--rollback CREATE INDEX IF NOT EXISTS idx_vital_sign_entries_day ON vital_sign_entries(day_id);
