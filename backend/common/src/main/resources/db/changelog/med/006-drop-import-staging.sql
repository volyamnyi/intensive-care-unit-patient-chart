--liquibase formatted sql

--changeset split-med:26
-- Legacy MedicineList one-shot import cancelled (no prod run ever happened):
-- drop its staging tables. FK children first, then the parent run table.
DROP TABLE IF EXISTS import_quarantine;
DROP TABLE IF EXISTS import_id_map;
DROP TABLE IF EXISTS import_run;

--rollback CREATE TABLE IF NOT EXISTS import_run (
--rollback     run_id UUID NOT NULL,
--rollback     source_hash VARCHAR(128) NOT NULL,
--rollback     started_at TIMESTAMP NOT NULL DEFAULT NOW(),
--rollback     finished_at TIMESTAMP,
--rollback     status VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
--rollback     counts TEXT,
--rollback     CONSTRAINT pk_import_run PRIMARY KEY (run_id)
--rollback );
--rollback CREATE TABLE IF NOT EXISTS import_id_map (
--rollback     old_kind VARCHAR(16) NOT NULL,
--rollback     old_id VARCHAR(64) NOT NULL,
--rollback     new_id UUID NOT NULL,
--rollback     run_id UUID NOT NULL,
--rollback     CONSTRAINT pk_import_id_map PRIMARY KEY (old_kind, old_id),
--rollback     CONSTRAINT fk_import_id_map_run FOREIGN KEY (run_id) REFERENCES import_run(run_id)
--rollback );
--rollback CREATE TABLE IF NOT EXISTS import_quarantine (
--rollback     id UUID NOT NULL,
--rollback     run_id UUID NOT NULL,
--rollback     old_list_id VARCHAR(64) NOT NULL,
--rollback     old_kind VARCHAR(16) NOT NULL,
--rollback     reason VARCHAR(64) NOT NULL,
--rollback     payload TEXT,
--rollback     CONSTRAINT pk_import_quarantine PRIMARY KEY (id),
--rollback     CONSTRAINT fk_import_quarantine_run FOREIGN KEY (run_id) REFERENCES import_run(run_id)
--rollback );
