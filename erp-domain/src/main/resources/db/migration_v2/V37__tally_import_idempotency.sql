CREATE TABLE IF NOT EXISTS tally_imports (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    idempotency_key VARCHAR(128) NOT NULL,
    idempotency_hash VARCHAR(64),
    reference_number VARCHAR(128),
    file_hash VARCHAR(64),
    file_name VARCHAR(256),
    journal_entry_id BIGINT REFERENCES journal_entries(id) ON DELETE SET NULL,
    ledgers_processed INTEGER NOT NULL DEFAULT 0,
    mapped_ledgers INTEGER NOT NULL DEFAULT 0,
    accounts_created INTEGER NOT NULL DEFAULT 0,
    opening_voucher_entries_processed INTEGER NOT NULL DEFAULT 0,
    opening_balance_rows_processed INTEGER NOT NULL DEFAULT 0,
    unmapped_groups_json TEXT,
    unmapped_items_json TEXT,
    errors_json TEXT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW(),
    version BIGINT NOT NULL DEFAULT 0,
    CONSTRAINT uq_tally_import_company_key UNIQUE (company_id, idempotency_key)
);

CREATE INDEX IF NOT EXISTS idx_tally_imports_company
    ON tally_imports(company_id);

CREATE INDEX IF NOT EXISTS idx_tally_imports_reference
    ON tally_imports(company_id, reference_number);
