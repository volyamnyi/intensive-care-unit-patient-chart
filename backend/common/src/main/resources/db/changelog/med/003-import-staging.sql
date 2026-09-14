--liquibase formatted sql

--changeset split-med:18
CREATE TABLE IF NOT EXISTS import_run (
    run_id UUID NOT NULL,
    source_hash VARCHAR(128) NOT NULL,
    started_at TIMESTAMP NOT NULL DEFAULT NOW(),
    finished_at TIMESTAMP,
    status VARCHAR(16) NOT NULL DEFAULT 'RUNNING',
    counts TEXT,
    CONSTRAINT pk_import_run PRIMARY KEY (run_id)
);

--rollback DROP TABLE import_run;

--changeset split-med:19
CREATE TABLE IF NOT EXISTS import_id_map (
    old_kind VARCHAR(16) NOT NULL,
    old_id VARCHAR(64) NOT NULL,
    new_id UUID NOT NULL,
    run_id UUID NOT NULL,
    CONSTRAINT pk_import_id_map PRIMARY KEY (old_kind, old_id),
    CONSTRAINT fk_import_id_map_run FOREIGN KEY (run_id) REFERENCES import_run(run_id)
);

--rollback DROP TABLE import_id_map;

--changeset split-med:20
CREATE TABLE IF NOT EXISTS import_quarantine (
    id UUID NOT NULL,
    run_id UUID NOT NULL,
    old_list_id VARCHAR(64) NOT NULL,
    old_kind VARCHAR(16) NOT NULL,
    reason VARCHAR(64) NOT NULL,
    payload TEXT,
    CONSTRAINT pk_import_quarantine PRIMARY KEY (id),
    CONSTRAINT fk_import_quarantine_run FOREIGN KEY (run_id) REFERENCES import_run(run_id)
);

--rollback DROP TABLE import_quarantine;
