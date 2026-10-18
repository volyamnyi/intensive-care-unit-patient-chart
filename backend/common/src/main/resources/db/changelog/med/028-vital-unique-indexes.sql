--liquibase formatted sql

--changeset split-med:30
--comment #382: exactly one vitals grid per prescription list. getOrCreate is a
-- find-then-create; two concurrent first-loads created two grids x 106 rows.
-- The service converges on the constraint (loser re-queries the winner's grid).
CREATE UNIQUE INDEX uq_vital_sign_lists_prescription
    ON vital_sign_lists (prescription_list_id);
--rollback DROP INDEX uq_vital_sign_lists_prescription;

--changeset split-med:31
--comment #382: one vitals entry per day/period slot. getOrCreateEntry is a
-- find-then-create with the same race; the service converges on the constraint.
CREATE UNIQUE INDEX uq_vital_entries_day_period
    ON vital_sign_entries (day_id, period);
--rollback DROP INDEX uq_vital_entries_day_period;