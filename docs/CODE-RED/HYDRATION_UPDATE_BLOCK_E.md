# Hydration Update (Block E)

## 2026-02-04
- Scope: Block E (P0/P1) Inventory safety — inventory adjustments idempotency + business-date correctness (commit 1).
- Changes:
  - Inventory adjustments require `Idempotency-Key` and enforce mismatch-safe replay.
  - Adjustment journals post with `adjustmentDate` (no default-to-today drift).
  - Predeploy scan added to detect adjustment/journal date mismatches.
- Tests:
  - `mvn -B -ntp -Dtest=CR_INV_AdjustmentIdempotencyTest,InventoryGlReconciliationIT,HighImpactRegressionIT test`
  - `bash scripts/verify_local.sh`
- Notes:
  - Full `verify_local` gate passed (schema/flyway/time scans + mvn verify).
