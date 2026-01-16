-- Add company-level PF deduction toggle
ALTER TABLE companies
    ADD COLUMN IF NOT EXISTS pf_enabled BOOLEAN NOT NULL DEFAULT true;
