-- Enhanced Payroll System
-- Adds overtime tracking, payroll runs, and line items

-- Add overtime and pay tracking to attendance
ALTER TABLE attendance ADD COLUMN IF NOT EXISTS regular_hours DECIMAL(5,2);
ALTER TABLE attendance ADD COLUMN IF NOT EXISTS overtime_hours DECIMAL(5,2);
ALTER TABLE attendance ADD COLUMN IF NOT EXISTS double_overtime_hours DECIMAL(5,2);
ALTER TABLE attendance ADD COLUMN IF NOT EXISTS base_pay DECIMAL(19,2);
ALTER TABLE attendance ADD COLUMN IF NOT EXISTS overtime_pay DECIMAL(19,2);
ALTER TABLE attendance ADD COLUMN IF NOT EXISTS total_pay DECIMAL(19,2);
ALTER TABLE attendance ADD COLUMN IF NOT EXISTS payroll_run_id BIGINT;

-- Payroll Runs table (batch processing) - guard with DO block to avoid errors when already present
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'payroll_runs') THEN
        CREATE TABLE payroll_runs (
            id BIGSERIAL PRIMARY KEY,
            public_id UUID NOT NULL DEFAULT gen_random_uuid(),
            company_id BIGINT NOT NULL REFERENCES companies(id),
            run_number VARCHAR(50),
            run_type VARCHAR(20) DEFAULT 'MONTHLY',
            period_start DATE,
            period_end DATE,
            status VARCHAR(20) NOT NULL DEFAULT 'DRAFT',
            total_employees INTEGER DEFAULT 0,
            total_present_days DECIMAL(10,2) DEFAULT 0,
            total_overtime_hours DECIMAL(10,2) DEFAULT 0,
            total_base_pay DECIMAL(19,2) DEFAULT 0,
            total_overtime_pay DECIMAL(19,2) DEFAULT 0,
            total_deductions DECIMAL(19,2) DEFAULT 0,
            total_net_pay DECIMAL(19,2) DEFAULT 0,
            journal_entry_id BIGINT,
            journal_entry_ref_id BIGINT REFERENCES journal_entries(id),
            run_date DATE,
            notes TEXT,
            total_amount DECIMAL(19,2),
            processed_by VARCHAR(255),
            idempotency_key VARCHAR(255) UNIQUE,
            created_by VARCHAR(255),
            created_at TIMESTAMP WITH TIME ZONE DEFAULT NOW(),
            approved_by VARCHAR(255),
            approved_at TIMESTAMP WITH TIME ZONE,
            posted_by VARCHAR(255),
            posted_at TIMESTAMP WITH TIME ZONE,
            remarks TEXT
        );
    END IF;
END$$;

-- Make old NOT NULL columns nullable for backward compatibility (ignore errors if already nullable)
DO $$
BEGIN
    ALTER TABLE payroll_runs ALTER COLUMN total_amount DROP NOT NULL;
EXCEPTION WHEN OTHERS THEN NULL;
END$$;
DO $$
BEGIN
    ALTER TABLE payroll_runs ALTER COLUMN run_number DROP NOT NULL;
EXCEPTION WHEN OTHERS THEN NULL;
END$$;

-- Add new columns to existing payroll_runs table if they don't exist
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS public_id UUID DEFAULT gen_random_uuid();
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS run_number VARCHAR(50);
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS run_type VARCHAR(20) DEFAULT 'MONTHLY';
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS period_start DATE;
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS period_end DATE;
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS total_employees INTEGER DEFAULT 0;
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS total_present_days DECIMAL(10,2) DEFAULT 0;
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS total_overtime_hours DECIMAL(10,2) DEFAULT 0;
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS total_base_pay DECIMAL(19,2) DEFAULT 0;
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS total_overtime_pay DECIMAL(19,2) DEFAULT 0;
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS total_deductions DECIMAL(19,2) DEFAULT 0;
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS total_net_pay DECIMAL(19,2) DEFAULT 0;
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS journal_entry_ref_id BIGINT;
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS approved_by VARCHAR(255);
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS approved_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS posted_by VARCHAR(255);
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS posted_at TIMESTAMP WITH TIME ZONE;
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS remarks TEXT;
ALTER TABLE payroll_runs ADD COLUMN IF NOT EXISTS created_by VARCHAR(255);

-- Create indexes only if the columns exist
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'payroll_runs' AND column_name = 'period_start') THEN
        CREATE INDEX IF NOT EXISTS idx_payroll_run_period ON payroll_runs(company_id, period_start, period_end);
    END IF;
END$$;
CREATE INDEX IF NOT EXISTS idx_payroll_run_status ON payroll_runs(company_id, status);

