-- Improve FX rate precision and enforce positivity
ALTER TABLE journal_entries
    ADD COLUMN fx_rate_new NUMERIC(19,6);

UPDATE journal_entries
SET fx_rate_new = CASE
    WHEN fx_rate IS NULL OR fx_rate <= 0 THEN 1
    ELSE fx_rate
END;

ALTER TABLE journal_entries DROP COLUMN fx_rate;
ALTER TABLE journal_entries RENAME COLUMN fx_rate_new TO fx_rate;

ALTER TABLE journal_entries
    ADD CONSTRAINT chk_fx_rate_positive CHECK (fx_rate > 0);
