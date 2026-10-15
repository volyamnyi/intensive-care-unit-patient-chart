--liquibase formatted sql

--changeset split-med:29 runInTransaction:false
--comment Unique (item_id, day_date) — catches data corruption and backs the double-click guard
DROP INDEX CONCURRENTLY IF EXISTS uq_prescription_item_days_item_date;
CREATE UNIQUE INDEX CONCURRENTLY uq_prescription_item_days_item_date
    ON prescription_item_days (item_id, day_date) WHERE is_deleted = false;
--rollback DROP INDEX IF EXISTS uq_prescription_item_days_item_date;
