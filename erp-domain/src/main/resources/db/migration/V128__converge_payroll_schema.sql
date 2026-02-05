-- CODE-RED convergence: align payroll schema to entity requirements (forward-only)

DO $$
BEGIN
    -- Normalize and backfill required payroll_runs fields.
    UPDATE payroll_runs
       SET public_id = gen_random_uuid()
     WHERE public_id IS NULL;

    UPDATE payroll_runs
       SET created_at = NOW()
     WHERE created_at IS NULL;

    UPDATE payroll_runs
       SET run_type = UPPER(BTRIM(run_type))
     WHERE run_type IS NOT NULL;

    UPDATE payroll_runs
       SET run_type = 'MONTHLY'
     WHERE run_type IS NULL;

    UPDATE payroll_runs
       SET period_start = COALESCE(period_start, run_date, created_at::date)
     WHERE period_start IS NULL;

    UPDATE payroll_runs
       SET period_end = COALESCE(period_end, run_date, created_at::date)
     WHERE period_end IS NULL;

    UPDATE payroll_runs
       SET run_number = COALESCE(NULLIF(BTRIM(run_number), ''), 'LEGACY-' || id::text)
     WHERE run_number IS NULL OR BTRIM(run_number) = '';

    UPDATE payroll_runs
       SET run_date = COALESCE(run_date, period_start)
     WHERE run_date IS NULL AND period_start IS NOT NULL;

    -- Fail closed if required fields are still missing or invalid.
    IF EXISTS (SELECT 1 FROM payroll_runs WHERE public_id IS NULL) THEN
        RAISE EXCEPTION 'payroll_runs.public_id has NULL values; backfill required before convergence';
    END IF;
    IF EXISTS (SELECT 1 FROM payroll_runs WHERE created_at IS NULL) THEN
        RAISE EXCEPTION 'payroll_runs.created_at has NULL values; backfill required before convergence';
    END IF;
    IF EXISTS (SELECT 1 FROM payroll_runs WHERE run_number IS NULL OR BTRIM(run_number) = '') THEN
        RAISE EXCEPTION 'payroll_runs.run_number has NULL/blank values; backfill required before convergence';
    END IF;
    IF EXISTS (SELECT 1 FROM payroll_runs WHERE run_type IS NULL) THEN
        RAISE EXCEPTION 'payroll_runs.run_type has NULL values; backfill required before convergence';
    END IF;
    IF EXISTS (SELECT 1 FROM payroll_runs WHERE run_type NOT IN ('WEEKLY', 'MONTHLY')) THEN
        RAISE EXCEPTION 'payroll_runs.run_type has unsupported values; normalize before convergence';
    END IF;
    IF EXISTS (SELECT 1 FROM payroll_runs WHERE period_start IS NULL OR period_end IS NULL) THEN
        RAISE EXCEPTION 'payroll_runs.period_start/period_end has NULL values; backfill required before convergence';
    END IF;
END $$;

ALTER TABLE payroll_runs ALTER COLUMN public_id SET NOT NULL;
ALTER TABLE payroll_runs ALTER COLUMN run_number SET NOT NULL;
ALTER TABLE payroll_runs ALTER COLUMN run_type SET NOT NULL;
ALTER TABLE payroll_runs ALTER COLUMN period_start SET NOT NULL;
ALTER TABLE payroll_runs ALTER COLUMN period_end SET NOT NULL;
ALTER TABLE payroll_runs ALTER COLUMN created_at SET NOT NULL;

ALTER TABLE payroll_runs ALTER COLUMN public_id SET DEFAULT gen_random_uuid();
ALTER TABLE payroll_runs ALTER COLUMN created_at SET DEFAULT NOW();
ALTER TABLE payroll_runs ALTER COLUMN run_type SET DEFAULT 'MONTHLY';
