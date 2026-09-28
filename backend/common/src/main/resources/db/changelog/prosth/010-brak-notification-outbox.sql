--liquibase formatted sql

--changeset split-prosth:28
--comment Brak email outbox (issue #320, v2 guaranteed delivery): every confirmed
--comment stage-6 brak inserts one PENDING row in the same transaction as the brak
--comment itself (atomic: no brak without a queued notification and vice versa).
--comment A scheduled worker retries PENDING/FAILED rows; terminal states are
--comment SENT/SKIPPED/DEAD. No such table existed in v1 (delivery was eager-only).
CREATE TABLE IF NOT EXISTS prosthetics_brak_notifications (
  id UUID NOT NULL,
  brak_event_id UUID NOT NULL,
  status VARCHAR(16) NOT NULL DEFAULT 'PENDING',
  attempts INTEGER NOT NULL DEFAULT 0,
  last_error TEXT,
  created_at TIMESTAMP NOT NULL DEFAULT NOW(),
  created_by BIGINT NOT NULL DEFAULT 0,
  updated_at TIMESTAMP NOT NULL DEFAULT NOW(),
  updated_by BIGINT NOT NULL DEFAULT 0,
  version INTEGER NOT NULL DEFAULT 0,
  is_deleted BOOLEAN DEFAULT FALSE,
  CONSTRAINT pk_prosthetics_brak_notifications PRIMARY KEY (id),
  CONSTRAINT uq_prosthetics_brak_notifications_event UNIQUE (brak_event_id)
);
--rollback DROP TABLE IF EXISTS prosthetics_brak_notifications;

--changeset split-prosth:29
--comment Index for the outbox sweep (status + attempts window, oldest first).
CREATE INDEX IF NOT EXISTS idx_brak_notifications_status
    ON prosthetics_brak_notifications(status);
--rollback DROP INDEX IF EXISTS idx_brak_notifications_status;
