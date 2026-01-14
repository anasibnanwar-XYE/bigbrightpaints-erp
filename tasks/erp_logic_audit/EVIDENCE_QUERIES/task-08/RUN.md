# Task 08 Evidence Run (Idempotency / Retry / Duplication)

## Environment
- DB: `psql -h localhost -p 55432 -U erp -d erp_domain`
- API: `http://localhost:8081`
- Company: MOCK (`company_id=6`, `X-Company-Id: MOCK`)

## SQL (read-only)

```bash
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/01_idempotency_duplicates.sql
psql -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/02_outbox_backlog_and_duplicates.sql
psql -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/03_partner_settlement_idempotency_index.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/04_candidate_ids.sql
psql -v company_id=<COMPANY_ID> -v idempotency_key=<IDEMPOTENCY_KEY> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/05_sales_order_idempotency_check.sql
psql -v company_id=<COMPANY_ID> -v idempotency_key=<IDEMPOTENCY_KEY> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/06_payroll_run_idempotency_check.sql
psql -v company_id=<COMPANY_ID> -v reference=<REFERENCE> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/07_purchase_return_reference_check.sql
psql -v company_id=<COMPANY_ID> -v since_ts=<ISO_TIMESTAMP> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/08_packaging_movement_check.sql
```

Pass/Fail:
- `01_idempotency_duplicates.sql`: **PASS** if all result sets are empty. **FAIL** if duplicates appear for idempotency keys.
- `02_outbox_backlog_and_duplicates.sql`: **REVIEW** if duplicate rows appear for the same (aggregate_type, aggregate_id, event_type).
- `03_partner_settlement_idempotency_index.sql`: **FAIL** if a unique constraint exists on `(company_id, idempotency_key)` while multi-allocation settlements reuse the same key.

## curl (GET-only)

```bash
export BASE_URL=http://localhost:8081
export TOKEN=<JWT>
export COMPANY_CODE=<COMPANY_CODE>

bash tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/curl/01_outbox_health_gets.sh
```

Pass/Fail:
- Outbox health: record `pendingEvents`, `retryingEvents`, `deadLetters` to correlate with SQL probes. Spikes require investigation.

## Concurrency probes (dev-only; controlled POST)

Prereqs:
- App started with `ERP_ENVIRONMENT_VALIDATION_ENABLED=false` (prod profile fails otherwise).
- Candidate IDs captured via `SQL/04_candidate_ids.sql`.

Helpers:
- `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/curl/02_parallel_post.sh` for parallel POSTs.

Flows:
- Sales order: parallel POST `/api/v1/sales/orders` with same `idempotencyKey` (expect 1 success + duplicates blocked). Re-send conflicting payload with same key (expect rejection).
- Payroll run: parallel POST `/api/v1/hr/payroll-runs` with same `idempotencyKey`. Re-send conflicting payload with same key (expect rejection).
- Purchase return: parallel POST `/api/v1/purchasing/raw-material-purchases/returns` with same `referenceNumber` (expect idempotent inventory + journal behavior).
- Bulk pack: parallel POST `/api/v1/factory/pack` against a seeded batch; check movements via `SQL/08_packaging_movement_check.sql`.

## Auth helper (admin JWT)

```bash
export BASE_URL=http://localhost:8081
export COMPANY_CODE=<COMPANY_CODE>
export ADMIN_EMAIL=<ADMIN_EMAIL>
export ADMIN_PASSWORD=<ADMIN_PASSWORD>

TOKEN=$(curl -sS -X POST -H 'Content-Type: application/json' \
  -d "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${ADMIN_PASSWORD}\",\"companyCode\":\"${COMPANY_CODE}\"}" \
  "${BASE_URL}/api/v1/auth/login" | jq -r '.accessToken')
```

## Run log

