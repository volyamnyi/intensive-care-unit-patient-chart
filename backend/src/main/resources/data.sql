-- Seed data: users (passwords are BCrypt hashes of user login + "123")
-- doctor1/doctor123, doctor2/doctor123, nurse1/nurse123, nurse2/nurse123, head1/head123, admin/admin123
INSERT INTO users (login, password_hash, full_name, role, email, speciality_code, speciality_name, phone)
VALUES
('doctor1', '$2a$10$LQeytYedrrlf3Dzg5jaUiuALhgGwku50pJL64hUrc/PkMHm7ulPpO', 'Олександр Мельник', 'DOCTOR', 'melnyk@hospital.ua', '101', 'Лікар-анестезіолог', '380501111111'),
('doctor2', '$2a$10$.6QnBurB4FnYYYi5vGdYy.eIzZtRIW4G17gcBRmpMCJDRDpqq8bDa', 'Наталія Бойко', 'DOCTOR', 'boyko@hospital.ua', '101', 'Лікар-анестезіолог', '380502222222'),
('nurse1', '$2a$10$kxLZ31lWPqY/vi0SUzrQd.vZHm8ej5kGsIXTfVbuewAtJVxLiegYq', 'Олена Ткаченко', 'NURSE', 'tkachenko@hospital.ua', '201', 'Медична сестра ВАІТ', '380503333333'),
('nurse2', '$2a$10$pOMXlVeSM4yP8Ol4dgNwkeoJRqiD9Eo8YmVeDTY85ro.yah0vzrXW', 'Марія Кравчук', 'NURSE', 'kravchuk@hospital.ua', '201', 'Медична сестра ВАІТ', '380504444444'),
('head1', '$2a$10$5Ek0UuP6a0DO6ILpAME7ruklzng5wTrWkdOLYpDpHT30HI/XF6XDO', 'Василь Гончарук', 'HEAD_OF_DEPARTMENT', 'goncharuk@hospital.ua', '301', 'Завідувач ВАІТ', '380505555555'),
('admin', '$2a$10$qlN4ZGI0YZGMiCUzf9OyGuK9N0oSx45Q8eH6KhNWUh6/5ajRpOjgu', 'Адмін Системи', 'ADMINISTRATOR', 'admin@hospital.ua', '999', 'Адміністратор', '380506666666')
ON CONFLICT (login) DO UPDATE SET
  password_hash = EXCLUDED.password_hash,
  full_name = EXCLUDED.full_name,
  role = EXCLUDED.role,
  email = EXCLUDED.email,
  speciality_code = EXCLUDED.speciality_code,
  speciality_name = EXCLUDED.speciality_name,
  phone = EXCLUDED.phone;
