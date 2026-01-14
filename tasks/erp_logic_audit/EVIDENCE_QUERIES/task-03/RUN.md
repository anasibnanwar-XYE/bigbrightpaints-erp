# Task 03 Evidence Run (Inventory Valuation + COGS)

## SQL (read-only)

```bash
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/SQL/06_inventory_valuation_fifo.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/SQL/07_inventory_control_vs_valuation.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/SQL/02_orphans_movements_without_journal.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/SQL/12_orphan_reservations.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/SQL/03_dispatch_slips_without_cogs_journal.sql
```

Pass/Fail:
- `06_inventory_valuation_fifo.sql`: **FAIL** if negative or clearly inconsistent valuation rows appear for current stock.
- `07_inventory_control_vs_valuation.sql`: **FAIL** if variance is outside tolerance (target = 0.00).
- `02_orphans_movements_without_journal.sql`: **FAIL** if any rows return.
- `12_orphan_reservations.sql`: **FAIL** if any rows return.
- `03_dispatch_slips_without_cogs_journal.sql`: **FAIL** if any rows return.

## curl (GET-only)

```bash
export BASE_URL=http://localhost:8081
export TOKEN=<JWT>
export COMPANY_CODE=<COMPANY_CODE>

bash tasks/erp_logic_audit/EVIDENCE_QUERIES/curl/01_accounting_reports_gets.sh
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
- Accounting reports: **PASS** if inventory valuation + reconciliation align with ledger expectations and checklist is green.

## 2026-01-14 rerun (BBP, company_id=5)

```bash
# Login for report GETs
TS=$(date -u +"%Y%m%dT%H%M%SZ"); curl -sS -X POST -H 'Content-Type: application/json' \
  -d '{"email":"admin@bbp.dev","password":"ChangeMe123!","companyCode":"BBP"}' \
  http://localhost:8081/api/v1/auth/login \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-03/OUTPUTS/${TS}_login.json"
jq -r '.accessToken' "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-03/OUTPUTS/${TS}_login.json" \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-03/OUTPUTS/${TS}_token.txt"

# SQL probes (valuation + ledger + orphans)
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v company_id=5 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/SQL/06_inventory_valuation_fifo.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-03/OUTPUTS/${TS}_sql_06_inventory_valuation_fifo.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v company_id=5 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/SQL/07_inventory_control_vs_valuation.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-03/OUTPUTS/${TS}_sql_07_inventory_control_vs_valuation.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v company_id=5 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/SQL/02_orphans_movements_without_journal.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-03/OUTPUTS/${TS}_sql_02_orphans_movements_without_journal.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v company_id=5 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/SQL/12_orphan_reservations.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-03/OUTPUTS/${TS}_sql_12_orphan_reservations.txt"
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -v company_id=5 -f tasks/erp_logic_audit/EVIDENCE_QUERIES/SQL/03_dispatch_slips_without_cogs_journal.sql \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-03/OUTPUTS/${TS}_sql_03_dispatch_slips_without_cogs_journal.txt"

# Inventory control account balance
TS=$(date -u +"%Y%m%dT%H%M%SZ"); PGPASSWORD=erp psql -h localhost -p 55432 -U erp -d erp_domain \
  -c "select c.id as company_id, c.code as company_code, c.default_inventory_account_id, a.name as inventory_account_name, a.balance as inventory_account_balance from companies c left join accounts a on a.id = c.default_inventory_account_id where c.id=5;" \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-03/OUTPUTS/${TS}_sql_inventory_account_balance.txt"

# Report GETs
TOKEN=$(cat tasks/erp_logic_audit/EVIDENCE_QUERIES/task-03/OUTPUTS/20260114T090224Z_token.txt)
TS=$(date -u +"%Y%m%dT%H%M%SZ"); BASE_URL=http://localhost:8081 COMPANY_CODE=BBP TOKEN=${TOKEN} \
  bash tasks/erp_logic_audit/EVIDENCE_QUERIES/curl/01_accounting_reports_gets.sh \
  > "tasks/erp_logic_audit/EVIDENCE_QUERIES/task-03/OUTPUTS/${TS}_accounting_reports_gets.txt"
```
