# V1 Portal User Journeys (Frontend + Backend UX)

Goal: give frontend engineers a portal-by-portal onboarding flow that matches backend safety boundaries and fail-closed behavior.

## 1) SUPER_ADMIN_CONSOLE Journey

1. Login with super-admin credentials.
2. Land on tenant list with lifecycle + usage summary.
3. Create tenant or open existing tenant detail.
4. Set quota values and lifecycle state with reason.
5. Confirm updated tenant metrics and audit trace.

Do not proceed if:
- lifecycle reason is missing
- authority is not `ROLE_SUPER_ADMIN`

## 2) ADMIN Journey

1. Login as tenant admin and load `/auth/me`.
2. Open users/roles/settings workspace.
3. Invite users and assign tenant-safe roles.
4. Review approval queue and pending exceptions.
5. Validate tenant boundaries by checking denial behavior for super-admin-only actions.

Do not proceed if:
- role matrix is unresolved
- portal surfaces cross-tenant controls

## 3) ACCOUNTING Journey

1. Open accounting dashboard and verify period state.
2. Complete readiness checklist (tax mode, default accounts, counterparties).
3. Perform first guided posting with pre-post preview.
4. Validate journal/subledger linkage and reconciliation signal.
5. Run period-close rehearsal path and verify lock/close boundaries.

Do not proceed if:
- period is locked/closed for write operations
- reconciliation links are missing
- idempotency conflicts are not handled with explicit operator guidance

## 4) FACTORY Journey

1. Open production plan board and create/confirm plan.
2. Execute production log and packing steps.
3. Complete packing and verify history trail.
4. Run cost allocation and confirm resulting state updates.
5. Validate dashboard reflects new operational state.

Do not proceed if:
- state transition is out-of-order
- duplicate submit behavior is non-deterministic

## 5) SALES Journey

1. Create sales order with idempotency-aware request.
2. Confirm or cancel order with proper reason flow.
3. Raise credit request and resolve approval/rejection path with reason.
4. Confirm dispatch and verify invoice progression.
5. Manage targets using assignee + change reason governance contract.

Do not proceed if:
- reason-coded governance fields are absent
- order state transitions skip required backend validations

## 6) DEALER Journey

1. Login as dealer and open dashboard.
2. View own orders, invoices, ledger, and aging.
3. Download invoice PDF for owned invoice only.
4. Submit credit-limit request with amount and reason.
5. Verify denied actions are clearly scoped to dealer restrictions.

Do not proceed if:
- dealer can query another dealer's data
- financial widgets do not reflect backend-calculated values

## 7) Cross-Portal UX Non-Negotiables
- Every write flow must surface backend fail-closed reasons.
- Every high-risk action needs explicit confirmation and recovery path.
- Role and tenant boundaries must be visible and enforceable in UX.
- Onboarding checklists are release artifacts, not optional guides.
