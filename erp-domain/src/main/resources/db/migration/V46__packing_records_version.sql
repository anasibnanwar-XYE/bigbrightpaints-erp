-- V46: Ensure packing_records has version column for optimistic locking

ALTER TABLE packing_records
    ADD COLUMN IF NOT EXISTS version BIGINT NOT NULL DEFAULT 0;
