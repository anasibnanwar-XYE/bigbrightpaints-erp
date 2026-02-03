# Hydration Update (Block D)

## 2026-02-04
- Scope: Block D (P0) Dealer AR safety — dealer receipts idempotency (commit 1).
- Changes:
  - Dealer receipt + hybrid receipt now reserve idempotency key before posting; mismatch-safe idempotency enforced.
  - Receipt allocations keyed by idempotency key (not journal reference) with deterministic canonical references.
  - Idempotency key accepted via request body or `Idempotency-Key` header on receipt endpoints.
- Tests:
  - `mvn -B -ntp -Dtest=CR_DealerReceiptSettlementAuditTrailTest test`
  - `bash scripts/verify_local.sh`
- Notes:
  - Full `verify_local` gate passed (schema/flyway/time scans + mvn verify).
