-- Flyway v2: accelerate case-insensitive idempotency lookups used by replay-safe accounting flows.
-- These are non-unique functional indexes; service-level normalization and conflict checks
-- enforce behavior while avoiding migration failures on existing mixed-case legacy data.

CREATE INDEX IF NOT EXISTS idx_partner_settlement_idempotency_ci
    ON public.partner_settlement_allocations USING btree (company_id, lower(idempotency_key))
    WHERE (idempotency_key IS NOT NULL);

CREATE INDEX IF NOT EXISTS idx_journal_reference_mapping_legacy_ci
    ON public.journal_reference_mappings USING btree (company_id, lower(legacy_reference));

CREATE INDEX IF NOT EXISTS idx_journal_reference_mapping_canonical_ci
    ON public.journal_reference_mappings USING btree (company_id, lower(canonical_reference));
