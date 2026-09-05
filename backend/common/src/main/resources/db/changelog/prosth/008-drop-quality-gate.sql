--liquibase formatted sql

--changeset split-prosth:23
--comment Quality Gate removal (QG-Removal Phase 2, issue #230): the QC subsystem
--comment (QualityGate/GateDecision/ReworkLoop, WAITING_REVIEW/CORRECTION/FAILED_QC,
--comment rework_count, rework_target_step_id, PROSTHETICS_GATE_DECISION) is deleted
--comment from the backend. This changeset removes its schema. Historic rows in removed
--comment states are remapped to the linear flow first; AuditLog history is kept as-is.
UPDATE prosthetics_flow_instances SET status = 'IN_PROGRESS' WHERE status IN ('WAITING_REVIEW', 'CORRECTION');
--rollback SELECT 1;

--changeset split-prosth:24
UPDATE prosthetics_flow_instances SET status = 'FAILED' WHERE status = 'FAILED_QC';
--rollback SELECT 1;

--changeset split-prosth:25
DROP INDEX IF EXISTS idx_gate_decisions_instance;
--rollback CREATE INDEX IF NOT EXISTS idx_gate_decisions_instance ON prosthetics_gate_decisions(instance_id);

--changeset split-prosth:26
DROP INDEX IF EXISTS idx_rework_loops_gate;
--rollback CREATE INDEX IF NOT EXISTS idx_rework_loops_gate ON prosthetics_rework_loops(gate_id);

--changeset split-prosth:27
DROP TABLE IF EXISTS prosthetics_gate_decisions;
--rollback CREATE TABLE IF NOT EXISTS prosthetics_gate_decisions (
--rollback     id UUID NOT NULL,
--rollback     instance_id UUID NOT NULL,
--rollback     gate_id UUID NOT NULL,
--rollback     decision VARCHAR(16) NOT NULL,
--rollback     criteria_confirmed JSONB,
--rollback     comment TEXT,
--rollback     decided_by BIGINT,
--rollback     decided_at TIMESTAMP NOT NULL DEFAULT NOW(),
--rollback     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
--rollback     created_by BIGINT NOT NULL DEFAULT 0,
--rollback     updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
--rollback     updated_by BIGINT NOT NULL DEFAULT 0,
--rollback     version INTEGER NOT NULL DEFAULT 0,
--rollback     is_deleted BOOLEAN DEFAULT FALSE,
--rollback     CONSTRAINT pk_prosthetics_gate_decisions PRIMARY KEY (id),
--rollback     CONSTRAINT fk_gate_decisions_instance FOREIGN KEY (instance_id) REFERENCES prosthetics_flow_instances(id),
--rollback     CONSTRAINT fk_gate_decisions_gate FOREIGN KEY (gate_id) REFERENCES prosthetics_quality_gates(id)
--rollback );

--changeset split-prosth:28
DROP TABLE IF EXISTS prosthetics_rework_loops;
--rollback CREATE TABLE IF NOT EXISTS prosthetics_rework_loops (
--rollback     id UUID NOT NULL,
--rollback     gate_id UUID NOT NULL,
--rollback     target_stage_id UUID,
--rollback     target_step_id UUID,
--rollback     rework_type VARCHAR(16) NOT NULL,
--rollback     max_attempts INTEGER NOT NULL DEFAULT 1,
--rollback     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
--rollback     created_by BIGINT NOT NULL DEFAULT 0,
--rollback     updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
--rollback     updated_by BIGINT NOT NULL DEFAULT 0,
--rollback     version INTEGER NOT NULL DEFAULT 0,
--rollback     is_deleted BOOLEAN DEFAULT FALSE,
--rollback     CONSTRAINT pk_prosthetics_rework_loops PRIMARY KEY (id),
--rollback     CONSTRAINT fk_rework_loops_gate FOREIGN KEY (gate_id) REFERENCES prosthetics_quality_gates(id)
--rollback );

--changeset split-prosth:29
DROP TABLE IF EXISTS prosthetics_quality_gates;
--rollback CREATE TABLE IF NOT EXISTS prosthetics_quality_gates (
--rollback     id UUID NOT NULL,
--rollback     stage_id UUID NOT NULL,
--rollback     name VARCHAR(255) NOT NULL,
--rollback     description TEXT,
--rollback     required_approver_role VARCHAR(32) NOT NULL,
--rollback     checklist JSONB,
--rollback     attachments_required BOOLEAN DEFAULT FALSE,
--rollback     created_at TIMESTAMP NOT NULL DEFAULT NOW(),
--rollback     created_by BIGINT NOT NULL DEFAULT 0,
--rollback     updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
--rollback     updated_by BIGINT NOT NULL DEFAULT 0,
--rollback     version INTEGER NOT NULL DEFAULT 0,
--rollback     is_deleted BOOLEAN DEFAULT FALSE,
--rollback     CONSTRAINT pk_prosthetics_quality_gates PRIMARY KEY (id),
--rollback     CONSTRAINT uq_quality_gate_stage UNIQUE (stage_id),
--rollback     CONSTRAINT fk_quality_gates_stage FOREIGN KEY (stage_id) REFERENCES prosthetics_template_stages(id)
--rollback );

--changeset split-prosth:30
ALTER TABLE prosthetics_flow_instances DROP COLUMN IF EXISTS rework_count;
--rollback ALTER TABLE prosthetics_flow_instances ADD COLUMN IF NOT EXISTS rework_count INTEGER DEFAULT 0;

--changeset split-prosth:31
ALTER TABLE prosthetics_template_steps DROP COLUMN IF EXISTS rework_target_step_id;
--rollback ALTER TABLE prosthetics_template_steps ADD COLUMN IF NOT EXISTS rework_target_step_id UUID;
