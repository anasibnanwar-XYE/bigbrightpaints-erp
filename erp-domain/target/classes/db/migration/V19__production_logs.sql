CREATE TABLE IF NOT EXISTS production_logs (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    brand_id BIGINT NOT NULL REFERENCES production_brands(id) ON DELETE CASCADE,
    product_id BIGINT NOT NULL REFERENCES production_products(id) ON DELETE CASCADE,
    production_code VARCHAR(64) NOT NULL,
    batch_colour VARCHAR(128),
    batch_size NUMERIC(18,2) NOT NULL,
    unit_of_measure VARCHAR(64) NOT NULL,
    produced_quantity NUMERIC(18,2) NOT NULL,
    produced_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    notes TEXT,
    created_by VARCHAR(255),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_production_log_code UNIQUE (company_id, production_code)
);

CREATE TABLE IF NOT EXISTS production_log_materials (
    id BIGSERIAL PRIMARY KEY,
    log_id BIGINT NOT NULL REFERENCES production_logs(id) ON DELETE CASCADE,
    raw_material_id BIGINT REFERENCES raw_materials(id) ON DELETE SET NULL,
    material_name VARCHAR(255) NOT NULL,
    quantity NUMERIC(18,4) NOT NULL,
    unit_of_measure VARCHAR(64) NOT NULL,
    cost_per_unit NUMERIC(18,4),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX IF NOT EXISTS idx_production_logs_company ON production_logs(company_id, produced_at DESC);
CREATE INDEX IF NOT EXISTS idx_production_log_materials_log ON production_log_materials(log_id);
