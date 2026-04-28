# API Contracts

## Shared transport rules

- All endpoints return the standard `ApiResponse` envelope.
- These routes are platform control-plane routes. The target tenant comes from the path, not from `X-Company-Code`.
- Never send `X-Company-Id`.
- `companyId` in these responses is a superadmin-only `tenantId` route helper.
  Do not reuse it as tenant-shell auth scope or a substitute for `companyCode`.

## `GET /api/v1/superadmin/tenants/new`

Use this to populate the V1 Add Client wizard before the user can submit.

Response `data` sections:

| Field | Type | Notes |
|---|---|---|
| `company.fields[]` | array | company identity, timezone, GST state, currency, and `coaTemplateCode` |
| `owner.fields[]` | array | owner email, display name, and phone marker |
| `commercial.fields[]` | array | `planId`, `billingStatus`, `trialDays`, and `supportTier` |
| `quotas.fields[]` | array | active user, API, storage, concurrency, soft-limit, and hard-limit controls |
| `modules.fields[]` | array | enabled module multi-select |
| `support.fields[]` | array | platform support notes and tags |
| `createModes[]` | array | `DRAFT` or `SEND_ACTIVATION`, including activation effects |
| `seedPolicy` | object | setup artifact readiness categories and activation gating note |

The wizard only collects those sections. Operational location setup is outside the V1 Add Client and owner setup corridor.

## `POST /api/v1/superadmin/tenants`

Creates a client as draft or sends the owner activation email.

Request shape:

```json
{
  "company": {
    "name": "Acme Paints",
    "code": "ACME",
    "timezone": "Asia/Kolkata",
    "stateCode": "KA",
    "baseCurrency": "INR",
    "defaultGstRate": 18,
    "coaTemplateCode": "SME"
  },
  "owner": {
    "email": "owner@example.com",
    "displayName": "Owner Example",
    "phone": "+919900000000"
  },
  "commercial": {
    "planId": "TRIAL",
    "billingStatus": "MANUAL",
    "trialDays": 14,
    "supportTier": "STANDARD"
  },
  "quotas": {
    "maxActiveUsers": 10,
    "maxApiRequests": 10000,
    "maxStorageBytes": 1073741824,
    "maxConcurrentRequests": 8,
    "softLimitEnabled": false,
    "hardLimitEnabled": true
  },
  "modules": {
    "enabled": ["ACCOUNTING", "SALES"]
  },
  "support": {
    "notes": "Safe platform note",
    "tags": ["TRIAL"]
  },
  "createMode": "SEND_ACTIVATION"
}
```

Response `data` includes:

- `tenantId`
- `companyCode`
- `status`
- `owner`
- `activation`
- `auditEventId`

Unknown request fields fail with `400`; do not silently preserve stale wizard state.

## Retired: flat tenant onboarding

`POST /api/v1/superadmin/tenants/onboard` is retired and hidden from the current OpenAPI contract. Treat any `410 Gone` response as a signal to use the V1 Add Client activation flow instead. Do not build new UI against the old flat create payload.

## `GET /api/v1/superadmin/tenants`

Query:

- Optional `status`

Response row shape:

- `companyId`
- `companyCode`
- `companyName`
- `timezone`
- `status`
- `plan`
- `billingStatus`
- `usage`
- `trialEndsAt`
- `health`
- `enabledModules`
- `mainAdmin`
- `lastActivityAt`

Use list rows for table display only. Load tenant detail before rendering recovery or support actions.

## `GET /api/v1/superadmin/tenants/{id}`

This is the tenant-detail source of truth.

Important nested objects:

- `onboarding.templateCode`
- `onboarding.adminEmail`
- `onboarding.adminUserId`
- `onboarding.tenantAdminProvisioned`
- `onboarding.completedAt`
- `limits.*`
- `usage.*`
- `supportContext.supportNotes`
- `supportContext.supportTags`
- `supportTimeline[]`
- `availableActions.*`
- `overview`
- `plan`
- `billing`
- `support`
- `bugs`
- `audit`
- `settings`

Frontend guidance:

- Render action buttons from `availableActions` first, not only from static role assumptions.
- Activation delivery status is exposed by the V1 activation flow, not by legacy credential-email fields.

## `PUT /api/v1/superadmin/tenants/{id}/lifecycle`

Request body:

| Field | Type | Required | Notes |
|---|---|---|---|
| `state` | string | yes | `ACTIVE`, `SUSPENDED`, `DEACTIVATED` only |
| `reason` | string | yes | max 1024 |

Response body:

- `companyId`
- `companyCode`
- `previousLifecycleState`
- `lifecycleState`
- `reason`

## `PUT /api/v1/superadmin/tenants/{id}/limits`

Request fields:

- `quotaMaxActiveUsers`
- `quotaMaxApiRequests`
- `quotaMaxStorageBytes`
- `quotaMaxConcurrentRequests`
- `quotaSoftLimitEnabled`
- `quotaHardLimitEnabled`

All numeric values must be `>= 0`.

## `PUT /api/v1/superadmin/tenants/{id}/modules`

Request body:

```json
{
  "enabledModules": ["ACCOUNTING", "SALES", "FACTORY"]
}
```

Treat module selection as full-state replacement, not patch semantics.

## Support and recovery endpoints

### `POST /api/v1/superadmin/tenants/{id}/support/warnings`

Request:

- `warningCategory` optional, max 100
- `message` required, max 500
- `requestedLifecycleState` optional, max 32
- `gracePeriodHours` optional, min 1

### `PUT /api/v1/superadmin/tenants/{id}/support/context`

Request:

- `supportNotes` optional, max 4000
- `supportTags` optional string set, each item max 64

### Retired tenant admin support reset

`POST /api/v1/superadmin/tenants/{id}/support/admin-password-reset` is retired and hidden from the current OpenAPI contract. Use password recovery / activation-specific routes rather than a Super Admin credential reset action.

### `POST /api/v1/superadmin/tenants/{id}/force-logout`

Optional request:

- `reason` max 300

Response:

- `companyId`
- `companyCode`
- `revokedUserCount`
- `reason`
- `actor`
- `occurredAt`

## Admin access endpoints

### `PUT /api/v1/superadmin/tenants/{id}/admins/main`

Request:

```json
{
  "adminUserId": 123
}
```

Response is `MainAdminSummaryDto`:

- `userId`
- `email`
- `displayName`
- `enabled`
- `replaceable`

### `POST /api/v1/superadmin/tenants/{id}/admins/{adminId}/email-change/request`

Request:

```json
{
  "newEmail": "next-admin@example.com"
}
```

Use the returned request metadata to open a confirmation step. Do not treat the request step as completion.

### `POST /api/v1/superadmin/tenants/{id}/admins/{adminId}/email-change/confirm`

Request:

```json
{
  "requestId": 456,
  "verificationToken": "..."
}
```

Frontend should require both values and prevent submission until both are present.

## Changelog endpoints

- `POST /api/v1/superadmin/changelog`
- `PUT /api/v1/superadmin/changelog/{id}`
- `DELETE /api/v1/superadmin/changelog/{id}`

Request fields for create and update:

- `version` required semver, max 32
- `title` required, max 255
- `body` required
- `isHighlighted` optional boolean