- Commands executed:
```bash
TS=$(date -u +"%Y%m%dT%H%M%SZ"); DB_PORT=55432 APP_PORT=8081 MANAGEMENT_PORT=19090 JWT_SECRET='dev-jwt-secret-32bytes-0123456789abcdef' ERP_SECURITY_ENCRYPTION_KEY='dev-encryption-key-32bytes-0123456789abcdef' SPRING_PROFILES_ACTIVE=prod,seed,mock docker compose up -d > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_compose_up.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); DB_PORT=55432 APP_PORT=8081 MANAGEMENT_PORT=19090 JWT_SECRET='dev-jwt-secret-32bytes-0123456789abcdef' ERP_SECURITY_ENCRYPTION_KEY='dev-encryption-key-32bytes-0123456789abcdef' ERP_ENVIRONMENT_VALIDATION_ENABLED=false SPRING_PROFILES_ACTIVE=prod,seed,mock docker compose up -d > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_compose_up_retry.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); docker logs --tail 200 erp_domain_app > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_app_logs_tail.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); DB_PORT=55432 APP_PORT=8081 MANAGEMENT_PORT=19090 JWT_SECRET='dev-jwt-secret-32bytes-0123456789abcdef' ERP_SECURITY_ENCRYPTION_KEY='dev-encryption-key-32bytes-0123456789abcdef' SPRING_PROFILES_ACTIVE=prod,seed,mock docker compose run -d --service-ports -e ERP_ENVIRONMENT_VALIDATION_ENABLED=false app > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_compose_run.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); docker ps --format 'table {{.Names}}\t{{.Status}}\t{{.Ports}}' > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_docker_ps.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); docker logs --tail 120 cli_backend_epic04-app-run-e803288ee81e > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_app_run_logs_tail.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); curl -sS -X POST -H 'Content-Type: application/json' -d '{"email":"mock.admin@bbp.com","password":"Password123!","companyCode":"MOCK"}' http://localhost:8081/api/v1/auth/login > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_login.json"; jq -r '.accessToken' "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_login.json" > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_token.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -t -A -c "select id from companies where code = 'MOCK';" > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_company_id.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=6 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/04_candidate_ids.sql > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_candidate_ids.txt"
TOKEN=$(jq -r '.accessToken' < tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T080710Z_login.json); TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=MOCK TOKEN="$TOKEN" bash tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/curl/01_outbox_health_gets.sh > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_outbox_health_gets.json"
TOKEN=$(jq -r '.accessToken' < tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T080710Z_login.json); TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=MOCK TOKEN="$TOKEN" ENDPOINT="/api/v1/sales/orders" REQUEST_FILE="tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081023Z_sales_order_request.json" OUTPUT_DIR="tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS" TAG="${TS}_sales_order_parallel" PARALLELISM=4 bash tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/curl/02_parallel_post.sh
TOKEN=$(jq -r '.accessToken' < tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T080710Z_login.json); TS=$(date -u +"%Y%m%dT%H%M%SZ"); curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: MOCK" -H 'Content-Type: application/json' --data @"tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081023Z_sales_order_conflict_request.json" http://localhost:8081/api/v1/sales/orders > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_sales_order_conflict_response.json"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=6 -v idempotency_key='TASK08-SO-20260114T081023Z' -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/05_sales_order_idempotency_check.sql > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_sql_sales_order_idempotency.txt"
TOKEN=$(jq -r '.accessToken' < tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T080710Z_login.json); TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=MOCK TOKEN="$TOKEN" ENDPOINT="/api/v1/hr/payroll-runs" REQUEST_FILE="tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081023Z_payroll_run_request.json" OUTPUT_DIR="tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS" TAG="${TS}_payroll_run_parallel" PARALLELISM=4 bash tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/curl/02_parallel_post.sh
TOKEN=$(jq -r '.accessToken' < tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T080710Z_login.json); TS=$(date -u +"%Y%m%dT%H%M%SZ"); curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: MOCK" -H 'Content-Type: application/json' --data @"tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081023Z_payroll_run_conflict_request.json" http://localhost:8081/api/v1/hr/payroll-runs > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_payroll_run_conflict_response.json"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=6 -v idempotency_key='TASK08-PAYROLL-20260114T081023Z' -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/06_payroll_run_idempotency_check.sql > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_sql_payroll_run_idempotency.txt"
TOKEN=$(jq -r '.accessToken' < tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T080710Z_login.json); TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=MOCK TOKEN="$TOKEN" ENDPOINT="/api/v1/purchasing/raw-material-purchases/returns" REQUEST_FILE="tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081023Z_purchase_return_request.json" OUTPUT_DIR="tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS" TAG="${TS}_purchase_return_parallel" PARALLELISM=4 bash tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/curl/02_parallel_post.sh
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=6 -v reference='TASK08-PR-20260114T081023Z' -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/07_purchase_return_reference_check.sql > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_sql_purchase_return_reference.txt"
SINCE_TS=$(date -u +"%Y-%m-%dT%H:%M:%SZ"); TS=$(date -u +"%Y%m%dT%H%M%SZ"); printf '%s\n' "$SINCE_TS" > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_bulk_pack_since_ts.txt"; TOKEN=$(jq -r '.accessToken' < tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T080710Z_login.json); BASE_URL=http://localhost:8081 COMPANY_CODE=MOCK TOKEN="$TOKEN" ENDPOINT="/api/v1/factory/pack" REQUEST_FILE="tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081023Z_bulk_pack_request.json" OUTPUT_DIR="tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS" TAG="${TS}_bulk_pack_parallel" PARALLELISM=4 bash tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/curl/02_parallel_post.sh
read -r SINCE_TS < tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081311Z_bulk_pack_since_ts.txt; TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=6 -v since_ts="${SINCE_TS}" -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/08_packaging_movement_check.sql > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_sql_packaging_movements.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=6 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/01_idempotency_duplicates.sql > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_sql_idempotency_duplicates.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/02_outbox_backlog_and_duplicates.sql > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_sql_outbox_backlog_and_duplicates.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/03_partner_settlement_idempotency_index.sql > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_sql_partner_settlement_idempotency_index.txt"
```

- Notes:
  - `docker compose up -d` with prod profile failed due to configuration validation; used `docker compose run -d --service-ports -e ERP_ENVIRONMENT_VALIDATION_ENABLED=false app`.
  - Initial login attempts returned empty JSON while the app restarted; later login succeeded.

