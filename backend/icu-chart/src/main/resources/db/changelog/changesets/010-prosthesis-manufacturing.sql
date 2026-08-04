--liquibase formatted sql

--changeset prosthetics-0:1
CREATE TABLE IF NOT EXISTS prosthetics_patients (
    id UUID NOT NULL,
    pib VARCHAR(255) NOT NULL,
    birth_date DATE,
    gender VARCHAR(16),
    height_cm INTEGER,
    weight_kg INTEGER,
    social_status VARCHAR(64),
    cause VARCHAR(255),
    amputation_date DATE,
    affected_limb VARCHAR(16),
    amputation_level VARCHAR(64),
    stump JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prosthetics_patients PRIMARY KEY (id)
);

--rollback DROP TABLE prosthetics_patients;

--changeset prosthetics-0:2
CREATE TABLE IF NOT EXISTS prosthetics_orders (
    id UUID NOT NULL,
    order_number VARCHAR(32) NOT NULL,
    patient_id UUID NOT NULL,
    prosthesis_type VARCHAR(64),
    product_type VARCHAR(32),
    amputation_level VARCHAR(64),
    limb_side VARCHAR(16),
    doctor_name VARCHAR(255),
    prescription_date DATE,
    materials JSONB,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    recipe_pdf_data BYTEA,
    recipe_pdf_generated_at TIMESTAMP,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prosthetics_orders PRIMARY KEY (id),
    CONSTRAINT uq_prosthetics_orders_number UNIQUE (order_number),
    CONSTRAINT fk_prosthetics_orders_patient FOREIGN KEY (patient_id) REFERENCES prosthetics_patients(id)
);

--rollback DROP TABLE prosthetics_orders;

--changeset prosthetics-0:3
CREATE TABLE IF NOT EXISTS prosthetics_flow_templates (
    id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    template_version INTEGER NOT NULL,
    product_type VARCHAR(32) NOT NULL,
    amputation_level VARCHAR(64),
    limb_side VARCHAR(16),
    status VARCHAR(32) NOT NULL DEFAULT 'DRAFT',
    estimated_duration_min INTEGER,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prosthetics_flow_templates PRIMARY KEY (id),
    CONSTRAINT uq_flow_template_name_version UNIQUE (name, template_version)
);

--rollback DROP TABLE prosthetics_flow_templates;

--changeset prosthetics-0:4
CREATE TABLE IF NOT EXISTS prosthetics_template_stages (
    id UUID NOT NULL,
    template_id UUID NOT NULL,
    order_index INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    type VARCHAR(32) NOT NULL,
    can_skip BOOLEAN DEFAULT FALSE,
    requires_approval BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prosthetics_template_stages PRIMARY KEY (id),
    CONSTRAINT fk_template_stages_template FOREIGN KEY (template_id) REFERENCES prosthetics_flow_templates(id)
);

--rollback DROP TABLE prosthetics_template_stages;

--changeset prosthetics-0:5
CREATE TABLE IF NOT EXISTS prosthetics_template_steps (
    id UUID NOT NULL,
    stage_id UUID NOT NULL,
    order_index INTEGER NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    step_type VARCHAR(32) NOT NULL,
    mandatory BOOLEAN NOT NULL DEFAULT TRUE,
    allow_backward BOOLEAN DEFAULT TRUE,
    auto_start_timer BOOLEAN DEFAULT FALSE,
    norm_duration_min INTEGER,
    rework_target_step_id UUID,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prosthetics_template_steps PRIMARY KEY (id),
    CONSTRAINT fk_template_steps_stage FOREIGN KEY (stage_id) REFERENCES prosthetics_template_stages(id)
);

--rollback DROP TABLE prosthetics_template_steps;

--changeset prosthetics-0:6
CREATE TABLE IF NOT EXISTS prosthetics_template_elements (
    id UUID NOT NULL,
    step_id UUID NOT NULL,
    order_index INTEGER NOT NULL,
    element_type VARCHAR(32) NOT NULL,
    label VARCHAR(500) NOT NULL,
    placeholder VARCHAR(500),
    required BOOLEAN DEFAULT FALSE,
    unit VARCHAR(32),
    min_value NUMERIC(12,3),
    max_value NUMERIC(12,3),
    min_count INTEGER,
    max_count INTEGER,
    mime_types VARCHAR(255),
    max_size_mb INTEGER,
    regex_pattern VARCHAR(500),
    options JSONB,
    validation_rules JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prosthetics_template_elements PRIMARY KEY (id),
    CONSTRAINT fk_template_elements_step FOREIGN KEY (step_id) REFERENCES prosthetics_template_steps(id)
);

--rollback DROP TABLE prosthetics_template_elements;

