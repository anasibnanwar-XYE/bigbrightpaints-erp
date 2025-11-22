-- Idempotency for partner settlements
ALTER TABLE partner_settlement_allocations
    ADD COLUMN IF NOT EXISTS idempotency_key VARCHAR(128);

CREATE UNIQUE INDEX IF NOT EXISTS idx_partner_settlement_idempotency
    ON partner_settlement_allocations(company_id, idempotency_key)
    WHERE idempotency_key IS NOT NULL;
