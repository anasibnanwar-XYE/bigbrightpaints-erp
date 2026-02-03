# Identity & Naming Contract (CODE-RED)

Last updated: 2026-02-03

Goal: remove “domain vocabulary” code smell where the same concept is named differently (or the same name means different
things). This is critical for tenant isolation, auditability, and preventing future security bugs.

This contract is **docs-first** in CODE-RED: implement changes only after the contract is agreed and tests are defined.

---

## 1) Canonical Identity Terms (Do/Don’t)

### Company (Tenant) Identifiers

Canonical definitions
- `companyId` (type: `Long`)
  - Meaning: the internal DB primary key (`companies.id`)
  - Where it appears: DB `company_id` foreign keys, internal service/entity relationships, internal admin tooling.
- `companyPublicId` (type: `UUID`)
  - Meaning: external stable identifier (`companies.public_id`) if/when we expose it broadly.
- `companyCode` (type: `String`)
  - Meaning: the external tenant key (`companies.code`) used for selection and context routing.
  - Used in: authentication flows, multi-company switching, request context, human-facing references.

Hard rule (CODE-RED)
- Never store `companyCode` in a variable/property named `companyId`.
- Never accept a caller-controlled “company selector” unless it is validated against authenticated membership.

Current code smell to eliminate (examples)
- Thread-local context is named `CompanyContextHolder.setCompanyId(...)` but holds `companyCode` (string).
- Header `X-Company-Id` and JWT claim `cid` actually carry a company *code* string, not the numeric DB id.
- API DTOs mix `companyCode` and `companyId` labels for the same value (e.g., `/auth/me`).

---

## 2) Canonical Request Context (Headers + JWT)

### Headers

Target (canonical)
- `X-Company-Code`: the company code string (tenant key).
- `Idempotency-Key`: required on write endpoints that mutate inventory/ledger/payment/allocations.
- `X-Request-Id`: optional request identifier (ingress).
- `X-Trace-Id`: optional end-to-end trace identifier (business flow).

Backwards compatibility (deprecation window)
- If `X-Company-Id` is present, it must be treated as an alias for `X-Company-Code` **only** while we deprecate it.
- If both are present, they must match (fail closed on mismatch).

### JWT Claims

Target (canonical)
- `companyCode`: the company code string (tenant key).

Backwards compatibility (deprecation window)
- Accept `cid` as legacy alias for `companyCode` while clients migrate.

---

## 3) API Field Naming Rules

Hard rules
- If a field contains a company **code string**, it must be named `companyCode`.
- If a field contains a numeric DB identifier, it must be named `companyId`.
- If a field contains an external UUID, it must be named `companyPublicId`.

Known mismatches to fix (plan items)
- Auth DTOs: `/auth/me` currently returns `companyId` but it is a code string; rename to `companyCode` in OpenAPI + DTO.
- Orchestrator controllers/services use `requireCompanyId()` returning a code string; rename to `requireCompanyCode()`.
- `JwtTokenService.generateAccessToken(subject, companyId, ...)` uses a misleading param name; it is a company code string.

---

## 4) Implementation Plan (Docs-Only Tasks)

P0 (security/clarity)
1) Align naming in auth + security context:
   - rename context holder APIs to `*CompanyCode*` (keep deprecated alias for compatibility during migration).
   - align JWT claim naming (`companyCode`), accept legacy `cid`.
   - document/introduce `X-Company-Code` header, accept legacy `X-Company-Id`.
2) Align orchestrator parameter naming:
   - `companyId` string parameters become `companyCode`.
3) Align OpenAPI schema + DTOs:
   - `MeResponse.companyId` → `companyCode` (and update any other mismatches).
4) Add guardrails:
   - add a lightweight lint/static check (or CI grep rule) preventing new usage of `cid`, `tenantId`, or `companyId` for code strings in `core.security`, `auth`, and `orchestrator`.

P1 (cleanup)
- Migrate remaining DTOs and internal naming to match the contract; remove deprecated aliases after the deprecation window.

---

## 5) Acceptance Criteria

- It is impossible to confuse “company code” with “company id” by reading code or OpenAPI.
- Every “company selector” value is validated against authenticated membership (fail closed).
- New code cannot introduce `companyId` variables that store `companyCode` strings (guardrail enforced).

