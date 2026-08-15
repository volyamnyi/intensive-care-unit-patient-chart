-- Integration test seed: MED module tables
-- Split from data-test.sql (DO NOT EDIT BY HAND)

-- Integration test seed data
-- Clean slate: truncate all tables so every test class starts from a known state
TRUNCATE prescription_executions, prescription_day_parts, prescription_item_days, prescription_items, prescription_lists, prescription_signatures, vital_sign_entries, vital_sign_days, vital_sign_lists, medicine_catalog_cache, allergy_cache, drug_interaction_rules, telegram_subscriptions RESTART IDENTITY CASCADE;

INSERT INTO prescription_lists (id, patient_id, department_id, document_name, status, editing_user_id, editing_started_at, created_at, created_by, updated_at, updated_by, version, is_deleted)
VALUES
('cccc0001-0001-0001-0001-000000000001', 1001, NULL, 'Test Prescription', 'Saved', NULL, NULL, NOW(), 11, NOW(), 11, 0, FALSE),
('cccc0002-0002-0002-0002-000000000002', 1002, NULL, 'Second Prescription', 'Finished', NULL, NULL, NOW(), 11, NOW(), 11, 0, FALSE);

INSERT INTO prescription_items (id, list_id, medicine_name, medicine_method, regime, status, sort_order, created_at, created_by, updated_at, updated_by, version, is_deleted)
VALUES
('dddd0001-0001-0001-0001-000000000001', 'cccc0001-0001-0001-0001-000000000001', 'Aspirin', 'PO', 'BID', 'Active', 0, NOW(), 11, NOW(), 11, 0, FALSE);

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
