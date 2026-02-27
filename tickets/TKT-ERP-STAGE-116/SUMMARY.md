# Ticket TKT-ERP-STAGE-116

- title: Sales portal scope hardening and canonical O2C workflow cleanup
- goal: Reduce sales-surface complexity to a strict role-scoped portal, preserve canonical dispatch/accounting integrity, and deliver production-grade contracts for dashboard, dealer credit lifecycle, and order-to-dispatch progress visibility.
- priority: high
- status: in_review
- base_branch: harness-engineering-orchestrator
- created_at: 2026-02-28T00:00:00Z
- updated_at: 2026-02-28T20:30:00Z

## Scope Anchors

- Keep: sales dashboard, dealer management, order entry, promotion management, credit request submission and review visibility.
- Enforce: dispatch finalization is factory-driven; sales can track progress but not perform dispatch-final actions.
- Enforce: approved dealer credit increase requests mutate dealer credit limits.
- Enforce: sales product selection uses catalog views, not inventory internals.

## Slice Board

| Slice | Owner | Status | Notes |
| --- | --- | --- | --- |
| S1 | backend | completed | `GET /api/v1/sales/dashboard` + bucketed order metrics |
| S2 | backend | completed | credit approval now increases dealer credit limit with fail-closed guards |
| S3 | docs + qa | in_progress | scope docs + targeted regression and QA review |

## Implemented Surface

- Sales dashboard endpoint:
  - `GET /api/v1/sales/dashboard`
  - response contract: `SalesDashboardDto(activeDealers,totalOrders,orderStatusBuckets,pendingCreditRequests)`
- Dispatch role boundary:
  - `POST /api/v1/sales/dispatch/confirm` now restricted to `ROLE_FACTORY` or `ROLE_ADMIN` with `dispatch.confirm`
- Credit approval mutation:
  - approving pending credit request now locks dealer row and increments `dealer.creditLimit`
  - audit metadata includes `oldLimit`, `newLimit`, `increment`
- Promotion media:
  - optional `imageUrl` in promotion request/response/domain
  - Flyway v2 migration adds `promotions.image_url`
