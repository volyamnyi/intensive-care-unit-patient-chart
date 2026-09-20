--liquibase formatted sql

--changeset split-med:21
CREATE TABLE IF NOT EXISTS drug_interaction_drugs (
    id UUID NOT NULL,
    atc_code VARCHAR(20) NOT NULL,
    source_id VARCHAR(50),
    generic_en VARCHAR(500),
    ukrainian_raw VARCHAR(500) NOT NULL,
    confidence NUMERIC(5,3),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_drug_interaction_drugs PRIMARY KEY (id),
    CONSTRAINT uq_drug_interaction_drugs_atc UNIQUE (atc_code)
);

--rollback DROP TABLE drug_interaction_drugs;

--changeset split-med:22
CREATE TABLE IF NOT EXISTS drug_interaction_pairs (
    id UUID NOT NULL,
    drug_a_atc VARCHAR(20) NOT NULL,
    drug_b_atc VARCHAR(20) NOT NULL,
    severity VARCHAR(16) NOT NULL,
    interaction TEXT NOT NULL,
    interaction_id VARCHAR(100),
    interaction_confidence NUMERIC(5,3),
    row_hash CHAR(64) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_drug_interaction_pairs PRIMARY KEY (id),
    CONSTRAINT uq_drug_interaction_pairs UNIQUE (drug_a_atc, drug_b_atc, severity, row_hash),
    CONSTRAINT ck_drug_interaction_pairs_order CHECK (drug_a_atc < drug_b_atc)
);

--rollback DROP TABLE drug_interaction_pairs;

--changeset split-med:23
CREATE INDEX IF NOT EXISTS idx_drug_interaction_pairs_a ON drug_interaction_pairs(drug_a_atc, severity);
CREATE INDEX IF NOT EXISTS idx_drug_interaction_pairs_b ON drug_interaction_pairs(drug_b_atc, severity);

--rollback DROP INDEX IF EXISTS idx_drug_interaction_pairs_b;
--rollback DROP INDEX IF EXISTS idx_drug_interaction_pairs_a;

--changeset split-med:24
ALTER TABLE prescription_items ADD COLUMN medicine_atc_code VARCHAR(20);

--rollback ALTER TABLE prescription_items DROP COLUMN medicine_atc_code;

--changeset split-med:25
DROP TABLE IF EXISTS drug_interaction_rules;

--rollback CREATE TABLE drug_interaction_rules (
--rollback     id UUID NOT NULL,
--rollback     ptg_code_a VARCHAR(50) NOT NULL,
--rollback     ptg_code_b VARCHAR(50) NOT NULL,
--rollback     severity VARCHAR(16) NOT NULL DEFAULT 'WARNING',
--rollback     description TEXT,
--rollback     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
--rollback     created_by BIGINT NOT NULL DEFAULT 0,
--rollback     updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
--rollback     updated_by BIGINT NOT NULL DEFAULT 0,
--rollback     version INTEGER NOT NULL DEFAULT 0,
--rollback     is_deleted BOOLEAN DEFAULT FALSE,
--rollback     CONSTRAINT pk_drug_interaction_rules PRIMARY KEY (id),
--rollback     CONSTRAINT uq_ptg_pair UNIQUE (ptg_code_a, ptg_code_b)
--rollback );
