ALTER TABLE inventory_adjustments
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128),
    ADD COLUMN IF NOT EXISTS idempotency_hash VARCHAR(64);

CREATE UNIQUE INDEX IF NOT EXISTS uq_inventory_adjustments_idempotency
    ON inventory_adjustments(company_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
