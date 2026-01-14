-- Params:
--   :company_id (numeric)

SELECT id, first_name, last_name, employee_type, monthly_salary, daily_wage, status
FROM employees
WHERE company_id = :company_id
ORDER BY id;
