--liquibase formatted sql

--changeset split-prosth:32
--comment Brak threshold escalation (epic #322, issue #327): a stage-6 brak owns
--comment a SINGLE outbox row for its event, and from the 3rd brak on the same
--comment event must additionally own a THRESHOLD row. The single-column unique
--comment from 010 forbids that second row, so replace it with a composite
--comment unique over (brak_event_id, kind): still at most one row per
--comment event-and-kind (both idempotencies hold), but SINGLE and THRESHOLD
--comment rows for the same event coexist.
ALTER TABLE prosthetics_brak_notifications
    DROP CONSTRAINT IF EXISTS uq_prosthetics_brak_notifications_event;
ALTER TABLE prosthetics_brak_notifications
    ADD CONSTRAINT uq_prosthetics_brak_notifications_event_kind
    UNIQUE (brak_event_id, kind);
--rollback ALTER TABLE prosthetics_brak_notifications
--rollback     DROP CONSTRAINT IF EXISTS uq_prosthetics_brak_notifications_event_kind;
--rollback ALTER TABLE prosthetics_brak_notifications
--rollback     ADD CONSTRAINT uq_prosthetics_brak_notifications_event
--rollback     UNIQUE (brak_event_id);
