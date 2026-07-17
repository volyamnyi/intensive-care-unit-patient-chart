-- Integration test seed data
-- Clean slate: truncate all seeded tables (no FK constraints to users)
TRUNCATE users CASCADE;
TRUNCATE episodes CASCADE;

INSERT INTO users (id, login, password_hash, full_name, role, email, speciality_code, speciality_name, phone, created_at, created_by, updated_at, updated_by, version)
VALUES
(11, 'doctor1', '$2a$10$LQeytYedrrlf3Dzg5jaUiuALhgGwku50pJL64hUrc/PkMHm7ulPpO', 'Олександр Мельник', 'DOCTOR', 'melnyk@hospital.ua', '101', 'Лікар-анестезіолог', '380501111111', NOW(), 11, NOW(), 11, 0),
(12, 'doctor2', '$2a$10$.6QnBurB4FnYYYi5vGdYy.eIzZtRIW4G17gcBRmpMCJDRDpqq8bDa', 'Наталія Бойко', 'DOCTOR', 'boyko@hospital.ua', '101', 'Лікар-анестезіолог', '380502222222', NOW(), 12, NOW(), 12, 0),
(13, 'nurse1', '$2a$10$kxLZ31lWPqY/vi0SUzrQd.vZHm8ej5kGsIXTfVbuewAtJVxLiegYq', 'Олена Ткаченко', 'NURSE', 'tkachenko@hospital.ua', '201', 'Медична сестра ВАІТ', '380503333333', NOW(), 13, NOW(), 13, 0),
(14, 'nurse2', '$2a$10$pOMXlVeSM4yP8Ol4dgNwkeoJRqiD9Eo8YmVeDTY85ro.yah0vzrXW', 'Марія Кравчук', 'NURSE', 'kravchuk@hospital.ua', '201', 'Медична сестра ВАІТ', '380504444444', NOW(), 14, NOW(), 14, 0),
(15, 'head1', '$2a$10$5Ek0UuP6a0DO6ILpAME7ruklzng5wTrWkdOLYpDpHT30HI/XF6XDO', 'Василь Гончарук', 'HEAD_OF_DEPARTMENT', 'goncharuk@hospital.ua', '301', 'Завідувач ВАІТ', '380505555555', NOW(), 15, NOW(), 15, 0),
(16, 'admin', '$2a$10$qlN4ZGI0YZGMiCUzf9OyGuK9N0oSx45Q8eH6KhNWUh6/5ajRpOjgu', 'Адмін Системи', 'ADMINISTRATOR', 'admin@hospital.ua', '999', 'Адміністратор', '380506666666', NOW(), 16, NOW(), 16, 0);

INSERT INTO episodes (id, patient_id, hospitalization_id, department_id, admission_date, status, created_at, created_by, updated_at, updated_by, version)
VALUES
('a1111111-1111-1111-1111-111111111111', 1001, NULL, NULL, NOW() - INTERVAL '2 days', 'ACTIVE', NOW(), 11, NOW(), 11, 0),
('a2222222-2222-2222-2222-222222222222', 1002, NULL, NULL, NOW() - INTERVAL '1 day', 'ACTIVE', NOW(), 11, NOW(), 11, 0),
('a3333333-3333-3333-3333-333333333333', 1003, NULL, NULL, NOW() - INTERVAL '3 days', 'ACTIVE', NOW(), 12, NOW(), 12, 0);

INSERT INTO clinical_days (id, episode_id, day_number, start_date_time, end_date_time, status, doctor_signed, nurse_signed, created_at, created_by, updated_at, updated_by, version)
VALUES
('b1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', 1, DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours' + INTERVAL '1 day', 'OPEN', false, false, NOW(), 11, NOW(), 11, 0),
('b1111112-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', 2, DATE_TRUNC('day', NOW()) - INTERVAL '1 day' + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', 'NURSE_SIGNED', false, true, NOW(), 11, NOW(), 11, 0),
('b2222222-2222-2222-2222-222222222222', 'a2222222-2222-2222-2222-222222222222', 1, DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours' + INTERVAL '1 day', 'OPEN', false, false, NOW(), 11, NOW(), 11, 0),
('b3333333-3333-3333-3333-333333333333', 'a3333333-3333-3333-3333-333333333333', 1, DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours' + INTERVAL '1 day', 'OPEN', false, false, NOW(), 12, NOW(), 12, 0),
('b4444444-4444-4444-4444-444444444444', 'a2222222-2222-2222-2222-222222222222', 2, DATE_TRUNC('day', NOW()) - INTERVAL '1 day' + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', 'NURSE_SIGNED', false, true, NOW(), 11, NOW(), 11, 0);
