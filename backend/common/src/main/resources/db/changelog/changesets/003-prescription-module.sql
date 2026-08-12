--liquibase formatted sql

--changeset phase-1:1
CREATE TABLE IF NOT EXISTS prescription_lists (
    id UUID NOT NULL,
    patient_id BIGINT NOT NULL,
    hospitalization_id UUID,
    department_id BIGINT,
    document_name VARCHAR(255),
    status VARCHAR(32) NOT NULL DEFAULT 'Saved',
    editing_user_id UUID,
    editing_started_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prescription_lists PRIMARY KEY (id)
);

--rollback DROP TABLE prescription_lists;

--changeset phase-1:2
CREATE TABLE IF NOT EXISTS prescription_items (
    id UUID NOT NULL,
    list_id UUID NOT NULL,
    medicine_name VARCHAR(500) NOT NULL,
    medicine_method VARCHAR(255),
    regime VARCHAR(255),
    status VARCHAR(32),
    sort_order INTEGER DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prescription_items PRIMARY KEY (id),
    CONSTRAINT fk_prescription_items_list FOREIGN KEY (list_id) REFERENCES prescription_lists(id)
);

--rollback DROP TABLE prescription_items;

--changeset phase-1:3
CREATE TABLE IF NOT EXISTS prescription_item_days (
    id UUID NOT NULL,
    item_id UUID NOT NULL,
    day_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prescription_item_days PRIMARY KEY (id),
    CONSTRAINT fk_item_days_item FOREIGN KEY (item_id) REFERENCES prescription_items(id)
);

--rollback DROP TABLE prescription_item_days;

--changeset phase-1:4
CREATE TABLE IF NOT EXISTS prescription_day_parts (
    id UUID NOT NULL,
    day_id UUID NOT NULL,
    period VARCHAR(8) NOT NULL CHECK (period IN ('morning','evening')),
    dose VARCHAR(100),
    is_planned BOOLEAN DEFAULT FALSE,
    is_planned_finished BOOLEAN DEFAULT FALSE,
    is_completed BOOLEAN DEFAULT FALSE,
    is_completed_finished BOOLEAN DEFAULT FALSE,
    doctor_name VARCHAR(255),
    nurse_name VARCHAR(255),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prescription_day_parts PRIMARY KEY (id),
    CONSTRAINT fk_day_parts_day FOREIGN KEY (day_id) REFERENCES prescription_item_days(id)
);

--rollback DROP TABLE prescription_day_parts;

--changeset phase-1:5
CREATE TABLE IF NOT EXISTS prescription_executions (
    id UUID NOT NULL,
    day_part_id UUID NOT NULL,
    executed_by UUID,
    executed_at TIMESTAMP,
    actual_dose VARCHAR(100),
    status VARCHAR(32),
    requires_2p_auth BOOLEAN DEFAULT FALSE,
    second_person_id UUID,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prescription_executions PRIMARY KEY (id),
    CONSTRAINT fk_executions_day_part FOREIGN KEY (day_part_id) REFERENCES prescription_day_parts(id)
);

--rollback DROP TABLE prescription_executions;

--changeset phase-1:6
CREATE TABLE IF NOT EXISTS prescription_signatures (
    id UUID NOT NULL,
    item_id UUID,
    user_id UUID NOT NULL,
    role VARCHAR(32) NOT NULL,
    signed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    hash VARCHAR(255),
    status VARCHAR(32),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prescription_signatures PRIMARY KEY (id),
    CONSTRAINT fk_signatures_item FOREIGN KEY (item_id) REFERENCES prescription_items(id)
);

--rollback DROP TABLE prescription_signatures;

--changeset phase-1:7
CREATE TABLE IF NOT EXISTS vital_sign_lists (
    id UUID NOT NULL,
    prescription_list_id UUID NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_vital_sign_lists PRIMARY KEY (id),
    CONSTRAINT fk_vital_lists_prescription FOREIGN KEY (prescription_list_id) REFERENCES prescription_lists(id)
);

--rollback DROP TABLE vital_sign_lists;

--changeset phase-1:8
CREATE TABLE IF NOT EXISTS vital_sign_days (
    id UUID NOT NULL,
    vital_list_id UUID NOT NULL,
    day_date DATE NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_vital_sign_days PRIMARY KEY (id),
    CONSTRAINT fk_vital_days_list FOREIGN KEY (vital_list_id) REFERENCES vital_sign_lists(id)
);

--rollback DROP TABLE vital_sign_days;

--changeset phase-1:9
CREATE TABLE IF NOT EXISTS vital_sign_entries (
    id UUID NOT NULL,
    day_id UUID NOT NULL,
    period VARCHAR(8) NOT NULL CHECK (period IN ('morning','evening')),
    temperature DECIMAL(4,1),
    systolic_bp INTEGER,
    diastolic_bp INTEGER,
    spo2 INTEGER,
    pulse INTEGER,
    stool VARCHAR(50),
    pain_score INTEGER CHECK (pain_score BETWEEN 0 AND 10),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_vital_sign_entries PRIMARY KEY (id),
    CONSTRAINT fk_vital_entries_day FOREIGN KEY (day_id) REFERENCES vital_sign_days(id)
);

--rollback DROP TABLE vital_sign_entries;

