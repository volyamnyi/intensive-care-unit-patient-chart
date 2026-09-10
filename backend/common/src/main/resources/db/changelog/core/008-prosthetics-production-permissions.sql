--liquibase formatted sql

--changeset split-core:8
-- Production monitoring permissions (manufacturing epic #271, issue #272):
-- four read-scoped codes for the /prosthetics/production dashboard.
-- Only DEFINITION rows are added here (same contract as 005): default grants are
-- Java-seeded by PermissionService.seedIfEmpty() from PermissionCatalog.defaultMatrix(),
-- which only fires when role_permissions is empty — pre-inserting grants here would
-- suppress the full default-matrix seed on a fresh install.
INSERT INTO permissions (code, label, description, category) VALUES
('PROSTHETICS_PRODUCTION_VIEW',
 'Моніторинг виробництва',
 'Доступ до сторінки моніторингу виробництва протезів',
 'Протезування'),
('PROSTHETICS_PRODUCTION_VIEW_ALL',
 'Виробництво всіх протезистів',
 'Перегляд виробів усіх протезистів, навантаження команди та аналітики',
 'Протезування'),
('PROSTHETICS_PRODUCTION_PATIENT_VIEW',
 'Дані пацієнта у виробництві',
 'Перегляд персональних даних пацієнта та документів у моніторингу виробництва',
 'Протезування'),
('PROSTHETICS_PRODUCTION_QUALITY_VIEW',
 'Якість виробництва',
 'Перегляд браків, доопрацювань та провалених процесів',
 'Протезування')
ON CONFLICT (code) DO NOTHING;

--rollback DELETE FROM permissions WHERE code IN ('PROSTHETICS_PRODUCTION_VIEW','PROSTHETICS_PRODUCTION_VIEW_ALL','PROSTHETICS_PRODUCTION_PATIENT_VIEW','PROSTHETICS_PRODUCTION_QUALITY_VIEW');
