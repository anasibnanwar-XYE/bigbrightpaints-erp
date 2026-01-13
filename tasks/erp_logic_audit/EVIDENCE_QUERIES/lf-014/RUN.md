# LF-014 Evidence Run (FG creation null discount)

## Objective
Capture before/after evidence for finished-good creation when discount default is null.

## Planned probes
1) Confirm discount default is null (SQL or GET defaults).
2) Attempt FG creation and capture response/logs (before fix).
3) Re-run after fix to confirm non-500 behavior.

## Command log
```bash
# Inspect current defaults
TS=$(date -u +"%Y%m%dT%H%M%SZ")
PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v company_id=5 \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-014/SQL/01_company_default_accounts.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-014/OUTPUTS/${TS}_company_default_accounts.txt"

# Force discount default to NULL for repro
PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -c "UPDATE companies SET default_discount_account_id = NULL WHERE id = 5;"

TS=$(date -u +"%Y%m%dT%H%M%SZ")
PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v company_id=5 \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-014/SQL/01_company_default_accounts.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-014/OUTPUTS/${TS}_company_default_accounts_after_null.txt"

# GET defaults
TS=$(date -u +"%Y%m%dT%H%M%SZ")
TOKEN_FILE=$(ls -t tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-015/OUTPUTS/*_token_after_fix.txt | head -n 1)
TOKEN=$(cat "$TOKEN_FILE")
BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" \
  tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-014/curl/01_default_accounts_get.sh \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-014/OUTPUTS/${TS}_default_accounts_get_after_null.txt"

# Create FG product with null discount default
TS=$(date -u +"%Y%m%dT%H%M%SZ")
REQUEST_FILE="tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-014/OUTPUTS/${TS}_create_product_request.json"
cat > "$REQUEST_FILE" <<'REQ'
{
  "brandName": "LF-014 Brand",
  "productName": "LF-014 Product",
  "category": "FINISHED_GOOD",
  "unitOfMeasure": "UNIT",
  "basePrice": 100.0,
  "gstRate": 18.0
}
REQ

BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" REQUEST_FILE="$REQUEST_FILE" \
  tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-014/curl/02_create_product.sh \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lf-014/OUTPUTS/${TS}_create_product_after_fix.txt"
```
