# COSTING Lead Evidence Run (LEAD-COST-001/002/005)

## Objective
Investigate COSTING LEADs only (no production code changes):
- LEAD-COST-001 (bulk pack: movements completeness + movement↔journal linkage)
- LEAD-COST-002 (bulk pack: journal idempotency / reference determinism)
- LEAD-COST-005 (wastage valuation basis)

## Environment
- DB: `psql -h localhost -p 55432 -U erp -d erp_domain`
- API: `http://localhost:8081`
- Company: BBP (`company_id=5`, `X-Company-Id: BBP`)

## Command log (abridged)
```bash
# Company lookup
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -f tasks/erp_logic_audit/EVIDENCE_QUERIES/SQL/00_company_lookup.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_00_company_lookup.txt"

# Run probes (baseline)
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/01_bulk_pack_child_receipts_missing_journal.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_01_bulk_pack_child_receipts_missing_journal.txt"

# Choose dev-only inputs
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/06_bulk_pack_candidates.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_06_bulk_pack_candidates.txt"

# Login and capture JWT
TS=$(date -u +"%Y%m%dT%H%M%SZ"); curl -sS -X POST -H 'Content-Type: application/json' \
  -d '{"email":"admin@bbp.dev","password":"ChangeMe123!","companyCode":"BBP"}' \
  http://localhost:8081/api/v1/auth/login \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_login.json";

# Dev-only POST probe: bulk pack twice with same request
TOKEN=$(jq -r '.accessToken' "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_login.json")
REQ=tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T105824Z_bulk_pack_request.json
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -X POST \
  -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: BBP" -H 'Content-Type: application/json' \
  --data @"${REQ}" http://localhost:8081/api/v1/factory/pack \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T105824Z_bulk_pack_response_1.json
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -X POST \
  -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: BBP" -H 'Content-Type: application/json' \
  --data @"${REQ}" http://localhost:8081/api/v1/factory/pack \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T105856Z_bulk_pack_response_2.json

# Re-run probes after POSTs
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/03_bulk_pack_journal_duplicates_by_semantic_reference.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_03_bulk_pack_journal_duplicates_by_semantic_reference.txt"
```

## Phase 5 fix verification run (LF-016/LF-017)

- Design fork: used deterministic pack reference + movement lookup with `lockById` (PESSIMISTIC_WRITE) for idempotency and concurrency safety, avoiding a new unique constraint/migration.

### Command log (abridged)
```bash
# Build app image with fixes
JWT_SECRET=... ERP_SECURITY_ENCRYPTION_KEY=... docker compose build app

# Run app with environment validation disabled (compose run)
JWT_SECRET=... ERP_SECURITY_ENCRYPTION_KEY=... ERP_ENVIRONMENT_VALIDATION_ENABLED=false DB_PORT=55432 APP_PORT=8081 MANAGEMENT_PORT=19090 \
  docker compose run -d --service-ports -e ERP_ENVIRONMENT_VALIDATION_ENABLED=false app

# Login
TS=$(date -u +"%Y%m%dT%H%M%SZ"); curl -sS -X POST -H 'Content-Type: application/json' \
  -d '{"email":"admin@bbp.dev","password":"ChangeMe123!","companyCode":"BBP"}' \
  http://localhost:8081/api/v1/auth/login \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_login_after_fix_3.json";
jq -r '.accessToken' "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_login_after_fix_3.json" \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_token_after_fix_3.txt"

# Seed a new bulk batch via production log
REQ=tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120607Z_production_log_request_for_bulk.json
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -X POST \
  -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: BBP" -H 'Content-Type: application/json' \
  --data @"${REQ}" http://localhost:8081/api/v1/factory/production/logs \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120613Z_production_log_create_for_bulk.txt

# Confirm bulk batch candidates (bulkBatchId=12)
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/06_bulk_pack_candidates.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_06_bulk_pack_candidates_after_fix_3.txt"

# Pack bulk batch twice with same request (idempotency)
REQ=tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120634Z_bulk_pack_request_after_fix.json
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -X POST \
  -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: BBP" -H 'Content-Type: application/json' \
  --data @"${REQ}" http://localhost:8081/api/v1/factory/pack \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120643Z_bulk_pack_after_fix_response_1.json
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -X POST \
  -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: BBP" -H 'Content-Type: application/json' \
  --data @"${REQ}" http://localhost:8081/api/v1/factory/pack \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120643Z_bulk_pack_after_fix_response_2.json

# Look up journal reference for pack (journal_entry_id=31)
PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -c "select id as journal_entry_id, reference_number from journal_entries where id=31;" \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120701Z_sql_08_bulk_pack_reference_lookup_after_fix.txt"

# Re-run probes for the deterministic pack reference
PACK_REF=PACK-PROD-20260113-004-fabfa2db3a9d7282
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -v pack_reference="${PACK_REF}" \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/01_bulk_pack_child_receipts_missing_journal.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_01_bulk_pack_child_receipts_missing_journal_after_fix.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -v pack_reference="${PACK_REF}" \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/02_bulk_pack_missing_bulk_issue_movement.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_02_bulk_pack_missing_bulk_issue_movement_after_fix.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -v pack_reference="${PACK_REF}" \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/03_bulk_pack_journal_duplicates_by_semantic_reference.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_03_bulk_pack_journal_duplicates_by_semantic_reference_after_fix.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -v pack_reference="${PACK_REF}" \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/04_bulk_pack_movements_vs_journals_linkage.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_04_bulk_pack_movements_vs_journals_linkage_after_fix.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -v pack_reference="${PACK_REF}" \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/07_bulk_pack_recent_journals.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_07_bulk_pack_recent_journals_after_fix.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -v pack_reference="${PACK_REF}" \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/08_bulk_pack_movements_by_type.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_08_bulk_pack_movements_by_type_after_fix.txt"
```

