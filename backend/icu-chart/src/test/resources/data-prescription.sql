-- Seed: 20 surgery (2001-2020) + 20 rehabilitation (2021-2040) patients
-- with randomly planned/completed prescription lists
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
        list_id := gen_random_uuid();
        INSERT INTO prescription_lists (id, patient_id, department_id, document_name, status, editing_user_id, editing_started_at, created_at, created_by, updated_at, updated_by, version, is_deleted)
        VALUES (list_id, p_id,
            CASE WHEN p_id <= 2020 THEN 2 ELSE 1 END,
            CASE WHEN p_id <= 2020 THEN 'Листок призначень - Хірургія'
                 ELSE 'Листок призначень - Реабілітація' END,
            'Saved', NULL, NULL, NOW(), 11, NOW(), 11, 0, FALSE);

        items_count := 2 + (p_id % 4);
        FOR i IN 1..items_count LOOP
            item_id := gen_random_uuid();
            drug_idx := 1 + ((p_id * 7 + i * 3) % array_length(med_names, 1));

            INSERT INTO prescription_items (id, list_id, medicine_name, medicine_method, regime, status, sort_order, created_at, created_by, updated_at, updated_by, version)
            VALUES (item_id, list_id, med_names[drug_idx], med_methods[drug_idx], '', 'Active', i - 1, NOW(), 11, NOW(), 11, 0);

            FOR d IN 0..20 LOOP
                day_id := gen_random_uuid();
                INSERT INTO prescription_item_days (id, item_id, day_date, created_at, created_by, updated_at, updated_by, version)
                VALUES (day_id, item_id, CURRENT_DATE + d, NOW(), 11, NOW(), 11, 0);

                FOR p_idx IN 1..4 LOOP
                    period := periods[p_idx];
                    part_id := gen_random_uuid();
                    is_planned := ((p_id * 13 + d * 7 + p_idx * 11) % 3) <> 0;
                    is_completed := is_planned AND ((p_id * 17 + d * 5 + p_idx * 3) % 4 = 0);
                    dose_val := CASE WHEN is_planned THEN doses[drug_idx] ELSE NULL END;

                    INSERT INTO prescription_day_parts (id, day_id, period, dose, is_planned, is_completed, is_planned_finished, is_completed_finished, created_at, created_by, updated_at, updated_by, version)
                    VALUES (part_id, day_id, period, dose_val, is_planned, is_completed, false, false, NOW(), 11, NOW(), 11, 0);
                END LOOP;
            END LOOP;
        END LOOP;

        vitallist_id := gen_random_uuid();
        INSERT INTO vital_sign_lists (id, prescription_list_id, created_at, created_by, updated_at, updated_by, version)
        VALUES (vitallist_id, list_id, NOW(), 11, NOW(), 11, 0);

        FOR d IN 0..20 LOOP
            vitalday_id := gen_random_uuid();
            INSERT INTO vital_sign_days (id, vital_list_id, day_date, created_at, created_by, updated_at, updated_by, version)
            VALUES (vitalday_id, vitallist_id, CURRENT_DATE + d, NOW(), 11, NOW(), 11, 0);

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
GO