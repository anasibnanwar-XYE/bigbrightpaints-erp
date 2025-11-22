-- V45: Align production log tables with domain model fields

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'production_logs'
          AND column_name = 'produced_quantity'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'production_logs'
          AND column_name = 'mixed_quantity'
    ) THEN
        ALTER TABLE production_logs
            RENAME COLUMN produced_quantity TO mixed_quantity;
    END IF;
END $$;

ALTER TABLE production_logs
    ADD COLUMN IF NOT EXISTS status VARCHAR(32) NOT NULL DEFAULT 'MIXED',
    ADD COLUMN IF NOT EXISTS total_packed_quantity NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS wastage_quantity NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS material_cost_total NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS labor_cost_total NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS overhead_cost_total NUMERIC(18,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS unit_cost NUMERIC(18,4) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS sales_order_id BIGINT,
    ADD COLUMN IF NOT EXISTS sales_order_number VARCHAR(128);

UPDATE production_logs
   SET status = COALESCE(status, 'MIXED'),
       total_packed_quantity = COALESCE(total_packed_quantity, 0),
       wastage_quantity = COALESCE(wastage_quantity, 0),
       material_cost_total = COALESCE(material_cost_total, 0),
       labor_cost_total = COALESCE(labor_cost_total, 0),
       overhead_cost_total = COALESCE(overhead_cost_total, 0),
       unit_cost = COALESCE(unit_cost, 0);

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_name = 'production_logs'
          AND column_name = 'produced_quantity'
    ) THEN
        ALTER TABLE production_logs
            DROP COLUMN produced_quantity;
    END IF;
END $$;

ALTER TABLE production_log_materials
    ADD COLUMN IF NOT EXISTS total_cost NUMERIC(18,4) NOT NULL DEFAULT 0;

UPDATE production_log_materials
   SET total_cost = COALESCE(total_cost, 0);