### Verification rerun (2026-01-14)
```bash
# Login (running app container)
TS=$(date -u +"%Y%m%dT%H%M%SZ"); curl -sS -X POST -H 'Content-Type: application/json' \
  -d '{"email":"admin@bbp.dev","password":"ChangeMe123!","companyCode":"BBP"}' \
  http://localhost:8081/api/v1/auth/login \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_login.json"
jq -r '.accessToken' "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_login.json" \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_token.txt"
TOKEN=$(cat "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_token.txt")

# Seed a new bulk batch via production log
REQ=tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084701Z_production_log_request_for_bulk.json
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -X POST \
  -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: BBP" -H 'Content-Type: application/json' \
  --data @"${REQ}" http://localhost:8081/api/v1/factory/production/logs \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084710Z_production_log_create_for_bulk.txt

# Confirm bulk batch candidates + chosen IDs
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/06_bulk_pack_candidates.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_06_bulk_pack_candidates_after_fix_4.txt"
PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -c "select pb.id as bulk_batch_id, pb.batch_code, pb.quantity_available from finished_good_batches pb join finished_goods fg on fg.id = pb.finished_good_id where fg.company_id=5 and coalesce(pb.is_bulk,false)=true and pb.quantity_available > 0 order by pb.id desc limit 1;" \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084729Z_sql_06_bulk_pack_pick_bulk_batch.txt
PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -c "select fg.id as child_sku_id, fg.product_code from finished_goods fg where fg.company_id=5 and fg.product_code not like '%-BULK' order by fg.id desc limit 1;" \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084729Z_sql_06_bulk_pack_pick_child_sku.txt

# Pack bulk batch twice with same request (idempotency)
REQ=tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084740Z_bulk_pack_request_after_fix.json
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -X POST \
  -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: BBP" -H 'Content-Type: application/json' \
  --data @"${REQ}" http://localhost:8081/api/v1/factory/pack \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084750Z_bulk_pack_after_fix_response_1.json
curl -sS -w "\nHTTP_STATUS:%{http_code}\n" -X POST \
  -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: BBP" -H 'Content-Type: application/json' \
  --data @"${REQ}" http://localhost:8081/api/v1/factory/pack \
  > tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084750Z_bulk_pack_after_fix_response_2.json

# Look up journal reference for pack (journal_entry_id=69)
PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -c "select id as journal_entry_id, reference_number from journal_entries where id=69;" \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084816Z_sql_08_bulk_pack_reference_lookup_after_fix.txt"

# Re-run probes for the deterministic pack reference
PACK_REF=PACK-PROD-20260113-004-e993a65696124669
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -v pack_reference="${PACK_REF}" \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/01_bulk_pack_child_receipts_missing_journal.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_01_bulk_pack_child_receipts_missing_journal_after_fix.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -v pack_reference="${PACK_REF}" \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/02_bulk_pack_missing_bulk_issue_movement.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_02_bulk_pack_missing_bulk_issue_movement_after_fix.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -v pack_reference="${PACK_REF}" \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/03_bulk_pack_journal_duplicates_by_semantic_reference.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_03_bulk_pack_journal_duplicates_by_semantic_reference_after_fix.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -v pack_reference="${PACK_REF}" \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/04_bulk_pack_movements_vs_journals_linkage.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_04_bulk_pack_movements_vs_journals_linkage_after_fix.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -v pack_reference="${PACK_REF}" \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/07_bulk_pack_recent_journals.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_07_bulk_pack_recent_journals_after_fix.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain -v company_id=5 -v pack_reference="${PACK_REF}" \
  -f tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/SQL/08_bulk_pack_movements_by_type.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/${TS}_sql_08_bulk_pack_movements_by_type_after_fix.txt"
```

