-- Prevent negative inventory balances and batch quantities
ALTER TABLE finished_goods
    ADD CONSTRAINT chk_finished_goods_current_stock_non_negative CHECK (current_stock >= 0);

ALTER TABLE finished_goods
    ADD CONSTRAINT chk_finished_goods_reserved_stock_non_negative CHECK (reserved_stock >= 0);

ALTER TABLE finished_good_batches
    ADD CONSTRAINT chk_fg_batch_quantity_total_non_negative CHECK (quantity_total >= 0);

ALTER TABLE finished_good_batches
    ADD CONSTRAINT chk_fg_batch_quantity_available_non_negative CHECK (quantity_available >= 0);

ALTER TABLE raw_materials
    ADD CONSTRAINT chk_raw_material_current_stock_non_negative CHECK (current_stock >= 0);

ALTER TABLE raw_material_batches
    ADD CONSTRAINT chk_raw_material_batch_quantity_non_negative CHECK (quantity >= 0);
