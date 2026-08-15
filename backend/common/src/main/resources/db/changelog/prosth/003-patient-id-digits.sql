--liquibase formatted sql

--changeset split-prosth:1
-- Unified patient ID format: digits-only strings (e.g. "1234"), no letters,
-- separators or spaces. Converts prosthetics patient identifiers from UUID
-- to VARCHAR(32) and enforces digits-only with CHECK constraints.

ALTER TABLE prosthetics_flow_instances DROP CONSTRAINT IF EXISTS fk_flow_instances_patient;
ALTER TABLE prosthetics_orders DROP CONSTRAINT IF EXISTS fk_prosthetics_orders_patient;

ALTER TABLE prosthetics_patients ALTER COLUMN id TYPE VARCHAR(32)
    USING (regexp_replace(id::text, '\D', '', 'g'));
ALTER TABLE prosthetics_orders ALTER COLUMN patient_id TYPE VARCHAR(32)
    USING (regexp_replace(patient_id::text, '\D', '', 'g'));
ALTER TABLE prosthetics_flow_instances ALTER COLUMN patient_id TYPE VARCHAR(32)
    USING (regexp_replace(patient_id::text, '\D', '', 'g'));

ALTER TABLE prosthetics_orders
    ADD CONSTRAINT fk_prosthetics_orders_patient
    FOREIGN KEY (patient_id) REFERENCES prosthetics_patients(id);
ALTER TABLE prosthetics_flow_instances
    ADD CONSTRAINT fk_flow_instances_patient
    FOREIGN KEY (patient_id) REFERENCES prosthetics_patients(id);

ALTER TABLE prosthetics_patients
    ADD CONSTRAINT chk_prosthetics_patients_id_digits CHECK (id ~ '^[0-9]+$');
ALTER TABLE prosthetics_orders
    ADD CONSTRAINT chk_prosthetics_orders_patient_digits CHECK (patient_id ~ '^[0-9]+$');
ALTER TABLE prosthetics_flow_instances
    ADD CONSTRAINT chk_prosthetics_flow_instances_patient_digits
    CHECK (patient_id IS NULL OR patient_id ~ '^[0-9]+$');

--rollback ALTER TABLE prosthetics_flow_instances DROP CONSTRAINT IF EXISTS chk_prosthetics_flow_instances_patient_digits;
--rollback ALTER TABLE prosthetics_orders DROP CONSTRAINT IF EXISTS chk_prosthetics_orders_patient_digits;
--rollback ALTER TABLE prosthetics_patients DROP CONSTRAINT IF EXISTS chk_prosthetics_patients_id_digits;
--rollback ALTER TABLE prosthetics_flow_instances DROP CONSTRAINT IF EXISTS fk_flow_instances_patient;
--rollback ALTER TABLE prosthetics_orders DROP CONSTRAINT IF EXISTS fk_prosthetics_orders_patient;
--rollback ALTER TABLE prosthetics_flow_instances ALTER COLUMN patient_id TYPE UUID USING NULL;
--rollback ALTER TABLE prosthetics_orders ALTER COLUMN patient_id TYPE UUID USING NULL;
--rollback ALTER TABLE prosthetics_patients ALTER COLUMN id TYPE UUID USING NULL;

