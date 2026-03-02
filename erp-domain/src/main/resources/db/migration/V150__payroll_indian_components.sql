-- Payroll Indian component redesign: structured earnings, statutory deductions, and payment date tracking

ALTER TABLE salary_structure_templates
    ADD COLUMN IF NOT EXISTS esi_eligibility_threshold DECIMAL(19,2) NOT NULL DEFAULT 21000.00,
    ADD COLUMN IF NOT EXISTS professional_tax DECIMAL(19,2) NOT NULL DEFAULT 200.00;

ALTER TABLE payroll_runs
    ADD COLUMN IF NOT EXISTS payment_date DATE;

ALTER TABLE payroll_run_lines
    ADD COLUMN IF NOT EXISTS basic_salary_component DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS hra_component DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS da_component DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS special_allowance_component DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS esi_deduction DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS professional_tax_deduction DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS loan_deduction DECIMAL(19,2) NOT NULL DEFAULT 0,
    ADD COLUMN IF NOT EXISTS leave_without_pay_deduction DECIMAL(19,2) NOT NULL DEFAULT 0;

UPDATE payroll_run_lines
SET loan_deduction = COALESCE(advance_deduction, 0)
WHERE COALESCE(loan_deduction, 0) = 0
  AND COALESCE(advance_deduction, 0) > 0;

INSERT INTO accounts (public_id, company_id, code, name, type, balance, active, hierarchy_level, version)
SELECT
    gen_random_uuid(),
    c.id,
    acc.code,
    acc.name,
    acc.type,
    0,
    true,
    1,
    0
FROM companies c
CROSS JOIN (VALUES
    ('ESI-PAYABLE', 'ESI Payable', 'LIABILITY'),
    ('TDS-PAYABLE', 'TDS Payable', 'LIABILITY'),
    ('PROFESSIONAL-TAX-PAYABLE', 'Professional Tax Payable', 'LIABILITY')
) AS acc(code, name, type)
WHERE NOT EXISTS (
    SELECT 1
    FROM accounts a
    WHERE a.company_id = c.id
      AND UPPER(a.code) = UPPER(acc.code)
);
