--liquibase formatted sql

--changeset split-icu:70
-- Add GCS field to hourly_records for §29/§30 general state + hourly monitoring
ALTER TABLE hourly_records ADD COLUMN gcs INTEGER CHECK (gcs BETWEEN 3 AND 15);

--changeset split-icu:71
-- Add vasopressor/inotropic support fields to hourly_records for §30
-- Units: мкг/кг/хв (μg/kg/min), 0 = drug not administered
ALTER TABLE hourly_records ADD COLUMN dopamine DECIMAL(5,2) CHECK (dopamine >= 0 AND dopamine <= 100);
ALTER TABLE hourly_records ADD COLUMN dobutamine DECIMAL(5,2) CHECK (dobutamine >= 0 AND dobutamine <= 100);
ALTER TABLE hourly_records ADD COLUMN norepinephrine DECIMAL(5,2) CHECK (norepinephrine >= 0 AND norepinephrine <= 100);
ALTER TABLE hourly_records ADD COLUMN epinephrine DECIMAL(5,2) CHECK (epinephrine >= 0 AND epinephrine <= 100);

