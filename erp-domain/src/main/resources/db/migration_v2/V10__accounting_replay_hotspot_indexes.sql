-- Flyway v2: replay hotspot index hardening for accounting settlement lookups.
-- Existing case-insensitive idempotency indexes are already in V8; this migration
-- only adds the missing ordered journal-entry lookup index used during replay.

CREATE INDEX IF NOT EXISTS idx_partner_settlement_company_journal_created_id
    ON public.partner_settlement_allocations USING btree (company_id, journal_entry_id, created_at, id);
