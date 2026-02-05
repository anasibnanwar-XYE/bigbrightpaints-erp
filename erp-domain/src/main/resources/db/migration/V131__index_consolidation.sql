-- CODE-RED convergence: remove redundant performance indexes (forward-only)

DO $$
BEGIN
    -- finished_goods current_stock duplicate index consolidation
    IF to_regclass('idx_finished_goods_current_stock') IS NULL THEN
        EXECUTE 'CREATE INDEX idx_finished_goods_current_stock ON finished_goods(current_stock)';
    END IF;
    IF to_regclass('idx_finished_goods_stock') IS NOT NULL THEN
        EXECUTE 'DROP INDEX idx_finished_goods_stock';
    END IF;

    -- finished_good_batches quantity_available duplicate index consolidation
    IF to_regclass('idx_finished_good_batches_qty_available') IS NULL THEN
        EXECUTE 'CREATE INDEX idx_finished_good_batches_qty_available ON finished_good_batches(quantity_available)';
    END IF;
    IF to_regclass('idx_finished_good_batches_quantity') IS NOT NULL THEN
        EXECUTE 'DROP INDEX idx_finished_good_batches_quantity';
    END IF;
END $$;
