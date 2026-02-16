# Cross Workflow Plan

Ticket: `TKT-ERP-STAGE-007`
Generated: `2026-02-16T20:16:31+00:00`

## In-Ticket Dependency Edges

| Upstream Slice | Upstream Agent | Downstream Slice | Downstream Agent | Contract |
| --- | --- | --- | --- | --- |
| SLICE-02 | purchasing-invoice-p2p | SLICE-01 | accounting-domain | ap/posting and settlement linkage |

## Recommended Merge Order

1. `SLICE-02` (purchasing-invoice-p2p)
2. `SLICE-01` (accounting-domain)

## Slice Coordination Notes

### SLICE-02 (purchasing-invoice-p2p)
- Upstream slices: none
- Downstream slices: SLICE-01
- External upstream agents to watch: auth-rbac-company
- External downstream agents to watch: inventory-domain

### SLICE-01 (accounting-domain)
- Upstream slices: SLICE-02
- Downstream slices: none
- External upstream agents to watch: auth-rbac-company, factory-production, hr-domain, orchestrator-runtime, sales-domain
- External downstream agents to watch: none

