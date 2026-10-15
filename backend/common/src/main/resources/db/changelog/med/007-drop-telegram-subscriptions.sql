--liquibase formatted sql

--changeset split-med:28
--comment Drop the dead telegram-subscriptions table — its only consumer (NotificationService/PrescriptionSchedulerService) was removed with the legacy MedicineList importer (commit b0730f28, 2026-10-01); no seed data, no frontend, no test assertions exist.
DROP TABLE IF EXISTS telegram_subscriptions;
--rollback CREATE TABLE telegram_subscriptions (
--rollback     chat_id BIGINT NOT NULL,
--rollback     subscribed_at TIMESTAMP NOT NULL DEFAULT NOW(),
--rollback     CONSTRAINT pk_telegram_subscriptions PRIMARY KEY (chat_id)
--rollback );
