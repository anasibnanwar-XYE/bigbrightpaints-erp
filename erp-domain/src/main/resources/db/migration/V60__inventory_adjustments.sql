-- Migration for inventory adjustments tables
CREATE TABLE IF NOT EXISTS inventory_adjustments (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    reference_number VARCHAR(255) NOT NULL,
    adjustment_date DATE NOT NULL,
    adjustment_type VARCHAR(50) NOT NULL,
    reason TEXT,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    journal_entry_id BIGINT,
    total_amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    created_by VARCHAR(255),
    version BIGINT NOT NULL DEFAULT 0
);

CREATE TABLE IF NOT EXISTS inventory_adjustment_lines (
    id BIGSERIAL PRIMARY KEY,
    adjustment_id BIGINT NOT NULL REFERENCES inventory_adjustments(id) ON DELETE CASCADE,
    finished_good_id BIGINT NOT NULL REFERENCES finished_goods(id),
    quantity DECIMAL(19,4) NOT NULL DEFAULT 0,
    unit_cost DECIMAL(19,4) NOT NULL DEFAULT 0,
    amount DECIMAL(19,4) NOT NULL DEFAULT 0,
    note TEXT,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_inventory_adjustments_company ON inventory_adjustments(company_id);
CREATE INDEX IF NOT EXISTS idx_inventory_adjustments_date ON inventory_adjustments(adjustment_date);
CREATE INDEX IF NOT EXISTS idx_inventory_adjustments_reference ON inventory_adjustments(reference_number);
CREATE INDEX IF NOT EXISTS idx_inventory_adjustment_lines_adjustment ON inventory_adjustment_lines(adjustment_id);
