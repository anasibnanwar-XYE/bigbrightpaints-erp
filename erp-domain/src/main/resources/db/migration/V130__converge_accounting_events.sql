-- CODE-RED convergence: normalize accounting_events uniqueness enforcement

DO $$
DECLARE
    canonical_idx text;
    constraint_name text;
    idx_name text;
BEGIN
    IF EXISTS (
        SELECT 1
        FROM accounting_events
        GROUP BY aggregate_id, sequence_number
        HAVING COUNT(*) > 1
    ) THEN
        RAISE EXCEPTION 'Duplicate accounting_events (aggregate_id, sequence_number) rows detected; resolve before convergence';
    END IF;

    IF NOT EXISTS (
        SELECT 1
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        WHERE t.relname = 'accounting_events'
          AND c.conname = 'uk_aggregate_sequence'
    ) THEN
        ALTER TABLE accounting_events
            ADD CONSTRAINT uk_aggregate_sequence UNIQUE (aggregate_id, sequence_number);
    END IF;

    SELECT idx.relname INTO canonical_idx
    FROM pg_constraint c
    JOIN pg_class t ON t.oid = c.conrelid
    JOIN pg_class idx ON idx.oid = c.conindid
    WHERE t.relname = 'accounting_events'
      AND c.conname = 'uk_aggregate_sequence';

    FOR constraint_name IN
        SELECT c.conname
        FROM pg_constraint c
        JOIN pg_class t ON t.oid = c.conrelid
        JOIN unnest(c.conkey) WITH ORDINALITY AS cols(attnum, ord) ON TRUE
        JOIN pg_attribute a ON a.attrelid = t.oid AND a.attnum = cols.attnum
        WHERE t.relname = 'accounting_events'
          AND c.contype = 'u'
        GROUP BY c.conname
        HAVING array_agg(a.attname::text ORDER BY a.attname) = ARRAY['aggregate_id', 'sequence_number']
           AND c.conname <> 'uk_aggregate_sequence'
    LOOP
        EXECUTE format('ALTER TABLE accounting_events DROP CONSTRAINT %I', constraint_name);
    END LOOP;

    FOR idx_name IN
        SELECT idx.relname
        FROM pg_index i
        JOIN pg_class idx ON idx.oid = i.indexrelid
        JOIN pg_class tbl ON tbl.oid = i.indrelid
        JOIN unnest(i.indkey) WITH ORDINALITY AS cols(attnum, ord) ON TRUE
        JOIN pg_attribute a ON a.attrelid = tbl.oid AND a.attnum = cols.attnum
        WHERE tbl.relname = 'accounting_events'
          AND i.indisunique
        GROUP BY idx.relname
        HAVING array_agg(a.attname::text ORDER BY a.attname) = ARRAY['aggregate_id', 'sequence_number']
    LOOP
        IF canonical_idx IS NULL OR idx_name <> canonical_idx THEN
            EXECUTE format('DROP INDEX IF EXISTS %I', idx_name);
        END IF;
    END LOOP;

    IF to_regclass('idx_acct_events_aggregate') IS NOT NULL THEN
        EXECUTE 'DROP INDEX idx_acct_events_aggregate';
    END IF;
END $$;
