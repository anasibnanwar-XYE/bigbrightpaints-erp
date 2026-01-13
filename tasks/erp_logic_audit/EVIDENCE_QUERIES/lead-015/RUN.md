# LEAD-015 Evidence Run (Production log list/detail 500)

## Objective
Reproduce or close the production log list/detail 500 and capture evidence before any fix.

## Planned probes
1) Baseline health check (management port).
2) Auth login; GET production log list and detail.
3) Capture response status/body and server logs if 500.
4) Re-run after fix if promoted.

## Command log
```bash
# Runtime (build + start)
TS=$(date -u +"%Y%m%dT%H%M%SZ")
ERP_SECURITY_ENCRYPTION_KEY="uFs4OAWuRLDPsS60S9JXVBCWrz0VY49exrq_MT6hX2U" \
JWT_SECRET="b2YKFKNDK6jiJw5Xyn9yX4nKQR3fpPWBbMUoTrjIg6SMSiLVFv_BBsTpPUKvmdUU" \
DB_PORT=55432 APP_PORT=8081 MANAGEMENT_PORT=19090 \
docker compose up -d > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-015/OUTPUTS/${TS}_compose_up_after_fix.txt"

TS=$(date -u +"%Y%m%dT%H%M%SZ")
ERP_SECURITY_ENCRYPTION_KEY="uFs4OAWuRLDPsS60S9JXVBCWrz0VY49exrq_MT6hX2U" \
JWT_SECRET="b2YKFKNDK6jiJw5Xyn9yX4nKQR3fpPWBbMUoTrjIg6SMSiLVFv_BBsTpPUKvmdUU" \
DB_PORT=55432 APP_PORT=8081 MANAGEMENT_PORT=19090 \
docker compose up -d --build > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-015/OUTPUTS/${TS}_compose_up_build_after_fix.txt"

# Config fix to allow app boot (GST accounts missing would trip config health)
PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -c \
"INSERT INTO accounts (company_id, code, name, type)
 VALUES (5, 'GST-IN', 'GST Input Tax', 'LIABILITY'),
        (5, 'GST-OUT', 'GST Output Tax', 'LIABILITY')
 ON CONFLICT (company_id, code) DO NOTHING;
 UPDATE companies
    SET gst_input_tax_account_id = (SELECT id FROM accounts WHERE company_id = 5 AND code = 'GST-IN'),
        gst_output_tax_account_id = (SELECT id FROM accounts WHERE company_id = 5 AND code = 'GST-OUT')
  WHERE id = 5;"

TS=$(date -u +"%Y%m%dT%H%M%SZ")
ERP_SECURITY_ENCRYPTION_KEY="uFs4OAWuRLDPsS60S9JXVBCWrz0VY49exrq_MT6hX2U" \
JWT_SECRET="b2YKFKNDK6jiJw5Xyn9yX4nKQR3fpPWBbMUoTrjIg6SMSiLVFv_BBsTpPUKvmdUU" \
DB_PORT=55432 APP_PORT=8081 MANAGEMENT_PORT=19090 \
docker compose up -d > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-015/OUTPUTS/${TS}_compose_up_after_gst_fix.txt"

sleep 5
TS=$(date -u +"%Y%m%dT%H%M%SZ")
MANAGEMENT_URL=http://localhost:19090 \
  tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-015/curl/00_health_management.sh \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-015/OUTPUTS/${TS}_health_management_after_fix.txt"

# Auth + endpoints
TS=$(date -u +"%Y%m%dT%H%M%SZ")
LOGIN_FILE="tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-015/OUTPUTS/${TS}_login_after_fix.json"
TOKEN_FILE="tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-015/OUTPUTS/${TS}_token_after_fix.txt"

curl -sS -X POST -H 'Content-Type: application/json' \
  -d '{"email":"admin@bbp.dev","password":"ChangeMe123!","companyCode":"BBP"}' \
  http://localhost:8081/api/v1/auth/login > "$LOGIN_FILE"
python3 -c "import json; print(json.load(open('$LOGIN_FILE'))['accessToken'])" > "$TOKEN_FILE"

TS=$(date -u +"%Y%m%dT%H%M%SZ")
TOKEN=$(cat "$TOKEN_FILE")
BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" \
  tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-015/curl/01_production_logs_list.sh \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-015/OUTPUTS/${TS}_production_logs_list_after_fix.txt"

TS=$(date -u +"%Y%m%dT%H%M%SZ")
LOG_ID=2
BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN="$TOKEN" LOG_ID="$LOG_ID" \
  tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-015/curl/02_production_logs_detail.sh \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-015/OUTPUTS/${TS}_production_logs_detail_after_fix.txt"

TS=$(date -u +"%Y%m%dT%H%M%SZ")
docker logs --tail 200 erp_domain_app \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/lead-015/OUTPUTS/${TS}_erp_domain_app_logs_after_fix.txt"
```
