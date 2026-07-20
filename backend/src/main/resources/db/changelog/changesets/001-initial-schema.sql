--liquibase formatted sql

--changeset patient-chart:1
CREATE TABLE users (
    id BIGSERIAL PRIMARY KEY,
    login VARCHAR(50) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    full_name VARCHAR(200) NOT NULL,
    role VARCHAR(30) NOT NULL,
    email VARCHAR(100),
    speciality_code VARCHAR(20),
    speciality_name VARCHAR(200),
    phone VARCHAR(20),
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by BIGINT,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_by BIGINT,
    version INTEGER NOT NULL DEFAULT 0,
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE episodes (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,
    version INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    patient_id BIGINT NOT NULL,
    hospitalization_id UUID,
    department_id UUID,
    admission_date TIMESTAMP NOT NULL,
    discharge_date TIMESTAMP,
    status VARCHAR(20) NOT NULL,
    height_cm DOUBLE PRECISION,
    ward VARCHAR(50),
    bed_number VARCHAR(20),
    admission_diagnosis VARCHAR(500)
);

CREATE TABLE clinical_days (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,
    version INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    episode_id UUID NOT NULL REFERENCES episodes(id),
    day_number INTEGER NOT NULL,
    start_date_time TIMESTAMP NOT NULL,
    end_date_time TIMESTAMP NOT NULL,
    status VARCHAR(20) NOT NULL,
    doctor_signed BOOLEAN,
    nurse_signed BOOLEAN,
    closed_at TIMESTAMP,
    weight_kg DOUBLE PRECISION
);

CREATE TABLE hourly_records (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,
    version INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    clinical_day_id UUID NOT NULL REFERENCES clinical_days(id),
    record_time TIMESTAMP NOT NULL,
    record_hour INTEGER NOT NULL,
    consciousness VARCHAR(50),
    temperature DOUBLE PRECISION,
    heart_rate INTEGER,
    respiratory_rate INTEGER,
    systolic_bp INTEGER,
    diastolic_bp INTEGER,
    mean_arterial_pressure INTEGER,
    spo2 DOUBLE PRECISION,
    glucose DOUBLE PRECISION,
    etco2 DOUBLE PRECISION,
    fio2 DOUBLE PRECISION,
    cvp DOUBLE PRECISION,
    urine_output DOUBLE PRECISION,
    drain_output DOUBLE PRECISION,
    stool TEXT,
    vomit TEXT,
    pain_score INTEGER,
    notes TEXT,
    CONSTRAINT uk_hourly_records_clinical_day_record_hour UNIQUE (clinical_day_id, record_hour)
);

CREATE TABLE medical_orders (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,
    version INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    clinical_day_id UUID NOT NULL REFERENCES clinical_days(id),
    category VARCHAR(50) NOT NULL,
    drug_name VARCHAR(200) NOT NULL,
    dose VARCHAR(100) NOT NULL,
    unit VARCHAR(50) NOT NULL,
    route VARCHAR(50) NOT NULL,
    frequency VARCHAR(100) NOT NULL,
    start_time TIMESTAMP NOT NULL,
    end_time TIMESTAMP,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE order_executions (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,
    version INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    order_id UUID NOT NULL REFERENCES medical_orders(id),
    executed_by BIGINT NOT NULL,
    executed_at TIMESTAMP NOT NULL,
    actual_dose VARCHAR(100),
    status VARCHAR(30) NOT NULL,
    comment TEXT
);

CREATE TABLE medical_notes (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,
    version INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    clinical_day_id UUID NOT NULL REFERENCES clinical_days(id),
    author_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    note_type VARCHAR(50) NOT NULL,
    text TEXT NOT NULL
);

CREATE TABLE clinical_scales (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,
    version INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    name VARCHAR(200) NOT NULL,
    description TEXT,
    is_automatic BOOLEAN,
    status VARCHAR(20) NOT NULL
);

CREATE TABLE scale_results (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,
    version INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    clinical_day_id UUID NOT NULL REFERENCES clinical_days(id),
    scale_id UUID NOT NULL REFERENCES clinical_scales(id),
    result VARCHAR(100) NOT NULL,
    calculated_at TIMESTAMP NOT NULL,
    calculated_by BIGINT NOT NULL
);

CREATE TABLE fluid_balances (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,
    version INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    clinical_day_id UUID NOT NULL REFERENCES clinical_days(id),
    hour INTEGER NOT NULL,
    intake DOUBLE PRECISION,
    output DOUBLE PRECISION,
    balance DOUBLE PRECISION,
    cumulative_balance DOUBLE PRECISION
);

CREATE TABLE signatures (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,
    version INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    clinical_day_id UUID NOT NULL REFERENCES clinical_days(id),
    user_id BIGINT NOT NULL,
    role VARCHAR(20) NOT NULL,
    signed_at TIMESTAMP NOT NULL,
    hash VARCHAR(256),
    status VARCHAR(20) NOT NULL
);

CREATE TABLE generated_pdfs (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,
    version INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    clinical_day_id UUID NOT NULL REFERENCES clinical_days(id),
    file_name VARCHAR(500) NOT NULL,
    file_version INTEGER NOT NULL,
    generated_at TIMESTAMP NOT NULL,
    generated_by BIGINT NOT NULL,
    checksum VARCHAR(256)
);

CREATE TABLE system_settings (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,
    version INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    key VARCHAR(100) NOT NULL UNIQUE,
    value TEXT NOT NULL,
    description TEXT
);

CREATE TABLE audit_logs (
    id UUID PRIMARY KEY,
    timestamp TIMESTAMP NOT NULL,
    user_id BIGINT,
    entity VARCHAR(100) NOT NULL,
    entity_id UUID,
    action VARCHAR(100) NOT NULL,
    old_value TEXT,
    new_value TEXT,
    correlation_id VARCHAR(100),
    details TEXT,
    ip_address VARCHAR(255),
    user_role VARCHAR(255),
    is_deleted BOOLEAN DEFAULT FALSE
);

CREATE TABLE lab_results (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,
    version INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    clinical_day_id UUID NOT NULL REFERENCES clinical_days(id),
    test_code VARCHAR(20) NOT NULL,
    test_name VARCHAR(100) NOT NULL,
    result VARCHAR(50) NOT NULL,
    unit VARCHAR(30),
    reference_min DOUBLE PRECISION,
    reference_max DOUBLE PRECISION,
    is_abnormal BOOLEAN,
    measured_at TIMESTAMP NOT NULL
);

CREATE TABLE ventilation_settings (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,
    version INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    clinical_day_id UUID NOT NULL REFERENCES clinical_days(id),
    record_hour INTEGER NOT NULL,
    mode VARCHAR(30),
    fio2 DOUBLE PRECISION,
    peep DOUBLE PRECISION,
    tidal_volume DOUBLE PRECISION,
    minute_volume DOUBLE PRECISION,
    pinsp DOUBLE PRECISION,
    psupport DOUBLE PRECISION,
    trigger_type VARCHAR(50),
    ie_ratio VARCHAR(10),
    respiratory_rate INTEGER,
    plateau_pressure DOUBLE PRECISION,
    mean_airway_pressure DOUBLE PRECISION
);

CREATE TABLE patient_state_assessments (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,
    version INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    clinical_day_id UUID NOT NULL REFERENCES clinical_days(id),
    record_hour INTEGER NOT NULL,
    consciousness VARCHAR(30),
    skin VARCHAR(30),
    edema VARCHAR(20),
    mucous_membranes VARCHAR(30),
    peripheral_circulation VARCHAR(30),
    bowel_sounds VARCHAR(30),
    general_condition VARCHAR(30),
    additional_notes VARCHAR(500)
);

CREATE TABLE reference_values (
    id UUID PRIMARY KEY,
    created_at TIMESTAMP NOT NULL,
    created_by BIGINT NOT NULL,
    updated_at TIMESTAMP NOT NULL,
    updated_by BIGINT NOT NULL,
    version INTEGER NOT NULL,
    is_deleted BOOLEAN DEFAULT FALSE,
    type VARCHAR(100) NOT NULL,
    code VARCHAR(100) NOT NULL,
    value VARCHAR(500) NOT NULL,
    sort_order INTEGER,
    is_active BOOLEAN DEFAULT TRUE,
    CONSTRAINT uk_reference_values_type_code UNIQUE (type, code)
);
