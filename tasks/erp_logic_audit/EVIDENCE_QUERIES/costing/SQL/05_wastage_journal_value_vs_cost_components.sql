-- Params:
--   :company_id (numeric)
--
-- Purpose:
--   Compare posted wastage journal value to (a) material-only valuation and (b) full unit_cost valuation.
--
-- Notes:
--   PackingService posts wastage journal with reference:
--     production_code || '-WASTE'

WITH logs AS (
  SELECT
    pl.id,
    pl.company_id,
    pl.production_code,
    pl.mixed_quantity,
    pl.wastage_quantity,
    pl.material_cost_total,
    pl.labor_cost_total,
    pl.overhead_cost_total,
    pl.unit_cost
  FROM production_logs pl
  WHERE pl.company_id = :company_id
    AND pl.wastage_quantity > 0
  ORDER BY pl.id DESC
  LIMIT 200
),
waste_journals AS (
  SELECT
    je.id AS journal_entry_id,
    je.reference_number,
    je.entry_date,
    je.status
  FROM journal_entries je
  WHERE je.company_id = :company_id
    AND je.reference_number LIKE '%-WASTE'
),
waste_lines AS (
  SELECT
    jl.journal_entry_id,
    SUM(jl.debit) AS total_debit,
    SUM(jl.credit) AS total_credit
  FROM journal_lines jl
  GROUP BY jl.journal_entry_id
)
SELECT
  l.production_code,
  l.mixed_quantity,
  l.wastage_quantity,
  l.material_cost_total,
  l.labor_cost_total,
  l.overhead_cost_total,
  l.unit_cost AS logged_unit_cost,
  (l.material_cost_total / NULLIF(l.mixed_quantity, 0)) AS material_unit_cost,
  (l.wastage_quantity * (l.material_cost_total / NULLIF(l.mixed_quantity, 0)))::numeric(18,2) AS expected_material_wastage_value,
  (l.wastage_quantity * l.unit_cost)::numeric(18,2) AS expected_full_wastage_value,
  je.journal_entry_id,
  je.entry_date,
  je.status,
  wl.total_debit::numeric(18,2) AS posted_debit,
  wl.total_credit::numeric(18,2) AS posted_credit,
  (wl.total_debit - (l.wastage_quantity * (l.material_cost_total / NULLIF(l.mixed_quantity, 0))))::numeric(18,2) AS delta_vs_material,
  (wl.total_debit - (l.wastage_quantity * l.unit_cost))::numeric(18,2) AS delta_vs_full
FROM logs l
LEFT JOIN waste_journals je
  ON je.reference_number = l.production_code || '-WASTE'
LEFT JOIN waste_lines wl
  ON wl.journal_entry_id = je.journal_entry_id
ORDER BY l.production_code DESC;
