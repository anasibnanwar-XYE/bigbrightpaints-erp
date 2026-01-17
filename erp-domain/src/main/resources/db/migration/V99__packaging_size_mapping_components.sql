ALTER TABLE packaging_size_mappings
    DROP CONSTRAINT IF EXISTS uq_packaging_size_company;

ALTER TABLE packaging_size_mappings
    ADD CONSTRAINT uq_packaging_size_material UNIQUE (company_id, packaging_size, raw_material_id);

CREATE INDEX IF NOT EXISTS idx_packaging_size_mappings_company_size
    ON packaging_size_mappings(company_id, packaging_size);
