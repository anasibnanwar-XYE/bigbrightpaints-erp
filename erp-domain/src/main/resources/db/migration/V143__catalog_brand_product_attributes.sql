ALTER TABLE production_brands
    ADD COLUMN IF NOT EXISTS logo_url VARCHAR(1024);

ALTER TABLE production_brands
    ADD COLUMN IF NOT EXISTS is_active BOOLEAN NOT NULL DEFAULT TRUE;

ALTER TABLE production_products
    ADD COLUMN IF NOT EXISTS hsn_code VARCHAR(32);

CREATE TABLE IF NOT EXISTS production_product_colors (
    product_id BIGINT NOT NULL REFERENCES production_products(id) ON DELETE CASCADE,
    color VARCHAR(128) NOT NULL,
    PRIMARY KEY (product_id, color)
);

CREATE TABLE IF NOT EXISTS production_product_sizes (
    product_id BIGINT NOT NULL REFERENCES production_products(id) ON DELETE CASCADE,
    size_label VARCHAR(64) NOT NULL,
    PRIMARY KEY (product_id, size_label)
);

CREATE TABLE IF NOT EXISTS production_product_carton_sizes (
    product_id BIGINT NOT NULL REFERENCES production_products(id) ON DELETE CASCADE,
    size_label VARCHAR(64) NOT NULL,
    pieces_per_carton INTEGER NOT NULL,
    PRIMARY KEY (product_id, size_label),
    CONSTRAINT chk_product_carton_sizes_positive CHECK (pieces_per_carton > 0)
);

INSERT INTO production_product_colors (product_id, color)
SELECT p.id, p.default_colour
FROM production_products p
WHERE p.default_colour IS NOT NULL
  AND BTRIM(p.default_colour) <> ''
ON CONFLICT DO NOTHING;

INSERT INTO production_product_sizes (product_id, size_label)
SELECT p.id, p.size_label
FROM production_products p
WHERE p.size_label IS NOT NULL
  AND BTRIM(p.size_label) <> ''
ON CONFLICT DO NOTHING;

INSERT INTO production_product_carton_sizes (product_id, size_label, pieces_per_carton)
SELECT p.id, p.size_label, 1
FROM production_products p
WHERE p.size_label IS NOT NULL
  AND BTRIM(p.size_label) <> ''
ON CONFLICT DO NOTHING;

CREATE INDEX IF NOT EXISTS idx_production_products_company_brand_active
    ON production_products(company_id, brand_id, is_active);

CREATE INDEX IF NOT EXISTS idx_production_product_colors_lower
    ON production_product_colors(LOWER(color));

CREATE INDEX IF NOT EXISTS idx_production_product_sizes_lower
    ON production_product_sizes(LOWER(size_label));
