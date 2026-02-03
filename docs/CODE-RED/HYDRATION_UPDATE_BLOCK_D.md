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

- Scope: Block D (P0) Dealer AR safety — dealer settlements idempotency + allocation uniqueness (commit 2).
- Changes:
  - Dealer settlement now reserves idempotency key + canonical reference before posting; mismatch-safe validation enforced.
  - Settlement journal lines are built centrally; invoice settlement reference uses canonical reference.
  - Allocation validation fails early for missing invoice/purchase mismatches.
- Tests:
  - `mvn -B -ntp -Dtest=CR_DealerReceiptSettlementAuditTrailTest,AccountingServiceTest test`
  - `bash scripts/verify_local.sh`
- Notes:
  - Full `verify_local` gate passed (schema/flyway/time scans + mvn verify).
