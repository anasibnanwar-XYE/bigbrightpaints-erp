-- Flyway v2: canonicalize journal reference mapping lookup column without editing applied migrations.

DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'journal_reference_mappings'
          AND column_name = 'legacy_reference'
    ) AND NOT EXISTS (
        SELECT 1
        FROM information_schema.columns
        WHERE table_schema = 'public'
          AND table_name = 'journal_reference_mappings'
          AND column_name = 'reference_key'
    ) THEN
        ALTER TABLE public.journal_reference_mappings
            RENAME COLUMN legacy_reference TO reference_key;
    END IF;
END $$;

DROP INDEX IF EXISTS public.uq_journal_reference_mapping_legacy;
DROP INDEX IF EXISTS public.idx_journal_reference_mapping_legacy_ci;

CREATE UNIQUE INDEX IF NOT EXISTS uq_journal_reference_mapping_reference_key
    ON public.journal_reference_mappings USING btree (company_id, reference_key);

CREATE INDEX IF NOT EXISTS idx_journal_reference_mapping_reference_key_ci
    ON public.journal_reference_mappings USING btree (company_id, lower(reference_key));
