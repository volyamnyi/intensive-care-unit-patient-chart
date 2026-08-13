-- ============================================================================
-- PROSTHETICS MANUFACTURING MODULE - SEED DATA
-- ============================================================================
-- Clear existing prosthetics data to allow re-seeding
DELETE FROM prosthetics_rework_loops WHERE gate_id IN (SELECT id FROM prosthetics_quality_gates WHERE stage_id IN (SELECT id FROM prosthetics_template_stages WHERE template_id IN (SELECT id FROM prosthetics_flow_templates WHERE name IN ('TP-UL-01', 'TP-LL-01'))));
DELETE FROM prosthetics_quality_gates WHERE stage_id IN (SELECT id FROM prosthetics_template_stages WHERE template_id IN (SELECT id FROM prosthetics_flow_templates WHERE name IN ('TP-UL-01', 'TP-LL-01')));
DELETE FROM prosthetics_template_elements WHERE step_id IN (SELECT id FROM prosthetics_template_steps WHERE stage_id IN (SELECT id FROM prosthetics_template_stages WHERE template_id IN (SELECT id FROM prosthetics_flow_templates WHERE name IN ('TP-UL-01', 'TP-LL-01'))));
DELETE FROM prosthetics_template_steps WHERE stage_id IN (SELECT id FROM prosthetics_template_stages WHERE template_id IN (SELECT id FROM prosthetics_flow_templates WHERE name IN ('TP-UL-01', 'TP-LL-01')));
DELETE FROM prosthetics_template_stages WHERE template_id IN (SELECT id FROM prosthetics_flow_templates WHERE name IN ('TP-UL-01', 'TP-LL-01'));
DELETE FROM prosthetics_orders WHERE order_number IN ('ПВ-26-0413', 'ПВ-26-0414');
DELETE FROM prosthetics_flow_templates WHERE name IN ('TP-UL-01', 'TP-LL-01');
DELETE FROM prosthetics_patients WHERE id IN ('900001', '900002');
DELETE FROM users WHERE login IN ('prosthetist1', 'prosthetist2', 'prosthetics_admin1');

-- Prosthetist users (passwords: doctor123)
INSERT INTO users (id, login, password_hash, full_name, role, email, speciality_code, speciality_name, phone, created_at, created_by, updated_at, updated_by, version)
VALUES
    (17, 'prosthetist1', '$2a$10$LQeytYedrrlf3Dzg5jaUiuALhgGwku50pJL64hUrc/PkMHm7ulPpO', 'Іван Петренко', 'PROSTHETIST', 'petrenko@hospital.ua', '401', 'Технолог протезування', '380507777777', NOW(), 16, NOW(), 16, 0),
    (18, 'prosthetist2', '$2a$10$.6QnBurB4FnYYYi5vGdYy.eIzZtRIW4G17gcBRmpMCJDRDpqq8bDa', 'Петро Коваль', 'PROSTHETIST', 'koval@hospital.ua', '401', 'Технолог протезування', '380508888888', NOW(), 16, NOW(), 16, 0),
    (19, 'prosthetics_admin1', '$2a$10$kxLZ31lWPqY/vi0SUzrQd.vZHm8ej5kGsIXTfVbuewAtJVxLiegYq', 'Семен Семенюк', 'PROSTHETICS_ADMINISTRATOR', 'semenuk@hospital.ua', '402', 'Адміністратор протезування', '380509999999', NOW(), 16, NOW(), 16, 0)
ON CONFLICT (login) DO UPDATE SET
    password_hash = EXCLUDED.password_hash,
    full_name = EXCLUDED.full_name,
    role = EXCLUDED.role,
    email = EXCLUDED.email,
    speciality_code = EXCLUDED.speciality_code,
    speciality_name = EXCLUDED.speciality_name,
    phone = EXCLUDED.phone;

