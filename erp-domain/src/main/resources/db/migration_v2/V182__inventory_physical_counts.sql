CREATE TABLE IF NOT EXISTS inventory_physical_counts (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    count_target VARCHAR(32) NOT NULL,
    inventory_item_id BIGINT NOT NULL,
    physical_quantity NUMERIC(19, 4) NOT NULL,
    count_date DATE NOT NULL,
    source_reference VARCHAR(255),
    note TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    created_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT chk_inventory_physical_count_target
        CHECK (count_target IN ('RAW_MATERIAL', 'FINISHED_GOOD'))
);

CREATE INDEX IF NOT EXISTS idx_inventory_physical_counts_company_target_item_date
    ON inventory_physical_counts (
        company_id,
        count_target,
        inventory_item_id,
        count_date DESC,
        created_at DESC,
        id DESC
    );
