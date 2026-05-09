# Removed Legacy Behavior

Last reviewed: 2026-05-09

This file lists behavior frontend developers should not rebuild after the
hard-cut cleanup. The current product has one canonical path for each workflow.

## Removed Frontend Assumptions

- No `X-Company-Id` tenant shell scope. Use `companyCode` and `X-Company-Code`.
- No tenant-shell routing from numeric tenant ids except superadmin tenant path
  params under `/platform/tenants/:tenantId`.
- No secondary identity bootstrap endpoint. Use `GET /api/v1/auth/me`.
- No frontend fallback from platform superadmin routes into tenant-admin product
  routes.
- No dealer-client supplied `dealerId` for `/api/v1/dealer-portal/**`.

## Removed API And Workflow Aliases

- No alternate dispatch posting route. Use `POST /api/v1/dispatch/confirm`.
- No sales-owned dispatch confirmation action.
- No accounting-owned dispatch confirmation action.
- No manual journal alias route outside `POST /api/v1/accounting/journal-entries`.
- No reversal alias route outside
  `POST /api/v1/accounting/journal-entries/{entryId}/reverse`.
- No compatibility replay behavior for old sales-order idempotency payload
  encodings. Current requests use the current canonical signature.
- No legacy dealer/admin notification path for tenant product screens.
- No retired Tally import runtime surface in the frontend.

## Removed Documentation And Tooling Assumptions

- No retrieval-generated or endpoint-inventory docs are part of the frontend contract.
- Do not treat old inventory markdown, generated endpoint lists, or review
  scratch documents as source material.
- Use `docs/frontend/`, `docs/frontend-api/`, `docs/frontend-portals/`, and
  OpenAPI/runtime tests as the supported contract surfaces.

## Removed Compatibility UX

- Do not show UI that asks users to pick old/new behavior.
- Do not show "try legacy path" recovery buttons.
- Do not silently retry a failed mutation through an alternate route.
- Do not preserve old role names, old tenant identifiers, or old endpoint names
  in frontend state.
- Do not build screens for deprecated endpoints just because tests or old docs
  mention them.

## Current Recovery Pattern

When the backend rejects a request:

1. Keep the user on the canonical workflow.
2. Render the exact validation or state blocker from the response.
3. Use trace/correlation metadata for support.
4. Let the user correct the current request.
5. Start a new idempotent user intent only when the user explicitly retries the
   current workflow.

## Source Of Truth

- Current shared frontend API contract: `docs/frontend/api-contract.md`
- Role map: `docs/frontend/roles-and-permissions.md`
- Workflow map: `docs/frontend/workflows.md`
- Deeper portal contracts: `docs/frontend-portals/**`
