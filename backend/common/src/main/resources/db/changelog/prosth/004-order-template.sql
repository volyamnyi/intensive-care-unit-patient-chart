--liquibase formatted sql

--changeset split-prosth:1
-- UPPER_LIMB order template fields: personal/contact data for the order form
ALTER TABLE prosthetics_patients ADD COLUMN IF NOT EXISTS phone VARCHAR(32);
ALTER TABLE prosthetics_patients ADD COLUMN IF NOT EXISTS email VARCHAR(255);
ALTER TABLE prosthetics_patients ADD COLUMN IF NOT EXISTS residence VARCHAR(255);
ALTER TABLE prosthetics_patients ADD COLUMN IF NOT EXISTS health_status VARCHAR(128);
ALTER TABLE prosthetics_patients ADD COLUMN IF NOT EXISTS amputation_site VARCHAR(255);
ALTER TABLE prosthetics_patients ADD COLUMN IF NOT EXISTS clinical_state JSONB;

--rollback ALTER TABLE prosthetics_patients DROP COLUMN clinical_state;
--rollback ALTER TABLE prosthetics_patients DROP COLUMN amputation_site;
--rollback ALTER TABLE prosthetics_patients DROP COLUMN health_status;
--rollback ALTER TABLE prosthetics_patients DROP COLUMN residence;
--rollback ALTER TABLE prosthetics_patients DROP COLUMN email;
--rollback ALTER TABLE prosthetics_patients DROP COLUMN phone;

--changeset split-prosth:2
-- UPPER_LIMB order template fields: product code, manufacturing approach, approval registry
ALTER TABLE prosthetics_orders ADD COLUMN IF NOT EXISTS product_code VARCHAR(128);
ALTER TABLE prosthetics_orders ADD COLUMN IF NOT EXISTS manufacturing_approach TEXT;
ALTER TABLE prosthetics_orders ADD COLUMN IF NOT EXISTS approval_number VARCHAR(32);
ALTER TABLE prosthetics_orders ADD COLUMN IF NOT EXISTS approval_registry VARCHAR(64);
ALTER TABLE prosthetics_orders ADD COLUMN IF NOT EXISTS approval_seq VARCHAR(16);

--rollback ALTER TABLE prosthetics_orders DROP COLUMN approval_seq;
--rollback ALTER TABLE prosthetics_orders DROP COLUMN approval_registry;
--rollback ALTER TABLE prosthetics_orders DROP COLUMN approval_number;
--rollback ALTER TABLE prosthetics_orders DROP COLUMN manufacturing_approach;
--rollback ALTER TABLE prosthetics_orders DROP COLUMN product_code;

