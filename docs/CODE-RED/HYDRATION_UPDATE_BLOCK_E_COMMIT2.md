# Hydration Update (Block E - Commit 2)

## 2026-02-04
- Scope: Opening stock import idempotency + manual raw material intake gating/idempotency (Block E, commit 2).
- Changes:
  - Opening stock import now requires idempotency (defaults to file hash), mismatch-safe replay, and posts via `AccountingFacade`; prod-gated.
  - Manual raw material intake is disabled by default; when enabled, requires `Idempotency-Key` and writes an audit envelope.
  - Added idempotency tables for opening stock imports and manual intake requests.
- Tests:
  - `mvn -B -ntp -Dtest=ProcureToPayE2ETest#rawMaterialIntakeDisabledByDefault test`
  - `bash scripts/verify_local.sh`
- Notes:
  - Full `verify_local` gate passed (schema/flyway/time scans + mvn verify).
