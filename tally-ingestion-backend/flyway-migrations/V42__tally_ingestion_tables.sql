-- V42: Tally CSV Ingestion Infrastructure
-- Purpose: Support one-click CSV import from Tally exports with deterministic ID generation,
-- validation, reconciliation, and audit logging

-- =========================================
-- 1. INGESTION RUN TRACKING
-- =========================================

CREATE TABLE tally_ingestion_runs (
    id BIGSERIAL PRIMARY KEY,
    run_id UUID NOT NULL DEFAULT gen_random_uuid(),
    company_id BIGINT NOT NULL REFERENCES companies(id),
    run_type VARCHAR(32) NOT NULL, -- 'FULL_IMPORT', 'INCREMENTAL', 'RECONCILIATION'
    status VARCHAR(32) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'RUNNING', 'COMPLETED', 'FAILED', 'PARTIAL'
    dry_run BOOLEAN NOT NULL DEFAULT FALSE,

    -- Statistics
    total_files INTEGER DEFAULT 0,
    files_processed INTEGER DEFAULT 0,
    total_rows INTEGER DEFAULT 0,
    rows_processed INTEGER DEFAULT 0,
    rows_succeeded INTEGER DEFAULT 0,
    rows_failed INTEGER DEFAULT 0,
    rows_skipped INTEGER DEFAULT 0,

    -- Timing
    started_at TIMESTAMPTZ,
    completed_at TIMESTAMPTZ,

    -- Metadata
    initiated_by BIGINT REFERENCES app_users(id),
    source_system VARCHAR(32) DEFAULT 'TALLY',
    source_version VARCHAR(32),
    configuration JSONB, -- Run configuration (mappings, overrides)
    error_summary TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT idx_unique_run_id UNIQUE(run_id)
);

CREATE INDEX idx_ingestion_runs_company ON tally_ingestion_runs(company_id);
CREATE INDEX idx_ingestion_runs_status ON tally_ingestion_runs(status);

-- =========================================
-- 2. FILE UPLOAD TRACKING
-- =========================================

