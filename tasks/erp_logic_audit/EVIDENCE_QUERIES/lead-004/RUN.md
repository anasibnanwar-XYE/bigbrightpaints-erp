# LEAD-004 Evidence Run

## Objective
Compare payroll preview (monthly summary) vs payroll run lines to verify PF deduction consistency.

## Command log
```bash
# Schema lookup (employees)
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -c "\\d employees" \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/${TS}_employees_schema.txt

# Login
TS=$(date -u +"%Y%m%dT%H%M%SZ"); curl -sS -X POST -H 'Content-Type: application/json' \
  -d '{"email":"admin@bbp.dev","password":"ChangeMe123!","companyCode":"BBP"}' \
  http://localhost:8081/api/v1/auth/login \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/${TS}_login.json
jq -r '.accessToken' tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/${TS}_login.json \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/${TS}_token.txt

# Create staff employee (monthly salary >= PF threshold, workingDaysPerMonth=1)
TOKEN=$(cat tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/*_token.txt | tail -n 1)
REQ=$(ls tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/*_employee_request.json | tail -n 1)
TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" REQ="$REQ" \
  bash tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/curl/01_create_employee.sh \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/${TS}_employee_create_response.txt

# Mark attendance for one day in the month
REQ=$(ls tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/*_attendance_request.json | tail -n 1)
TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" EMPLOYEE_ID=1 REQ="$REQ" \
  bash tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/curl/02_mark_attendance.sh \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/${TS}_attendance_mark_response.txt

# Monthly summary preview (includes PF)
TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" YEAR=2026 MONTH=1 \
  bash tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/curl/03_monthly_summary.sh \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/${TS}_monthly_summary.txt

# Create + calculate monthly payroll run
TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" YEAR=2026 MONTH=1 \
  bash tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/curl/04_create_monthly_run.sh \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/${TS}_monthly_run_create.txt
BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" RUN_ID=1 \
  bash tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/curl/05_calculate_run.sh \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/${TS}_monthly_run_calculate.txt
BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" RUN_ID=1 \
  bash tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/curl/06_run_lines.sh \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/${TS}_monthly_run_lines.txt
```

## Outputs captured
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/*_employee_request.json`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/*_employee_create_response.txt`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/*_attendance_request.json`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/*_attendance_mark_response.txt`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/*_monthly_summary.txt`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/*_monthly_run_create.txt`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/*_monthly_run_calculate.txt`
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-004/OUTPUTS/*_monthly_run_lines.txt`
