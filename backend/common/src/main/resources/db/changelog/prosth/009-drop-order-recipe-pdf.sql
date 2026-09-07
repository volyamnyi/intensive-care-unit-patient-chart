--changeset split-prosth:24
--comment: Drop legacy order-recipe PDF columns. The order document is owned
--by MIS (see spiDocumentProsthesCheck.documentUrl surfaced via
--GET /api/prosthesis-manufacturing/orders/{id}/document-url). ICU Chart
--no longer generates or caches the recipe PDF locally — MIS Data Policy.
ALTER TABLE prosthetics_orders DROP COLUMN IF EXISTS recipe_pdf_data;
ALTER TABLE prosthetics_orders DROP COLUMN IF EXISTS recipe_pdf_generated_at;

--rollback ALTER TABLE prosthetics_orders ADD COLUMN recipe_pdf_data bytea;
--rollback ALTER TABLE prosthetics_orders ADD COLUMN recipe_pdf_generated_at timestamp;
