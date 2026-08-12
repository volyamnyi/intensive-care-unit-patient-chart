--liquibase formatted sql

--changeset superhumans:015-1
-- JwtAuthenticationFilter stores the request URI in audit_logs.entity; URIs with two
-- UUID path segments (e.g. /instances/{uuid}/steps/{uuid}/complete) exceed 100 chars.
ALTER TABLE audit_logs ALTER COLUMN entity TYPE VARCHAR(255);

--rollback ALTER TABLE audit_logs ALTER COLUMN entity TYPE VARCHAR(100);
