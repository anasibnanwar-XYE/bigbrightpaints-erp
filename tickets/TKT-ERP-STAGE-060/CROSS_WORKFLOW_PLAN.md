# Cross Workflow Plan

Ticket: `TKT-ERP-STAGE-060`
Generated: `2026-02-17T17:39:41+00:00`

## In-Ticket Dependency Edges

- none

## Recommended Merge Order

1. `SLICE-01` (auth-rbac-company)
2. `SLICE-02` (release-ops)
3. `SLICE-03` (frontend-documentation)
4. `SLICE-04` (refactor-techdebt-gc)
5. `SLICE-05` (repo-cartographer)

## Slice Coordination Notes

### SLICE-01 (auth-rbac-company)
- Upstream slices: none
- Downstream slices: none
- External upstream agents to watch: none
- External downstream agents to watch: accounting-domain, factory-production, hr-domain, inventory-domain, purchasing-invoice-p2p, reports-admin-portal, sales-domain

### SLICE-02 (release-ops)
- Upstream slices: none
- Downstream slices: none
- External upstream agents to watch: data-migration
- External downstream agents to watch: none

### SLICE-03 (frontend-documentation)
- Upstream slices: none
- Downstream slices: none
- External upstream agents to watch: none
- External downstream agents to watch: none

### SLICE-04 (refactor-techdebt-gc)
- Upstream slices: none
- Downstream slices: none
- External upstream agents to watch: none
- External downstream agents to watch: none

### SLICE-05 (repo-cartographer)
- Upstream slices: none
- Downstream slices: none
- External upstream agents to watch: none
- External downstream agents to watch: none

