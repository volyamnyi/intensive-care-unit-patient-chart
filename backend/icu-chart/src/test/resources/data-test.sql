-- Integration test seed data
-- Clean slate: truncate all tables so every test class starts from a known state
TRUNCATE users, episodes, clinical_days, hourly_records, medical_orders, order_executions,
        medical_notes, clinical_scales, scale_results, fluid_balances, signatures,
        generated_pdfs, patient_state_assessments, ventilation_settings, lab_results,
        audit_logs, system_settings, reference_values,
        prescription_executions, prescription_day_parts, prescription_item_days,
        prescription_items, prescription_lists, prescription_signatures,
        vital_sign_entries, vital_sign_days, vital_sign_lists,
        medicine_catalog_cache, allergy_cache, drug_interaction_rules, telegram_subscriptions
        RESTART IDENTITY CASCADE;

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
('b1111112-1111-1111-1111-111111111112', 'a1111111-1111-1111-1111-111111111111', 3, DATE_TRUNC('day', NOW()) - INTERVAL '1 day' + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', 'NURSE_SIGNED', false, true, NOW(), 11, NOW(), 11, 0),
('b2222222-2222-2222-2222-222222222222', 'a2222222-2222-2222-2222-222222222222', 1, DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours' + INTERVAL '1 day', 'OPEN', false, false, NOW(), 11, NOW(), 11, 0),
('b3333333-3333-3333-3333-333333333333', 'a3333333-3333-3333-3333-333333333333', 1, DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours' + INTERVAL '1 day', 'OPEN', false, false, NOW(), 12, NOW(), 12, 0),
('b4444444-4444-4444-4444-444444444444', 'a2222222-2222-2222-2222-222222222222', 2, DATE_TRUNC('day', NOW()) - INTERVAL '1 day' + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', 'NURSE_SIGNED', false, true, NOW(), 11, NOW(), 11, 0);

INSERT INTO prescription_lists (id, patient_id, document_name, status, created_at, created_by, updated_at, updated_by, version)
VALUES
('cccc0001-0001-0001-0001-000000000001', 1001, 'Test Prescription', 'Saved', NOW(), 11, NOW(), 11, 0),
('cccc0002-0002-0002-0002-000000000002', 1002, 'Second Prescription', 'Finished', NOW(), 11, NOW(), 11, 0);

INSERT INTO prescription_items (id, list_id, medicine_name, medicine_method, regime, status, sort_order, created_at, created_by, updated_at, updated_by, version)
VALUES
('dddd0001-0001-0001-0001-000000000001', 'cccc0001-0001-0001-0001-000000000001', 'Aspirin', 'PO', 'BID', 'Active', 0, NOW(), 11, NOW(), 11, 0);

INSERT INTO prescription_item_days (id, item_id, day_date, created_at, created_by, updated_at, updated_by, version)
VALUES
('eeee0001-0001-0001-0001-000000000001', 'dddd0001-0001-0001-0001-000000000001', CURRENT_DATE, NOW(), 11, NOW(), 11, 0);

INSERT INTO prescription_day_parts (id, day_id, period, dose, is_planned, is_completed, created_at, created_by, updated_at, updated_by, version)
VALUES
('ffff0001-0001-0001-0001-000000000001', 'eeee0001-0001-0001-0001-000000000001', 'morning', '50mg', false, false, NOW(), 11, NOW(), 11, 0),
('ffff0002-0002-0002-0002-000000000002', 'eeee0001-0001-0001-0001-000000000001', 'day', '25mg', false, false, NOW(), 11, NOW(), 11, 0),
('ffff0003-0003-0003-0003-000000000003', 'eeee0001-0001-0001-0001-000000000001', 'evening', NULL, false, false, NOW(), 11, NOW(), 11, 0),
('ffff0004-0004-0004-0004-000000000004', 'eeee0001-0001-0001-0001-000000000001', 'night', NULL, false, false, NOW(), 11, NOW(), 11, 0);

INSERT INTO vital_sign_lists (id, prescription_list_id, created_at, created_by, updated_at, updated_by, version)
VALUES
('bbbb0001-0001-0001-0001-000000000001', 'cccc0001-0001-0001-0001-000000000001', NOW(), 11, NOW(), 11, 0);

INSERT INTO vital_sign_days (id, vital_list_id, day_date, created_at, created_by, updated_at, updated_by, version)
VALUES
('bbbb0002-0002-0002-0002-000000000001', 'bbbb0001-0001-0001-0001-000000000001', CURRENT_DATE, NOW(), 11, NOW(), 11, 0);

INSERT INTO vital_sign_entries (id, day_id, period, temperature, systolic_bp, diastolic_bp, spo2, pulse, created_at, created_by, updated_at, updated_by, version)
VALUES
('bbbb0003-0003-0003-0003-000000000001', 'bbbb0002-0002-0002-0002-000000000001', 'morning', 36.6, 120, 80, 98, 72, NOW(), 11, NOW(), 11, 0),
('bbbb0004-0004-0004-0004-000000000001', 'bbbb0002-0002-0002-0002-000000000001', 'evening', NULL, NULL, NULL, NULL, NULL, NOW(), 11, NOW(), 11, 0);

-- ============================================================================
-- Seed: 20 surgery (2001-2020) + 20 rehabilitation (2021-2040) patients
-- with randomly planned/completed prescription lists
-- ============================================================================
DO $$
DECLARE
    med_names TEXT[] := ARRAY['Ceftriaxone','Dopamine','Norepinephrine','Paracetamol',
        'Morphine','Heparin','NaCl 0.9%','Metronidazole','Omeprazole','Midazolam',
        'Propofol','Insulin','Fentanyl','Dexamethasone','Pantoprazole',
        'Ondansetron','Amlodipine','Glucose 5%'];
    med_methods TEXT[] := ARRAY['IV','IV','IV','PO',
        'IV','SC','IV','IV','PO','IV',
        'IV','SC','IV','PO','PO',
        'IV','PO','IV'];
    doses TEXT[] := ARRAY['1g','200mg','4mcg','500mg',
        '10mg','5000IU','500ml','500mg','40mg','5mg',
        '100mg','4IU','50mcg','8mg','40mg',
        '4mg','5mg','500ml'];
    p_id BIGINT;
    list_id UUID;
    item_id UUID;
    day_id UUID;
    part_id UUID;
    vitallist_id UUID;
    vitalday_id UUID;
    entry_id UUID;
    i INT;
    d INT;
    period TEXT;
    drug_idx INT;
    items_count INT;
    is_planned BOOLEAN;
    is_completed BOOLEAN;
    dose_val TEXT;
    periods TEXT[] := ARRAY['morning','day','evening','night'];
    vital_periods TEXT[] := ARRAY['morning','evening'];
    p_idx INT;
BEGIN
    FOR p_id IN 2001..2040 LOOP
        -- 1 prescription list per patient
        list_id := gen_random_uuid();
        INSERT INTO prescription_lists (id, patient_id, document_name, status, created_at, created_by, updated_at, updated_by, version)
        VALUES (list_id, p_id,
            CASE WHEN p_id <= 2020 THEN 'Листок призначень - Хірургія'
                 ELSE 'Листок призначень - Реабілітація' END,
            'Saved', NOW(), 11, NOW(), 11, 0);

        -- 2-5 items per list (varies by patient_id)
        items_count := 2 + (p_id % 4);
        FOR i IN 1..items_count LOOP
            item_id := gen_random_uuid();
            drug_idx := 1 + ((p_id * 7 + i * 3) % array_length(med_names, 1));

            INSERT INTO prescription_items (id, list_id, medicine_name, medicine_method, regime, status, sort_order, created_at, created_by, updated_at, updated_by, version)
            VALUES (item_id, list_id, med_names[drug_idx], med_methods[drug_idx], '', 'Active', i - 1, NOW(), 11, NOW(), 11, 0);

            -- 21 days per item
            FOR d IN 0..20 LOOP
                day_id := gen_random_uuid();
                INSERT INTO prescription_item_days (id, item_id, day_date, created_at, created_by, updated_at, updated_by, version)
                VALUES (day_id, item_id, CURRENT_DATE + d, NOW(), 11, NOW(), 11, 0);

                -- 4 day-parts per day
                FOR p_idx IN 1..4 LOOP
                    period := periods[p_idx];
                    part_id := gen_random_uuid();
                    -- ~67% cells are planned
                    is_planned := ((p_id * 13 + d * 7 + p_idx * 11) % 3) <> 0;
                    -- ~25% of planned cells are completed
                    is_completed := is_planned AND ((p_id * 17 + d * 5 + p_idx * 3) % 4 = 0);
                    dose_val := CASE WHEN is_planned THEN doses[drug_idx] ELSE NULL END;

                    INSERT INTO prescription_day_parts (id, day_id, period, dose, is_planned, is_completed, is_planned_finished, is_completed_finished, created_at, created_by, updated_at, updated_by, version)
                    VALUES (part_id, day_id, period, dose_val, is_planned, is_completed, false, false, NOW(), 11, NOW(), 11, 0);
                END LOOP;
            END LOOP;
        END LOOP;

        -- vital signs list for each prescription list
        vitallist_id := gen_random_uuid();
        INSERT INTO vital_sign_lists (id, prescription_list_id, created_at, created_by, updated_at, updated_by, version)
        VALUES (vitallist_id, list_id, NOW(), 11, NOW(), 11, 0);

        -- 21 days of vitals
        FOR d IN 0..20 LOOP
            vitalday_id := gen_random_uuid();
            INSERT INTO vital_sign_days (id, vital_list_id, day_date, created_at, created_by, updated_at, updated_by, version)
            VALUES (vitalday_id, vitallist_id, CURRENT_DATE + d, NOW(), 11, NOW(), 11, 0);

            -- morning + evening
            FOR i IN 1..2 LOOP
                entry_id := gen_random_uuid();
                INSERT INTO vital_sign_entries (id, day_id, period, temperature, systolic_bp, diastolic_bp, spo2, pulse, stool, pain_score, created_at, created_by, updated_at, updated_by, version)
                VALUES (entry_id, vitalday_id, vital_periods[i],
                    CASE WHEN (p_id + d + i) % 3 <> 0
                        THEN 36.0 + (((p_id * 11 + d * 7 + i * 3) % 20)::decimal / 10.0)
                        ELSE NULL END,
                    CASE WHEN (p_id + d + i) % 3 <> 0
                        THEN 100 + ((p_id * 13 + d * 5 + i * 7) % 60)::integer
                        ELSE NULL END,
                    CASE WHEN (p_id + d + i) % 3 <> 0
                        THEN 60 + ((p_id * 7 + d * 11 + i * 5) % 40)::integer
                        ELSE NULL END,
                    CASE WHEN (p_id + d + i) % 3 <> 0
                        THEN 92 + ((p_id * 19 + d * 3 + i * 13) % 8)::integer
                        ELSE NULL END,
                    CASE WHEN (p_id + d + i) % 3 <> 0
                        THEN 60 + ((p_id * 23 + d * 13 + i * 17) % 50)::integer
                        ELSE NULL END,
                    CASE WHEN (p_id + d + i) % 7 = 0 THEN 'normal' ELSE NULL END,
                    CASE WHEN (p_id + d + i) % 5 = 0
                        THEN ((p_id * 3 + d * 2 + i) % 5)::integer
                        ELSE NULL END,
                    NOW(), 11, NOW(), 11, 0);
            END LOOP;
        END LOOP;
    END LOOP;
END $$;