CREATE TABLE tally_ingestion_files (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES tally_ingestion_runs(id) ON DELETE CASCADE,
    file_name VARCHAR(255) NOT NULL,
    file_type VARCHAR(32) NOT NULL, -- 'ACCOUNTS', 'DEALERS', 'SUPPLIERS', 'PRODUCTS', 'INVENTORY', 'PRICING'
    file_size_bytes BIGINT,
    file_hash VARCHAR(64), -- SHA-256 for deduplication

    status VARCHAR(32) NOT NULL DEFAULT 'PENDING', -- 'PENDING', 'PROCESSING', 'COMPLETED', 'FAILED'
    total_rows INTEGER DEFAULT 0,
    processed_rows INTEGER DEFAULT 0,
    failed_rows INTEGER DEFAULT 0,

    s3_bucket VARCHAR(255),
    s3_key VARCHAR(500),

    error_message TEXT,
    processed_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_ingestion_files_run ON tally_ingestion_files(run_id);
CREATE INDEX idx_ingestion_files_hash ON tally_ingestion_files(file_hash);

-- =========================================
-- 3. STAGING TABLES (Raw CSV Data)
-- =========================================

-- Staging: Accounts/Ledgers
CREATE TABLE stg_tally_accounts (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES tally_ingestion_runs(id) ON DELETE CASCADE,
    row_number INTEGER NOT NULL,

    -- Tally fields
    ledger_name VARCHAR(500),
    ledger_group VARCHAR(255),
    opening_balance NUMERIC(18,2),
    dr_cr VARCHAR(10), -- 'Dr' or 'Cr'
    address TEXT,
    city VARCHAR(255),
    state VARCHAR(255),
    pincode VARCHAR(20),
    pan VARCHAR(20),
    gstin VARCHAR(20),
    bank_name VARCHAR(255),
    account_number VARCHAR(100),
    ifsc_code VARCHAR(20),

    -- Processing fields
    raw_data JSONB, -- Original CSV row as JSON
    source_hash VARCHAR(64), -- Hash for idempotency
    validation_status VARCHAR(32) DEFAULT 'PENDING', -- 'PENDING', 'VALID', 'INVALID'
    validation_errors JSONB,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMPTZ,

    -- Mapped IDs (after processing)
    mapped_account_id BIGINT,
    mapped_dealer_id BIGINT,
    mapped_supplier_id BIGINT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stg_accounts_run ON stg_tally_accounts(run_id);
CREATE INDEX idx_stg_accounts_hash ON stg_tally_accounts(source_hash);
CREATE INDEX idx_stg_accounts_processed ON stg_tally_accounts(processed);

-- Staging: Products/Stock Items
CREATE TABLE stg_tally_products (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES tally_ingestion_runs(id) ON DELETE CASCADE,
    row_number INTEGER NOT NULL,

    -- Tally fields
    stock_item_name VARCHAR(500),
    stock_group VARCHAR(255),
    stock_category VARCHAR(255),
    base_unit VARCHAR(50),
    alternate_unit VARCHAR(50),
    conversion_factor NUMERIC(10,4),
    gst_rate NUMERIC(5,2),
    hsn_code VARCHAR(20),
    item_code VARCHAR(100),
    barcode VARCHAR(100),
    brand VARCHAR(255),

    -- Variant attributes (parsed from name)
    base_product_name VARCHAR(255),
    color VARCHAR(100),
    size VARCHAR(100),
    pack_size VARCHAR(100),

    -- Processing fields
    raw_data JSONB,
    source_hash VARCHAR(64),
    validation_status VARCHAR(32) DEFAULT 'PENDING',
    validation_errors JSONB,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMPTZ,

    -- Mapped IDs
    mapped_brand_id BIGINT,
    mapped_product_id BIGINT,
    mapped_variant_id BIGINT,
    generated_sku VARCHAR(100),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stg_products_run ON stg_tally_products(run_id);
CREATE INDEX idx_stg_products_hash ON stg_tally_products(source_hash);
CREATE INDEX idx_stg_products_processed ON stg_tally_products(processed);

-- Staging: Dealers (from Sundry Debtors)
CREATE TABLE stg_tally_dealers (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES tally_ingestion_runs(id) ON DELETE CASCADE,
    row_number INTEGER NOT NULL,

    -- Tally fields
    party_name VARCHAR(500),
    ledger_name VARCHAR(500),
    address TEXT,
    city VARCHAR(255),
    state VARCHAR(255),
    pincode VARCHAR(20),
    country VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(50),
    mobile VARCHAR(50),
    pan VARCHAR(20),
    gstin VARCHAR(20),
    credit_period_days INTEGER,
    credit_limit NUMERIC(18,2),
    opening_balance NUMERIC(18,2),
    dr_cr VARCHAR(10),

    -- Processing fields
    raw_data JSONB,
    source_hash VARCHAR(64),
    validation_status VARCHAR(32) DEFAULT 'PENDING',
    validation_errors JSONB,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMPTZ,

    -- Mapped IDs
    mapped_dealer_id BIGINT,
    generated_dealer_code VARCHAR(64),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stg_dealers_run ON stg_tally_dealers(run_id);
CREATE INDEX idx_stg_dealers_hash ON stg_tally_dealers(source_hash);
CREATE INDEX idx_stg_dealers_gstin ON stg_tally_dealers(gstin);

-- Staging: Suppliers (from Sundry Creditors)
CREATE TABLE stg_tally_suppliers (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES tally_ingestion_runs(id) ON DELETE CASCADE,
    row_number INTEGER NOT NULL,

    -- Tally fields (similar to dealers)
    party_name VARCHAR(500),
    ledger_name VARCHAR(500),
    address TEXT,
    city VARCHAR(255),
    state VARCHAR(255),
    pincode VARCHAR(20),
    country VARCHAR(100),
    email VARCHAR(255),
    phone VARCHAR(50),
    mobile VARCHAR(50),
    pan VARCHAR(20),
    gstin VARCHAR(20),
    payment_terms_days INTEGER,
    credit_limit NUMERIC(18,2),
    opening_balance NUMERIC(18,2),
    dr_cr VARCHAR(10),

    -- Processing fields
    raw_data JSONB,
    source_hash VARCHAR(64),
    validation_status VARCHAR(32) DEFAULT 'PENDING',
    validation_errors JSONB,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMPTZ,

    -- Mapped IDs
    mapped_supplier_id BIGINT,
    generated_supplier_code VARCHAR(64),

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stg_suppliers_run ON stg_tally_suppliers(run_id);
CREATE INDEX idx_stg_suppliers_hash ON stg_tally_suppliers(source_hash);
CREATE INDEX idx_stg_suppliers_gstin ON stg_tally_suppliers(gstin);

-- Staging: Inventory
CREATE TABLE stg_tally_inventory (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES tally_ingestion_runs(id) ON DELETE CASCADE,
    row_number INTEGER NOT NULL,

    -- Tally fields
    stock_item_name VARCHAR(500),
    godown_name VARCHAR(255),
    batch_name VARCHAR(255),
    quantity NUMERIC(18,4),
    rate NUMERIC(18,4),
    value NUMERIC(18,2),
    manufacturing_date DATE,
    expiry_date DATE,

    -- Processing fields
    raw_data JSONB,
    source_hash VARCHAR(64),
    validation_status VARCHAR(32) DEFAULT 'PENDING',
    validation_errors JSONB,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMPTZ,

    -- Mapped IDs
    mapped_variant_id BIGINT,
    mapped_location_id BIGINT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stg_inventory_run ON stg_tally_inventory(run_id);
CREATE INDEX idx_stg_inventory_processed ON stg_tally_inventory(processed);

-- Staging: Pricing
CREATE TABLE stg_tally_pricing (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES tally_ingestion_runs(id) ON DELETE CASCADE,
    row_number INTEGER NOT NULL,

    -- Tally fields
    stock_item_name VARCHAR(500),
    price_level VARCHAR(100),
    price_date DATE,
    rate NUMERIC(18,4),
    discount_percent NUMERIC(5,2),

    -- Processing fields
    raw_data JSONB,
    source_hash VARCHAR(64),
    validation_status VARCHAR(32) DEFAULT 'PENDING',
    validation_errors JSONB,
    processed BOOLEAN DEFAULT FALSE,
    processed_at TIMESTAMPTZ,

    -- Mapped IDs
    mapped_variant_id BIGINT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_stg_pricing_run ON stg_tally_pricing(run_id);

-- =========================================
-- 4. MAPPING CONFIGURATION TABLES
-- =========================================

-- Brand mapping/normalization
CREATE TABLE tally_brand_mappings (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    tally_brand_name VARCHAR(255) NOT NULL,
    canonical_brand_name VARCHAR(255) NOT NULL,
    brand_code VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_by BIGINT REFERENCES app_users(id),
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT idx_unique_brand_mapping UNIQUE(company_id, tally_brand_name)
);

-- Category mapping
CREATE TABLE tally_category_mappings (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    tally_category VARCHAR(255) NOT NULL,
    internal_category VARCHAR(255) NOT NULL,
    parent_category VARCHAR(255),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT idx_unique_category_mapping UNIQUE(company_id, tally_category)
);

-- UOM (Unit of Measure) mapping
CREATE TABLE tally_uom_mappings (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    tally_uom VARCHAR(100) NOT NULL,
    internal_uom VARCHAR(50) NOT NULL,
    conversion_factor NUMERIC(10,6) DEFAULT 1.0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT idx_unique_uom_mapping UNIQUE(company_id, tally_uom)
);

-- Ledger to Account Type mapping
CREATE TABLE tally_ledger_mappings (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    tally_ledger_group VARCHAR(255) NOT NULL,
    account_type VARCHAR(50) NOT NULL, -- 'ASSET', 'LIABILITY', 'REVENUE', 'EXPENSE', etc.
    default_account_code VARCHAR(50),
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT idx_unique_ledger_mapping UNIQUE(company_id, tally_ledger_group)
);

-- Tax slab mapping
CREATE TABLE tally_tax_mappings (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    tally_gst_rate NUMERIC(5,2) NOT NULL,
    internal_tax_slab VARCHAR(50) NOT NULL,
    cgst_rate NUMERIC(5,2),
    sgst_rate NUMERIC(5,2),
    igst_rate NUMERIC(5,2),
    cess_rate NUMERIC(5,2) DEFAULT 0,
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT idx_unique_tax_mapping UNIQUE(company_id, tally_gst_rate)
);

-- =========================================
-- 5. ID & SKU GENERATION REGISTRY
-- =========================================

CREATE TABLE tally_id_registry (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    entity_type VARCHAR(50) NOT NULL, -- 'DEALER', 'SUPPLIER', 'PRODUCT', 'VARIANT', 'ACCOUNT'

    -- Source identifiers (for matching)
    source_key VARCHAR(500) NOT NULL, -- Hash input: e.g., "ledger_name+gstin"
    source_hash VARCHAR(64) NOT NULL, -- SHA-256 of source_key

    -- Generated identifiers
    generated_id VARCHAR(100) NOT NULL, -- The deterministic ID
    generated_code VARCHAR(100), -- Human-readable code (dealer_code, supplier_code)
    generated_sku VARCHAR(100), -- For product variants

    -- Mapping to actual entities
    mapped_entity_id BIGINT, -- FK to actual table (dealers.id, suppliers.id, etc.)

    -- Metadata
    first_seen_run_id BIGINT REFERENCES tally_ingestion_runs(id),
    last_seen_run_id BIGINT REFERENCES tally_ingestion_runs(id),
    occurrence_count INTEGER DEFAULT 1,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT idx_unique_id_registry UNIQUE(company_id, entity_type, source_hash)
);

CREATE INDEX idx_id_registry_company ON tally_id_registry(company_id);
CREATE INDEX idx_id_registry_type ON tally_id_registry(entity_type);
CREATE INDEX idx_id_registry_hash ON tally_id_registry(source_hash);
CREATE INDEX idx_id_registry_generated ON tally_id_registry(generated_id);

-- SKU collision prevention
CREATE TABLE tally_sku_registry (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id),
    sku VARCHAR(100) NOT NULL,
    variant_id BIGINT,
    brand VARCHAR(255),
    base_product VARCHAR(255),
    color VARCHAR(100),
    size VARCHAR(100),
    pack VARCHAR(100),
    counter_suffix INTEGER DEFAULT 0, -- For collision resolution
    is_active BOOLEAN DEFAULT TRUE,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),

    CONSTRAINT idx_unique_sku UNIQUE(company_id, sku)
);

CREATE INDEX idx_sku_registry_variant ON tally_sku_registry(variant_id);

-- =========================================
-- 6. RECONCILIATION & MATCHING
-- =========================================

CREATE TABLE tally_reconciliation_matches (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES tally_ingestion_runs(id) ON DELETE CASCADE,
    entity_type VARCHAR(50) NOT NULL,

    -- Source (from staging)
    staging_table VARCHAR(100) NOT NULL,
    staging_id BIGINT NOT NULL,
    source_identifier VARCHAR(500), -- e.g., "party_name + gstin"

    -- Target (existing entity)
    target_table VARCHAR(100) NOT NULL,
    target_id BIGINT NOT NULL,
    target_identifier VARCHAR(500),

    -- Match details
    match_type VARCHAR(50) NOT NULL, -- 'EXACT', 'FUZZY', 'PARTIAL', 'MANUAL'
    match_score NUMERIC(5,2), -- 0-100 confidence score
    match_criteria JSONB, -- What fields matched

    -- Resolution
    resolution_status VARCHAR(32) DEFAULT 'PENDING', -- 'PENDING', 'APPROVED', 'REJECTED', 'MERGED'
    resolution_action VARCHAR(32), -- 'UPDATE', 'SKIP', 'CREATE_NEW', 'MERGE'
    resolved_by BIGINT REFERENCES app_users(id),
    resolved_at TIMESTAMPTZ,
    resolution_notes TEXT,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_reconciliation_run ON tally_reconciliation_matches(run_id);
CREATE INDEX idx_reconciliation_status ON tally_reconciliation_matches(resolution_status);

-- =========================================
-- 7. ERROR TRACKING
-- =========================================

CREATE TABLE tally_ingestion_errors (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES tally_ingestion_runs(id) ON DELETE CASCADE,
    file_id BIGINT REFERENCES tally_ingestion_files(id) ON DELETE CASCADE,

    -- Error location
    staging_table VARCHAR(100),
    staging_row_id BIGINT,
    row_number INTEGER,

    -- Error details
    error_type VARCHAR(50) NOT NULL, -- 'VALIDATION', 'MAPPING', 'DUPLICATE', 'REFERENCE', 'TRANSFORMATION'
    error_code VARCHAR(50),
    error_message TEXT NOT NULL,
    error_details JSONB,

    -- Original data
    original_data JSONB,

    -- Resolution
    is_resolved BOOLEAN DEFAULT FALSE,
    resolution_notes TEXT,
    resolved_by BIGINT REFERENCES app_users(id),
    resolved_at TIMESTAMPTZ,

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_errors_run ON tally_ingestion_errors(run_id);
CREATE INDEX idx_errors_type ON tally_ingestion_errors(error_type);
CREATE INDEX idx_errors_resolved ON tally_ingestion_errors(is_resolved);

-- =========================================
-- 8. AUDIT LOG
-- =========================================

CREATE TABLE tally_ingestion_audit (
    id BIGSERIAL PRIMARY KEY,
    run_id BIGINT NOT NULL REFERENCES tally_ingestion_runs(id) ON DELETE CASCADE,

    -- What was done
    action VARCHAR(100) NOT NULL, -- 'CREATED', 'UPDATED', 'SKIPPED', 'MATCHED', 'FAILED'
    entity_type VARCHAR(50) NOT NULL,
    entity_id BIGINT,
    entity_identifier VARCHAR(500),

    -- Source
    source_table VARCHAR(100),
    source_id BIGINT,

    -- Details
    changes JSONB, -- before/after values
    metadata JSONB, -- Additional context

    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE INDEX idx_audit_run ON tally_ingestion_audit(run_id);
CREATE INDEX idx_audit_entity ON tally_ingestion_audit(entity_type, entity_id);
CREATE INDEX idx_audit_action ON tally_ingestion_audit(action);

-- =========================================
-- 9. DEFAULT MAPPINGS
-- =========================================

-- Insert default Tally ledger group to account type mappings
INSERT INTO tally_ledger_mappings (company_id, tally_ledger_group, account_type, default_account_code, is_active)
SELECT
    c.id,
    mapping.ledger_group,
    mapping.account_type,
    mapping.default_code,
    true
FROM companies c
CROSS JOIN (VALUES
    ('Sundry Debtors', 'ASSET', 'AR'),
    ('Sundry Creditors', 'LIABILITY', 'AP'),
    ('Stock-in-Hand', 'ASSET', 'INV'),
    ('Sales Accounts', 'REVENUE', 'REV'),
    ('Purchase Accounts', 'EXPENSE', 'PURCH'),
    ('Direct Expenses', 'EXPENSE', 'COGS'),
    ('Indirect Expenses', 'EXPENSE', 'OPEX'),
    ('Direct Incomes', 'REVENUE', 'OTHER_REV'),
    ('Indirect Incomes', 'REVENUE', 'OTHER_REV'),
    ('Duties & Taxes', 'LIABILITY', 'TAX'),
    ('Bank Accounts', 'ASSET', 'BANK'),
    ('Cash-in-Hand', 'ASSET', 'CASH'),
    ('Capital Account', 'EQUITY', 'CAPITAL'),
    ('Loans & Advances', 'ASSET', 'LOAN'),
    ('Fixed Assets', 'ASSET', 'FA'),
    ('Investments', 'ASSET', 'INVEST'),
    ('Reserves & Surplus', 'EQUITY', 'RESERVE')
) AS mapping(ledger_group, account_type, default_code)
WHERE NOT EXISTS (
    SELECT 1 FROM tally_ledger_mappings tlm
    WHERE tlm.company_id = c.id AND tlm.tally_ledger_group = mapping.ledger_group
);

-- Insert default UOM mappings
INSERT INTO tally_uom_mappings (company_id, tally_uom, internal_uom, conversion_factor, is_active)
SELECT
    c.id,
    mapping.tally_uom,
    mapping.internal_uom,
    mapping.factor,
    true
FROM companies c
CROSS JOIN (VALUES
    ('Nos', 'PCS', 1.0),
    ('Pcs', 'PCS', 1.0),
    ('No', 'PCS', 1.0),
    ('Kg', 'KG', 1.0),
    ('Kgs', 'KG', 1.0),
    ('gm', 'KG', 0.001),
    ('Grams', 'KG', 0.001),
    ('Ltr', 'L', 1.0),
    ('Litres', 'L', 1.0),
    ('Lt', 'L', 1.0),
    ('ML', 'L', 0.001),
    ('Mtr', 'M', 1.0),
    ('Meters', 'M', 1.0),
    ('Ft', 'M', 0.3048),
    ('Feet', 'M', 0.3048),
    ('Sq Ft', 'SQM', 0.092903),
    ('Sq Mtr', 'SQM', 1.0),
    ('Box', 'BOX', 1.0),
    ('Boxes', 'BOX', 1.0),
    ('Carton', 'CTN', 1.0),
    ('Dozen', 'PCS', 12.0),
    ('Doz', 'PCS', 12.0),
    ('Pairs', 'PAIR', 1.0),
    ('Set', 'SET', 1.0),
    ('Bags', 'BAG', 1.0),
    ('Rolls', 'ROLL', 1.0),
    ('Bundles', 'BUNDLE', 1.0)
) AS mapping(tally_uom, internal_uom, factor)
WHERE NOT EXISTS (
    SELECT 1 FROM tally_uom_mappings tum
    WHERE tum.company_id = c.id AND tum.tally_uom = mapping.tally_uom
);

-- Insert default tax mappings (Indian GST)
INSERT INTO tally_tax_mappings (company_id, tally_gst_rate, internal_tax_slab, cgst_rate, sgst_rate, igst_rate, is_active)
SELECT
    c.id,
    mapping.gst_rate,
    mapping.tax_slab,
    mapping.cgst,
    mapping.sgst,
    mapping.igst,
    true
FROM companies c
CROSS JOIN (VALUES
    (0.00, 'GST_0', 0.00, 0.00, 0.00),
    (5.00, 'GST_5', 2.50, 2.50, 5.00),
    (12.00, 'GST_12', 6.00, 6.00, 12.00),
    (18.00, 'GST_18', 9.00, 9.00, 18.00),
    (28.00, 'GST_28', 14.00, 14.00, 28.00)
) AS mapping(gst_rate, tax_slab, cgst, sgst, igst)
WHERE NOT EXISTS (
    SELECT 1 FROM tally_tax_mappings ttm
    WHERE ttm.company_id = c.id AND ttm.tally_gst_rate = mapping.gst_rate
);

-- =========================================
-- 10. VIEWS FOR REPORTING
-- =========================================

CREATE VIEW v_tally_ingestion_summary AS
SELECT
    r.id,
    r.run_id,
    c.name AS company_name,
    r.run_type,
    r.status,
    r.dry_run,
    r.total_files,
    r.files_processed,
    r.total_rows,
    r.rows_succeeded,
    r.rows_failed,
    r.rows_skipped,
    CASE
        WHEN r.total_rows > 0 THEN ROUND((r.rows_succeeded::NUMERIC / r.total_rows) * 100, 2)
        ELSE 0
    END AS success_rate,
    r.started_at,
    r.completed_at,
    EXTRACT(EPOCH FROM (r.completed_at - r.started_at)) AS duration_seconds,
    u.display_name AS initiated_by_name
FROM tally_ingestion_runs r
JOIN companies c ON c.id = r.company_id
LEFT JOIN app_users u ON u.id = r.initiated_by
ORDER BY r.created_at DESC;

CREATE VIEW v_tally_error_summary AS
SELECT
    e.run_id,
    e.error_type,
    COUNT(*) AS error_count,
    COUNT(DISTINCT e.staging_table) AS affected_tables,
    MIN(e.created_at) AS first_error_at,
    MAX(e.created_at) AS last_error_at
FROM tally_ingestion_errors e
WHERE e.is_resolved = FALSE
GROUP BY e.run_id, e.error_type;

-- =========================================
-- 11. FUNCTIONS & TRIGGERS
-- =========================================

-- Function to generate deterministic ID from source data
CREATE OR REPLACE FUNCTION generate_deterministic_id(
    p_prefix VARCHAR,
    p_source_data VARCHAR
) RETURNS VARCHAR AS $$
DECLARE
    v_hash VARCHAR;
    v_short_id VARCHAR;
BEGIN
    -- Generate SHA-256 hash of source data
    v_hash := encode(digest(p_source_data, 'sha256'), 'hex');

    -- Take first 12 characters of hash for short ID
    v_short_id := substring(v_hash, 1, 12);

    -- Return prefixed ID
    RETURN p_prefix || '_' || v_short_id;
END;
$$ LANGUAGE plpgsql;

-- Trigger to update updated_at timestamp
CREATE OR REPLACE FUNCTION update_updated_at_column()
RETURNS TRIGGER AS $$
BEGIN
    NEW.updated_at = NOW();
    RETURN NEW;
END;
$$ LANGUAGE plpgsql;

CREATE TRIGGER update_ingestion_runs_updated_at BEFORE UPDATE ON tally_ingestion_runs
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

CREATE TRIGGER update_id_registry_updated_at BEFORE UPDATE ON tally_id_registry
    FOR EACH ROW EXECUTE FUNCTION update_updated_at_column();

-- =========================================
-- PERMISSIONS
-- =========================================

-- Grant appropriate permissions (adjust based on your user/role setup)
-- GRANT SELECT, INSERT, UPDATE ON ALL TABLES IN SCHEMA public TO erp_app_user;
-- GRANT USAGE, SELECT ON ALL SEQUENCES IN SCHEMA public TO erp_app_user;
