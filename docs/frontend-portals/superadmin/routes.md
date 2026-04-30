# Routes

Last reviewed: 2026-04-30

Recommended frontend route map:

| UI route | Screen | Loader APIs | Primary actions | Guard |
| --- | --- | --- | --- | --- |
| `/platform/dashboard` | Platform overview | `GET /api/v1/superadmin/dashboard` | open client, support, bug, audit, infra drilldowns | `ROLE_SUPER_ADMIN` |
| `/platform/clients` | Client list | `GET /api/v1/superadmin/tenants?q=&status=&page=&size=&sort=&includeArchived=` | filter, sort, open detail, start Add Client | `ROLE_SUPER_ADMIN` |
| `/platform/clients/new` | Add Client wizard | `GET /api/v1/superadmin/tenants/new`, `GET /api/v1/superadmin/tenants/coa-templates` | `POST /api/v1/superadmin/tenants` | `ROLE_SUPER_ADMIN` |
| `/platform/clients/:tenantId` | Client overview | `GET /api/v1/superadmin/tenants/{id}` | activation, lifecycle, plan, billing, support, audit tab actions | `ROLE_SUPER_ADMIN` |
| `/platform/clients/:tenantId/onboarding` | Activation and seed state | tenant detail, `GET /api/v1/superadmin/tenants/{id}/seed-status` | activation send/resend/copy/expire, seed repair | `ROLE_SUPER_ADMIN` |
| `/platform/clients/:tenantId/plan-limits` | Plan, entitlements, quota policy | tenant detail, `GET /api/v1/superadmin/tenants/{id}/entitlements`, `GET /api/v1/superadmin/tenants/{id}/quota-policy` | assign plan, set/remove overrides, update limits/modules | `ROLE_SUPER_ADMIN` |
| `/platform/clients/:tenantId/usage` | Usage dashboard/history | `GET /api/v1/superadmin/tenants/{id}/usage`, `GET /api/v1/superadmin/tenants/{id}/usage/history?periodType=` | quota check, review warning/block state | `ROLE_SUPER_ADMIN` |
| `/platform/clients/:tenantId/billing` | Subscription and ledger | subscription, ledger, billing metrics | create subscription/invoice/payment/adjustment, grace/suspend/resume/cancel/archive | `ROLE_SUPER_ADMIN` |
| `/platform/support` | Support/SLA queue | `GET /api/v1/superadmin/support/tickets?status=&category=&slaStatus=&q=&page=&size=&sort=` | refresh SLA, open ticket | `ROLE_SUPER_ADMIN` |
| `/platform/support/:ticketId` | Ticket detail/chat | detail, messages, timeline | reply, internal note, status change, convert, Sentry link/sync | `ROLE_SUPER_ADMIN` |
| `/platform/audit` | Platform/security/suspicious audit | audit list routes with filters/page/size | acknowledge/resolve/reopen suspicious events | `ROLE_SUPER_ADMIN` |
| `/platform/infra` | Infra health/cost | `GET /api/v1/superadmin/infra/health`, cost snapshot routes | create/update/archive cost snapshots | `ROLE_SUPER_ADMIN` |
| `/platform/settings` | Platform settings | `GET /api/v1/superadmin/settings` | `PUT /api/v1/superadmin/settings` | `ROLE_SUPER_ADMIN` |
| `/platform/profile` | Self profile/session security | profile and session routes | profile update, password change, session revoke | `ROLE_SUPER_ADMIN` |
| `/platform/changelog` | Release-note authoring | product changelog read surface plus Super Admin changelog mutations | create, edit, delete | `ROLE_SUPER_ADMIN` |

Owner setup is not inside the Super Admin shell. The activated tenant owner uses the tenant shell and public setup corridor routes: `GET /api/v1/setup/status`, `PUT /api/v1/setup/company-details`, `PUT /api/v1/setup/gst`, `PUT /api/v1/setup/accounting`, `POST /api/v1/setup/invite-team`, and `POST /api/v1/setup/finish`.

## Route design rules

- Use `tenantId` from the URL for tenant mutations.
- Use explicit confirmation modals for activation copy, expire, suspension, cancel, archive, seed repair, Sentry link, cost archive, and suspicious-event remediation.
- Re-fetch tenant detail or the relevant list after every accepted mutation.
- Preserve filter state in the URL for support, audit, cost snapshots, tenant lists, usage history, and plan/template history.
- Treat retired route registry entries as not routable UI destinations.
