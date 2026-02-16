# Cross Workflow Plan

Ticket: `TKT-ERP-STAGE-005`
Generated: `2026-02-16T18:27:21+00:00`

## In-Ticket Dependency Edges

- `SLICE-01` -> `SLICE-02`: tenant lifecycle status consumed by runtime company-context enforcement

## Recommended Merge Order

1. `SLICE-01` (auth-rbac-company)
2. `SLICE-02` (refactor-techdebt-gc)

## Slice Coordination Notes

### SLICE-01 (auth-rbac-company)
- Upstream slices: none
- Downstream slices: SLICE-02
- External upstream agents to watch: none
- External downstream agents to watch: accounting-domain, factory-production, hr-domain, inventory-domain, purchasing-invoice-p2p, reports-admin-portal, sales-domain

### SLICE-02 (refactor-techdebt-gc)
- Upstream slices: SLICE-01
- Downstream slices: none
- External upstream agents to watch: none
- External downstream agents to watch: none
