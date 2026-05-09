# Accounting Portal Scope Guardrail

Last reviewed: 2026-05-05

Status: mandatory, do not remove.

## Invariant

HR, PURCHASING, INVENTORY, and REPORTS come under the Accounting portal in frontend scope.

This invariant protects route ownership, API contract coverage, QA scope, and release sign-off.

## Change-Control Rule

Any scope change must be applied in one atomic change set with explicit evidence:

1. Updated canonical portal and frontend API docs for every affected portal.
2. Updated `docs/openapi-endpoint-contract.md` module mapping and examples.
3. Added change evidence covering rationale, impact, and verification plan.

## Required References

- `docs/frontend-portals/accounting/README.md`
- `docs/frontend-api/README.md`
- `docs/openapi-endpoint-contract.md`

## Fail-Closed Policy

If any required artifact is missing or out-of-sync, release gates must fail closed until reconciled.
