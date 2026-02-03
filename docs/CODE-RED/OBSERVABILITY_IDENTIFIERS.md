# Observability Identifiers Contract (CODE-RED)

Last updated: 2026-02-03

Goal: enterprise-grade traceability across modules (sales ↔ inventory ↔ invoice ↔ accounting ↔ payroll ↔ orchestrator)
without “identifier soup” (traceId/correlationId/requestId/idempotencyKey/referenceNumber all mixed).

This is CODE-RED “centralization”: define the canonical identifiers and propagation rules first, then implement safely.

---

## 1) Canonical Identifiers (Definitions)

### `requestId`
- Meaning: one HTTP ingress request (single hop).
- Source: `X-Request-Id` header (preferred) or generated on ingress if missing.
- Usage: logs, error responses, audit rows; helps correlate retries from clients/load balancers.

### `traceId`
- Meaning: end-to-end business flow (can span multiple requests/events).
- Source: `X-Trace-Id` header or generated when a business workflow begins (orchestrator commands always have one).
- Usage: trace/audit/outbox, cross-module incident response.

### `correlationId`
- Meaning: one logical “unit of work” across fan-out/fan-in (often per transaction or per outbox batch).
- Source: derived from `traceId` + event id, or explicitly created when needed.
- Usage: event processing correlation, consumer idempotency tracking.

### `idempotencyKey`
- Meaning: exactly-once scope for a **write** request (double-click/network/orchestrator retries).
- Source: `Idempotency-Key` header or an explicit field in request DTO; must be stable for retries.
- Rule: mismatch-safe (same key, different payload → conflict 409).

### `referenceNumber`
- Meaning: business document reference (journal reference, invoice number, slip number, etc.).
- Rule: must not be treated as idempotency unless explicitly documented (and mismatch-safe checks exist).

---

## 2) Header Contract (Enterprise Defaults)

Recommended standard headers
- `X-Request-Id`: optional; echoed back in response headers.
- `X-Trace-Id`: optional for most endpoints; required for orchestrator write commands; echoed back in response headers.
- `Idempotency-Key`: required for inventory/accounting/payroll/manufacturing write actions (CODE-RED list lives in plan-v2).
- `X-Company-Code`: tenant context selector (see `docs/CODE-RED/IDENTITY_AND_NAMING.md`).

---

## 3) Propagation Rules (Do/Don’t)

Ingress (HTTP)
- Always normalize identifiers at ingress and attach them to:
  - request attributes (server-side)
  - MDC/log context
  - response headers (echo)

Async (Outbox)
- Every outbox row must be queryable by identifiers without parsing payload:
  - `companyId` (numeric)
  - `traceId`
  - `correlationId`
  - `idempotencyKey` (if applicable)
  - `entityType` + `entityId` (when known)

Audit
- Every privileged write must capture:
  - who (`userId`/username)
  - tenant (`companyId` and `companyCode` if relevant)
  - what (`eventType`/action)
  - identifiers (`requestId`, `traceId`, `idempotencyKey`, `referenceNumber` when applicable)
  - outcome (`SUCCESS`/`FAILED`) + error code

Error handling
- Never generate a new `traceId` for an error response if one already exists for the request; reuse the ingress identifiers.

Accounting event store
- Do not generate a brand-new `correlationId` detached from trace/request context if the event is caused by a request.
- Accept context identifiers (or derive them deterministically) so accounting audit rows can be linked to business traces.

---

## 4) Known Smells / Risks (Why This Matters)

- Orchestrator uses `traceId`, but other modules have no guaranteed trace context; audit may look for `X-Trace-Id` but nothing sets it.
- Exception handling can emit a fresh traceId unrelated to the request, breaking cross-service debugging.
- Accounting event store uses its own `correlationId` UUID, preventing easy linkage to orchestrator traces.
- Some APIs treat `referenceNumber` as idempotency (or fall back between them), which is ambiguous and can hide conflicts.

---

## 5) CODE-RED Implementation Tasks (Docs-Only)

P0 (observability correctness)
1) Add an ingress context initializer (filter/interceptor):
   - normalize `requestId` + `traceId` from headers or generate
   - bind to MDC + request attributes + response headers
2) Standardize error responses:
   - reuse the request’s `requestId/traceId` if present
3) Standardize outbox envelope:
   - store identifier columns (do not rely on payload-only)
4) Standardize accounting event identifiers:
   - accept/propagate `traceId/correlationId` from context (do not always generate new UUIDs)
5) Clarify API contracts:
   - document `referenceNumber` vs `idempotencyKey` (no fallback unless explicitly designed + mismatch-safe)

P1 (cleanup)
- Update internal services to always use `ObservabilityContext` instead of ad-hoc header reads.

---

## 6) Acceptance Criteria

- Given any journal entry reference, we can find: the triggering requestId, traceId, actor, and outbox events without parsing payload JSON.
- Retried writes produce the same identifiers and do not create duplicate side effects.
- Incident response can correlate: HTTP request → orchestrator trace → accounting journal(s) → ledger changes.