--changeset prosthetics-0:7
CREATE TABLE IF NOT EXISTS prosthetics_quality_gates (
    id UUID NOT NULL,
    stage_id UUID NOT NULL,
    name VARCHAR(255) NOT NULL,
    description TEXT,
    required_approver_role VARCHAR(32) NOT NULL,
    checklist JSONB,
    attachments_required BOOLEAN DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prosthetics_quality_gates PRIMARY KEY (id),
    CONSTRAINT uq_quality_gate_stage UNIQUE (stage_id),
    CONSTRAINT fk_quality_gates_stage FOREIGN KEY (stage_id) REFERENCES prosthetics_template_stages(id)
);

--rollback DROP TABLE prosthetics_quality_gates;

--changeset prosthetics-0:8
CREATE TABLE IF NOT EXISTS prosthetics_rework_loops (
    id UUID NOT NULL,
    gate_id UUID NOT NULL,
    target_stage_id UUID,
    target_step_id UUID,
    rework_type VARCHAR(16) NOT NULL,
    max_attempts INTEGER NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prosthetics_rework_loops PRIMARY KEY (id),
    CONSTRAINT fk_rework_loops_gate FOREIGN KEY (gate_id) REFERENCES prosthetics_quality_gates(id)
);

--rollback DROP TABLE prosthetics_rework_loops;

--changeset prosthetics-0:9
CREATE TABLE IF NOT EXISTS prosthetics_flow_instances (
    id UUID NOT NULL,
    template_id UUID NOT NULL,
    patient_id UUID,
    order_id UUID NOT NULL,
    assigned_user_id BIGINT,
    status VARCHAR(32) NOT NULL DEFAULT 'NEW',
    current_stage_id UUID,
    current_step_id UUID,
    start_time TIMESTAMP,
    end_time TIMESTAMP,
    total_active_seconds BIGINT DEFAULT 0,
    total_idle_seconds BIGINT DEFAULT 0,
    rework_count INTEGER DEFAULT 0,
    fail_reason TEXT,
    template_snapshot JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prosthetics_flow_instances PRIMARY KEY (id),
    CONSTRAINT fk_flow_instances_template FOREIGN KEY (template_id) REFERENCES prosthetics_flow_templates(id),
    CONSTRAINT fk_flow_instances_patient FOREIGN KEY (patient_id) REFERENCES prosthetics_patients(id),
    CONSTRAINT fk_flow_instances_order FOREIGN KEY (order_id) REFERENCES prosthetics_orders(id)
);

--rollback DROP TABLE prosthetics_flow_instances;

--changeset prosthetics-0:10
CREATE TABLE IF NOT EXISTS prosthetics_step_executions (
    id UUID NOT NULL,
    instance_id UUID NOT NULL,
    stage_id UUID NOT NULL,
    step_id UUID NOT NULL,
    attempt_number INTEGER NOT NULL DEFAULT 1,
    status VARCHAR(32) NOT NULL DEFAULT 'NOT_STARTED',
    started_at TIMESTAMP,
    completed_at TIMESTAMP,
    active_seconds BIGINT DEFAULT 0,
    values JSONB,
    completed_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prosthetics_step_executions PRIMARY KEY (id),
    CONSTRAINT uq_step_execution_attempt UNIQUE (instance_id, step_id, attempt_number),
    CONSTRAINT fk_step_executions_instance FOREIGN KEY (instance_id) REFERENCES prosthetics_flow_instances(id)
);

--rollback DROP TABLE prosthetics_step_executions;

--changeset prosthetics-0:11
CREATE TABLE IF NOT EXISTS prosthetics_resource_usages (
    id UUID NOT NULL,
    instance_id UUID NOT NULL,
    step_execution_id UUID,
    material VARCHAR(255) NOT NULL,
    qty NUMERIC(12,3),
    unit VARCHAR(32),
    minutes INTEGER,
    recorded_by BIGINT,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prosthetics_resource_usages PRIMARY KEY (id),
    CONSTRAINT fk_resource_usages_instance FOREIGN KEY (instance_id) REFERENCES prosthetics_flow_instances(id),
    CONSTRAINT fk_resource_usages_execution FOREIGN KEY (step_execution_id) REFERENCES prosthetics_step_executions(id)
);

--rollback DROP TABLE prosthetics_resource_usages;

--changeset prosthetics-0:12
CREATE TABLE IF NOT EXISTS prosthetics_gate_decisions (
    id UUID NOT NULL,
    instance_id UUID NOT NULL,
    gate_id UUID NOT NULL,
    decision VARCHAR(16) NOT NULL,
    criteria_confirmed JSONB,
    comment TEXT,
    decided_by BIGINT,
    decided_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prosthetics_gate_decisions PRIMARY KEY (id),
    CONSTRAINT fk_gate_decisions_instance FOREIGN KEY (instance_id) REFERENCES prosthetics_flow_instances(id),
    CONSTRAINT fk_gate_decisions_gate FOREIGN KEY (gate_id) REFERENCES prosthetics_quality_gates(id)
);