-- Prosthetics patients
-- NOTE: demographic data (pib, birth_date, gender, height/weight, phone, email, residence)
-- MUST match the MIS Integration Layer (wiremock __files/patients_52.json) — MIS is the
-- single source of truth for patient demographics. Only prosthesis-specific fields
-- (cause, amputation, clinical_state, stump) are maintained locally.
INSERT INTO prosthetics_patients (id, created_at, created_by, updated_at, updated_by, version, pib, birth_date, gender, height_cm, weight_kg, social_status, cause, amputation_date, affected_limb, amputation_level, amputation_site, phone, email, residence, health_status, clinical_state, stump)
VALUES
    ('900001', NOW(), 17, NOW(), 17, 0, 'Сніжко Іван Петрович', '1991-03-14', 'MALE', 182, 84, 'Військовослужбовець', 'Мінно-вибухова травма', '2024-11-08', 'RIGHT', 'upper_third_forearm', 'вище кисті (верхня третина передпліччя)', '380933329111', 'snizhko.ivan@example.com', 'м. Миколаїв, вул. Чапаєва, буд. 54-А, кв. 17', 'задовільний', '{"rom":"Обсяг рухів у плечовому та ліктьовому суглобах у повному обсязі","contractures":"Контрактури, рубці та деформації відсутні","nerves":"Стан нервів задовільний","vessels":"Стан судин задовільний","motor_functions":"Рухи в плечовому та ліктьовому суглобах збережені, рухи в кисті відсутні","healthy_limb":"Ліва кінцівка без патологій","cardiovascular":"Компенсований","respiratory":"Без патологій","digestive":"Без патологій","diseases":"Хронічні захворювання відсутні","activity":"Потребує активної реабілітації"}', '[{"label":"Форма кукси","value":"Циліндрична"},{"label":"Довжина кукси, см","value":"18"},{"label":"Обхват, см","value":"24"}]'),
    ('900002', NOW(), 18, NOW(), 18, 0, 'Гаврилюк Олена Миколаївна', '1986-11-02', 'FEMALE', 168, 71, 'Цивільна особа', 'ДТП', '2025-02-19', 'LEFT', 'below_knee', NULL, '380671112233', 'havryliuk.olena@example.com', 'м. Київ, вул. Володимирська, 25', NULL, NULL, '[{"label":"Форма кукси","value":"Конічна"},{"label":"Довжина кукси, см","value":"12"},{"label":"Обхват, см","value":"28"}]')
ON CONFLICT (id) DO NOTHING;

-- Prosthetics orders
INSERT INTO prosthetics_orders (id, created_at, created_by, updated_at, updated_by, version, order_number, patient_id, prosthesis_type, product_type, amputation_level, limb_side, doctor_name, prescription_date, materials, product_code, manufacturing_approach, approval_number, approval_registry, approval_seq, status, recipe_pdf_generated_at)
VALUES
    ('b0000001-0000-0000-0000-000000000001', NOW(), 17, NOW(), 17, 0, 'ПВ-26-0413', '900001', 'Механічний протез передпліччя', 'UPPER_LIMB', 'upper_third_forearm', 'LEFT', 'Ламбракіс М.', '2026-06-21', '{"items":[{"name":"Гільза","qty":1,"unit":"шт","articul":"UL-SL-01"},{"name":"Приймальна гільза","qty":1,"unit":"шт","articul":"UL-RS-01"},{"name":"Кріплення","qty":1,"unit":"компл","articul":"UL-FX-01"},{"name":"Штучна кисть","qty":1,"unit":"шт","articul":"UL-HD-01"}]}', '06 18 09.В-ТР.-32-069.03-04910-02-150', 'протез верхньої кінцівки з повною рухомістю', '02112128', '60-044-СУПЕРЛЮДИ', '42', 'ACTIVE', NOW()),
    ('b0000002-0000-0000-0000-000000000002', NOW(), 18, NOW(), 18, 0, 'ПВ-26-0414', '900002', 'Протез гомілки', 'LOWER_LIMB', 'below_knee', 'RIGHT', 'Ламбракіс М.', '2026-05-10', '{"items":[{"name":"Чашка","qty":1,"unit":"шт"}]}', NULL, NULL, NULL, NULL, NULL, 'ACTIVE', NOW())
ON CONFLICT (id) DO NOTHING;

-- Flow templates
INSERT INTO prosthetics_flow_templates (id, created_at, created_by, updated_at, updated_by, version, name, description, template_version, product_type, amputation_level, limb_side, status, estimated_duration_min)
VALUES
    ('c0000001-0000-0000-0000-000000000001', NOW(), 17, NOW(), 17, 0, 'TP-UL-01', 'Операційна карта виготовлення протеза передпліччя з тяговим керуванням', 1, 'UPPER_LIMB', 'upper_third_forearm', 'LEFT', 'ACTIVE', 480),
    ('c0000002-0000-0000-0000-000000000002', NOW(), 18, NOW(), 18, 0, 'TP-LL-01', 'Протез гомілки', 1, 'LOWER_LIMB', 'below_knee', 'RIGHT', 'DRAFT', 600)
ON CONFLICT (id) DO NOTHING;

-- Template stages
INSERT INTO prosthetics_template_stages (id, template_id, order_index, name, type, can_skip, requires_approval)
VALUES
    ('d0000001-0000-0000-0000-000000000001', 'c0000001-0000-0000-0000-000000000001', 1, 'Зняття мірок та виготовлення гіпсового негатива', 'CLINICAL', false, false),
    ('d0000002-0000-0000-0000-000000000002', 'c0000001-0000-0000-0000-000000000001', 2, 'Виготовлення гіпсового позитива', 'TECHNICAL', false, false),
    ('d0000005-0000-0000-0000-000000000005', 'c0000001-0000-0000-0000-000000000001', 3, 'Вакуумне термоформування тестової гільзи + примірка', 'TECHNICAL', false, false),
    ('d0000003-0000-0000-0000-000000000003', 'c0000001-0000-0000-0000-000000000001', 4, 'Виготовлення прототипу протеза', 'TECHNICAL', false, false)
