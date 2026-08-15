--liquibase formatted sql

--changeset split-core:1
--comment Create permissions catalog and role-permission matrix tables for dynamic RBAC
CREATE TABLE IF NOT EXISTS permissions (
    code VARCHAR(64) PRIMARY KEY,
    label VARCHAR(255) NOT NULL,
    description VARCHAR(1000),
    category VARCHAR(64) NOT NULL
);
--rollback DROP TABLE IF EXISTS permissions;

--changeset split-core:2
--comment Role-permission grants: presence of a row means the role holds the permission (default deny)
CREATE TABLE IF NOT EXISTS role_permissions (
    role VARCHAR(32) NOT NULL,
    permission_code VARCHAR(64) NOT NULL,
    PRIMARY KEY (role, permission_code),
    CONSTRAINT fk_role_permissions_permission FOREIGN KEY (permission_code)
        REFERENCES permissions (code) ON DELETE CASCADE
);
--rollback DROP TABLE IF EXISTS role_permissions;

--changeset split-core:3
--comment Seed permissions catalog rows (default grants are seeded by PermissionService when the table is empty)
INSERT INTO permissions (code, label, description, category) VALUES
('EPISODE_CREATE', 'Створення епізоду', 'Створення нового епізоду (карти інтенсивної терапії)', 'Клінічні операції'),
('CLINICAL_DAY_CREATE', 'Створення клінічного дня', 'Створення нового клінічного дня для епізоду', 'Клінічні операції'),
('SIGN_NURSE', 'Підпис медсестрою', 'Підписання клінічного дня на етапі медсестри', 'Клінічні операції'),
('SIGN_DOCTOR', 'Підпис лікарем', 'Підписання клінічного дня на етапі лікаря', 'Клінічні операції'),
('REOPEN_DAY', 'Перевідкриття дня', 'Перевідкриття підписаного клінічного дня', 'Клінічні операції'),
('PRESCRIPTION_CREATE', 'Створення призначень', 'Створення та планування лікарських призначень і медичних замовлень', 'Клінічні операції'),
('PRESCRIPTION_EXECUTE', 'Виконання призначень', 'Виконання та завершення призначень медсестрою', 'Клінічні операції'),
('VITALS_ENTER', 'Введення показників', 'Введення та редагування показників пацієнта (vital signs)', 'Клінічні операції'),
('PATIENT_VIEW', 'Перегляд даних пацієнта', 'Перегляд даних пацієнта та клінічної документації (read-only)', 'Клінічні операції'),
('SCALE_APACHE_SOFA', 'Шкали APACHE II / SOFA', 'Створення результатів клінічних шкал APACHE II та SOFA', 'Клінічні операції'),
('SCALE_CAMICU_BRADEN_RASS', 'Шкали CAM-ICU / Браден / RASS', 'Створення результатів клінічних шкал CAM-ICU, Браден, RASS', 'Клінічні операції'),
('AUDIT_ACCESS', 'Журнал аудиту', 'Перегляд журналу аудиту', 'Адміністрування'),
('AUDITOR_VIEW', 'Read-only доступ аудитора', 'Службовий read-only доступ ролі AUDITOR', 'Адміністрування'),
('PROSTHETICS_DASHBOARD', 'Дашборд протезування', 'Перегляд власних процесів протезування та довідників', 'Протезування'),
('PROSTHETICS_INSTANCE_CREATE', 'Створення процесу', 'Створення процесу виготовлення протеза (Wizard)', 'Протезування'),
('PROSTHETICS_STEP_COMPLETE', 'Виконання кроків', 'Заповнення та завершення кроків процесу, завантаження файлів', 'Протезування'),
('PROSTHETICS_PAUSE_RESUME', 'Пауза / відновлення', 'Призупинення та відновлення процесу', 'Протезування'),
('PROSTHETICS_GATE_DECISION', 'Рішення quality gate', 'Рішення контролю якості (PASS / REWORK / FAIL)', 'Протезування'),
('PROSTHETICS_TEMPLATE_MANAGE', 'Керування шаблонами', 'Створення та редагування шаблонів технологічних процесів', 'Протезування'),
('PROSTHETICS_ORDER_MANAGE', 'Пацієнти та замовлення', 'Створення пацієнтів і замовлень протезування', 'Протезування'),
('MODULE_ICU_ACCESS', 'Модуль: Карта інтенсивної терапії', 'Навігація до модуля карти інтенсивної терапії (лікар / медсестра)', 'Модулі'),
('MODULE_MEDICATION_ACCESS', 'Модуль: Листок лікарських призначень', 'Навігація до модуля листка лікарських призначень', 'Модулі'),
('MODULE_PROSTHETICS_ACCESS', 'Модуль: Виробництво протезів', 'Навігація до модуля виробництва протезів (перегляд процесів)', 'Модулі'),
('MODULE_ADMIN_ACCESS', 'Модуль: Адміністрування', 'Навігація до адміністративної панелі', 'Модулі')
ON CONFLICT (code) DO UPDATE SET
    label = EXCLUDED.label,
    description = EXCLUDED.description,
    category = EXCLUDED.category;
--rollback DELETE FROM permissions WHERE code IN ('EPISODE_CREATE','CLINICAL_DAY_CREATE','SIGN_NURSE','SIGN_DOCTOR','REOPEN_DAY','PRESCRIPTION_CREATE','PRESCRIPTION_EXECUTE','VITALS_ENTER','PATIENT_VIEW','SCALE_APACHE_SOFA','SCALE_CAMICU_BRADEN_RASS','AUDIT_ACCESS','AUDITOR_VIEW','PROSTHETICS_DASHBOARD','PROSTHETICS_INSTANCE_CREATE','PROSTHETICS_STEP_COMPLETE','PROSTHETICS_PAUSE_RESUME','PROSTHETICS_GATE_DECISION','PROSTHETICS_TEMPLATE_MANAGE','PROSTHETICS_ORDER_MANAGE','MODULE_ICU_ACCESS','MODULE_MEDICATION_ACCESS','MODULE_PROSTHETICS_ACCESS','MODULE_ADMIN_ACCESS');

