--liquibase formatted sql

--changeset split-icu:72
-- Add gastric tube output (Зонд) to hourly_records losses for form № 003-15/о
ALTER TABLE hourly_records ADD COLUMN gastric_output DOUBLE PRECISION CHECK (gastric_output >= 0);

--changeset split-icu:73
-- Add care section fields (Положення у ліжку, Головний кінець ліжка) to hourly_records for form № 003-15/о
ALTER TABLE hourly_records ADD COLUMN bed_position VARCHAR(100);
ALTER TABLE hourly_records ADD COLUMN head_end VARCHAR(100);

