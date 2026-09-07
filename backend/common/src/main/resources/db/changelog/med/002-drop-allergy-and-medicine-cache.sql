--changeset split-med:17
--comment: Drop allergy_cache and medicine_catalog_cache (#258). The allergy
--flow is removed entirely (MIS does not expose a usable endpoint in this
--environment — owner signed off the removal). The medicine catalog cache
--is removed because the catalog is now read live from MIS
--(spiMedicineItemKindDetails) on every request — no denormalisation
--is needed and a stale cache would defeat the live read.

DROP INDEX IF EXISTS idx_allergy_cache_patient;
DROP INDEX IF EXISTS idx_medicine_catalog_name;
DROP TABLE IF EXISTS allergy_cache;
DROP TABLE IF EXISTS medicine_catalog_cache;

--rollback CREATE TABLE IF NOT EXISTS medicine_catalog_cache (
--rollback     id BIGINT NOT NULL,
--rollback     name VARCHAR(500) NOT NULL,
--rollback     category_ref INTEGER,
--rollback     ptg_code VARCHAR(50),
--rollback     is_high_risk BOOLEAN DEFAULT FALSE,
--rollback     cached_at TIMESTAMP NOT NULL DEFAULT NOW(),
--rollback     CONSTRAINT pk_medicine_catalog_cache PRIMARY KEY (id)
--rollback );
--rollback CREATE TABLE IF NOT EXISTS allergy_cache (
--rollback     id UUID NOT NULL,
--rollback     patient_id BIGINT NOT NULL,
--rollback     allergen_name VARCHAR(500) NOT NULL,
--rollback     source_document_id INTEGER,
--rollback     cached_at TIMESTAMP NOT NULL DEFAULT NOW(),
--rollback     CONSTRAINT pk_allergy_cache PRIMARY KEY (id),
--rollback     CONSTRAINT uq_allergy_patient_name UNIQUE (patient_id, allergen_name)
--rollback );
--rollback CREATE INDEX IF NOT EXISTS idx_medicine_catalog_name ON medicine_catalog_cache(name);
--rollback CREATE INDEX IF NOT EXISTS idx_allergy_cache_patient ON allergy_cache(patient_id);
