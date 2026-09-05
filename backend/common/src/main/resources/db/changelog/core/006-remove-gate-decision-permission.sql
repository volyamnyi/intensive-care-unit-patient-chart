--liquibase formatted sql

--changeset split-core:6
-- Quality Gate removal (QG-Removal Phase 2, issue #230): delete the
-- PROSTHETICS_GATE_DECISION permission definition and its grants. Grants are
-- Java-seeded from PermissionCatalog (which no longer contains the code), so
-- fresh installs never recreate them; on rollback the definition row returns
-- and grants are restored via the admin UI or a fresh seed.
DELETE FROM role_permissions WHERE permission_code = 'PROSTHETICS_GATE_DECISION';
DELETE FROM permissions WHERE code = 'PROSTHETICS_GATE_DECISION';
--rollback INSERT INTO permissions (code, label, description, category) VALUES
--rollback ('PROSTHETICS_GATE_DECISION', 'Рішення quality gate', 'Рішення контролю якості (PASS / REWORK / FAIL)', 'Протезування')
--rollback ON CONFLICT (code) DO NOTHING;
