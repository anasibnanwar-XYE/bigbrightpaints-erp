ALTER TABLE opening_stock_imports
    ADD COLUMN IF NOT EXISTS results_json TEXT;
