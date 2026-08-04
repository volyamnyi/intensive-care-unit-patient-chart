TRUNCATE prosthetics_rework_loops, prosthetics_gate_decisions, prosthetics_evidence_files, prosthetics_failure_snapshots, prosthetics_step_executions, prosthetics_resource_usages, prosthetics_flow_instances, prosthetics_template_elements, prosthetics_template_steps, prosthetics_template_stages, prosthetics_flow_templates, prosthetics_orders, prosthetics_patients, users RESTART IDENTITY CASCADE;

INSERT INTO users (id, login, password_hash, full_name, role, email, speciality_code, speciality_name, phone, created_at, created_by, updated_at, updated_by, version)
VALUES
(21, 'prosthetist1', '$2a$10$LQeytYedrrlf3Dzg5jaUiuALhgGwku50pJL64hUrc/PkMHm7ulPpO', 'Романюк Олег Петрович', 'PROSTHETIST', 'romanyuk@hospital.ua', '401', 'Технолог виготовлення протезів', '380507777777', NOW(), 21, NOW(), 21, 0),
(22, 'prosthetist2', '$2a$10$LQeytYedrrlf3Dzg5jaUiuALhgGwku50pJL64hUrc/PkMHm7ulPpO', 'Шевчук Іван Миколайович', 'PROSTHETIST', 'shevchuk@hospital.ua', '401', 'Технолог виготовлення протезів', '380508888888', NOW(), 22, NOW(), 22, 0),
(23, 'prosthetics_admin1', '$2a$10$LQeytYedrrlf3Dzg5jaUiuALhgGwku50pJL64hUrc/PkMHm7ulPpO', 'Пташник Олена Сергіївна', 'PROSTHETICS_ADMINISTRATOR', 'ptadmin@hospital.ua', '402', 'Адміністратор протезування', '380509999999', NOW(), 23, NOW(), 23, 0);

INSERT INTO prosthetics_patients (id, pib, birth_date, gender, height_cm, weight_kg, social_status, cause, amputation_date, affected_limb, amputation_level, stump)
VALUES
('a0000001-0000-4000-8000-000000000001', 'Сніжко Оксана Володимирівна', '1978-05-12', 'female', 168, 72, 'social', 'trauma', '2025-03-15', 'left', 'upper', '[{"label":"19 см","value":"19"}]'),
('a0000002-0000-4000-8000-000000000002', 'Гаврилюк Тарас Олексійович', '1985-11-20', 'male', 175, 80, 'social', 'trauma', '2025-06-01', 'right', 'lower', '[{"label":"22 см","value":"22"}]');

INSERT INTO prosthetics_orders (id, order_number, patient_id, prosthesis_type, product_type, amputation_level, limb_side, doctor_name, prescription_date, materials, status)
VALUES
('b0000001-0000-4000-8000-000000000001', 'ПВ-26-0413', 'a0000001-0000-4000-8000-000000000001', 'протез передпліччя', 'upper_limb', 'upper', 'left', 'Бондаренко І.П.', '2026-07-10', '[{"name":"термопласт","qty":2}]', 'NEW'),
('b0000002-0000-4000-8000-000000000002', 'ПВ-26-0414', 'a0000002-0000-4000-8000-000000000002', 'протез нижньої кінцівки', 'lower_limb', 'lower', 'right', 'Петренко М.С.', '2026-07-12', '[{"name":"карбон","qty":1}]', 'NEW');

INSERT INTO prosthetics_flow_templates (id, name, description, template_version, product_type, amputation_level, limb_side, status, estimated_duration_min)
VALUES
('c0000001-0000-4000-8000-000000000001', 'TP-UL-01', 'Шаблон виготовлення протезу передпліччя', 1, 'upper_limb', 'upper', 'left', 'ACTIVE', 240),
('c0000002-0000-4000-8000-000000000002', 'TP-LL-01', 'Шаблон виготовлення протезу нижньої кінцівки', 1, 'lower_limb', 'lower', 'right', 'DRAFT', 360);

INSERT INTO prosthetics_template_stages (id, template_id, order_index, name, type, can_skip, requires_approval)
VALUES
('d0000001-0000-4000-8000-000000000001', 'c0000001-0000-4000-8000-000000000001', 0, 'Клінічне обстеження', 'TECHNICAL', false, false),
('d0000002-0000-4000-8000-000000000002', 'c0000001-0000-4000-8000-000000000001', 1, 'Виготовлення', 'TECHNICAL', false, false),
('d0000003-0000-4000-8000-000000000003', 'c0000001-0000-4000-8000-000000000001', 2, 'Контроль якості', 'QUALITY', false, true);

INSERT INTO prosthetics_template_steps (id, stage_id, order_index, name, description, step_type, mandatory, allow_backward, auto_start_timer, norm_duration_min)
VALUES
('e0000001-0000-4000-8000-000000000001', 'd0000001-0000-4000-8000-000000000001', 0, 'Вимірювання', 'Вимірювання розмірів залишків', 'MEASUREMENT', true, true, true, 30),
('e0000002-0000-4000-8000-000000000002', 'd0000001-0000-4000-8000-000000000001', 1, 'Анатомічний анамнез', 'Збір анамнезу', 'ANAMNESIS', true, false, false, 20),
('e0000003-0000-4000-8000-000000000003', 'd0000002-0000-4000-8000-000000000002', 0, 'Приплічка', 'Виготовлення приплічки', 'MANUFACTURING', true, false, false, 120);

INSERT INTO prosthetics_template_elements (id, step_id, order_index, element_type, label, placeholder, required, unit, min_value, max_value, min_count, max_count, mime_types, max_size_mb, regex_pattern, options)
VALUES
('f0000001-0000-4000-8000-000000000001', 'e0000001-0000-4000-8000-000000000001', 0, 'NUMERIC_INPUT', 'Довжина кукси, см', 'Введіть довжину', true, 'см', 1, 60, 1, 1, NULL, NULL, NULL, NULL),
('f0000002-0000-4000-8000-000000000002', 'e0000001-0000-4000-8000-000000000001', 1, 'TEXT_INPUT', 'Матеріал', 'Введіть матеріал', true, NULL, NULL, NULL, NULL, NULL, NULL, NULL, '^[A-Za-z]+$', NULL),
('f0000003-0000-4000-8000-000000000003', 'e0000002-0000-4000-8000-000000000002', 0, 'TEXT_INPUT', 'Диагноз', 'Введіть діагноз', true, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL, NULL),
('f0000004-0000-4000-8000-000000000004', 'e0000003-0000-4000-8000-000000000003', 0, 'FILE_UPLOAD', 'Креслення', 'Завантажте креслення', true, NULL, NULL, NULL, 1, 5, '["image/png","image/jpeg","application/pdf"]', 10, NULL, NULL);

INSERT INTO prosthetics_quality_gates (id, stage_id, name, description, required_approver_role, checklist, attachments_required)
VALUES
('g0000001-0000-4000-8000-000000000001', 'd0000003-0000-4000-8000-000000000003', 'Приймальний контроль', 'Контроль якості перед видачею', 'PROSTHETICS_ADMINISTRATOR', '["Відповідність технічному завданню","Повнота документації","Візуальний контроль"]', false);

INSERT INTO prosthetics_rework_loops (id, gate_id, target_stage_id, target_step_id, rework_type, max_attempts)
VALUES
('h0000001-0000-4000-8000-000000000001', 'g0000001-0000-4000-8000-000000000001', 'd0000002-0000-4000-8000-000000000002', 'e0000003-0000-4000-8000-000000000003', 'PARTIAL', 2);