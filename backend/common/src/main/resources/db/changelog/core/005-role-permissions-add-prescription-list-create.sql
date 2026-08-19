--liquibase formatted sql

--changeset split-core:5
-- A distinct permission for creating a NEW prescription list instance, separate from
-- PRESCRIPTION_CREATE (planning: adding items, planning/cancelling doses).
-- Only the DEFINITION row is added here, because definitions are SQL-seeded (split-core:3)
-- and already-applied databases must not fail checksum validation — so it is a new changeset.
-- The DEFAULT GRANTS (DOCTOR, HEAD_OF_DEPARTMENT) are intentionally NOT seeded in SQL: grants are
-- Java-seeded by PermissionService.seedIfEmpty() from PermissionCatalog.defaultMatrix(), and that
-- path only fires when role_permissions is empty. Pre-inserting grants here would make the count
-- non-zero and suppress the full default-matrix seed on a fresh install.
INSERT INTO permissions (code, label, description, category) VALUES
('PRESCRIPTION_LIST_CREATE',
 'Створення листка лікарських призначень',
 'Створення нового листка лікарських призначень (екземпляра) для пацієнта',
 'Клінічні операції')
ON CONFLICT (code) DO NOTHING;

--rollback DELETE FROM permissions WHERE code = 'PRESCRIPTION_LIST_CREATE';
