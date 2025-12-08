-- Bulk-to-Size Packaging Support
-- Allows creating child batches (sized SKUs) from parent bulk batches

-- Add parent batch reference for traceability
ALTER TABLE finished_good_batches ADD COLUMN IF NOT EXISTS parent_batch_id BIGINT REFERENCES finished_good_batches(id);

-- Flag to identify bulk batches (no size, intermediate state)
ALTER TABLE finished_good_batches ADD COLUMN IF NOT EXISTS is_bulk BOOLEAN DEFAULT false;

-- Size label for child batches (e.g., "1L", "4L", "20L")
ALTER TABLE finished_good_batches ADD COLUMN IF NOT EXISTS size_label VARCHAR(50);

-- Index for efficient parent-child lookups
CREATE INDEX IF NOT EXISTS idx_fg_batch_parent ON finished_good_batches(parent_batch_id);
CREATE INDEX IF NOT EXISTS idx_fg_batch_bulk ON finished_good_batches(is_bulk) WHERE is_bulk = true;
