--liquibase formatted sql

--changeset patient-chart:6
-- Make clinical_day_id nullable (episode-level scales have no clinical day)
ALTER TABLE scale_results ALTER COLUMN clinical_day_id DROP NOT NULL;

--changeset patient-chart:7
-- Add episode_id for episode-level scale results
ALTER TABLE scale_results ADD COLUMN episode_id UUID;
ALTER TABLE scale_results ADD CONSTRAINT fk_scale_results_episode FOREIGN KEY (episode_id) REFERENCES episodes(id);

--changeset patient-chart:8
-- Widen result column from VARCHAR(100) to TEXT for large scale results
ALTER TABLE scale_results ALTER COLUMN result TYPE TEXT;

--changeset patient-chart:9
-- Add raw_data JSONB column for storing calculator input parameters
ALTER TABLE scale_results ADD COLUMN raw_data JSONB;
