-- Packaging Size Mappings table for auto-deducting bucket/container raw materials during packing
CREATE TABLE packaging_size_mappings (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    company_id BIGINT NOT NULL REFERENCES companies(id),
    packaging_size VARCHAR(50) NOT NULL,
    raw_material_id BIGINT NOT NULL REFERENCES raw_materials(id),
    units_per_pack INT NOT NULL DEFAULT 1,
    carton_size INT,
    liters_per_unit NUMERIC(19,4) NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT uq_packaging_size_company UNIQUE (company_id, packaging_size)
);

CREATE INDEX idx_packaging_size_mappings_company ON packaging_size_mappings(company_id);

-- Add packaging cost tracking to packing records
ALTER TABLE packing_records ADD COLUMN IF NOT EXISTS packaging_cost NUMERIC(19,4) DEFAULT 0;
ALTER TABLE packing_records ADD COLUMN IF NOT EXISTS packaging_material_id BIGINT REFERENCES raw_materials(id);
ALTER TABLE packing_records ADD COLUMN IF NOT EXISTS packaging_quantity NUMERIC(19,4);

-- GST/Non-GST Inventory Type for raw materials
-- STANDARD = GST applicable (default)
-- PRIVATE = Non-GST / Private stock
ALTER TABLE raw_materials ADD COLUMN IF NOT EXISTS inventory_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD';
ALTER TABLE raw_materials ADD COLUMN IF NOT EXISTS gst_rate NUMERIC(5,2) DEFAULT 0;

-- GST/Non-GST Inventory Type for finished goods
ALTER TABLE finished_goods ADD COLUMN IF NOT EXISTS inventory_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD';

-- Separate stock tracking for GST vs Non-GST (for raw materials that need dual tracking)
ALTER TABLE raw_materials ADD COLUMN IF NOT EXISTS private_stock NUMERIC(19,4) NOT NULL DEFAULT 0;

-- Add inventory type to raw material batches for tracking source
ALTER TABLE raw_material_batches ADD COLUMN IF NOT EXISTS inventory_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD';

-- Add inventory type to finished good batches
ALTER TABLE finished_good_batches ADD COLUMN IF NOT EXISTS inventory_type VARCHAR(20) NOT NULL DEFAULT 'STANDARD';

-- Comments for clarity
COMMENT ON COLUMN raw_materials.inventory_type IS 'STANDARD = GST applicable, PRIVATE = Non-GST private stock';
COMMENT ON COLUMN raw_materials.private_stock IS 'Separate stock counter for private/non-GST inventory';
COMMENT ON COLUMN packaging_size_mappings.packaging_size IS 'e.g., 1L, 5L, 10L, 20L';
COMMENT ON COLUMN packaging_size_mappings.liters_per_unit IS 'Liters of paint per packaging unit';
COMMENT ON COLUMN packing_records.packaging_cost IS 'Cost of packaging materials (buckets) consumed';
