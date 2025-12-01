-- Employee payroll enhancements
-- Add salary, wage, payment schedule, and bank details

ALTER TABLE employees ADD COLUMN phone VARCHAR(20);
ALTER TABLE employees ADD COLUMN employee_type VARCHAR(20) DEFAULT 'STAFF';
ALTER TABLE employees ADD COLUMN monthly_salary DECIMAL(19,2);
ALTER TABLE employees ADD COLUMN daily_wage DECIMAL(19,2);
ALTER TABLE employees ADD COLUMN payment_schedule VARCHAR(20) DEFAULT 'MONTHLY';
ALTER TABLE employees ADD COLUMN working_days_per_month INTEGER DEFAULT 26;
ALTER TABLE employees ADD COLUMN weekly_off_days INTEGER DEFAULT 1;
ALTER TABLE employees ADD COLUMN bank_account_number VARCHAR(50);
ALTER TABLE employees ADD COLUMN bank_name VARCHAR(100);
ALTER TABLE employees ADD COLUMN ifsc_code VARCHAR(20);
ALTER TABLE employees ADD COLUMN advance_balance DECIMAL(19,2) DEFAULT 0;

-- Add reset token fields to app_users
ALTER TABLE app_users ADD COLUMN reset_token VARCHAR(255);
ALTER TABLE app_users ADD COLUMN reset_expiry TIMESTAMP;
