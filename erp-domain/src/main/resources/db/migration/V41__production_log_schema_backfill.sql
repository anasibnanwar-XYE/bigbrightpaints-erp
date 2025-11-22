-- Align production_logs schema with current domain model (cost totals, packing progress, sales linkage)
ALTER TABLE production_logs
    ADD COLUMN IF NOT EXISTS mixed_quantity NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'MIXED',
    ADD COLUMN IF NOT EXISTS total_packed_quantity NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS wastage_quantity NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS material_cost_total NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS labor_cost_total NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS overhead_cost_total NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS unit_cost NUMERIC(18,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS sales_order_id BIGINT,
    ADD COLUMN IF NOT EXISTS sales_order_number VARCHAR(64);

-- Backfill mixed_quantity for existing rows (test DB starts empty, but keep for safety)
UPDATE production_logs
SET mixed_quantity = COALESCE(mixed_quantity, 0) + COALESCE(produced_quantity, 0)
WHERE mixed_quantity = 0;

-- Packing records table used by PackingService
CREATE TABLE IF NOT EXISTS packing_records (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    production_log_id BIGINT NOT NULL REFERENCES production_logs(id) ON DELETE CASCADE,
    finished_good_id BIGINT NOT NULL REFERENCES finished_goods(id) ON DELETE CASCADE,
    finished_good_batch_id BIGINT REFERENCES finished_good_batches(id) ON DELETE SET NULL,
    packaging_size VARCHAR(128) NOT NULL,
    quantity_packed NUMERIC(18,2) NOT NULL DEFAULT 0,
    pieces_count INT,
    boxes_count INT,
    pieces_per_box INT,
    packed_date DATE NOT NULL DEFAULT CURRENT_DATE,
    packed_by VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_packing_records_company ON packing_records(company_id, packed_date DESC);
CREATE INDEX IF NOT EXISTS idx_packing_records_log ON packing_records(production_log_id);
