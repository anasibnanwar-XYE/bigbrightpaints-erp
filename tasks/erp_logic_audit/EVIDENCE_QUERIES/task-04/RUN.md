# Task 04 Evidence Run (Production Costing + WIP)

## SQL (read-only)

```bash
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-04/SQL/01_production_rm_cost_vs_log.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-04/SQL/02_production_wip_debit_credit_delta.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-04/SQL/03_production_receipt_journal_mismatch.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-04/SQL/04_production_wastage_missing_journal.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-04/SQL/05_packaging_movements_missing_journal.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-04/SQL/06_company_default_accounts.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-04/SQL/07_account_candidates_wip_inventory.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-04/SQL/08_production_seed_readiness.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-04/SQL/09_raw_material_seed_candidates.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-04/SQL/10_production_log_status.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-04/SQL/11_production_journal_lines.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/SQL/02_orphans_movements_without_journal.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/SQL/07_inventory_control_vs_valuation.sql
```

Pass/Fail:
- `01_production_rm_cost_vs_log.sql`: **FAIL** if any rows return (material costs drift).
- `02_production_wip_debit_credit_delta.sql`: **FLAG** if rows return; check if WIP debits/credits are expected to differ for labor/overhead.
- `03_production_receipt_journal_mismatch.sql`: **FAIL** if any rows return (receipt not linked to expected journal).
- `04_production_wastage_missing_journal.sql`: **FAIL** if any rows return.
- `05_packaging_movements_missing_journal.sql`: **FAIL** if any rows return.
- `06_company_default_accounts.sql`: **INFO** (defaults required for posting).
- `07_account_candidates_wip_inventory.sql`: **INFO** (identify WIP/inventory accounts).
- `08_production_seed_readiness.sql`: **INFO** (identify missing WIP metadata).
- `09_raw_material_seed_candidates.sql`: **INFO** (identify raw materials with available batches).
- `10_production_log_status.sql`: **INFO** (confirm packed quantity + costs).
- `11_production_journal_lines.sql`: **INFO** (journal lines tied to production logs).
- `02_orphans_movements_without_journal.sql`: **FAIL** if any rows return.
- `07_inventory_control_vs_valuation.sql`: **FAIL** if variance outside tolerance (target = 0.00).

## curl (GET-only)

```bash
export BASE_URL=http://localhost:8081
export TOKEN=<JWT>
export COMPANY_CODE=<COMPANY_CODE>

bash tasks/erp_logic_audit/EVIDENCE_QUERIES/curl/01_accounting_reports_gets.sh
bash tasks/erp_logic_audit/EVIDENCE_QUERIES/task-04/curl/01_production_gets.sh
```

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

Pass/Fail:
- Production endpoints: **PASS** if production logs/packing history are readable and consistent with SQL references.
- Accounting reports: **PASS** if inventory valuation + reconciliation align with ledger expectations.

## Dev-only seed to unblock LEAD-012 (record inputs/outputs)
> Use only in dev. Capture request/response JSON in `OUTPUTS/` and reference them in notes.

0) If default discount account is null, set it before creating finished goods (prevents 500 from null defaults):
```bash
curl -sS -X POST "${BASE_URL}/api/v1/accounting/accounts" \
  -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: ${COMPANY_CODE}" \
  -H 'Content-Type: application/json' \
  -d '{"code":"6100","name":"Sales Discounts","type":"EXPENSE"}'

curl -sS -X GET "${BASE_URL}/api/v1/accounting/default-accounts" \
  -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: ${COMPANY_CODE}"

curl -sS -X PUT "${BASE_URL}/api/v1/accounting/default-accounts" \
  -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: ${COMPANY_CODE}" \
  -H 'Content-Type: application/json' \
  -d '{"inventoryAccountId":<INV_ID>,"cogsAccountId":<COGS_ID>,"revenueAccountId":<REV_ID>,"discountAccountId":<DISC_ID>,"taxAccountId":<TAX_ID>}'
```

1) Identify product + account IDs:
   - Use `08_production_seed_readiness.sql` + `07_account_candidates_wip_inventory.sql`.
2) Update production product metadata (ensure WIP + semi-finished accounts exist):
```bash
curl -sS -X PUT "${BASE_URL}/api/v1/accounting/catalog/products/<PRODUCT_ID>" \
  -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: ${COMPANY_CODE}" \
  -H 'Content-Type: application/json' \
  -d '{"metadata":{"wipAccountId":<WIP_ACCOUNT_ID>,"semiFinishedAccountId":<SEMI_FINISHED_ACCOUNT_ID>}}'
```
3) Create a minimal production log (materials + labor/overhead if needed):
```bash
curl -sS -X POST "${BASE_URL}/api/v1/factory/production/logs" \
  -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: ${COMPANY_CODE}" \
  -H 'Content-Type: application/json' \
  -d '{"brandId":<BRAND_ID>,"productId":<PRODUCT_ID>,"batchSize":10,"mixedQuantity":10,"laborCost":1,"overheadCost":1,"materials":[{"rawMaterialId":<RAW_MATERIAL_ID>,"quantity":1,"unitOfMeasure":"KG"}]}'
```
4) Record packing (if log not fully packed):
```bash
curl -sS -X POST "${BASE_URL}/api/v1/factory/packing-records" \
  -H "Authorization: Bearer ${TOKEN}" -H "X-Company-Id: ${COMPANY_CODE}" \
  -H 'Content-Type: application/json' \
  -d '{"productionLogId":<LOG_ID>,"packedDate":"<YYYY-MM-DD>","packedBy":"audit","lines":[{"packagingSize":"1L","quantityLiters":10,"piecesCount":10}]}'
```
