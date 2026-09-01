--liquibase formatted sql

--changeset split-prosth:17
ALTER TABLE prosthetics_flow_instances ADD COLUMN IF NOT EXISTS parent_instance_id UUID REFERENCES prosthetics_flow_instances(id);
ALTER TABLE prosthetics_flow_instances ADD COLUMN IF NOT EXISTS branch_sequence INTEGER DEFAULT 1;
ALTER TABLE prosthetics_flow_instances ADD COLUMN IF NOT EXISTS origin_stage_id UUID;
ALTER TABLE prosthetics_flow_instances ADD COLUMN IF NOT EXISTS origin_step_id UUID;
ALTER TABLE prosthetics_flow_instances ADD COLUMN IF NOT EXISTS defect_payload JSONB;
CREATE INDEX IF NOT EXISTS idx_flow_instances_parent ON prosthetics_flow_instances(parent_instance_id);
--rollback DROP INDEX IF EXISTS idx_flow_instances_parent;
--rollback ALTER TABLE prosthetics_flow_instances DROP COLUMN IF EXISTS defect_payload;
--rollback ALTER TABLE prosthetics_flow_instances DROP COLUMN IF EXISTS origin_step_id;
--rollback ALTER TABLE prosthetics_flow_instances DROP COLUMN IF EXISTS origin_stage_id;
--rollback ALTER TABLE prosthetics_flow_instances DROP COLUMN IF EXISTS branch_sequence;
--rollback ALTER TABLE prosthetics_flow_instances DROP COLUMN IF EXISTS parent_instance_id;

--changeset split-prosth:18
CREATE TABLE IF NOT EXISTS prosthetics_brak_events (
  id UUID NOT NULL,
  instance_id UUID NOT NULL,
  stage_id UUID NOT NULL,
  step_id UUID NOT NULL,
  soft_tissue_misalignment BOOLEAN NOT NULL DEFAULT FALSE,
  pain_discomfort BOOLEAN NOT NULL DEFAULT FALSE,
  note TEXT,
  return_stage_id UUID NOT NULL,
  new_instance_id UUID REFERENCES prosthetics_flow_instances(id),
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by BIGINT NOT NULL DEFAULT 0,
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by BIGINT NOT NULL DEFAULT 0,
  version INTEGER NOT NULL DEFAULT 0,
  is_deleted BOOLEAN DEFAULT FALSE,
  CONSTRAINT pk_prosthetics_brak_events PRIMARY KEY (id),
  CONSTRAINT fk_brak_events_instance FOREIGN KEY (instance_id) REFERENCES prosthetics_flow_instances(id),
  CONSTRAINT fk_brak_events_new_instance FOREIGN KEY (new_instance_id) REFERENCES prosthetics_flow_instances(id),
  CONSTRAINT chk_brak_note_len CHECK (note IS NULL OR length(note) <= 1000)
);
CREATE INDEX IF NOT EXISTS idx_brak_events_instance ON prosthetics_brak_events(instance_id);
CREATE INDEX IF NOT EXISTS idx_brak_events_new_instance ON prosthetics_brak_events(new_instance_id);
--rollback DROP TABLE prosthetics_brak_events;

--changeset split-prosth:19
DROP INDEX IF EXISTS uq_flow_instances_active_order;
CREATE UNIQUE INDEX uq_flow_instances_active_order
  ON prosthetics_flow_instances(order_id)
  WHERE status NOT IN ('FAILED','COMPLETED','BRANCHED');
--rollback DROP INDEX IF EXISTS uq_flow_instances_active_order;
--rollback CREATE UNIQUE INDEX uq_flow_instances_active_order ON prosthetics_flow_instances(order_id) WHERE status NOT IN ('FAILED','COMPLETED');
