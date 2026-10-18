--liquibase formatted sql

--changeset split-prosth:35
--comment Separate SMTP work from the outbox row lock. PROCESSING rows are
--comment reclaimed by the sweep after the lease expires.
ALTER TABLE prosthetics_brak_notifications
    ADD COLUMN IF NOT EXISTS lease_until TIMESTAMP,
    ADD COLUMN IF NOT EXISTS claimed_by VARCHAR(100);

ALTER TABLE prosthetics_brak_notifications
    DROP CONSTRAINT IF EXISTS ck_prosthetics_brak_notifications_status;

ALTER TABLE prosthetics_brak_notifications
    ADD CONSTRAINT ck_prosthetics_brak_notifications_status
    CHECK (status IN ('PENDING', 'PROCESSING', 'SENT', 'FAILED', 'SKIPPED', 'DEAD'));

CREATE INDEX IF NOT EXISTS idx_brak_notifications_processing_lease
    ON prosthetics_brak_notifications(status, lease_until);

--rollback DROP INDEX IF EXISTS idx_brak_notifications_processing_lease;
--rollback ALTER TABLE prosthetics_brak_notifications DROP CONSTRAINT IF EXISTS ck_prosthetics_brak_notifications_status;
--rollback ALTER TABLE prosthetics_brak_notifications ADD CONSTRAINT ck_prosthetics_brak_notifications_status CHECK (status IN ('PENDING', 'SENT', 'FAILED', 'SKIPPED', 'DEAD'));
--rollback ALTER TABLE prosthetics_brak_notifications DROP COLUMN IF EXISTS claimed_by;
--rollback ALTER TABLE prosthetics_brak_notifications DROP COLUMN IF EXISTS lease_until;
