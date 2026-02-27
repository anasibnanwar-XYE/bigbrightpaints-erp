# Cross Workflow Plan

## Canonical Flow (O2C)

1. Sales creates and confirms order (proforma intent).
2. Inventory reservation and shortage routing happen through canonical services.
3. Factory packaging slip lifecycle progresses to dispatch-ready.
4. Factory dispatch confirms shipped payload (pricing/quantity overrides allowed under existing governance).
5. Dispatch confirmation emits final invoice + AR/COGS postings and dealer ledger effects.

## Role Boundaries

- Sales:
  - dealer CRUD + lookup
  - order/proforma lifecycle
  - promotion lifecycle
  - credit increase request lifecycle (request/update/list)
  - progress visibility across packaging/dispatch outcome
- Factory:
  - dispatch finalization and dispatch-level payload control
- Accounting/Admin:
  - credit approvals
  - final financial oversight

## Immediate Risk Controls

- Keep canonical dispatch implementation path unchanged in service layer.
- Restrict only controller-level role exposure for dispatch-final actions.
- Add regression test proving credit approval mutates dealer credit limit.
