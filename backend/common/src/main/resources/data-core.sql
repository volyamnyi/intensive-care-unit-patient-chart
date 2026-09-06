-- Core module seed data (system settings)
-- Source: data.sql (split by module, DO NOT EDIT BY HAND)

-- Application users are NOT seeded here. They are provisioned from environment
-- variables by UserSeedService (see its javadoc for the variable contract),
-- so no user values — and no password material — ever live in this file.

-- System settings
INSERT INTO system_settings (id, key, value, description, created_at, created_by, updated_at, updated_by, version)
VALUES
('00000000-0000-0000-0000-000000000001', 'institution_name', 'КНП "Міська лікарня №1"', 'Назва закладу охорони здоров''я', NOW(), 16, NOW(), 16, 0),
('00000000-0000-0000-0000-000000000002', 'institution_edrpou', '12345678', 'Код ЄДРПОУ закладу охорони здоров''я', NOW(), 16, NOW(), 16, 0)
ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;
