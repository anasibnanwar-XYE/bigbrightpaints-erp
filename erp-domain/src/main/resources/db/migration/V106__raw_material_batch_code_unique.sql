DO $$
BEGIN
    IF EXISTS (
        SELECT 1
        FROM information_schema.tables
        WHERE table_schema = 'public'
          AND table_name = 'raw_material_batches'
    ) THEN
        WITH ranked AS (
            SELECT id,
                   raw_material_id,
                   batch_code,
                   ROW_NUMBER() OVER (
                       PARTITION BY raw_material_id, batch_code
                       ORDER BY id
                   ) AS rn
            FROM raw_material_batches
        )
        UPDATE raw_material_batches b
        SET batch_code = b.batch_code || '-DUP-' || b.id
        FROM ranked r
        WHERE b.id = r.id
          AND r.rn > 1;

        IF NOT EXISTS (
            SELECT 1
            FROM pg_constraint
            WHERE conname = 'uk_raw_material_batch_code'
        ) THEN
            ALTER TABLE raw_material_batches
                ADD CONSTRAINT uk_raw_material_batch_code UNIQUE (raw_material_id, batch_code);
        END IF;
    END IF;
END $$;
