--liquibase formatted sql

--changeset split-prosth:30
--comment Brak threshold escalation (epic #322): notification kind on the outbox.
--comment SINGLE rows keep the per-brak stage-6 behaviour from issue #320;
--comment THRESHOLD rows carry the order-wide escalation (brak count >= 3 over
--comment both trigger steps d0000017/e0000028 and d0000020/e0000032, summed per
--comment orderId). Existing rows default to SINGLE, so delivery is unchanged.
ALTER TABLE prosthetics_brak_notifications
    ADD COLUMN IF NOT EXISTS kind VARCHAR(16) NOT NULL DEFAULT 'SINGLE';
--rollback ALTER TABLE prosthetics_brak_notifications DROP COLUMN IF EXISTS kind;

--changeset split-prosth:31
--comment Order anchor for THRESHOLD rows: lets operators and tests list all
--comment escalations of one order chain. NULL for SINGLE rows (one row per brak
--comment event, no order grouping needed). Deliberately NO partial unique index
--comment on (order_id) WHERE kind='THRESHOLD': the owner decision in #322 is
--comment repeated notification on every brak with count >= 3, so several
--comment THRESHOLD rows per order must be allowed. Idempotency is one row per
--comment triggering event via uq_prosthetics_brak_notifications_event.
ALTER TABLE prosthetics_brak_notifications
    ADD COLUMN IF NOT EXISTS order_id UUID;
CREATE INDEX IF NOT EXISTS idx_brak_notifications_order
    ON prosthetics_brak_notifications(order_id);
--rollback DROP INDEX IF EXISTS idx_brak_notifications_order;
--rollback ALTER TABLE prosthetics_brak_notifications DROP COLUMN IF EXISTS order_id;