--changeset phase-1:10
CREATE TABLE IF NOT EXISTS medicine_catalog_cache (
    id BIGINT NOT NULL,
    name VARCHAR(500) NOT NULL,
    category_ref INTEGER,
    ptg_code VARCHAR(50),
    is_high_risk BOOLEAN DEFAULT FALSE,
    cached_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_medicine_catalog_cache PRIMARY KEY (id)
);

--rollback DROP TABLE medicine_catalog_cache;

--changeset phase-1:11
CREATE TABLE IF NOT EXISTS allergy_cache (
    id UUID NOT NULL,
    patient_id BIGINT NOT NULL,
    allergen_name VARCHAR(500) NOT NULL,
    source_document_id INTEGER,
    cached_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_allergy_cache PRIMARY KEY (id),
    CONSTRAINT uq_allergy_patient_name UNIQUE (patient_id, allergen_name)
);

--rollback DROP TABLE allergy_cache;

--changeset phase-1:12
CREATE TABLE IF NOT EXISTS drug_interaction_rules (
    id UUID NOT NULL,
    ptg_code_a VARCHAR(50) NOT NULL,
    ptg_code_b VARCHAR(50) NOT NULL,
    severity VARCHAR(16) NOT NULL DEFAULT 'WARNING',
    description TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_drug_interaction_rules PRIMARY KEY (id),
    CONSTRAINT uq_ptg_pair UNIQUE (ptg_code_a, ptg_code_b)
);

--rollback DROP TABLE drug_interaction_rules;

--changeset phase-1:13
CREATE TABLE IF NOT EXISTS telegram_subscriptions (
    chat_id BIGINT NOT NULL,
    subscribed_at TIMESTAMP NOT NULL DEFAULT NOW(),
    CONSTRAINT pk_telegram_subscriptions PRIMARY KEY (chat_id)
);

--rollback DROP TABLE telegram_subscriptions;

--changeset phase-1:14
CREATE INDEX IF NOT EXISTS idx_prescription_lists_patient ON prescription_lists(patient_id);
CREATE INDEX IF NOT EXISTS idx_prescription_lists_status ON prescription_lists(status);
CREATE INDEX IF NOT EXISTS idx_prescription_items_list ON prescription_items(list_id);
CREATE INDEX IF NOT EXISTS idx_prescription_item_days_item ON prescription_item_days(item_id);
CREATE INDEX IF NOT EXISTS idx_prescription_day_parts_day ON prescription_day_parts(day_id);
CREATE INDEX IF NOT EXISTS idx_prescription_executions_day_part ON prescription_executions(day_part_id);
CREATE INDEX IF NOT EXISTS idx_vital_sign_lists_prescription ON vital_sign_lists(prescription_list_id);
CREATE INDEX IF NOT EXISTS idx_vital_sign_days_list ON vital_sign_days(vital_list_id);
CREATE INDEX IF NOT EXISTS idx_vital_sign_entries_day ON vital_sign_entries(day_id);
CREATE INDEX IF NOT EXISTS idx_allergy_cache_patient ON allergy_cache(patient_id);
CREATE INDEX IF NOT EXISTS idx_medicine_catalog_name ON medicine_catalog_cache(name);

--rollback DROP INDEX IF EXISTS idx_medicine_catalog_name;
--rollback DROP INDEX IF EXISTS idx_allergy_cache_patient;
--rollback DROP INDEX IF EXISTS idx_vital_sign_entries_day;
--rollback DROP INDEX IF EXISTS idx_vital_sign_days_list;
--rollback DROP INDEX IF EXISTS idx_vital_sign_lists_prescription;
--rollback DROP INDEX IF EXISTS idx_prescription_executions_day_part;
--rollback DROP INDEX IF EXISTS idx_prescription_day_parts_day;
--rollback DROP INDEX IF EXISTS idx_prescription_item_days_item;
--rollback DROP INDEX IF EXISTS idx_prescription_items_list;
--rollback DROP INDEX IF EXISTS idx_prescription_lists_status;
--rollback DROP INDEX IF EXISTS idx_prescription_lists_patient;

--changeset phase-1:15
ALTER TABLE prescription_day_parts DROP CONSTRAINT IF EXISTS prescription_day_parts_period_check;
ALTER TABLE prescription_day_parts ADD CONSTRAINT prescription_day_parts_period_check CHECK (period IN ('morning','day','evening','night'));

--rollback ALTER TABLE prescription_day_parts DROP CONSTRAINT IF EXISTS prescription_day_parts_period_check;
--rollback ALTER TABLE prescription_day_parts ADD CONSTRAINT prescription_day_parts_period_check CHECK (period IN ('morning','evening'));

--changeset phase-1:16
ALTER TABLE vital_sign_entries DROP CONSTRAINT IF EXISTS vital_sign_entries_period_check;
ALTER TABLE vital_sign_entries ADD CONSTRAINT vital_sign_entries_period_check CHECK (period IN ('morning','day','evening','night'));

--rollback ALTER TABLE vital_sign_entries DROP CONSTRAINT IF EXISTS vital_sign_entries_period_check;
--rollback ALTER TABLE vital_sign_entries ADD CONSTRAINT vital_sign_entries_period_check CHECK (period IN ('morning','evening'));