-- Payroll Run Lines (individual employee pay records)
DO $$
BEGIN
    IF NOT EXISTS (SELECT 1 FROM information_schema.tables WHERE table_name = 'payroll_run_lines') THEN
        CREATE TABLE payroll_run_lines (
            id BIGSERIAL PRIMARY KEY,
            payroll_run_id BIGINT NOT NULL REFERENCES payroll_runs(id) ON DELETE CASCADE,
            employee_id BIGINT REFERENCES employees(id),
            present_days DECIMAL(5,2) DEFAULT 0,
            half_days DECIMAL(5,2) DEFAULT 0,
            absent_days DECIMAL(5,2) DEFAULT 0,
            leave_days DECIMAL(5,2) DEFAULT 0,
            holiday_days DECIMAL(5,2) DEFAULT 0,
            regular_hours DECIMAL(10,2) DEFAULT 0,
            overtime_hours DECIMAL(10,2) DEFAULT 0,
            double_ot_hours DECIMAL(10,2) DEFAULT 0,
            daily_rate DECIMAL(19,2),
            hourly_rate DECIMAL(19,2),
            ot_rate_multiplier DECIMAL(5,2) DEFAULT 1.5,
            double_ot_multiplier DECIMAL(5,2) DEFAULT 2.0,
            base_pay DECIMAL(19,2) DEFAULT 0,
            overtime_pay DECIMAL(19,2) DEFAULT 0,
            holiday_pay DECIMAL(19,2) DEFAULT 0,
            gross_pay DECIMAL(19,2) DEFAULT 0,
            advance_deduction DECIMAL(19,2) DEFAULT 0,
            pf_deduction DECIMAL(19,2) DEFAULT 0,
            tax_deduction DECIMAL(19,2) DEFAULT 0,
            other_deductions DECIMAL(19,2) DEFAULT 0,
            total_deductions DECIMAL(19,2) DEFAULT 0,
            net_pay DECIMAL(19,2) DEFAULT 0,
            payment_status VARCHAR(20) DEFAULT 'PENDING',
            payment_reference VARCHAR(100),
            remarks TEXT,
            -- Backward compatibility fields
            name VARCHAR(255),
            days_worked INTEGER,
            daily_wage DECIMAL(19,2),
            advances DECIMAL(19,2),
            line_total DECIMAL(19,2),
            notes TEXT
        );
    END IF;
END$$;

-- Add columns to existing payroll_run_lines table
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS employee_id BIGINT;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS present_days DECIMAL(5,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS half_days DECIMAL(5,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS absent_days DECIMAL(5,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS leave_days DECIMAL(5,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS holiday_days DECIMAL(5,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS regular_hours DECIMAL(10,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS overtime_hours DECIMAL(10,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS double_ot_hours DECIMAL(10,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS daily_rate DECIMAL(19,2);
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS hourly_rate DECIMAL(19,2);
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS ot_rate_multiplier DECIMAL(5,2) DEFAULT 1.5;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS double_ot_multiplier DECIMAL(5,2) DEFAULT 2.0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS base_pay DECIMAL(19,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS overtime_pay DECIMAL(19,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS holiday_pay DECIMAL(19,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS gross_pay DECIMAL(19,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS advance_deduction DECIMAL(19,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS pf_deduction DECIMAL(19,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS tax_deduction DECIMAL(19,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS other_deductions DECIMAL(19,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS total_deductions DECIMAL(19,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS net_pay DECIMAL(19,2) DEFAULT 0;
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS payment_status VARCHAR(20) DEFAULT 'PENDING';
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS payment_reference VARCHAR(100);
ALTER TABLE payroll_run_lines ADD COLUMN IF NOT EXISTS remarks TEXT;

CREATE INDEX IF NOT EXISTS idx_payroll_line_run ON payroll_run_lines(payroll_run_id);
DO $$
BEGIN
    IF EXISTS (SELECT 1 FROM information_schema.columns WHERE table_name = 'payroll_run_lines' AND column_name = 'employee_id') THEN
        CREATE INDEX IF NOT EXISTS idx_payroll_line_employee ON payroll_run_lines(employee_id);
    END IF;
END$$;

-- Add foreign key from attendance to payroll run
DO $$
BEGIN
    IF NOT EXISTS (
        SELECT 1 FROM information_schema.table_constraints
        WHERE table_name = 'attendance' AND constraint_name = 'fk_attendance_payroll_run'
    ) THEN
        ALTER TABLE attendance ADD CONSTRAINT fk_attendance_payroll_run
            FOREIGN KEY (payroll_run_id) REFERENCES payroll_runs(id);
    END IF;
END$$;

-- Add overtime rate to employees
ALTER TABLE employees ADD COLUMN IF NOT EXISTS overtime_rate_multiplier DECIMAL(5,2) DEFAULT 1.5;
ALTER TABLE employees ADD COLUMN IF NOT EXISTS double_ot_rate_multiplier DECIMAL(5,2) DEFAULT 2.0;
ALTER TABLE employees ADD COLUMN IF NOT EXISTS standard_hours_per_day DECIMAL(5,2) DEFAULT 8;
