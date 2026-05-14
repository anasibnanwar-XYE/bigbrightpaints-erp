# Sales States And Errors

Last reviewed: 2026-04-02

## Important States

- dealer `ACTIVE`, `ON_HOLD`, `SUSPENDED`
- order `DRAFT`, `CONFIRMED`, `PROCESSING`, `RESERVED`, `PENDING_PRODUCTION`,
  `PENDING_INVENTORY`, `READY_TO_SHIP`, `DISPATCHED`, `INVOICED`, `SETTLED`,
  `ON_HOLD`, `REJECTED`, `CLOSED`, `CANCELLED`, `DELETED`
- credit request `PENDING`, `APPROVED`, `REJECTED`, `EXPIRED`
- invoice readiness `NOT_READY`, `READY`, `PARTIAL`

## Blocking Errors

- duplicate dealer code, GST number, or commercial email
- invalid price list or tax setup for the dealer
- SKU not ready for selling or reservation
- reservation failure because stock is unavailable
- credit block or exposure limit breach
- dispatch not yet confirmed by factory
- invoice unavailable because dispatch is incomplete

## UI Rules

- Dealer-directory list endpoints default to active-only results; if the screen
  needs held or suspended dealers, call the directory with `status=ALL`.
- Dealer-directory calls with `page` and `size` return a sliced list only; do
  not fabricate page totals or page counts in the UI.
- Order search with no matches returns an empty `content` array; render a normal
  empty state instead of a backend-error banner.
- Order-search status filters use canonical order lifecycle statuses only.
- Separate commercial validation errors from backend faults.
- When dispatch is still pending, show a status explanation and last operational
  milestone instead of a generic failure banner.
- When invoice data is missing, show whether the blocker is dispatch, partial
  dispatch, or a read error.
