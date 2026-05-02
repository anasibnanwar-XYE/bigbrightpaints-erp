ALTER TABLE companies
  ADD COLUMN IF NOT EXISTS setup_company_details_completed_at timestamptz,
  ADD COLUMN IF NOT EXISTS setup_gst_completed_at timestamptz,
  ADD COLUMN IF NOT EXISTS setup_accounting_completed_at timestamptz,
  ADD COLUMN IF NOT EXISTS setup_invite_team_completed_at timestamptz,
  ADD COLUMN IF NOT EXISTS setup_finished_at timestamptz;
