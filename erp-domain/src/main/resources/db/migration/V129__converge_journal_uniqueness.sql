-- CODE-RED convergence: enforce single uniqueness mechanism for journal_entries reference numbers

DO $$
DECLARE
    canonical_idx text;
    constraint_name text;
    idx_name text;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM journal_entries
        GROUP BY company_id, reference_number
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate journal_entries (company_id, reference_number) rows detected; resolve before convergence';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        WHERE t.relname = 'journal_entries'
          AND c.conname = 'uk_journal_company_reference'
    ) THEN
        ALTER TABLE journal_entries
            ADD CONSTRAINT uk_journal_company_reference UNIQUE (company_id, reference_number);
    END IF;

    SELECT idx.relname INTO canonical_idx
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    JOIN pg_class idx ON idx.oid = c.conindid
    WHERE t.relname = 'journal_entries'
      AND c.conname = 'uk_journal_company_reference';

    FOR constraint_name IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN unnest(c.conkey) WITH ORDINALITY AS cols(attnum, ord) ON TRUE
        JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = cols.attnum
        WHERE t.relname = 'journal_entries'
          AND c.contype = 'u'
        GROUP BY c.conname
        HAVING array_agg(a.attname::text ORDER BY a.attname) = ARRAY['company_id', 'reference_number']
           AND c.conname <> 'uk_journal_company_reference'
    LOOP
        EXECUTE format('ALTER TABLE journal_entries DROP CONSTRAINT %I', constraint_name);
    END LOOP;

    FOR idx_name IN
        SELECT idx.relname
        FROM pg_index i
        JOIN pg_class idx ON idx.oid = i.indexrelid
        JOIN pg_class tbl ON tbl.oid = i.indrelid
        JOIN unnest(i.indkey) WITH ORDINALITY AS cols(attnum, ord) ON TRUE
        JOIN pg_attribute a ON a.attrelid = tbl.oid AND a.attnum = cols.attnum
        WHERE tbl.relname = 'journal_entries'
          AND i.indisunique
        GROUP BY idx.relname
        HAVING array_agg(a.attname::text ORDER BY a.attname) = ARRAY['company_id', 'reference_number']
    LOOP
        IF canonical_idx IS NULL OR idx_name <> canonical_idx THEN
            EXECUTE format('DROP INDEX IF EXISTS %I', idx_name);
        END IF;
    END LOOP;
END $$;
