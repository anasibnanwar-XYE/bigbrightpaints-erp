# Accounting Portal Frontend Scope Guardrail

Status: mandatory, do not remove.

## Invariant

HR, PURCHASING, INVENTORY, and REPORTS come under the Accounting portal in frontend scope.

This scope is required for:
- route ownership,
- API contract coverage,
- QA test ownership,
- release sign-off readiness.

## Change-Control Rule

Do not remove, split, or move these domains out of Accounting portal scope unless the same commit includes all items below:
1. Updated portal endpoint map and frontend handoff docs for every affected portal.
2. Updated `docs/endpoint-inventory.md` module mapping and examples.
3. An `asyncloop` entry with rationale, impact, and verification plan.