--rollback DROP TABLE prosthetics_gate_decisions;

--changeset prosthetics-0:13
CREATE TABLE IF NOT EXISTS prosthetics_evidence_files (
    id UUID NOT NULL,
    step_execution_id UUID NOT NULL,
    file_name VARCHAR(500) NOT NULL,
    mime_type VARCHAR(100) NOT NULL,
    size_bytes BIGINT NOT NULL,
    checksum VARCHAR(64),
    file_data BYTEA NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prosthetics_evidence_files PRIMARY KEY (id),
    CONSTRAINT fk_evidence_files_execution FOREIGN KEY (step_execution_id) REFERENCES prosthetics_step_executions(id)
);

--rollback DROP TABLE prosthetics_evidence_files;

--changeset prosthetics-0:14
CREATE TABLE IF NOT EXISTS prosthetics_failure_snapshots (
    id UUID NOT NULL,
    instance_id UUID NOT NULL,
    category VARCHAR(64) NOT NULL,
    description TEXT,
    snapshot JSONB,
    created_at TIMESTAMP NOT NULL DEFAULT NOW(),
    created_by BIGINT NOT NULL DEFAULT 0,
    updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
    updated_by BIGINT NOT NULL DEFAULT 0,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE,
    CONSTRAINT pk_prosthetics_failure_snapshots PRIMARY KEY (id),
    CONSTRAINT uq_failure_snapshot_instance UNIQUE (instance_id),
    CONSTRAINT fk_failure_snapshots_instance FOREIGN KEY (instance_id) REFERENCES prosthetics_flow_instances(id)
);

--rollback DROP TABLE prosthetics_failure_snapshots;

--changeset prosthetics-0:15
CREATE UNIQUE INDEX IF NOT EXISTS uq_flow_instances_active_order
    ON prosthetics_flow_instances(order_id)
    WHERE status NOT IN ('FAILED', 'COMPLETED');
CREATE INDEX IF NOT EXISTS idx_prosthetics_patients_pib ON prosthetics_patients(pib);
CREATE INDEX IF NOT EXISTS idx_prosthetics_orders_patient ON prosthetics_orders(patient_id);
CREATE INDEX IF NOT EXISTS idx_prosthetics_orders_status ON prosthetics_orders(status);
CREATE INDEX IF NOT EXISTS idx_template_stages_template ON prosthetics_template_stages(template_id);
CREATE INDEX IF NOT EXISTS idx_template_steps_stage ON prosthetics_template_steps(stage_id);
CREATE INDEX IF NOT EXISTS idx_template_elements_step ON prosthetics_template_elements(step_id);
CREATE INDEX IF NOT EXISTS idx_rework_loops_gate ON prosthetics_rework_loops(gate_id);
CREATE INDEX IF NOT EXISTS idx_flow_instances_assignee ON prosthetics_flow_instances(assigned_user_id);
CREATE INDEX IF NOT EXISTS idx_flow_instances_status ON prosthetics_flow_instances(status);
CREATE INDEX IF NOT EXISTS idx_step_executions_instance ON prosthetics_step_executions(instance_id);
CREATE INDEX IF NOT EXISTS idx_resource_usages_instance ON prosthetics_resource_usages(instance_id);
CREATE INDEX IF NOT EXISTS idx_gate_decisions_instance ON prosthetics_gate_decisions(instance_id);
CREATE INDEX IF NOT EXISTS idx_evidence_files_execution ON prosthetics_evidence_files(step_execution_id);

--rollback DROP INDEX IF EXISTS idx_evidence_files_execution;
--rollback DROP INDEX IF EXISTS idx_gate_decisions_instance;
--rollback DROP INDEX IF EXISTS idx_resource_usages_instance;
--rollback DROP INDEX IF EXISTS idx_step_executions_instance;
--rollback DROP INDEX IF EXISTS idx_flow_instances_status;
--rollback DROP INDEX IF EXISTS idx_flow_instances_assignee;
--rollback DROP INDEX IF EXISTS idx_rework_loops_gate;
--rollback DROP INDEX IF EXISTS idx_template_elements_step;
--rollback DROP INDEX IF EXISTS idx_template_steps_stage;
--rollback DROP INDEX IF EXISTS idx_template_stages_template;
--rollback DROP INDEX IF EXISTS idx_prosthetics_orders_status;
--rollback DROP INDEX IF EXISTS idx_prosthetics_orders_patient;
--rollback DROP INDEX IF EXISTS idx_prosthetics_patients_pib;
--rollback DROP INDEX IF EXISTS uq_flow_instances_active_order;
