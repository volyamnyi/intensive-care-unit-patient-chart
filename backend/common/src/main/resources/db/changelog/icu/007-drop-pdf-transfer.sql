--liquibase formatted sql

--changeset split-icu:78
--comment: Drop PDF MIS-transfer tracking (Phase 16: transfer banned, PDFs stay
--local for download/print in-module; MisService.sendPdf deleted).
ALTER TABLE generated_pdfs DROP COLUMN IF EXISTS transfer_status;
ALTER TABLE generated_pdfs DROP COLUMN IF EXISTS transfer_error;
ALTER TABLE generated_pdfs DROP COLUMN IF EXISTS transferred_at;

--rollback ALTER TABLE generated_pdfs ADD COLUMN transfer_status VARCHAR(20);
--rollback ALTER TABLE generated_pdfs ADD COLUMN transfer_error VARCHAR(500);
--rollback ALTER TABLE generated_pdfs ADD COLUMN transferred_at TIMESTAMP;
