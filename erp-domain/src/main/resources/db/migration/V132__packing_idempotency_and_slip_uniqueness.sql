-- CODE-RED: packing retries must be idempotent, and active packaging slips must be unique per order role.

CREATE TABLE packing_request_records (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(128) NOT NULL,
    idempotency_hash VARCHAR(64),
    production_log_id BIGINT NOT NULL REFERENCES production_logs(id) ON DELETE CASCADE,
    packing_record_id BIGINT REFERENCES packing_records(id) ON DELETE SET NULL,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    UNIQUE(company_id, idempotency_key)
);

CREATE INDEX idx_packing_request_records_company_log
    ON packing_request_records(company_id, production_log_id);

ALTER TABLE packaging_slips
    ADD COLUMN is_backorder BOOLEAN;

UPDATE packaging_slips
SET is_backorder = CASE
    WHEN UPPER(status) = 'BACKORDER' THEN TRUE
    ELSE FALSE
END
WHERE is_backorder IS NULL;

ALTER TABLE packaging_slips
    ALTER COLUMN is_backorder SET DEFAULT FALSE;

ALTER TABLE packaging_slips
    ALTER COLUMN is_backorder SET NOT NULL;

-- One active primary slip per order.
CREATE UNIQUE INDEX uq_packaging_slips_order_primary_active
    ON packaging_slips(company_id, sales_order_id)
    WHERE is_backorder = FALSE
      AND UPPER(status) IN ('PENDING', 'RESERVED', 'PENDING_PRODUCTION', 'PENDING_STOCK');

-- One active backorder slip per order.
CREATE UNIQUE INDEX uq_packaging_slips_order_backorder_active
    ON packaging_slips(company_id, sales_order_id)
    WHERE is_backorder = TRUE
      AND UPPER(status) = 'BACKORDER';
