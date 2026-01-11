# Reconciliation Evidence Checklist (Prod Candidate)

Purpose: provide repeatable SQL/API checks for reconciliation and period close readiness.

## Inputs
- `COMPANY_CODE` (e.g., OPS)
- `COMPANY_ID` (numeric id for SQL joins)
- `AS_OF_DATE` (YYYY-MM-DD)
- `TOLERANCE` (default 0.01)
- DB connection: `DB_HOST`, `DB_PORT`, `DB_USER`, `DB_NAME`
- API (optional): `BASE_URL`, `TOKEN` (JWT)

## Evidence capture
- Use a UTC `RUN_ID` and store SQL text + output under `docs/ops_and_debug/LOGS/` with that prefix.
- Record results in `docs/ops_and_debug/EVIDENCE.md` using the evidence template.

Example shell setup:
```bash
RUN_ID=$(date -u +"%Y%m%dT%H%M%SZ")
export DB_HOST=localhost DB_PORT=55432 DB_USER=erp DB_NAME=erp_domain
export COMPANY_CODE=OPS COMPANY_ID=1 AS_OF_DATE=2026-01-31 TOLERANCE=0.01
```

## 1) Orphan journals (no lines)
Expected: zero rows.
```sql
-- journal_entries without journal_lines
select je.id, je.reference_number, je.entry_date
from journal_entries je
left join journal_lines jl on jl.journal_entry_id = je.id
where jl.id is null
order by je.id;
```

## 2) Orphan raw material movements (purchases/returns)
Expected: `missing_journal = 0`.
```sql
select count(*) as missing_journal
from raw_material_movements m
join raw_materials r on r.id = m.raw_material_id
where r.company_id = :company_id
  and m.reference_type in ('RAW_MATERIAL_PURCHASE', 'PURCHASE_RETURN')
  and m.journal_entry_id is null;
```

## 3) Orphan inventory movements (adjustments)
Expected: `missing_journal = 0`.
```sql
select count(*) as missing_journal
from inventory_movements m
join finished_goods f on f.id = m.finished_good_id
where f.company_id = :company_id
  and m.reference_type = 'ADJUSTMENT'
  and m.journal_entry_id is null;
```

## 4) AR control vs dealer ledger tie-out (as-of)
Expected: `abs(variance) <= :tolerance`.
```sql
with ar_control as (
  select coalesce(sum(balance), 0) as balance
  from accounts
  where company_id = :company_id
    and type = 'ASSET'
    and (upper(code) like '%AR%' or upper(code) like '%RECEIVABLE%')
),
ar_ledger as (
  select coalesce(sum(debit - credit), 0) as balance
  from dealer_ledger_entries
  where company_id = :company_id
    and entry_date <= :as_of
)
select ar_control.balance as ar_control,
       ar_ledger.balance as dealer_ledger,
       (ar_control.balance - ar_ledger.balance) as variance
from ar_control, ar_ledger;
```

## 5) AP control vs supplier ledger tie-out (as-of)
Expected: `abs(variance) <= :tolerance`.
```sql
with ap_control as (
  select coalesce(sum(balance), 0) as balance
  from accounts
  where company_id = :company_id
    and type = 'LIABILITY'
    and (upper(code) like '%AP%' or upper(code) like '%PAYABLE%')
),
ap_ledger as (
  select coalesce(sum(credit - debit), 0) as balance
  from supplier_ledger_entries
  where company_id = :company_id
    and entry_date <= :as_of
)
select ap_control.balance as ap_control,
       ap_ledger.balance as supplier_ledger,
       (ap_control.balance - ap_ledger.balance) as variance
from ap_control, ap_ledger;
```

## 6) Inventory valuation vs GL inventory control
Expected: `variance` within tolerance (0.01).

SQL to confirm the inventory control account balance (default account preferred):
```sql
select c.code as company_code, a.id as inventory_account_id, a.balance as ledger_balance
from companies c
join accounts a on a.id = c.default_inventory_account_id
where c.id = :company_id;
```

API reconciliation (uses FIFO valuation logic):
```bash
curl -fsS -H "Authorization: Bearer $TOKEN" -H "X-Company-Id: $COMPANY_CODE" \
  "$BASE_URL/api/v1/reports/inventory-reconciliation"
```
Expected fields: `physicalInventoryValue`, `ledgerInventoryBalance`, `variance`.

## SQL execution helper (psql)
```bash
psql -h "$DB_HOST" -p "$DB_PORT" -U "$DB_USER" -d "$DB_NAME" \
  -v company_id="$COMPANY_ID" -v as_of="$AS_OF_DATE" -v tolerance="$TOLERANCE" \
  -f docs/ops_and_debug/LOGS/${RUN_ID}_task05_M3_recon_checks.sql \
  | tee docs/ops_and_debug/LOGS/${RUN_ID}_task05_M3_recon_checks.txt
```
