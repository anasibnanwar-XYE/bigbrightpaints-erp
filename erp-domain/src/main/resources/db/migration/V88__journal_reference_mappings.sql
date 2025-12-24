CREATE TABLE IF NOT EXISTS journal_reference_mappings (
    id BIGSERIAL PRIMARY KEY,
    company_id BIGINT NOT NULL REFERENCES companies(id) ON DELETE CASCADE,
    legacy_reference VARCHAR(64) NOT NULL,
    canonical_reference VARCHAR(64) NOT NULL,
    entity_type VARCHAR(64),
    entity_id BIGINT,
    created_at TIMESTAMPTZ NOT NULL DEFAULT NOW()
);

CREATE UNIQUE INDEX IF NOT EXISTS uq_journal_reference_mapping_legacy
    ON journal_reference_mappings(company_id, legacy_reference);

CREATE INDEX IF NOT EXISTS idx_journal_reference_mapping_canonical
    ON journal_reference_mappings(company_id, canonical_reference);

INSERT INTO journal_reference_mappings (
    company_id,
    legacy_reference,
    canonical_reference,
    entity_type,
    entity_id,
    created_at
)
SELECT
    company_id,
    reference_number,
    reference_number,
    'JOURNAL_ENTRY',
    id,
    NOW()
FROM journal_entries
ON CONFLICT (company_id, legacy_reference) DO NOTHING;

UPDATE journal_reference_mappings m
SET canonical_reference = regexp_replace(m.legacy_reference, '^SALE-', 'INV-')
WHERE m.legacy_reference LIKE 'SALE-%'
  AND NOT EXISTS (
      SELECT 1
      FROM journal_entries je
      WHERE je.company_id = m.company_id
        AND je.reference_number = regexp_replace(m.legacy_reference, '^SALE-', 'INV-')
  );
