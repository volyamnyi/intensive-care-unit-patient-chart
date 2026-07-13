-- Seed data for the new domain model
-- Users use fixed UUIDs to be referenced by other seed data

INSERT INTO users (id, login, password_hash, full_name, role, email, speciality_code, speciality_name, phone, created_at, created_by, updated_at, updated_by, version)
VALUES
('11111111-1111-1111-1111-111111111111', 'doctor1', '$2a$10$LQeytYedrrlf3Dzg5jaUiuALhgGwku50pJL64hUrc/PkMHm7ulPpO', 'Олександр Мельник', 'DOCTOR', 'melnyk@hospital.ua', '101', 'Лікар-анестезіолог', '380501111111', NOW(), '11111111-1111-1111-1111-111111111111', NOW(), '11111111-1111-1111-1111-111111111111', 0),
('22222222-2222-2222-2222-222222222222', 'doctor2', '$2a$10$.6QnBurB4FnYYYi5vGdYy.eIzZtRIW4G17gcBRmpMCJDRDpqq8bDa', 'Наталія Бойко', 'DOCTOR', 'boyko@hospital.ua', '101', 'Лікар-анестезіолог', '380502222222', NOW(), '22222222-2222-2222-2222-222222222222', NOW(), '22222222-2222-2222-2222-222222222222', 0),
('33333333-3333-3333-3333-333333333333', 'nurse1', '$2a$10$kxLZ31lWPqY/vi0SUzrQd.vZHm8ej5kGsIXTfVbuewAtJVxLiegYq', 'Олена Ткаченко', 'NURSE', 'tkachenko@hospital.ua', '201', 'Медична сестра ВАІТ', '380503333333', NOW(), '33333333-3333-3333-3333-333333333333', NOW(), '33333333-3333-3333-3333-333333333333', 0),
('44444444-4444-4444-4444-444444444444', 'nurse2', '$2a$10$pOMXlVeSM4yP8Ol4dgNwkeoJRqiD9Eo8YmVeDTY85ro.yah0vzrXW', 'Марія Кравчук', 'NURSE', 'kravchuk@hospital.ua', '201', 'Медична сестра ВАІТ', '380504444444', NOW(), '44444444-4444-4444-4444-444444444444', NOW(), '44444444-4444-4444-4444-444444444444', 0),
('55555555-5555-5555-5555-555555555555', 'head1', '$2a$10$5Ek0UuP6a0DO6ILpAME7ruklzng5wTrWkdOLYpDpHT30HI/XF6XDO', 'Василь Гончарук', 'HEAD_OF_DEPARTMENT', 'goncharuk@hospital.ua', '301', 'Завідувач ВАІТ', '380505555555', NOW(), '55555555-5555-5555-5555-555555555555', NOW(), '55555555-5555-5555-5555-555555555555', 0),
('66666666-6666-6666-6666-666666666666', 'admin', '$2a$10$qlN4ZGI0YZGMiCUzf9OyGuK9N0oSx45Q8eH6KhNWUh6/5ajRpOjgu', 'Адмін Системи', 'ADMINISTRATOR', 'admin@hospital.ua', '999', 'Адміністратор', '380506666666', NOW(), '66666666-6666-6666-6666-666666666666', NOW(), '66666666-6666-6666-6666-666666666666', 0)
ON CONFLICT (login) DO UPDATE SET
  password_hash = EXCLUDED.password_hash,
  full_name = EXCLUDED.full_name,
  role = EXCLUDED.role,
  email = EXCLUDED.email,
  speciality_code = EXCLUDED.speciality_code,
  speciality_name = EXCLUDED.speciality_name,
  phone = EXCLUDED.phone;

-- Reset seed data to prevent data pollution from prior test runs
-- CASCADE handles all FK-dependent tables (clinical_days, hourly_records, etc.)
TRUNCATE episodes CASCADE;

-- Seed episodes (patient IDs match those in MockMISServiceImpl)
INSERT INTO episodes (id, patient_id, hospitalization_id, department_id, admission_date, status, created_at, created_by, updated_at, updated_by, version)
VALUES
('a1111111-1111-1111-1111-111111111111', '00000000-0000-0000-0000-000000001001', NULL, NULL, NOW() - INTERVAL '2 days', 'ACTIVE', NOW(), '11111111-1111-1111-1111-111111111111', NOW(), '11111111-1111-1111-1111-111111111111', 0),
('a2222222-2222-2222-2222-222222222222', '00000000-0000-0000-0000-000000001002', NULL, NULL, NOW() - INTERVAL '1 day', 'ACTIVE', NOW(), '11111111-1111-1111-1111-111111111111', NOW(), '11111111-1111-1111-1111-111111111111', 0),
('a3333333-3333-3333-3333-333333333333', '00000000-0000-0000-0000-000000001003', NULL, NULL, NOW() - INTERVAL '3 days', 'ACTIVE', NOW(), '22222222-2222-2222-2222-222222222222', NOW(), '22222222-2222-2222-2222-222222222222', 0)
ON CONFLICT (id) DO NOTHING;

-- Seed clinical days (open days + one nurse-signed day for doctor sign-off test)
INSERT INTO clinical_days (id, episode_id, day_number, start_date_time, end_date_time, status, doctor_signed, nurse_signed, created_at, created_by, updated_at, updated_by, version)
VALUES
('b1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', 1, DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours' + INTERVAL '1 day', 'OPEN', false, false, NOW(), '11111111-1111-1111-1111-111111111111', NOW(), '11111111-1111-1111-1111-111111111111', 0),
('b1111112-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', 2, DATE_TRUNC('day', NOW()) - INTERVAL '1 day' + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', 'NURSE_SIGNED', false, true, NOW(), '11111111-1111-1111-1111-111111111111', NOW(), '11111111-1111-1111-1111-111111111111', 0),
('b2222222-2222-2222-2222-222222222222', 'a2222222-2222-2222-2222-222222222222', 1, DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours' + INTERVAL '1 day', 'OPEN', false, false, NOW(), '11111111-1111-1111-1111-111111111111', NOW(), '11111111-1111-1111-1111-111111111111', 0),
('b3333333-3333-3333-3333-333333333333', 'a3333333-3333-3333-3333-333333333333', 1, DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours' + INTERVAL '1 day', 'OPEN', false, false, NOW(), '22222222-2222-2222-2222-222222222222', NOW(), '22222222-2222-2222-2222-222222222222', 0)
ON CONFLICT (id) DO NOTHING;
