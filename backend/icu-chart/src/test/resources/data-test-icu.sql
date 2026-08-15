-- Integration test seed: ICU module tables
-- Split from data-test.sql (DO NOT EDIT BY HAND)

-- Integration test seed data
-- Clean slate: truncate all tables so every test class starts from a known state
TRUNCATE episodes, clinical_days, hourly_records, medical_orders, order_executions, medical_notes, clinical_scales, scale_results, fluid_balances, signatures, generated_pdfs, patient_state_assessments, ventilation_settings, lab_results RESTART IDENTITY CASCADE;

INSERT INTO episodes (id, patient_id, hospitalization_id, department_id, admission_date, status, created_at, created_by, updated_at, updated_by, version)
VALUES
('a1111111-1111-1111-1111-111111111111', 1001, NULL, NULL, NOW() - INTERVAL '2 days', 'ACTIVE', NOW(), 11, NOW(), 11, 0),
('a2222222-2222-2222-2222-222222222222', 1002, NULL, NULL, NOW() - INTERVAL '1 day', 'ACTIVE', NOW(), 11, NOW(), 11, 0),
('a3333333-3333-3333-3333-333333333333', 1003, NULL, NULL, NOW() - INTERVAL '3 days', 'ACTIVE', NOW(), 12, NOW(), 12, 0);

INSERT INTO clinical_days (id, episode_id, day_number, start_date_time, end_date_time, status, doctor_signed, nurse_signed, created_at, created_by, updated_at, updated_by, version)
VALUES
('b1111112-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', 1, DATE_TRUNC('day', NOW()) - INTERVAL '1 day' + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', 'NURSE_SIGNED', false, true, NOW(), 11, NOW(), 11, 0),
('b1111111-1111-1111-1111-111111111111', 'a1111111-1111-1111-1111-111111111111', 2, DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours' + INTERVAL '1 day', 'OPEN', false, false, NOW(), 11, NOW(), 11, 0),
('b1111112-1111-1111-1111-111111111112', 'a1111111-1111-1111-1111-111111111111', 3, DATE_TRUNC('day', NOW()) - INTERVAL '1 day' + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', 'NURSE_SIGNED', false, true, NOW(), 11, NOW(), 11, 0),
('b4444444-4444-4444-4444-444444444444', 'a2222222-2222-2222-2222-222222222222', 1, DATE_TRUNC('day', NOW()) - INTERVAL '1 day' + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', 'NURSE_SIGNED', false, true, NOW(), 11, NOW(), 11, 0),
('b2222222-2222-2222-2222-222222222222', 'a2222222-2222-2222-2222-222222222222', 2, DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours' + INTERVAL '1 day', 'OPEN', false, false, NOW(), 11, NOW(), 11, 0),
('b3333333-3333-3333-3333-333333333333', 'a3333333-3333-3333-3333-333333333333', 1, DATE_TRUNC('day', NOW()) + INTERVAL '8 hours', DATE_TRUNC('day', NOW()) + INTERVAL '8 hours' + INTERVAL '1 day', 'OPEN', false, false, NOW(), 12, NOW(), 12, 0);

INSERT INTO clinical_scales (id, name, description, is_automatic, status, created_at, created_by, updated_at, updated_by, version)
VALUES
('c1111111-1111-1111-1111-111111111101', 'GCS', 'Glasgow Coma Scale', true, 'ACTIVE', NOW(), 11, NOW(), 11, 0),
('c1111111-1111-1111-1111-111111111102', 'RASS', 'Richmond Agitation-Sedation Scale', true, 'ACTIVE', NOW(), 11, NOW(), 11, 0),
('c1111111-1111-1111-1111-111111111103', 'SOFA', 'Sequential Organ Failure Assessment', false, 'ACTIVE', NOW(), 11, NOW(), 11, 0),
('c1111111-1111-1111-1111-111111111104', 'APACHE II', 'Acute Physiology And Chronic Health Evaluation II', false, 'ACTIVE', NOW(), 11, NOW(), 11, 0),
('c1111111-1111-1111-1111-111111111105', 'CAM-ICU', 'Confusion Assessment Method for the ICU', false, 'ACTIVE', NOW(), 11, NOW(), 11, 0),
('c1111111-1111-1111-1111-111111111106', 'Браден', 'Braden Scale — pressure injury risk assessment', false, 'ACTIVE', NOW(), 11, NOW(), 11, 0);

-- Seed an episode-level scale result for APACHE II
INSERT INTO scale_results (id, clinical_day_id, scale_id, episode_id, result, raw_data, calculated_at, calculated_by, created_at, created_by, updated_at, updated_by, version)
VALUES
('c1111111-1111-1111-1111-111111111201', NULL, 'c1111111-1111-1111-1111-111111111104', 'a3333333-3333-3333-3333-333333333333', '25', '{"temperatureC":38.5}', NOW(), 12, NOW(), 12, NOW(), 12, 0);
