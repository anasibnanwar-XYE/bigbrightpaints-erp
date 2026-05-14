# Frontend Workflows

Last reviewed: 2026-05-09

This is the cross-portal workflow guide. Each portal folder contains the
screen-level workflow details and Playwright journey checklist.

## Bootstrap And Shell Selection

1. Call `GET /api/v1/auth/me`.
2. If unauthenticated, route to login.
3. If `mustChangePassword=true`, route to password change.
4. If user has `ROLE_SUPER_ADMIN`, enter the superadmin platform shell.
5. For tenant-scoped roles, require `companyCode` and enter the matching tenant
   shell.
6. Render only navigation allowed by the active role.

## Tenant Onboarding

Owner: superadmin.

1. Open Add Client in `/platform/*`.
2. Load tenant creation options and COA templates.
3. Create tenant through `POST /api/v1/superadmin/tenants`.
4. Send or copy activation only from explicit superadmin actions.
5. Owner completes activation and setup through auth/setup routes.
6. Frontend treats accounting as ready only when setup signals show seeded COA,
   default accounting period, and tenant admin provisioning completed.

## Tenant User Management

Owner: tenant-admin.

1. Bootstrap with `GET /api/v1/auth/me`.
2. Load `/api/v1/admin/users`.
3. Create/update only assignable tenant roles:
   `ROLE_ACCOUNTING`, `ROLE_FACTORY`, `ROLE_SALES`, `ROLE_DEALER`.
4. Re-fetch list/detail after lock, unlock, MFA disable, session revoke, or
   force-reset actions.
5. Do not show controls for `ROLE_ADMIN` or `ROLE_SUPER_ADMIN` targets inside
   tenant-admin user management.

## Order To Cash

Owners: sales, factory, accounting, dealer-client.

1. Sales creates and confirms orders.
2. Sales handles dealer credit posture and override requests.
3. Factory owns production/packing and dispatch execution.
4. Factory posts shipment only through `POST /api/v1/dispatch/confirm`.
5. Accounting owns downstream journal/reconciliation/settlement correction.
6. Dealer-client shows dealer-safe order, invoice, ledger, aging, support, and
   credit-request views.

Frontend rule: sales may read dispatch and invoice follow-up for an order, but
must not execute dispatch or accounting correction.

## Procure To Pay

Owner: accounting and purchasing-backed backend workflows.

1. Supplier and purchase workflows create stock/AP state.
2. Accounting owns reconciliation, supplier aging, settlements, reports, and
   correction flows.
3. Frontend must keep GRN, invoice, settlement, and period-close blockers
   visible instead of hiding failed posting state.

## Production And Packing

Owner: factory.

1. Create production logs only for ready SKUs.
2. Block production when SKU readiness reports missing inventory/accounting
   mappings.
3. Pack batches using required packaging mappings and batch traceability.
4. Use dispatch preview and pending queues before confirmation.
5. Confirm dispatch through the canonical dispatch endpoint only.

## Period Close

Owner: accounting plus tenant-admin approval.

1. Accounting reviews month-end checklist and reconciliation blockers.
2. Accounting requests close.
3. Tenant-admin approval inbox shows the request.
4. Tenant-admin approves or rejects.
5. Accounting renders closed-period state and reporting outputs from the current
   period state. Reopen controls are not part of the accounting UI; superadmin
   owns reopen through control-plane workflows.

Frontend rule: do not bypass period close with direct journal or report-export
workarounds.

## Support And Approvals

- Tenant-admin owns internal support tickets under `/api/v1/admin/support/**`.
- Accounting support workflows use accounting portal support routes where
  documented.
- Dealer-client owns dealer-originated support tickets under
  `/api/v1/dealer-portal/support/**`.
- Export approvals belong to tenant-admin, even when the requested export came
  from accounting.

## Validation Checklist Before Building A Screen

- Correct portal folder selected.
- Role can see the screen.
- Endpoint belongs to that portal.
- Tenant-scoped requests use `X-Company-Code`.
- Error states cover `400`, `401`, `403`, `404`, `409`, `422`, and `429` where
  applicable.
- Playwright journey exists or is added in the portal folder.
