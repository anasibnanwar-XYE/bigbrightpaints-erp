# Task 01 — Pre‑Deploy Blockers Remediation (Inventory / Dispatch / Reservations / Integration / Security / Timezone)

This task turns the read‑only review findings into an actionable, test‑backed remediation plan.

## Executive Risk Summary (deployment decision)
- Overall assessment: **RED** (deploy blocked until EPIC 01–04 Milestones are DONE and validated).
- Primary blast radius: inventory valuation + COGS, fulfillment state integrity, and post‑dispatch correctness (incl. accounting).

## Work Scheduler (always-on)
- If async verify is RUNNING: pick the next independent milestone and proceed (tests first, then minimal fixes).
- If async verify FAILED: follow the failure triage discipline (first failing test + stack trace, inspect Surefire, classify, minimal fix).
- If blocked: record blocker + evidence needed in `HYDRATION.md`, then immediately switch to another independent milestone.

## Execution Contract (non‑negotiable for any agent)
- Start (and stay) on branch: `predeploy-blockers-v1` (create if missing). Never work on `main` directly.
- No new business features, no new endpoints; only correctness, stability, security, and documentation.
- Every fix must have:
  - a targeted regression test (or a strong justification if not testable),
  - and the CI gate run: `cd erp-domain && mvn -B -ntp verify`.
- Prefer “fail closed” over silently clamping/forgiving data drift.

## Common Commands (repo-specific)
- Type-check (compile): `cd erp-domain && mvn -B -ntp -DskipTests compile`
- Targeted tests: `cd erp-domain && mvn -B -ntp -Dtest=ClassName1,ClassName2 test`
- Full suite gate (CI entrypoint): `cd erp-domain && mvn -B -ntp verify`
- Async full suite gate (Codex Cloud pattern):
  - `nohup bash -lc 'cd erp-domain && mvn -B -ntp verify' > /tmp/task01-verify.log 2>&1 & echo $! > /tmp/task01-verify.pid`
  - `tail -n 160 /tmp/task01-verify.log`

---

## EPIC 00 — Baseline + Guardrails + Evidence Capture
Objective:
- Establish a reproducible baseline and convert “likely” issues into deterministic failing tests.

Scope:
- CI entrypoint: `cd erp-domain && mvn -B -ntp verify`
- Primary fix surface: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/FinishedGoodsService.java`

Milestones:

Milestone 00 — Baseline verify (async-friendly)
- Implementation steps:
  - Run full suite gate (async if needed) and capture results into `HYDRATION.md`.
- Validate steps:
  - `cd erp-domain && mvn -B -ntp verify`
- Full suite gate:
  - Pass means: 0 failing tests; JaCoCo gates satisfied.

Milestone 01 — Add regression tests for critical findings (tests first)
- What to read (start here):
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/FinishedGoodsService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/FinishedGoodBatchRepository.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/service/IntegrationCoordinator.java`
- Tests to add (names are suggestions; follow existing test patterns):
  - `FinishedGoodsServiceTest#dispatchUsesWacIncludingReserved`
  - `FinishedGoodsServiceTest#reserveDoesNotOverrideTerminalStatus`
  - `FinishedGoodsServiceTest#cancelBackorderClearsReservation`
  - `FinishedGoodsServiceTest#previewUsesReservedForOrder`
  - `IntegrationCoordinatorTest#acceptsDispatchStatus`
- Verify steps:
  - Each test must fail on current `main` behavior and pass only after the corresponding fix.
- Validate steps:
  - `cd erp-domain && mvn -B -ntp -Dtest=FinishedGoodsServiceTest,IntegrationCoordinatorTest test`

Milestone 02 — Re-check “auto-edited” files for semantic drift (pre-deploy hygiene)
- Files to review (do not “format fix”; confirm behavior is unchanged):
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/audit/AuditService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/event/AccountingEventStore.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/service/CommandDispatcher.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/controller/OrchestratorController.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/sales/service/DealerService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/SecurityMonitoringService.java`
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/service/TraceService.java`
- Validate steps:
  - No behavioral changes introduced by prior mechanical edits.

---

## EPIC 01 — WAC / COGS Correctness When Stock Is Reserved (**deploy blocker**)
Symptom:
- WAC can become `0` when all stock is reserved, causing dispatch COGS/inventory relief to be `0`.

Root cause:
- WAC uses `FinishedGoodBatch.quantityAvailable` (unreserved) instead of on-hand (available + reserved).
  - Evidence: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/domain/FinishedGoodBatchRepository.java`
  - WAC use-site: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/FinishedGoodsService.java` (`weightedAverageCost`, `resolveDispatchUnitCost`)

Milestones:

Milestone 01 — Compute WAC using on-hand quantity (batch `quantityTotal`)
- Minimal fix:
  - Update WAC query to weight by on-hand (`quantityTotal`) instead of `quantityAvailable`.
  - Keep the `> 0` guard to avoid divide-by-zero.
