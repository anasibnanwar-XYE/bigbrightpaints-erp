# Cross Workflow Plan

Ticket: `TKT-ERP-STAGE-097`
Generated: `2026-02-20T10:25:46+00:00`

## In-Ticket Dependency Edges

| Upstream Slice | Upstream Agent | Downstream Slice | Downstream Agent | Contract |
| --- | --- | --- | --- | --- |
| SLICE-01 | auth-rbac-company | SLICE-02 | purchasing-invoice-p2p | tenant-scoped supplier/AP access rules |
| SLICE-01 | auth-rbac-company | SLICE-03 | reports-admin-portal | admin/report access boundaries |
| SLICE-01 | auth-rbac-company | SLICE-04 | sales-domain | tenant and role boundary contract |

## Recommended Merge Order

1. `SLICE-01` (auth-rbac-company)
2. `SLICE-02` (purchasing-invoice-p2p)
3. `SLICE-03` (reports-admin-portal)
4. `SLICE-04` (sales-domain)
5. `SLICE-05` (refactor-techdebt-gc)

## Slice Coordination Notes

### SLICE-01 (auth-rbac-company)
- Upstream slices: none
- Downstream slices: SLICE-02, SLICE-03, SLICE-04
- External upstream agents to watch: none
- External downstream agents to watch: accounting-domain, factory-production, hr-domain, inventory-domain

### SLICE-02 (purchasing-invoice-p2p)
- Upstream slices: SLICE-01
- Downstream slices: none
- External upstream agents to watch: none
- External downstream agents to watch: accounting-domain, inventory-domain

### SLICE-03 (reports-admin-portal)
- Upstream slices: SLICE-01
- Downstream slices: none
- External upstream agents to watch: none
- External downstream agents to watch: none

### SLICE-04 (sales-domain)
- Upstream slices: SLICE-01
- Downstream slices: none
- External upstream agents to watch: orchestrator-runtime
- External downstream agents to watch: accounting-domain, inventory-domain

### SLICE-05 (refactor-techdebt-gc)
- Upstream slices: none
- Downstream slices: none
- External upstream agents to watch: none
- External downstream agents to watch: none