ON CONFLICT (id) DO NOTHING;

-- Template steps
INSERT INTO prosthetics_template_steps (id, stage_id, order_index, name, description, step_type, mandatory, allow_backward, auto_start_timer, norm_duration_min)
VALUES
    ('e0000002-0000-0000-0000-000000000002', 'd0000001-0000-0000-0000-000000000001', 1, 'Зняття мірок (з пацієнтом)', 'Ознайомтеся із замовленням на протез. Виміряйте параметри', 'MEASUREMENT', true, true, true, 15),
    ('e0000003-0000-0000-0000-000000000003', 'd0000001-0000-0000-0000-000000000001', 2, 'Виготовлення гіпсового негатива', 'Підтвердіть виготовлення гіпсового негатива', 'INFORMATION', true, true, false, 10),
    ('e0000004-0000-0000-0000-000000000004', 'd0000001-0000-0000-0000-000000000001', 3, 'Перевірка якості гіпсового негатива (з пацієнтом)', 'Перевірте відповідність негатива антропометричним даним', 'INFORMATION', true, true, false, 10),
    ('e0000011-0000-0000-0000-000000000001', 'd0000002-0000-0000-0000-000000000002', 1, 'Виготовлення гіпсового позитива', 'Виготовте гіпсовий позитив', 'INFORMATION', true, true, false, 10),
    ('e0000005-0000-0000-0000-000000000005', 'd0000002-0000-0000-0000-000000000002', 2, 'Перевірка якості гіпсового позитива на відповідність бланку замірів', 'Звірте позитив із заповненим бланком замірів', 'INFORMATION', true, true, false, 10),
    ('e0000041-0000-0000-0000-000000000001', 'd0000005-0000-0000-0000-000000000005', 1, 'Вакуумне термоформування тестової гільзи', 'Виконайте вакуумне термоформування тестової гільзи на гіпсовому позитиві', 'INFORMATION', true, true, false, 15),
    ('e0000042-0000-0000-0000-000000000002', 'd0000005-0000-0000-0000-000000000005', 2, 'Примірка тестової гільзи (з пацієнтом)', 'Проведіть примірку тестової гільзи, перевірте прилягання та комфорт', 'INFORMATION', true, true, false, 20),
    ('e0000060-0000-0000-0000-000000000001', 'd0000003-0000-0000-0000-000000000003', 1, 'Формування комплектації протеза', 'Заповніть компоненти комплектації', 'INFORMATION', true, true, false, 10)
ON CONFLICT (id) DO NOTHING;

