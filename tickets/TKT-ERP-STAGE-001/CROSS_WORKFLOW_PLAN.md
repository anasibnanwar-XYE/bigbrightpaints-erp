# Cross Workflow Plan

Ticket: `TKT-ERP-STAGE-001`
Generated: `2026-02-16T08:44:26+00:00`

## In-Ticket Dependency Edges

| Upstream Slice | Upstream Agent | Downstream Slice | Downstream Agent | Contract |
| --- | --- | --- | --- | --- |
| SLICE-01 | auth-rbac-company | SLICE-02 | sales-domain | tenant and role boundary contract |

## Recommended Merge Order

1. `SLICE-01` (auth-rbac-company)
2. `SLICE-02` (sales-domain)

## Slice Coordination Notes

### SLICE-01 (auth-rbac-company)
- Upstream slices: none
- Downstream slices: SLICE-02
- External upstream agents to watch: none
- External downstream agents to watch: accounting-domain, factory-production, hr-domain, inventory-domain, purchasing-invoice-p2p, reports-admin-portal

### SLICE-02 (sales-domain)
- Upstream slices: SLICE-01
- Downstream slices: none
- External upstream agents to watch: orchestrator-runtime
- External downstream agents to watch: accounting-domain, inventory-domain

