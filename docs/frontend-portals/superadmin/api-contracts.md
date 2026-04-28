# API Contracts

## Shared transport rules

- All endpoints return the standard `ApiResponse` envelope.
- These routes are platform control-plane routes. The target tenant comes from the path, not from `X-Company-Code`.
- Never send `X-Company-Id`.
- `companyId` in these responses is a superadmin-only `tenantId` route helper.
  Do not reuse it as tenant-shell auth scope or a substitute for `companyCode`.

## `GET /api/v1/superadmin/tenants/coa-templates`

Use this to populate the onboarding wizard before the user can submit.

Response `data[]` fields:

| Field | Type | Notes |
|---|---|---|
| `code` | string | canonical template key submitted later as `coaTemplateCode` |
| `name` | string | display label |
| `description` | string | help text for selection UI |
| `accountCount` | integer | show as impact hint only, not as a validation input |

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
- `lifecycleState`
- `lifecycleReason`
- `activeUserCount`
- `quotaMaxActiveUsers`
- `apiActivityCount`
- `quotaMaxApiRequests`
- `auditStorageBytes`
- `quotaMaxStorageBytes`
- `currentConcurrentRequests`
- `quotaMaxConcurrentRequests`
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

Frontend guidance:

- Render action buttons from `availableActions` first, not only from static role assumptions.
- Activation delivery status will be exposed by the V1 activation flow, not by legacy credential-email fields.

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