-- Template elements
INSERT INTO prosthetics_template_elements (id, step_id, order_index, element_type, label, placeholder, required, unit, min_value, max_value, options, validation_rules)
VALUES
    ('f0000001-0000-0000-0000-000000000001', 'e0000002-0000-0000-0000-000000000002', 1, 'CHECKBOX', 'Засоби індивідуального захисту: нестерильні оглядові нітрилові рукавички', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000004-0000-0000-0000-000000000001', 'e0000003-0000-0000-0000-000000000003', 1, 'CHECKBOX', 'Гіпсовий негатив виготовлено', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000005-0000-0000-0000-000000000001', 'e0000004-0000-0000-0000-000000000004', 1, 'CHECKBOX', 'Гіпсовий негатив перевірено на відповідність антропометричним даним', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000013-0000-0000-0000-000000000001', 'e0000011-0000-0000-0000-000000000001', 1, 'CHECKBOX', 'Гіпсовий позитив виготовлено', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000006-0000-0000-0000-000000000001', 'e0000005-0000-0000-0000-000000000005', 1, 'CHECKBOX', 'Гіпсовий позитив перевірено на відповідність бланку замірів', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000044-0000-0000-0000-000000000001', 'e0000041-0000-0000-0000-000000000001', 1, 'CHECKBOX', 'Засоби індивідуального захисту: захисні окуляри', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000045-0000-0000-0000-000000000001', 'e0000041-0000-0000-0000-000000000001', 2, 'CHECKBOX', 'Засоби індивідуального захисту: респіратор', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000046-0000-0000-0000-000000000001', 'e0000041-0000-0000-0000-000000000001', 3, 'CHECKBOX', 'Засоби індивідуального захисту: захисні навушники', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000047-0000-0000-0000-000000000001', 'e0000041-0000-0000-0000-000000000001', 5, 'CHECKBOX', 'Засоби індивідуального захисту: латексні рукавички підвищеної міцності', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000048-0000-0000-0000-000000000001', 'e0000041-0000-0000-0000-000000000001', 6, 'CHECKBOX', 'Засоби індивідуального захисту: м’які тканинні терморукавиці', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000042-0000-0000-0000-000000000001', 'e0000041-0000-0000-0000-000000000001', 7, 'CHECKBOX', 'Гільза сформована', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000049-0000-0000-0000-000000000001', 'e0000041-0000-0000-0000-000000000001', 8, 'CHECKBOX', 'Краї заокруглені та відполіровані, зроблено отвір для примірки', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000043-0000-0000-0000-000000000001', 'e0000042-0000-0000-0000-000000000002', 1, 'CHECKBOX', 'Тестову гільзу перевірено на відповідність фактичним антропометричним даним пацієнта', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000050-0000-0000-0000-000000000001', 'e0000042-0000-0000-0000-000000000002', 2, 'CHECKBOX', 'Засоби індивідуального захисту: нестерильні оглядові нітрилові рукавички', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000021-0000-0000-0000-000000000001', 'e0000021-0000-0000-0000-000000000001', 1, 'CHECKBOX', 'Гільза приєднана', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000022-0000-0000-0000-000000000002', 'e0000021-0000-0000-0000-000000000001', 2, 'CHECKBOX', 'Зап''ясток встановлено', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000031-0000-0000-0000-000000000001', 'e0000031-0000-0000-0000-000000000001', 1, 'CHECKBOX', 'Розмір відповідає', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000032-0000-0000-0000-000000000002', 'e0000031-0000-0000-0000-000000000001', 2, 'CHECKBOX', 'Вага в межах норми', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000041-0000-0000-0000-000000000001', 'e0000032-0000-0000-0000-000000000002', 1, 'SIGNATURE_CAPTURE', 'Підпис', 'Підтвердіть', true, NULL, NULL, NULL, NULL, NULL),
    ('f0000051-0000-0000-0000-000000000001', 'e0000022-0000-0000-0000-000000000002', 1, 'TEXT_INPUT', 'Кисть', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000052-0000-0000-0000-000000000002', 'e0000022-0000-0000-0000-000000000002', 2, 'TEXT_INPUT', 'Гак', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000053-0000-0000-0000-000000000003', 'e0000022-0000-0000-0000-000000000002', 3, 'TEXT_INPUT', 'Блок зап''ястья', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000054-0000-0000-0000-000000000004', 'e0000022-0000-0000-0000-000000000002', 4, 'TEXT_INPUT', 'Бандаж', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000055-0000-0000-0000-000000000005', 'e0000022-0000-0000-0000-000000000002', 5, 'TEXTAREA', 'Інші компоненти', NULL, false, NULL, NULL, NULL, NULL, NULL),
    ('f0000056-0000-0000-0000-000000000006', 'e0000022-0000-0000-0000-000000000002', 6, 'CHECKBOX', 'Комплектацію сформовано (лист для збірки комплектації на склад)', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000061-0000-0000-0000-000000000001', 'e0000060-0000-0000-0000-000000000001', 1, 'TEXT_INPUT', 'Кисть', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000062-0000-0000-0000-000000000002', 'e0000060-0000-0000-0000-000000000001', 2, 'TEXT_INPUT', 'Гак', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000063-0000-0000-0000-000000000003', 'e0000060-0000-0000-0000-000000000001', 3, 'TEXT_INPUT', 'Блок зап''ястья', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000064-0000-0000-0000-000000000004', 'e0000060-0000-0000-0000-000000000001', 4, 'TEXT_INPUT', 'Бандаж', NULL, true, NULL, NULL, NULL, NULL, NULL),
    ('f0000065-0000-0000-0000-000000000005', 'e0000060-0000-0000-0000-000000000001', 5, 'TEXTAREA', 'Інші компоненти', NULL, false, NULL, NULL, NULL, NULL, NULL),
    ('f0000066-0000-0000-0000-000000000006', 'e0000060-0000-0000-0000-000000000001', 6, 'CHECKBOX', 'Комплектацію сформовано (лист для збірки комплектації на склад)', NULL, true, NULL, NULL, NULL, NULL, NULL)
ON CONFLICT (id) DO NOTHING;

-- Rework loops
INSERT INTO prosthetics_rework_loops (id, gate_id, target_stage_id, target_step_id, rework_type, max_attempts, created_at, created_by, updated_at, updated_by, version)
VALUES
    ('a0000002-0000-0000-0000-000000000001', 'a0000001-0000-0000-0000-000000000001', 'd0000002-0000-0000-0000-000000000002', 'e0000011-0000-0000-0000-000000000001', 'PARTIAL', 3, NOW(), 17, NOW(), 17, 0)
ON CONFLICT (id) DO NOTHING;
