--liquibase formatted sql

-- Secondary indexes for the ICU clinical tables (CR-07 #419).
-- The icu changelog previously carried ONLY the two UNIQUE constraints
-- (hourly_records, order_executions): PostgreSQL does not auto-index FK or
-- join columns, so the patient-day screen — the hottest page in the system —
-- paid ~8 seq scans per request (orders, notes, scales, fluid balance,
-- signatures, labs, ventilation, patient state), each growing linearly with
-- total archived history. Every index below is created CONCURRENTLY with
-- runInTransaction:false (live-table rule); each changeset drops first so a
-- partially failed run can be retried safely (a failed CONCURRENTLY build
-- leaves an INVALID index that the drop clears).

--changeset split-icu:80 runInTransaction:false
--comment episodes.patient_id — patient episode history / create-card search
DROP INDEX CONCURRENTLY IF EXISTS idx_episodes_patient;
CREATE INDEX CONCURRENTLY idx_episodes_patient ON episodes (patient_id);
--rollback DROP INDEX IF EXISTS idx_episodes_patient;

--changeset split-icu:81 runInTransaction:false
--comment episodes(status, patient_id) — dashboard active lists; doubles as the probe index for the one-ACTIVE-episode guard
DROP INDEX CONCURRENTLY IF EXISTS idx_episodes_status_patient;
CREATE INDEX CONCURRENTLY idx_episodes_status_patient ON episodes (status, patient_id);
--rollback DROP INDEX IF EXISTS idx_episodes_status_patient;

--changeset split-icu:82 runInTransaction:false
--comment clinical_days(episode_id, day_number) — day list per episode + per-episode latest-day lookups
DROP INDEX CONCURRENTLY IF EXISTS idx_clinical_days_episode;
CREATE INDEX CONCURRENTLY idx_clinical_days_episode ON clinical_days (episode_id, day_number);
--rollback DROP INDEX IF EXISTS idx_clinical_days_episode;

--changeset split-icu:83 runInTransaction:false
--comment medical_orders.clinical_day_id — therapy panel of the patient-day screen
DROP INDEX CONCURRENTLY IF EXISTS idx_medical_orders_day;
CREATE INDEX CONCURRENTLY idx_medical_orders_day ON medical_orders (clinical_day_id);
--rollback DROP INDEX IF EXISTS idx_medical_orders_day;

--changeset split-icu:84 runInTransaction:false
--comment medical_notes.clinical_day_id — notes panel
DROP INDEX CONCURRENTLY IF EXISTS idx_medical_notes_day;
CREATE INDEX CONCURRENTLY idx_medical_notes_day ON medical_notes (clinical_day_id);
--rollback DROP INDEX IF EXISTS idx_medical_notes_day;

--changeset split-icu:85 runInTransaction:false
--comment scale_results.clinical_day_id — day-level scale history + PDF section
DROP INDEX CONCURRENTLY IF EXISTS idx_scale_results_day;
CREATE INDEX CONCURRENTLY idx_scale_results_day ON scale_results (clinical_day_id);
--rollback DROP INDEX IF EXISTS idx_scale_results_day;

--changeset split-icu:86 runInTransaction:false
--comment scale_results.episode_id — episode-level scale history + PDF section
DROP INDEX CONCURRENTLY IF EXISTS idx_scale_results_episode;
CREATE INDEX CONCURRENTLY idx_scale_results_episode ON scale_results (episode_id);
--rollback DROP INDEX IF EXISTS idx_scale_results_episode;

--changeset split-icu:87 runInTransaction:false
--comment fluid_balances(clinical_day_id, hour) — balance grid
DROP INDEX CONCURRENTLY IF EXISTS idx_fluid_balances_day;
CREATE INDEX CONCURRENTLY idx_fluid_balances_day ON fluid_balances (clinical_day_id, hour);
--rollback DROP INDEX IF EXISTS idx_fluid_balances_day;

--changeset split-icu:88 runInTransaction:false
--comment signatures.clinical_day_id — sign state of the day
DROP INDEX CONCURRENTLY IF EXISTS idx_signatures_day;
CREATE INDEX CONCURRENTLY idx_signatures_day ON signatures (clinical_day_id);
--rollback DROP INDEX IF EXISTS idx_signatures_day;

--changeset split-icu:89 runInTransaction:false
--comment generated_pdfs(clinical_day_id, file_version DESC) — version probes avoid seq-scanning the bytea table
DROP INDEX CONCURRENTLY IF EXISTS idx_generated_pdfs_day;
CREATE INDEX CONCURRENTLY idx_generated_pdfs_day ON generated_pdfs (clinical_day_id, file_version DESC);
--rollback DROP INDEX IF EXISTS idx_generated_pdfs_day;

--changeset split-icu:90 runInTransaction:false
--comment lab_results.clinical_day_id — lab panel
DROP INDEX CONCURRENTLY IF EXISTS idx_lab_results_day;
CREATE INDEX CONCURRENTLY idx_lab_results_day ON lab_results (clinical_day_id);
--rollback DROP INDEX IF EXISTS idx_lab_results_day;

--changeset split-icu:91 runInTransaction:false
--comment ventilation_settings(clinical_day_id, record_hour) — ventilation panel
DROP INDEX CONCURRENTLY IF EXISTS idx_ventilation_day;
CREATE INDEX CONCURRENTLY idx_ventilation_day ON ventilation_settings (clinical_day_id, record_hour);
--rollback DROP INDEX IF EXISTS idx_ventilation_day;

--changeset split-icu:92 runInTransaction:false
--comment patient_state_assessments(clinical_day_id, record_hour) — patient state panel
DROP INDEX CONCURRENTLY IF EXISTS idx_patient_state_day;
CREATE INDEX CONCURRENTLY idx_patient_state_day ON patient_state_assessments (clinical_day_id, record_hour);
--rollback DROP INDEX IF EXISTS idx_patient_state_day;
