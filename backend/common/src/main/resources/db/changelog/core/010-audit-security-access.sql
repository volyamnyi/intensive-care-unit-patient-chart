--liquibase formatted sql

--changeset split-core:10
-- Audit v2 security permission (epic #329, issue #333): only the DEFINITION row is
-- added here (same contract as 005/008): default grants are Java-seeded by
-- PermissionService.seedIfEmpty() from PermissionCatalog.defaultMatrix(), which only
-- fires when role_permissions is empty — pre-inserting grants here would suppress the
-- full default-matrix seed on a fresh install (proven live: a grant row left the fresh
-- matrix with a single AUDITOR grant and denied ADMIN the audit console).
SET LOCAL lock_timeout = '5s';
SET LOCAL statement_timeout = '60s';
INSERT INTO permissions (code, label, description, category)
VALUES ('AUDIT_SECURITY_ACCESS', 'Журнал безпекових подій',
        'Перегляд безпекових подій аудиту (окремо від бізнес-журналу)', 'Адміністрування')
ON CONFLICT (code) DO NOTHING;

--rollback DELETE FROM permissions WHERE code = 'AUDIT_SECURITY_ACCESS';