- Verify steps:
  - If all stock is reserved (`quantityAvailable == 0` but `quantityTotal > 0`), WAC must remain non-zero.
- Validate steps:
  - `cd erp-domain && mvn -B -ntp -Dtest=FinishedGoodsServiceTest#dispatchUsesWacIncludingReserved test`

Milestone 02 — Fail closed on zero/invalid dispatch cost
- Minimal fix:
  - If shipped quantity > 0 and resolved unit cost is `0` while on-hand exists, reject dispatch (clear error) rather than silently posting `0` COGS.
- Verify steps:
  - Prevent new `InventoryMovement` rows of type `DISPATCH` with unitCost `0` unless the FG truly has zero-cost valuation.

---

## EPIC 02 — Reservation / Packaging Slip State Machine Integrity (**deploy blocker**)
Symptoms:
- `reserveForOrder(...)` can select the “most recent” slip and overwrite terminal states (e.g., `DISPATCHED`, `BACKORDER`) back to `RESERVED` / `PENDING_PRODUCTION`.
- Backorder cancellation releases stock totals but does not cancel/update `InventoryReservation` rows, enabling double-release later.

Evidence:
- Slip selection + status overwrite:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/FinishedGoodsService.java` (`reserveForOrder`, `selectMostRecentSlip`, `updateSlipStatusBasedOnAvailability`)
- Backorder cancel:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/FinishedGoodsService.java` (`cancelBackorderSlip`)

Milestones:

Milestone 01 — Define and enforce monotonic slip transitions
- Minimal fix:
  - Explicitly treat `DISPATCHED`, `CANCELLED` as terminal; `BACKORDER` as special (do not “re-reserve” it).
  - In `reserveForOrder(...)`, choose an eligible slip (e.g., `PENDING`, `RESERVED`, `PENDING_PRODUCTION`) and ignore terminal ones; if none exist, create a new slip.
  - Prevent `updateSlipStatusBasedOnAvailability(...)` from overwriting terminal/special statuses.
- Validate steps:
  - `cd erp-domain && mvn -B -ntp -Dtest=FinishedGoodsServiceTest#reserveDoesNotOverrideTerminalStatus test`

Milestone 02 — Make backorder cancellation update reservation rows (and reconcile totals)
- Minimal fix:
  - When cancelling a `BACKORDER` slip, cancel/release the corresponding `InventoryReservation` rows (order-scoped) and reconcile:
    - `FinishedGood.reservedStock`
    - `FinishedGoodBatch.quantityAvailable`
  - Prefer a single reconciliation path (avoid duplicating release logic in multiple methods).
- Validate steps:
  - `cd erp-domain && mvn -B -ntp -Dtest=FinishedGoodsServiceTest#cancelBackorderClearsReservation test`

Milestone 03 — Backorder creation/reservations consistency
- Minimal fix options (pick one and document):
  1) Backorder slip is purely “paper”; reservations stay on the order, and cancel/re-reserve flows reconcile them safely.
  2) Backorder slip owns reservations; create/update reservation rows explicitly for backorder quantities.
- Verify steps:
  - No state where batch availability and reservation rows diverge after: reserve → partial dispatch → cancel backorder → reserve again.

---

## EPIC 03 — Dispatch Preview Correctness for Reserved Slips (**deploy blocker**)
Symptom:
- Dispatch preview can show `available=0` and `hasShortage=true` even when the slip has reserved stock.

Root cause:
- Preview uses `FinishedGoodBatch.quantityAvailable` (unreserved), ignoring reservations allocated to this slip/order.
  - Evidence: `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/FinishedGoodsService.java` (`getDispatchPreview`)

Milestones:

Milestone 01 — Compute “available to ship” including this order’s reservation
- Minimal fix:
  - Compute availability from reservation rows for the slip/order (preferred), or compute effective availability as `batch.quantityAvailable + reservedForThisOrder`.
  - Ensure `suggestedShip` and `hasShortage` reflect the slip’s reserved allocation.
- Validate steps:
  - `cd erp-domain && mvn -B -ntp -Dtest=FinishedGoodsServiceTest#previewUsesReservedForOrder test`

---

## EPIC 04 — Orchestrator Dispatch Status Compatibility (**deploy blocker if enabled**)
Symptom:
- Orchestrator deprecated dispatch sends `DISPATCHED`, but integration coordinator rejects it.

Evidence:
- Sender: `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/controller/OrchestratorController.java`
- Receiver: `erp-domain/src/main/java/com/bigbrightpaints/erp/orchestrator/service/IntegrationCoordinator.java` (`updateFulfillment`)

Milestones:

Milestone 01 — Map or accept `DISPATCHED`
- Minimal fix options:
  1) Change orchestrator to send `SHIPPED` (or `READY_TO_SHIP`) instead of `DISPATCHED`.
  2) Update `IntegrationCoordinator.updateFulfillment(...)` to accept `DISPATCHED` as an alias (map to `SHIPPED`).
