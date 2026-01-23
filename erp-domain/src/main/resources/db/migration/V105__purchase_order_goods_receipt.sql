CREATE TABLE IF NOT EXISTS purchase_orders (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    order_number VARCHAR(128) NOT NULL,
    order_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'OPEN',
    memo TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(company_id, order_number)
);

CREATE INDEX IF NOT EXISTS idx_purchase_orders_company ON purchase_orders(company_id);

CREATE TABLE IF NOT EXISTS purchase_order_items (
    id BIGSERIAL PRIMARY KEY,
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE CASCADE,
    raw_material_id BIGINT NOT NULL REFERENCES raw_materials(id) ON DELETE RESTRICT,
    quantity NUMERIC(18,4) NOT NULL,
    unit VARCHAR(64) NOT NULL,
    cost_per_unit NUMERIC(18,4) NOT NULL,
    line_total NUMERIC(18,4) NOT NULL,
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_purchase_order_items_order ON purchase_order_items(purchase_order_id);

CREATE TABLE IF NOT EXISTS goods_receipts (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    supplier_id BIGINT NOT NULL REFERENCES suppliers(id) ON DELETE RESTRICT,
    purchase_order_id BIGINT NOT NULL REFERENCES purchase_orders(id) ON DELETE RESTRICT,
    receipt_number VARCHAR(128) NOT NULL,
    receipt_date DATE NOT NULL,
    status VARCHAR(32) NOT NULL DEFAULT 'RECEIVED',
    memo TEXT,
    version BIGINT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    UNIQUE(company_id, receipt_number)
);

CREATE INDEX IF NOT EXISTS idx_goods_receipts_company ON goods_receipts(company_id);
CREATE INDEX IF NOT EXISTS idx_goods_receipts_purchase_order ON goods_receipts(purchase_order_id);

CREATE TABLE IF NOT EXISTS goods_receipt_items (
    id BIGSERIAL PRIMARY KEY,
    goods_receipt_id BIGINT NOT NULL REFERENCES goods_receipts(id) ON DELETE CASCADE,
    raw_material_id BIGINT NOT NULL REFERENCES raw_materials(id) ON DELETE RESTRICT,
    raw_material_batch_id BIGINT REFERENCES raw_material_batches(id) ON DELETE SET NULL,
    batch_code VARCHAR(128) NOT NULL,
    quantity NUMERIC(18,4) NOT NULL,
    unit VARCHAR(64) NOT NULL,
    cost_per_unit NUMERIC(18,4) NOT NULL,
    line_total NUMERIC(18,4) NOT NULL,
    notes TEXT,
    version BIGINT NOT NULL DEFAULT 0
);

CREATE INDEX IF NOT EXISTS idx_goods_receipt_items_receipt ON goods_receipt_items(goods_receipt_id);

ALTER TABLE raw_material_purchases
    ADD COLUMN IF NOT EXISTS purchase_order_id BIGINT REFERENCES purchase_orders(id) ON DELETE SET NULL,
    ADD COLUMN IF NOT EXISTS goods_receipt_id BIGINT REFERENCES goods_receipts(id) ON DELETE SET NULL;

CREATE INDEX IF NOT EXISTS idx_raw_material_purchases_purchase_order ON raw_material_purchases(purchase_order_id);
CREATE INDEX IF NOT EXISTS idx_raw_material_purchases_goods_receipt ON raw_material_purchases(goods_receipt_id);
CREATE UNIQUE INDEX IF NOT EXISTS uq_raw_material_purchases_goods_receipt ON raw_material_purchases(goods_receipt_id);
