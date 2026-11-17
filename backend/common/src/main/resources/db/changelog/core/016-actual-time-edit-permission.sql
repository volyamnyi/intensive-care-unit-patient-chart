--liquibase formatted sql

--changeset split-core:17
-- A distinct permission for correcting the recorded actual administration/dispensing
-- time of an executed dose (epic actual-time, issue #444). Only the DEFINITION row
-- is added here (same contract as 005/008): default grants are Java-seeded by
-- PermissionService.seedIfEmpty() from PermissionCatalog.defaultMatrix(), which only
-- fires when role_permissions is empty. Pre-inserting grants here would make the
-- count non-zero and suppress the full default-matrix seed on a fresh install.
-- ON CONFLICT (code) DO UPDATE keeps label/description in sync on re-apply.
INSERT INTO permissions (code, label, description, category) VALUES
('PRESCRIPTION_ACTUAL_TIME_EDIT',
 'Коригування фактичного часу виконання призначення',
 'Коригування фактичного часу виконання призначення у листку лікарських призначень',
 'Клінічні операції')
ON CONFLICT (code) DO UPDATE SET
    label = EXCLUDED.label,
    description = EXCLUDED.description,
    category = EXCLUDED.category;

--rollback DELETE FROM permissions WHERE code = 'PRESCRIPTION_ACTUAL_TIME_EDIT';