## Outputs captured (notable)
- Bulk pack POST evidence:
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T105824Z_bulk_pack_request.json`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T105824Z_bulk_pack_response_1.json`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T105856Z_bulk_pack_response_2.json`
- Bulk pack movement/journal linkage evidence:
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T105920Z_sql_01_bulk_pack_child_receipts_missing_journal.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T105920Z_sql_02_bulk_pack_missing_bulk_issue_movement.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T105920Z_sql_03_bulk_pack_journal_duplicates_by_semantic_reference.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T110014Z_sql_07_bulk_pack_recent_journals.txt`
- Wastage valuation snapshot:
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T105157Z_sql_05_wastage_journal_value_vs_cost_components.txt`

- Fix verification (LF-016/LF-017):
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120607Z_production_log_request_for_bulk.json`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120613Z_production_log_create_for_bulk.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120634Z_bulk_pack_request_after_fix.json`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120643Z_bulk_pack_after_fix_response_1.json`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120643Z_bulk_pack_after_fix_response_2.json`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120701Z_sql_08_bulk_pack_reference_lookup_after_fix.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120827Z_sql_01_bulk_pack_child_receipts_missing_journal_after_fix.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120827Z_sql_02_bulk_pack_missing_bulk_issue_movement_after_fix.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120827Z_sql_03_bulk_pack_journal_duplicates_by_semantic_reference_after_fix.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120827Z_sql_04_bulk_pack_movements_vs_journals_linkage_after_fix.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120827Z_sql_07_bulk_pack_recent_journals_after_fix.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260113T120827Z_sql_08_bulk_pack_movements_by_type_after_fix.txt`

- Fix verification rerun (LF-016/LF-017, 2026-01-14):
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084652Z_login.json`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084652Z_token.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084701Z_production_log_request_for_bulk.json`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084710Z_production_log_create_for_bulk.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084715Z_sql_06_bulk_pack_candidates_after_fix_4.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084729Z_sql_06_bulk_pack_pick_bulk_batch.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084729Z_sql_06_bulk_pack_pick_child_sku.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084740Z_bulk_pack_request_after_fix.json`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084750Z_bulk_pack_after_fix_response_1.json`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084750Z_bulk_pack_after_fix_response_2.json`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084816Z_sql_08_bulk_pack_reference_lookup_after_fix.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084831Z_sql_01_bulk_pack_child_receipts_missing_journal_after_fix.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084832Z_sql_02_bulk_pack_missing_bulk_issue_movement_after_fix.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084832Z_sql_03_bulk_pack_journal_duplicates_by_semantic_reference_after_fix.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084832Z_sql_04_bulk_pack_movements_vs_journals_linkage_after_fix.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084832Z_sql_07_bulk_pack_recent_journals_after_fix.txt`
  - `tasks/erp_logic_audit/EVIDENCE_QUERIES/costing/OUTPUTS/20260114T084832Z_sql_08_bulk_pack_movements_by_type_after_fix.txt`
