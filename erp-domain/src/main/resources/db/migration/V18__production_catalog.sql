CREATE TABLE IF NOT EXISTS production_brands (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    name VARCHAR(255) NOT NULL,
    code VARCHAR(64) NOT NULL,
    description TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_production_brand_code UNIQUE (company_id, code),
    CONSTRAINT uq_production_brand_name UNIQUE (company_id, name)
);

CREATE TABLE IF NOT EXISTS production_products (
    id BIGSERIAL PRIMARY KEY,
    public_id UUID NOT NULL DEFAULT gen_random_uuid(),
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    brand_id BIGINT NOT NULL REFERENCES production_brands(id) ON DELETE CASCADE,
    product_name VARCHAR(255) NOT NULL,
    category VARCHAR(64) NOT NULL,
    default_colour VARCHAR(128),
    size_label VARCHAR(64),
    unit_of_measure VARCHAR(64),
    sku_code VARCHAR(128) NOT NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE,
    metadata JSONB,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    updated_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    CONSTRAINT uq_production_product_sku UNIQUE (company_id, sku_code),
    CONSTRAINT uq_production_product_name UNIQUE (brand_id, product_name)
);

CREATE INDEX IF NOT EXISTS idx_production_brands_company ON production_brands(company_id);
CREATE INDEX IF NOT EXISTS idx_production_products_company ON production_products(company_id);
CREATE INDEX IF NOT EXISTS idx_production_products_brand ON production_products(brand_id);
