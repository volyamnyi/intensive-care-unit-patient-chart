--liquibase formatted sql

--changeset split-prosth:21
ALTER TABLE prosthetics_flow_instances ALTER COLUMN pause_category TYPE VARCHAR(32);
--rollback ALTER TABLE prosthetics_flow_instances ALTER COLUMN pause_category TYPE VARCHAR(16);

--changeset split-prosth:22
UPDATE prosthetics_flow_instances SET pause_category = NULL WHERE pause_category IN ('PATIENT','MATERIAL','TECH_IDLE');
--rollback SELECT 1;