- Validate steps:
  - `cd erp-domain && mvn -B -ntp -Dtest=IntegrationCoordinatorTest#acceptsDispatchStatus test`

---

## EPIC 05 — Security: Refresh Token Persistence + Revocation TTL Alignment (high risk)
Symptoms:
- Refresh tokens are in-memory (restart/multi-instance breaks refresh): `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/auth/service/RefreshTokenService.java`
- Revocation retention mismatch: user revocations deleted after 24h, refresh TTL defaults 30d:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/TokenBlacklistService.java` (`cleanupExpiredTokens`)
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/core/security/JwtProperties.java` (`refreshTokenTtlSeconds`)

Milestones:

Milestone 01 — Persist refresh tokens (DB/Redis) with TTL
- Minimal fix:
  - Replace in-memory map with persistent store keyed by token/jti, including expiry and user identity.
- Verify steps:
  - Refresh succeeds after service restart and in multi-node deployments.

Milestone 02 — Retain revocations until refresh expiry (or revoke refresh tokens explicitly)
- Minimal fix options:
  1) Keep user revocations until `now >= refresh_expiry` (use configured TTL).
  2) On “revoke all user tokens”, revoke refresh tokens directly (persisted store) instead of relying on short-lived revocation rows.
- Verify steps:
  - A revoked refresh token remains invalid until it expires.

Milestone 03 — Ensure admin actions revoke refresh tokens too
- Evidence:
  - `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/admin/service/AdminUserService.java` calls `tokenBlacklistService.revokeAllUserTokens(...)` (access-token focused).
- Minimal fix:
  - Expand revocation flow to also revoke refresh tokens for the user.

---

## EPIC 06 — Timezone Correctness for Dates (high risk)
Symptom:
- Multiple modules use `LocalDate.now()` (server timezone), risking period/date drift (UTC vs company TZ).

Evidence (examples; inventory all call sites):
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/DealerLedgerEntry.java` (`@PrePersist`)
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/domain/Invoice.java` (`issueDate` default)
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AgingReportService.java`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/AuditDigestScheduler.java`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/event/AccountingEventStore.java`
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/event/InventoryAccountingEventListener.java`

Milestones:

Milestone 01 — Centralize company “business date” provider
- Minimal fix:
  - Add a single provider (e.g., `CompanyClock` / `BusinessDateService`) that computes `LocalDate` in company TZ.
  - Stop using `LocalDate.now()` in posting-critical paths; set dates at service boundaries.

Milestone 02 — Add regression tests around day-boundary
- Verify steps:
  - For a company in UTC+X with server in UTC, postings near midnight land on the correct company date.

---

## EPIC 07 — Inventory Dispatch Safety (clamps hide drift) (high risk)
Symptom:
- Batch totals are clamped with `.max(0)` without validating batch-level sufficiency, which can hide inventory drift.

Evidence:
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/inventory/service/FinishedGoodsService.java` (dispatch confirmation updates)

Milestones:

Milestone 01 — Validate sufficiency; do not clamp silently
- Minimal fix:
  - If shipping would make `batch.quantityTotal` or `finishedGood.reservedStock` negative beyond tolerance, reject dispatch with a clear error.
- Verify steps:
  - Data drift is detected early; operators get a concrete error instead of masked inventory corruption.

---

## EPIC 08 — Accounting Precision (medium)
Symptom:
- `JournalEntry.foreignAmountTotal` is `Double` (precision loss risk).

Evidence:
- `erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/domain/JournalEntry.java`

Milestones:

Milestone 01 — Convert to `BigDecimal` (with migration)
- Minimal fix:
  - Replace `Double` with `BigDecimal` with explicit scale/rounding; migrate existing data.

---

## EPIC 09 — Prod Mode Detection Correctness (medium)
Symptom:
- Production-mode detection is wrong for comma-separated profiles (may expose details).

Evidence:
- `erp-domain/src/main/java/com/bigbrightpaints/erp/core/exception/GlobalExceptionHandler.java` (`activeProfile`)

Milestones:

Milestone 01 — Parse active profiles list
- Minimal fix:
  - Treat `spring.profiles.active` as a list (split by comma) and match `prod|production` within it.

---

## “Must verify before deploy” checklist (manual + automated)
- [ ] Run CI gate: `cd erp-domain && mvn -B -ntp verify`
- [ ] Walk an end-to-end dispatch with backorder + cancel-backorder:
  - reserve → partial dispatch → confirm reservations/batch totals → cancel backorder slip → ensure reservations cleared → re-reserve/dispatch again
- [ ] Verify COGS with reserved stock (WAC cannot drop to 0 when inventory exists)
- [ ] Verify dispatch preview reflects reserved quantities (no false shortage)
- [ ] Verify orchestrator dispatch status mapping end-to-end (if orchestrator is enabled)

