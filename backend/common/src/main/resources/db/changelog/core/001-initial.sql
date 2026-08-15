--liquibase formatted sql

--changeset split-core:1
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

