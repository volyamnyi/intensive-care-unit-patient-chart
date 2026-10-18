--liquibase formatted sql

-- Drop the non-unique index that split-icu:94 superseded (#382).
-- uq_clinical_days_episode_day is unique on exactly (episode_id, day_number)
-- - the same columns as idx_clinical_days_episode from icu/080 - so the plain
-- index became redundant: one lookup index instead of two, one less index to
-- maintain on every clinical-day insert. The applied icu/080 changeset is left
-- untouched (checksum safety); the rollback recreates the original index.

--changeset split-icu:95
--comment clinical_days(episode_id, day_number) - superseded by the unique uq_clinical_days_episode_day (split-icu:94)
DROP INDEX IF EXISTS idx_clinical_days_episode;
--rollback CREATE INDEX CONCURRENTLY IF NOT EXISTS idx_clinical_days_episode ON clinical_days (episode_id, day_number);
