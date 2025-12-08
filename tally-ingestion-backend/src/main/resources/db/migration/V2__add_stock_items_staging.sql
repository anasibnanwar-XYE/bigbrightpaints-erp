-- V2: Add staging table for Tally XML Stock Summary import
-- Purpose: Support XML stock summary imports with classification and field editing

-- =========================================
-- STAGING TABLE: Stock Items from XML
-- =========================================

CREATE TABLE stg_tally_stock_items (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES tally_ingestion_runs(id) ON DELETE CASCADE,
    row_number INTEGER NOT NULL,

    -- Tally XML fields
    item_name VARCHAR(500) NOT NULL,
    closing_quantity NUMERIC(19,4),
    unit_of_measure VARCHAR(50),
    closing_rate NUMERIC(19,4),
    closing_amount NUMERIC(19,2),

    -- Classification (user-editable)
    item_type VARCHAR(32), -- 'RAW_MATERIAL', 'FINISHED_PRODUCT', 'PACKAGING', 'ASSET', 'EXPENSE', 'UNKNOWN'
    category VARCHAR(100),
    brand VARCHAR(100),
    size_label VARCHAR(50),
    color VARCHAR(100),

    -- User-editable mapping fields
    mapped_sku VARCHAR(100),
    mapped_product_code VARCHAR(100),
    base_product_name VARCHAR(300),
    gst_rate NUMERIC(5,2),
    hsn_code VARCHAR(20),
    notes TEXT,

    -- Processing fields
    raw_data JSONB,
    source_hash VARCHAR(64),
    validation_status VARCHAR(32) DEFAULT 'PENDING', -- 'PENDING', 'VALID', 'INVALID', 'NEEDS_REVIEW'
    validation_errors JSONB,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMPTZ,

    -- Mapped IDs (after import to ERP)
    mapped_product_id BIGINT,
    mapped_raw_material_id BIGINT,
    mapped_batch_id BIGINT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_stg_stock_items_run ON stg_tally_stock_items(run_id);
CREATE INDEX idx_stg_stock_items_type ON stg_tally_stock_items(item_type);
CREATE INDEX idx_stg_stock_items_validation ON stg_tally_stock_items(validation_status);
CREATE INDEX idx_stg_stock_items_processed ON stg_tally_stock_items(processed);
CREATE INDEX idx_stg_stock_items_name ON stg_tally_stock_items(item_name);

-- Add XML_STOCK_SUMMARY as a valid file type for future reference
COMMENT ON TABLE stg_tally_stock_items IS 'Staging table for Tally XML Stock Summary imports. Supports item classification and field editing before final import.';
