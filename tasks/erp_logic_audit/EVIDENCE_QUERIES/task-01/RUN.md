# Task 01 Evidence Run (O2C Logic Hunt)

## SQL (read-only)

```bash
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-01/SQL/01_o2c_document_linkage.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-01/SQL/02_o2c_cogs_reference_gaps.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-01/SQL/03_o2c_journals_unlinked.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-01/SQL/04_o2c_tax_total_variances.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-01/SQL/05_o2c_ar_tieout.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-01/SQL/06_o2c_idempotency_duplicates.sql
psql -v company_id=<COMPANY_ID> -f tasks/erp_logic_audit/EVIDENCE_QUERIES/task-01/SQL/07_o2c_orphan_reservations.sql
```

Pass/Fail:
- `01_o2c_document_linkage.sql`: **FAIL** if any rows return.
- `02_o2c_cogs_reference_gaps.sql`: **FAIL** if any rows return.
- `03_o2c_journals_unlinked.sql`: **FLAG** any rows; confirm whether they are system-generated vs manual journals.
- `04_o2c_tax_total_variances.sql`: **FLAG** any rows; confirm rounding policy vs arithmetic defect.
- `05_o2c_ar_tieout.sql`: **FAIL** if variance is outside tolerance (target = 0.00).
- `06_o2c_idempotency_duplicates.sql`: **FAIL** if any rows return.
- `07_o2c_orphan_reservations.sql`: **FLAG** any rows; confirm whether slips are expected to exist.

## curl (GET-only)

```bash
export BASE_URL=http://localhost:8080
export TOKEN=<JWT>
export COMPANY_CODE=<COMPANY_CODE>

bash tasks/erp_logic_audit/EVIDENCE_QUERIES/task-01/curl/01_o2c_accounting_reports_gets.sh
bash tasks/erp_logic_audit/EVIDENCE_QUERIES/task-01/curl/02_o2c_dealer_portal_gets.sh
```

## Auth helpers (admin + dealer JWT)

Admin JWT:

```bash
export BASE_URL=http://localhost:8080
export COMPANY_CODE=<COMPANY_CODE>
export ADMIN_EMAIL=<ADMIN_EMAIL>
export ADMIN_PASSWORD=<ADMIN_PASSWORD>

ADMIN_TOKEN=$(curl -sS -X POST -H 'Content-Type: application/json' \
  -d "{\"email\":\"${ADMIN_EMAIL}\",\"password\":\"${ADMIN_PASSWORD}\",\"companyCode\":\"${COMPANY_CODE}\"}" \
  "${BASE_URL}/api/v1/auth/login" | jq -r '.accessToken')
```

Dealer portal JWT (create a dealer first; admin tokens are rejected for dealer portal endpoints):

```bash
DEALER_PAYLOAD=$(jq -nc \
  --arg name "Audit Dealer" \
  --arg companyName "Audit Dealer" \
  --arg contactEmail "dealer@example.com" \
  --arg contactPhone "+910000000000" \
  --arg address "Test" \
  --argjson creditLimit 0 \
  '{name:$name, companyName:$companyName, contactEmail:$contactEmail, contactPhone:$contactPhone, address:$address, creditLimit:$creditLimit}')

DEALER_RESPONSE=$(curl -sS -X POST \
  -H "Authorization: Bearer ${ADMIN_TOKEN}" \
  -H "Content-Type: application/json" \
  -H "X-Company-Id: ${COMPANY_CODE}" \
  -d "${DEALER_PAYLOAD}" \
  "${BASE_URL}/api/v1/dealers")

PORTAL_EMAIL=$(echo "${DEALER_RESPONSE}" | jq -r '.data.portalEmail')
PORTAL_PASSWORD=$(echo "${DEALER_RESPONSE}" | jq -r '.data.generatedPassword')

DEALER_TOKEN=$(curl -sS -X POST -H 'Content-Type: application/json' \
  -d "{\"email\":\"${PORTAL_EMAIL}\",\"password\":\"${PORTAL_PASSWORD}\",\"companyCode\":\"${COMPANY_CODE}\"}" \
  "${BASE_URL}/api/v1/auth/login" | jq -r '.accessToken')
```

Pass/Fail:
- Accounting reports: **PASS** if AR totals and reconciliation outputs are consistent with ledger expectations.
- Dealer portal: **PASS** if dealer sees only their own orders/invoices/ledger; **FAIL** if cross-dealer data appears.

## Targeted repro (dev-only; optional)
- Replay dispatch confirmation for the same packing slip and compare:
  - `inventory_movements` counts by `(reference_type, reference_id, movement_type)`
  - `journal_entries` by `INV-*` and `COGS-*` references
- **FAIL** if duplicates are created under the same slip/order reference.
