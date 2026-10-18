--liquibase formatted sql

--changeset split-icu:93
--comment #382: one ACTIVE episode per patient. EpisodeService.createEpisode
-- checks-then-inserts; two concurrent creates produced two ACTIVE rows. The
-- partial index makes the loser fail with a constraint the handler maps to the
-- same 422 EPISODE_ALREADY_ACTIVE as the sequential check.
CREATE UNIQUE INDEX uq_episodes_active_patient
    ON episodes (patient_id)
    WHERE status = 'ACTIVE';
--rollback DROP INDEX uq_episodes_active_patient;

--changeset split-icu:94
--comment #382: clinical day numbers are unique within an episode. The
-- dayNumber = lastDay+1 read-then-insert could race into a duplicate; the
-- constraint turns the race into a 409 instead of silent data corruption.
CREATE UNIQUE INDEX uq_clinical_days_episode_day
    ON clinical_days (episode_id, day_number);
--rollback DROP INDEX uq_clinical_days_episode_day;