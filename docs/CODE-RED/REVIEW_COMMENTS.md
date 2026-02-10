# CODE-RED Review Comments Ledger

Purpose: track open review findings and questions that remain after code reviews. This is a working ledger for
follow-up work; do not use it to record final policy decisions (use `docs/CODE-RED/decision-log.md` for that).

## How To Use (Agents)
1. Read this file at the start of each CODE-RED bundle to pick up unresolved risks/questions.
2. Append a new entry for each review bundle you complete.
3. For each item, include:
   - Severity (`HIGH`, `MEDIUM`, `LOW`)
   - Impact summary
   - Code anchor (`path:line`) or doc anchor
   - Required evidence or test
   - Owner / next action
4. When an item is resolved, mark it `CLOSED` with the commit hash or doc update that fixed it.

---

## 2026-02-04 — EPIC 04 Payroll Safety (Run → Post → Pay)
Status: CLOSED (2026-02-05; decisions recorded in `docs/CODE-RED/decision-log.md`)
Owner: CODE-RED execution agents

### Findings (CLOSED)
- HIGH: Unique index on payroll run identity can fail migration if duplicates already exist for the same
  `(company_id, run_type, period_start, period_end)`. This is a deploy blocker by design.
  Anchor: `erp-domain/src/main/resources/db/migration/V125__payroll_run_identity_unique.sql:1`
  Evidence: `scripts/db_predeploy_scans.sql` scan #6 must return zero rows; do not auto-delete duplicates in migration.
  Closed (2026-02-05): Decision recorded in `docs/CODE-RED/decision-log.md` (Payroll Run Identity Uniqueness Is Deploy-Blocking).
- MEDIUM: Payroll payment recording now enforces `SALARY-PAYABLE` on the posting journal and requires the payment
  amount to match the payable amount exactly. Partial payroll payments are not supported.
  Anchor: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AccountingService.java:1032`
  Closed (2026-02-05): Decision recorded in `docs/CODE-RED/decision-log.md` (Payroll Payments Clear Salary Payable)
  and deploy readiness captured in `docs/CODE-RED/P0_DEPLOY_BLOCKERS.md` (SALARY-PAYABLE required).
- MEDIUM: Legacy HR payroll run creation endpoint now returns HTTP 410 with canonical path; any client still
  calling `/api/v1/hr/payroll-runs` will fail hard.
  Anchors:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/controller/HrController.java:132`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/service/HrService.java:197`
  Closed (2026-02-05): Decision recorded in `docs/CODE-RED/decision-log.md` (Legacy HR Payroll Run Creation Is Hard-Gated).
- LOW: Payroll run idempotency signature includes `remarks`. Retries that change remarks will 409; this can
  surprise clients that inject timestamps or different comments on retry.
  Anchor: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/service/PayrollService.java:96`
  Closed (2026-02-05): Decision recorded in `docs/CODE-RED/decision-log.md` (Payroll Run Idempotency Treats Remarks As Material).
- LOW: `markAsPaid` skips already-PAID lines, so a retry cannot overwrite a bad `paymentReference`. This is
  consistent with idempotency but is a behavioral change.
  Anchor: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/hr/service/PayrollService.java:533`
  Closed (2026-02-05): Decision recorded in `docs/CODE-RED/decision-log.md` (Payroll Mark-Paid Is Immutable On Retries).

### Open Questions (Answered)
- Do we support partial payroll payments? Answer: No. Payroll payments are liability clearing and must fully
  clear SALARY-PAYABLE; no partial payments under CODE-RED. See `docs/CODE-RED/decision-log.md`.
- Are all tenants standardized on `SALARY-PAYABLE`? Answer: Not guaranteed; it is now required configuration.
  If a tenant used a different payable account, create SALARY-PAYABLE or add a mapping before deploy.
  See `docs/CODE-RED/P0_DEPLOY_BLOCKERS.md`.
- Is it acceptable to hard-gate legacy payroll run creation in all environments? Answer: Yes in prod; if a
  migration window is required, use feature-flagged aliasing in non-prod only. See `docs/CODE-RED/decision-log.md`.
