# LF-011 Evidence Run (GST accounts missing)

## Objective
Show config health flags missing GST accounts and GST return returns a clear validation error.

## Planned probes
1) Set GST input/output accounts to NULL for company.
2) Run configuration health endpoint.
3) Run GST return endpoint.

## Command log
```bash
# Null out GST accounts
PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -c "UPDATE companies SET gst_input_tax_account_id = NULL, gst_output_tax_account_id = NULL WHERE id = 5;"

# Confirm GST config in DB
TS=$(date -u +"%Y%m%dT%H%M%SZ")
PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v company_id=5 \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-011/SQL/01_company_tax_accounts.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-011/OUTPUTS/${TS}_company_tax_accounts_after_null.txt"

# Config health endpoint
TS=$(date -u +"%Y%m%dT%H%M%SZ")
TOKEN_FILE=$(ls -t tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-015/OUTPUTS/*_token_after_fix.txt | head -n 1)
TOKEN=$(cat "$TOKEN_FILE")
BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" \
  tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-011/curl/01_config_health_get.sh \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-011/OUTPUTS/${TS}_config_health_after_null.txt"

# GST return endpoint (should return validation error)
TS=$(date -u +"%Y%m%dT%H%M%SZ")
BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" \
  tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-011/curl/02_gst_return_get.sh \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-011/OUTPUTS/${TS}_gst_return_after_null.txt"
```
