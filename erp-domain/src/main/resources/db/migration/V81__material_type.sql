-- Add material_type to distinguish production vs packaging materials
-- PRODUCTION = used in manufacturing (pigments, resins, solvents)
-- PACKAGING = used in packing (buckets, cans, cartons, lids)

ALTER TABLE raw_materials ADD COLUMN IF NOT EXISTS material_type VARCHAR(20) NOT NULL DEFAULT 'PRODUCTION';

-- Index for filtering by type
CREATE INDEX IF NOT EXISTS idx_raw_material_type ON raw_materials(company_id, material_type);

-- For existing packaging-related items, you can update them manually:
-- UPDATE raw_materials SET material_type = 'PACKAGING' WHERE sku LIKE '%BUCKET%' OR sku LIKE '%CAN%' OR name ILIKE '%bucket%';
