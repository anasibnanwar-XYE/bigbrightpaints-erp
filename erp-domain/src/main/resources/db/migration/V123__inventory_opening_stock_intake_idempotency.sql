-- Opening stock import + manual raw material intake idempotency tracking.

CREATE TABLE IF NOT EXISTS opening_stock_imports (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(128) NOT NULL,
    idempotency_hash VARCHAR(64),
    reference_number VARCHAR(128),
    file_hash VARCHAR(64),
    file_name VARCHAR(256),
    journal_entry_id BIGINT REFERENCES journal_entries(id) ON DELETE SET NULL,
    rows_processed INTEGER NOT NULL DEFAULT 0,
    raw_materials_created INTEGER NOT NULL DEFAULT 0,
    raw_material_batches_created INTEGER NOT NULL DEFAULT 0,
    finished_goods_created INTEGER NOT NULL DEFAULT 0,
    finished_good_batches_created INTEGER NOT NULL DEFAULT 0,
    errors_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(company_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_opening_stock_imports_company
    ON opening_stock_imports(company_id);

CREATE INDEX IF NOT EXISTS idx_opening_stock_imports_reference
    ON opening_stock_imports(company_id, reference_number);

CREATE TABLE IF NOT EXISTS raw_material_intake_requests (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(128) NOT NULL,
    idempotency_hash VARCHAR(64),
    raw_material_id BIGINT REFERENCES raw_materials(id) ON DELETE SET NULL,
    raw_material_batch_id BIGINT REFERENCES raw_material_batches(id) ON DELETE SET NULL,
    raw_material_movement_id BIGINT REFERENCES raw_material_movements(id) ON DELETE SET NULL,
    journal_entry_id BIGINT REFERENCES journal_entries(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(company_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_raw_material_intake_requests_company
    ON raw_material_intake_requests(company_id);
