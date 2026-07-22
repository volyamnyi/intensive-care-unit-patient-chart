--liquibase formatted sql

--changeset patient-chart:2
ALTER TABLE episodes ADD COLUMN attending_doctor_id BIGINT;

--changeset patient-chart:3
ALTER TABLE signatures ADD COLUMN cert_serial_number VARCHAR(64);
ALTER TABLE signatures ADD COLUMN cert_issuer VARCHAR(256);
ALTER TABLE signatures ADD COLUMN cert_subject VARCHAR(256);
ALTER TABLE signatures ADD COLUMN cert_valid_from TIMESTAMP;
ALTER TABLE signatures ADD COLUMN cert_valid_until TIMESTAMP;

--changeset patient-chart:4
ALTER TABLE generated_pdfs ADD COLUMN file_data OID;
ALTER TABLE generated_pdfs ADD COLUMN transfer_status VARCHAR(20) DEFAULT 'PENDING';
ALTER TABLE generated_pdfs ADD COLUMN transfer_error VARCHAR(500);
ALTER TABLE generated_pdfs ADD COLUMN transferred_at TIMESTAMP;
