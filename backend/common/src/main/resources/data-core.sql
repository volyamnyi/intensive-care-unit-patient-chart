-- Core module seed data (users, system settings)
-- Source: data.sql (split by module, DO NOT EDIT BY HAND)

-- Seed data for the new domain model
-- Users use Long IDs referenced from MockMisServiceImpl

-- System settings
INSERT INTO system_settings (id, key, value, description, created_at, created_by, updated_at, updated_by, version)
VALUES
('00000000-0000-0000-0000-000000000001', 'institution_name', 'КНП "Міська лікарня №1"', 'Назва закладу охорони здоров''я', NOW(), 16, NOW(), 16, 0),
('00000000-0000-0000-0000-000000000002', 'institution_edrpou', '12345678', 'Код ЄДРПОУ закладу охорони здоров''я', NOW(), 16, NOW(), 16, 0)
ON CONFLICT (key) DO UPDATE SET value = EXCLUDED.value;

-- Users seed data
INSERT INTO users (id, login, password_hash, full_name, role, email, speciality_code, speciality_name, phone, created_at, created_by, updated_at, updated_by, version)
VALUES
(11, 'doctor1', '$2a$10$LQeytYedrrlf3Dzg5jaUiuALhgGwku50pJL64hUrc/PkMHm7ulPpO', 'Олександр Мельник', 'DOCTOR', 'melnyk@hospital.ua', '101', 'Лікар-анестезіолог', '380501111111', NOW(), 11, NOW(), 11, 0),
(12, 'doctor2', '$2a$10$.6QnBurB4FnYYYi5vGdYy.eIzZtRIW4G17gcBRmpMCJDRDpqq8bDa', 'Наталія Бойко', 'DOCTOR', 'boyko@hospital.ua', '101', 'Лікар-анестезіолог', '380502222222', NOW(), 12, NOW(), 12, 0),
(13, 'nurse1', '$2a$10$kxLZ31lWPqY/vi0SUzrQd.vZHm8ej5kGsIXTfVbuewAtJVxLiegYq', 'Олена Ткаченко', 'NURSE', 'tkachenko@hospital.ua', '201', 'Медична сестра ВАІТ', '380503333333', NOW(), 13, NOW(), 13, 0),
(14, 'nurse2', '$2a$10$pOMXlVeSM4yP8Ol4dgNwkeoJRqiD9Eo8YmVeDTY85ro.yah0vzrXW', 'Марія Кравчук', 'NURSE', 'kravchuk@hospital.ua', '201', 'Медична сестра ВАІТ', '380504444444', NOW(), 14, NOW(), 14, 0),
(15, 'head1', '$2a$10$5Ek0UuP6a0DO6ILpAME7ruklzng5wTrWkdOLYpDpHT30HI/XF6XDO', 'Василь Гончарук', 'HEAD_OF_DEPARTMENT', 'goncharuk@hospital.ua', '301', 'Завідувач ВАІТ', '380505555555', NOW(), 15, NOW(), 15, 0),
(16, 'admin', '$2a$10$qlN4ZGI0YZGMiCUzf9OyGuK9N0oSx45Q8eH6KhNWUh6/5ajRpOjgu', 'Адмін Системи', 'ADMINISTRATOR', 'admin@hospital.ua', '999', 'Адміністратор', '380506666666', NOW(), 16, NOW(), 16, 0)
ON CONFLICT (login) DO UPDATE SET
  password_hash = EXCLUDED.password_hash,
  full_name = EXCLUDED.full_name,
  role = EXCLUDED.role,
  email = EXCLUDED.email,
  speciality_code = EXCLUDED.speciality_code,
  speciality_name = EXCLUDED.speciality_name,
  phone = EXCLUDED.phone;

-- ===== Prosthesis Manufacturing module seed (Phase 1, Issue #145) =====
-- Users: prosthetist1 / prosthetist2 / prosthetics_admin1 (password: doctor123, same BCrypt hash as doctor1)
INSERT INTO users (id, login, password_hash, full_name, role, email, speciality_code, speciality_name, phone, created_at, created_by, updated_at, updated_by, version)
VALUES
(21, 'prosthetist1', '$2a$10$LQeytYedrrlf3Dzg5jaUiuALhgGwku50pJL64hUrc/PkMHm7ulPpO', 'Олег Романюк', 'PROSTHETIST', 'romanyuk@hospital.ua', '401', 'Протезування та ортезування', '380507777777', NOW(), 21, NOW(), 21, 0),
(22, 'prosthetist2', '$2a$10$LQeytYedrrlf3Dzg5jaUiuALhgGwku50pJL64hUrc/PkMHm7ulPpO', 'Ірина Шевчук', 'PROSTHETIST', 'shevchuk@hospital.ua', '401', 'Протезування та ортезування', '380508888888', NOW(), 22, NOW(), 22, 0),
(23, 'prosthetics_admin1', '$2a$10$LQeytYedrrlf3Dzg5jaUiuALhgGwku50pJL64hUrc/PkMHm7ulPpO', 'Тарас Мельник', 'PROSTHETICS_ADMINISTRATOR', 'ptadmin@hospital.ua', '402', 'Адміністрування протезного виробництва', '380509999999', NOW(), 23, NOW(), 23, 0)
ON CONFLICT (login) DO UPDATE SET
  role = EXCLUDED.role,
  full_name = EXCLUDED.full_name,
  password_hash = EXCLUDED.password_hash;