- Outputs captured (notable):
  - Requests: `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081023Z_sales_order_request.json`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081023Z_payroll_run_request.json`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081023Z_purchase_return_request.json`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081023Z_bulk_pack_request.json`.
  - Parallel responses: `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081144Z_sales_order_parallel_*.json`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081217Z_payroll_run_parallel_*.json`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081243Z_purchase_return_parallel_*.json`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081311Z_bulk_pack_parallel_*.json`.
  - Conflict responses: `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081157Z_sales_order_conflict_response.json`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081225Z_payroll_run_conflict_response.json`.
  - SQL checks: `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081206Z_sql_sales_order_idempotency.txt`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081233Z_sql_payroll_run_idempotency.txt`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081253Z_sql_purchase_return_reference.txt`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081326Z_sql_packaging_movements.txt`.
  - Outbox/duplication probes: `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081132Z_outbox_health_gets.json`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081336Z_sql_idempotency_duplicates.txt`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T081344Z_sql_outbox_backlog_and_duplicates.txt`.

## 2026-01-14 rerun (LEAD-019/LEAD-020 confirmation)

```bash
# Login (MOCK)
RUN_TS=$(date -u +"%Y%m%dT%H%M%SZ"); curl -sS -X POST -H 'Content-Type: application/json' \
  -d '{"email":"mock.admin@bbp.com","password":"Password123!","companyCode":"MOCK"}' \
  http://localhost:8081/api/v1/auth/login \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${RUN_TS}_login.json"
jq -r '.accessToken' "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${RUN_TS}_login.json" \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${RUN_TS}_token.txt"

# Candidate IDs
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v company_id=6 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/04_candidate_ids.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_candidate_ids.txt"

# Sales order idempotency conflict (same key, different totals)
TOKEN=$(cat tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090801Z_token.txt)
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: MOCK" -H 'Content-Type: application/json' \
  --data @"tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090801Z_sales_order_request.json" \
  http://localhost:8081/api/v1/sales/orders \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090831Z_sales_order_response.json
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: MOCK" -H 'Content-Type: application/json' \
  --data @"tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090801Z_sales_order_conflict_request.json" \
  http://localhost:8081/api/v1/sales/orders \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090838Z_sales_order_conflict_response.json
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v company_id=6 -v idempotency_key='TASK08-SO-20260114T090801Z' \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/05_sales_order_idempotency_check.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_sql_sales_order_idempotency.txt"

# Payroll run idempotency conflict (same key, different totals)
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: MOCK" -H 'Content-Type: application/json' \
  --data @"tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090801Z_payroll_run_request.json" \
  http://localhost:8081/api/v1/hr/payroll-runs \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090849Z_payroll_run_response.json
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: MOCK" -H 'Content-Type: application/json' \
  --data @"tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090801Z_payroll_run_conflict_request.json" \
  http://localhost:8081/api/v1/hr/payroll-runs \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090855Z_payroll_run_conflict_response.json
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v company_id=6 -v idempotency_key='TASK08-PAYROLL-20260114T090801Z' \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/06_payroll_run_idempotency_check.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_sql_payroll_run_idempotency.txt"

# Purchase return idempotency (raw material stock drift + movement duplication)
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -c "select id, sku, name, current_stock from raw_materials where company_id=6 and id=6;" \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_sql_raw_material_stock_before_return.txt"
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: MOCK" -H 'Content-Type: application/json' \
  --data @"tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090801Z_purchase_return_request.json" \
  http://localhost:8081/api/v1/purchasing/raw-material-purchases/returns \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090916Z_purchase_return_response_1.json
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -c "select id, sku, name, current_stock from raw_materials where company_id=6 and id=6;" \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_sql_raw_material_stock_after_return_1.txt"
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: MOCK" -H 'Content-Type: application/json' \
  --data @"tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090801Z_purchase_return_request.json" \
  http://localhost:8081/api/v1/purchasing/raw-material-purchases/returns \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090930Z_purchase_return_response_2.json
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -c "select id, sku, name, current_stock from raw_materials where company_id=6 and id=6;" \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_sql_raw_material_stock_after_return_2.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v company_id=6 -v reference='TASK08-PR-20260114T090801Z' \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/SQL/07_purchase_return_reference_check.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/${TS}_sql_purchase_return_reference.txt"
```

Outputs captured:
- Requests: `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090801Z_sales_order_request.json`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090801Z_payroll_run_request.json`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090801Z_purchase_return_request.json`.
- Conflict responses: `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090838Z_sales_order_conflict_response.json`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090855Z_payroll_run_conflict_response.json`.
- Stock drift evidence: `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090908Z_sql_raw_material_stock_before_return.txt`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090922Z_sql_raw_material_stock_after_return_1.txt`, `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090938Z_sql_raw_material_stock_after_return_2.txt`.
- Movement duplication: `tasks/erp_logic_audit/EVIDENCE_QUERIES/task-08/OUTPUTS/20260114T090944Z_sql_purchase_return_reference.txt`.
