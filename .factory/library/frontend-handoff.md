# Frontend Handoff

API contracts, flow documentation, and design guidance for frontend developers.

**What belongs here:** Endpoint maps per module, request/response schemas, user flow descriptions, state machines, error codes, and UI hints.
**Updated by:** Backend workers after implementing/refactoring each module.

---

## Documentation Format Per Module

Each module section should include:
1. **Endpoint Map** - All REST endpoints with HTTP method, path, auth requirements, request/response types
2. **User Flows** - Step-by-step flows a frontend would implement (e.g., "Create Sales Order" flow with all API calls in sequence)
3. **State Machines** - Entity lifecycle states and valid transitions (e.g., Order: Draft -> Confirmed -> Dispatched -> Invoiced)
4. **Error Codes** - Module-specific error codes the frontend should handle with suggested UX behavior
5. **Data Contracts** - Key request/response DTOs with field descriptions and validation rules
6. **UI Hints** - Suggested form fields, required vs optional, dropdowns vs free text, dependent fields

---

## Modules (populated by workers as features complete)

### Auth
_To be documented_

### Tenant & Admin

#### Endpoint Map (SUPER_ADMIN only)

| Method | Path | Auth | Request | Response `data` |
|---|---|---|---|---|
| GET | `/api/v1/superadmin/dashboard` | `ROLE_SUPER_ADMIN` | None | `SuperAdminDashboardDto` |
| GET | `/api/v1/superadmin/tenants` | `ROLE_SUPER_ADMIN` | Optional query: `status=ACTIVE|SUSPENDED|DEACTIVATED` | `List<SuperAdminTenantDto>` |
| POST | `/api/v1/superadmin/tenants/{id}/suspend` | `ROLE_SUPER_ADMIN` | None | `SuperAdminTenantDto` |
| POST | `/api/v1/superadmin/tenants/{id}/activate` | `ROLE_SUPER_ADMIN` | None | `SuperAdminTenantDto` |
| POST | `/api/v1/superadmin/tenants/{id}/deactivate` | `ROLE_SUPER_ADMIN` | None | `SuperAdminTenantDto` |
| POST | `/api/v1/superadmin/tenants/{id}/lifecycle-state` | `ROLE_SUPER_ADMIN` | `CompanyLifecycleStateRequest` | `CompanyLifecycleStateDto` |
| GET | `/api/v1/superadmin/tenants/{id}/usage` | `ROLE_SUPER_ADMIN` | None | `SuperAdminTenantUsageDto` |
| GET | `/api/v1/superadmin/tenants/coa-templates` | `ROLE_SUPER_ADMIN` | None | `List<CoATemplateDto>` |
| POST | `/api/v1/superadmin/tenants/onboard` | `ROLE_SUPER_ADMIN` | `TenantOnboardingRequest` | `TenantOnboardingResponse` |

All responses are wrapped in `ApiResponse<T>`.

#### Module Feature Gating (tenant runtime behavior)

Module access is enforced per-tenant using `companies.enabled_modules` (JSON array of module keys).

- **Gatable modules**: `MANUFACTURING`, `HR_PAYROLL`, `PURCHASING`, `PORTAL`, `REPORTS_ADVANCED`
- **Core modules (always enabled, cannot be disabled)**: `AUTH`, `ACCOUNTING`, `SALES`, `INVENTORY`

If a request hits a disabled gatable module, backend returns **403** with `ErrorCode.MODULE_DISABLED` (`BUS_010`).

Current runtime path mapping used by backend:
- `MANUFACTURING`: `/api/v1/factory/**`, `/api/v1/production/**`
- `HR_PAYROLL`: `/api/v1/hr/**`, `/api/v1/payroll/**`
- `PURCHASING`: `/api/v1/purchasing/**`, `/api/v1/suppliers/**`
- `PORTAL`: `/api/v1/portal/**`, `/api/v1/dealer-portal/**`
- `REPORTS_ADVANCED`: `/api/v1/reports/**`, `/api/v1/accounting/reports/**`

#### User Flows

1. **Load platform dashboard**
   1. `GET /api/v1/superadmin/dashboard`
   2. Render cards: total tenants, active/suspended, total users, API calls, storage, recent activity.

2. **Browse tenants**
   1. `GET /api/v1/superadmin/tenants`
   2. Optional filter: `GET /api/v1/superadmin/tenants?status=SUSPENDED`
   3. Render each row with status + usage summary.

3. **Suspend tenant**
   1. User clicks Suspend in tenant row
   2. `POST /api/v1/superadmin/tenants/{id}/suspend`
   3. Update row status to `SUSPENDED`.

4. **Activate tenant**
   1. User clicks Activate in tenant row
   2. `POST /api/v1/superadmin/tenants/{id}/activate`
   3. Update row status to `ACTIVE`.

5. **Deactivate tenant**
   1. User clicks Deactivate in tenant row
   2. `POST /api/v1/superadmin/tenants/{id}/deactivate`
   3. Update row status to `DEACTIVATED`.

6. **Set lifecycle state explicitly (with reason)**
   1. `POST /api/v1/superadmin/tenants/{id}/lifecycle-state` with `{ state, reason }`
   2. Use for auditable transition requests from superadmin console.

7. **Inspect tenant usage**
   1. `GET /api/v1/superadmin/tenants/{id}/usage`
   2. Show API calls, active users, storage, last activity timestamp.

8. **Tenant onboarding (single-call bootstrap)**
   1. `GET /api/v1/superadmin/tenants/coa-templates` to populate template dropdown (Generic / Indian Standard / Manufacturing).
   2. Submit onboarding form once via `POST /api/v1/superadmin/tenants/onboard`.
   3. Backend creates tenant company, admin user, full chart of accounts from selected template, default accounting period, and baseline system settings.
   4. Frontend stores/display one-time `adminTemporaryPassword` immediately after success.

9. **Tenant runtime lifecycle enforcement expectations**
   - `ACTIVE`: read/write allowed.
   - `SUSPENDED`: **read-only** (write methods return 403).
   - `DEACTIVATED`: all requests blocked with 403.

#### State Machine (superadmin view)

- `ACTIVE` -> `SUSPENDED` via `POST /api/v1/superadmin/tenants/{id}/suspend`
- `SUSPENDED` -> `ACTIVE` via `POST /api/v1/superadmin/tenants/{id}/activate`
- `ACTIVE` -> `DEACTIVATED` via `POST /api/v1/superadmin/tenants/{id}/deactivate`
- `SUSPENDED` -> `DEACTIVATED` via `POST /api/v1/superadmin/tenants/{id}/deactivate`
- `DEACTIVATED` is terminal (no activation path from this state)

#### Error Codes / Error Handling

- **403 Forbidden**: caller is not `ROLE_SUPER_ADMIN`.
  - Frontend behavior: block page access and show “Superadmin access required”.
- **403 + `BUS_010` (`MODULE_DISABLED`)**: tenant module is disabled.
  - Frontend behavior: show contextual “Module disabled for this tenant” empty/error state and hide write actions for that module.
- **403** (tenant lifecycle runtime guard): suspended write or deactivated access.
  - Frontend behavior: show non-retryable state banner (“Tenant suspended: read-only” or “Tenant deactivated”).
- **400 Business validation error** (invalid filter/status or unknown tenant id).
  - Frontend behavior: show inline toast with server message, keep current page state.

#### Data Contracts

- `SuperAdminDashboardDto`
  - `totalTenants: number`
  - `activeTenants: number`
  - `suspendedTenants: number`
  - `deactivatedTenants: number`
  - `totalUsers: number`
  - `totalApiCalls: number`
  - `totalStorageBytes: number`
  - `recentActivityAt: string | null` (ISO-8601)

- `SuperAdminTenantDto`
  - `companyId: number`
  - `companyCode: string`
  - `companyName: string`
  - `status: "ACTIVE" | "SUSPENDED" | "DEACTIVATED"`
  - `activeUsers: number`
  - `apiCallCount: number`
  - `storageBytes: number`
  - `lastActivityAt: string | null` (ISO-8601)

- `SuperAdminTenantUsageDto`
  - `companyId: number`
  - `companyCode: string`
  - `status: "ACTIVE" | "SUSPENDED" | "DEACTIVATED"`
  - `apiCallCount: number`
  - `activeUsers: number`
  - `storageBytes: number`
  - `lastActivityAt: string | null` (ISO-8601)

- `CompanyLifecycleStateRequest`
  - `state: "ACTIVE" | "SUSPENDED" | "DEACTIVATED"` (required)
  - `reason: string` (required, max 1024)

- `CompanyLifecycleStateDto`
  - `companyId: number`
  - `companyCode: string`
  - `previousLifecycleState: "ACTIVE" | "SUSPENDED" | "DEACTIVATED"`
  - `lifecycleState: "ACTIVE" | "SUSPENDED" | "DEACTIVATED"`
  - `reason: string`

- `CoATemplateDto`
  - `code: "GENERIC" | "INDIAN_STANDARD" | "MANUFACTURING"`
  - `name: string`
  - `description: string`
  - `accountCount: number` (always 50-100)

- `TenantOnboardingRequest`
  - `name: string` (required, max 255)
  - `code: string` (required, max 64, normalized uppercase)
  - `timezone: string` (required, max 64)
  - `defaultGstRate?: number` (0-100)
  - `maxActiveUsers?: number >= 0`
  - `maxApiRequests?: number >= 0`
  - `maxStorageBytes?: number >= 0`
  - `maxConcurrentUsers?: number >= 0`
  - `softLimitEnabled?: boolean`
  - `hardLimitEnabled?: boolean`
  - `firstAdminEmail: string` (required email)
  - `firstAdminDisplayName?: string`
  - `coaTemplateCode: "GENERIC" | "INDIAN_STANDARD" | "MANUFACTURING"` (required)

- `TenantOnboardingResponse`
  - `companyId: number`
  - `companyCode: string`
  - `templateCode: string`
  - `accountsCreated: number`
  - `accountingPeriodId: number`
  - `adminEmail: string`
  - `adminTemporaryPassword: string` (show once, do not persist in browser local storage)
  - `credentialsEmailSent: boolean`
  - `systemSettingsInitialized: boolean`

#### UI Hints

- Use a status filter dropdown with `ACTIVE`, `SUSPENDED`, and `DEACTIVATED`.
- Show human-readable storage units (KB/MB/GB) while keeping raw bytes for sorting.
- For lifecycle buttons, show confirmation modals before suspend/activate/deactivate.
- Treat `lastActivityAt = null` as “No activity yet”.
- Refresh dashboard + tenant list after lifecycle mutation to keep aggregates in sync.
- For onboarding, fetch templates first and show `name + description + accountCount` in template picker.
- On onboarding success, immediately display/copy `adminTemporaryPassword` in a modal and force explicit acknowledgement.
- Disable duplicate submits on onboarding button until API response is received.

### Accounting

Comprehensive frontend handoff for `VAL-DOC-003` (chart of accounts, journals, settlement, period controls, reconciliation, GST, audit, catalog bridge, and temporal/reporting endpoints).

> Response envelope convention: almost all endpoints return `ApiResponse<T>` where payload is in `data`; PDF endpoints return raw `byte[]`; CSV endpoint returns `text/csv` string.

#### Complete Endpoint Map (all accounting controllers)

| Method | Path | Auth | Request body | Response `data` type |
|---|---|---|---|---|
| `GET` | `/api/v1/accounting/accounts` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `List<AccountDto>` |
| `POST` | `/api/v1/accounting/accounts` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `AccountRequest` | `AccountDto` |
| `GET` | `/api/v1/accounting/accounts/tree` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `List<AccountHierarchyService.AccountNode>` |
| `GET` | `/api/v1/accounting/accounts/tree/{type}` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `List<AccountHierarchyService.AccountNode>` |
| `GET` | `/api/v1/accounting/accounts/{accountId}/activity` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `TemporalBalanceService.AccountActivityReport` |
| `GET` | `/api/v1/accounting/accounts/{accountId}/balance/as-of` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `BigDecimal` |
| `GET` | `/api/v1/accounting/accounts/{accountId}/balance/compare` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `TemporalBalanceService.BalanceComparison` |
| `POST` | `/api/v1/accounting/accruals` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `AccrualRequest` | `JournalEntryDto` |
| `GET` | `/api/v1/accounting/aging/dealers/{dealerId}` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `AgingSummaryResponse` |
| `GET` | `/api/v1/accounting/aging/dealers/{dealerId}/pdf` | `hasAuthority('ROLE_ADMIN')` | `—` | `byte[]` |
| `GET` | `/api/v1/accounting/aging/suppliers/{supplierId}` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `AgingSummaryResponse` |
| `GET` | `/api/v1/accounting/aging/suppliers/{supplierId}/pdf` | `hasAuthority('ROLE_ADMIN')` | `—` | `byte[]` |
| `GET` | `/api/v1/accounting/audit-trail` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `PageResponse<AccountingAuditTrailEntryDto>` |
| `GET` | `/api/v1/accounting/audit/digest` | `hasAuthority('ROLE_ADMIN')` | `—` | `AuditDigestResponse` |
| `GET` | `/api/v1/accounting/audit/digest.csv` | `hasAuthority('ROLE_ADMIN')` | `—` | `String` |
| `GET` | `/api/v1/accounting/audit/transactions` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `PageResponse<AccountingTransactionAuditListItemDto>` |
| `GET` | `/api/v1/accounting/audit/transactions/{journalEntryId}` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `AccountingTransactionAuditDetailDto` |
| `POST` | `/api/v1/accounting/bad-debts/write-off` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `BadDebtWriteOffRequest` | `JournalEntryDto` |
| `POST` | `/api/v1/accounting/catalog/import` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `multipart/form-data` | `CatalogImportResponse` |
| `GET` | `/api/v1/accounting/catalog/products` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `List<ProductionProductDto>` |
| `POST` | `/api/v1/accounting/catalog/products` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `ProductCreateRequest` | `ProductionProductDto` |
| `POST` | `/api/v1/accounting/catalog/products/bulk-variants` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `BulkVariantRequest` | `BulkVariantResponse` |
| `PUT` | `/api/v1/accounting/catalog/products/{id}` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `ProductUpdateRequest` | `ProductionProductDto` |
| `GET` | `/api/v1/accounting/configuration/health` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `ConfigurationHealthService.ConfigurationHealthReport` |
| `POST` | `/api/v1/accounting/credit-notes` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `CreditNoteRequest` | `JournalEntryDto` |
| `GET` | `/api/v1/accounting/date-context` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `Map<String, Object>` |
| `POST` | `/api/v1/accounting/dealers/{dealerId}/auto-settle` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `AutoSettlementRequest` | `PartnerSettlementResponse` |
| `POST` | `/api/v1/accounting/debit-notes` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `DebitNoteRequest` | `JournalEntryDto` |
| `GET` | `/api/v1/accounting/default-accounts` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `CompanyDefaultAccountsResponse` |
| `PUT` | `/api/v1/accounting/default-accounts` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `CompanyDefaultAccountsRequest` | `CompanyDefaultAccountsResponse` |
| `GET` | `/api/v1/accounting/gst/reconciliation` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `GstReconciliationDto` |
| `GET` | `/api/v1/accounting/gst/return` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `GstReturnDto` |
| `POST` | `/api/v1/accounting/inventory/landed-cost` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `LandedCostRequest` | `JournalEntryDto` |
| `POST` | `/api/v1/accounting/inventory/revaluation` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `InventoryRevaluationRequest` | `JournalEntryDto` |
| `POST` | `/api/v1/accounting/inventory/wip-adjustment` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `WipAdjustmentRequest` | `JournalEntryDto` |
| `GET` | `/api/v1/accounting/journal-entries` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `List<JournalEntryDto>` |
| `POST` | `/api/v1/accounting/journal-entries` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `JournalEntryRequest` | `JournalEntryDto` |
| `POST` | `/api/v1/accounting/journal-entries/{entryId}/cascade-reverse` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `JournalEntryReversalRequest` | `List<JournalEntryDto>` |
| `POST` | `/api/v1/accounting/journal-entries/{entryId}/reverse` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `JournalEntryReversalRequest` | `JournalEntryDto` |
| `GET` | `/api/v1/accounting/journals` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `List<JournalListItemDto>` |
| `POST` | `/api/v1/accounting/journals/manual` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `ManualJournalRequest` | `JournalEntryDto` |
| `POST` | `/api/v1/accounting/journals/{entryId}/reverse` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `JournalEntryReversalRequest` | `JournalEntryDto` |
| `GET` | `/api/v1/accounting/month-end/checklist` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `MonthEndChecklistDto` |
| `POST` | `/api/v1/accounting/month-end/checklist/{periodId}` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `MonthEndChecklistUpdateRequest` | `MonthEndChecklistDto` |
| `POST` | `/api/v1/accounting/payroll/payments` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `PayrollPaymentRequest` | `JournalEntryDto` |
| `POST` | `/api/v1/accounting/payroll/payments/batch` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `PayrollBatchPaymentRequest` | `PayrollBatchPaymentResponse` |
| `GET` | `/api/v1/accounting/periods` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `List<AccountingPeriodDto>` |
| `POST` | `/api/v1/accounting/periods` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `AccountingPeriodUpsertRequest` | `AccountingPeriodDto` |
| `PUT` | `/api/v1/accounting/periods/{periodId}` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `AccountingPeriodUpdateRequest` | `AccountingPeriodDto` |
| `POST` | `/api/v1/accounting/periods/{periodId}/close` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `AccountingPeriodCloseRequest` | `AccountingPeriodDto` |
| `POST` | `/api/v1/accounting/periods/{periodId}/lock` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `AccountingPeriodLockRequest` | `AccountingPeriodDto` |
| `POST` | `/api/v1/accounting/periods/{periodId}/reopen` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `AccountingPeriodReopenRequest` | `AccountingPeriodDto` |
| `POST` | `/api/v1/accounting/receipts/dealer` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `DealerReceiptRequest` | `JournalEntryDto` |
| `POST` | `/api/v1/accounting/receipts/dealer/hybrid` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `DealerReceiptSplitRequest` | `JournalEntryDto` |
| `POST` | `/api/v1/accounting/reconciliation/bank` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `BankReconciliationRequest` | `BankReconciliationSummaryDto` |
| `GET` | `/api/v1/accounting/reconciliation/subledger` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `ReconciliationService.SubledgerReconciliationReport` |
| `GET` | `/api/v1/accounting/reports/aging/dealer/{dealerId}` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `AgingReportService.DealerAgingDetail` |
| `GET` | `/api/v1/accounting/reports/aging/dealer/{dealerId}/detailed` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `AgingReportService.DealerAgingDetailedReport` |
| `GET` | `/api/v1/accounting/reports/aging/receivables` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `AgingReportService.AgedReceivablesReport` |
| `GET` | `/api/v1/accounting/reports/balance-sheet/hierarchy` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `AccountHierarchyService.BalanceSheetHierarchy` |
| `GET` | `/api/v1/accounting/reports/dso/dealer/{dealerId}` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `AgingReportService.DSOReport` |
| `GET` | `/api/v1/accounting/reports/income-statement/hierarchy` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `AccountHierarchyService.IncomeStatementHierarchy` |
| `GET` | `/api/v1/accounting/sales/returns` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_SALES')` | `—` | `List<JournalEntryDto>` |
| `POST` | `/api/v1/accounting/sales/returns` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `SalesReturnRequest` | `JournalEntryDto` |
| `POST` | `/api/v1/accounting/settlements/dealers` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `DealerSettlementRequest` | `PartnerSettlementResponse` |
| `POST` | `/api/v1/accounting/settlements/suppliers` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `SupplierSettlementRequest` | `PartnerSettlementResponse` |
| `GET` | `/api/v1/accounting/statements/dealers/{dealerId}` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `PartnerStatementResponse` |
| `GET` | `/api/v1/accounting/statements/dealers/{dealerId}/pdf` | `hasAuthority('ROLE_ADMIN')` | `—` | `byte[]` |
| `GET` | `/api/v1/accounting/statements/suppliers/{supplierId}` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `PartnerStatementResponse` |
| `GET` | `/api/v1/accounting/statements/suppliers/{supplierId}/pdf` | `hasAuthority('ROLE_ADMIN')` | `—` | `byte[]` |
| `POST` | `/api/v1/accounting/suppliers/payments` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `SupplierPaymentRequest` | `JournalEntryDto` |
| `POST` | `/api/v1/accounting/suppliers/{supplierId}/auto-settle` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `AutoSettlementRequest` | `PartnerSettlementResponse` |
| `GET` | `/api/v1/accounting/trial-balance/as-of` | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | `—` | `TemporalBalanceService.TrialBalanceSnapshot` |

_Total documented accounting endpoints: **73**._

#### Required User Flows (API call sequences)

1. **Chart of accounts setup**
   1. `GET /api/v1/accounting/accounts` (bootstrap account list)
   2. `GET /api/v1/accounting/accounts/tree` (hierarchy rendering)
   3. `POST /api/v1/accounting/accounts` (create account)
   4. `GET /api/v1/accounting/default-accounts` + `PUT /api/v1/accounting/default-accounts` (default mappings)
   5. Optional typed tree views: `GET /api/v1/accounting/accounts/tree/{type}`

2. **Manual journal entry**
   1. Dropdown preload: `GET /api/v1/accounting/accounts`, `GET /api/v1/dealers`, `GET /api/v1/suppliers`
   2. Create manual journal: `POST /api/v1/accounting/journals/manual` (preferred multi-line path)
   3. List and filter: `GET /api/v1/accounting/journals?fromDate&toDate&type&sourceModule`
   4. Reverse (single): `POST /api/v1/accounting/journals/{entryId}/reverse` or `/journal-entries/{entryId}/reverse`
   5. Reverse (cascade): `POST /api/v1/accounting/journal-entries/{entryId}/cascade-reverse`

3. **Auto-settlement (hands-off)**
   1. Dealer lookup + outstanding context: `GET /api/v1/dealers`, statement/aging endpoints as needed
   2. Dealer auto-settle: `POST /api/v1/accounting/dealers/{dealerId}/auto-settle`
   3. Supplier auto-settle: `POST /api/v1/accounting/suppliers/{supplierId}/auto-settle`
   4. For explicit allocation flows use `POST /settlements/dealers`, `POST /settlements/suppliers`, `POST /receipts/dealer`, `POST /suppliers/payments`

4. **Period close / reopen**
   1. Load periods: `GET /api/v1/accounting/periods`
   2. Validate readiness: `GET /api/v1/accounting/month-end/checklist?periodId={id}`
   3. Optionally mark checklist controls: `POST /api/v1/accounting/month-end/checklist/{periodId}`
   4. Close: `POST /api/v1/accounting/periods/{periodId}/close` (requires non-empty `note`; `force` optional)
   5. Reopen: `POST /api/v1/accounting/periods/{periodId}/reopen` (requires `reason`; auto-reverses closing journal when applicable)

5. **Bank reconciliation**
   1. Account source: `GET /api/v1/accounting/accounts` (ASSET/bank account selection)
   2. Submit statement match: `POST /api/v1/accounting/reconciliation/bank`
   3. Cross-check AR/AP controls: `GET /api/v1/accounting/reconciliation/subledger`

6. **GST return preparation**
   1. Run tax return: `GET /api/v1/accounting/gst/return?period=YYYY-MM`
   2. Run component reconciliation: `GET /api/v1/accounting/gst/reconciliation?period=YYYY-MM`
   3. Optional diagnostics for audit period: `GET /api/v1/accounting/audit-trail`

#### State Machines

1. **Journal lifecycle** (`JournalEntry.status`)
   - `DRAFT/PENDING` -> `POSTED` on successful creation/posting
   - `POSTED` -> `REVERSED` via reversal endpoints
   - `POSTED` -> `VOIDED` when reversal request uses `voidOnly=true` (still creates correction linkage)
   - Guardrails: entry must balance, period must be open, already-reversed/voided entries are rejected

2. **Accounting period lifecycle** (`AccountingPeriodStatus`)
   - `OPEN` -> `LOCKED` via `/periods/{id}/lock`
   - `OPEN` or `LOCKED` -> `CLOSED` via `/periods/{id}/close` (checklist + reconciliation + balancing validations)
   - `LOCKED/CLOSED` -> `OPEN` via `/periods/{id}/reopen` (reason required; closes snapshot + reverses closing journal when present)

3. **Settlement lifecycle** (frontend orchestration state)
   - `INITIATED` (draft UI form)
   - `VALIDATED` (allocations/payments pass amount/account checks)
   - `POSTED` (journal + allocation rows persisted, returns `PartnerSettlementResponse`)
   - `PARTIALLY_SETTLED` / `FULLY_SETTLED` determined by outstanding balance after allocation
   - `REVERSED` when the settlement-linked journal is reversed

#### Accounting ErrorCodes (all referenced in accounting module)

| ErrorCode enum | Wire code | Description | Suggested frontend behavior |
|---|---|---|---|
| `BUSINESS_CONSTRAINT_VIOLATION` | `BUS_004` | Business rule violation | Show business-rule toast/banner; do not auto-retry; keep action disabled until user changes input/state. |
| `BUSINESS_DUPLICATE_ENTRY` | `BUS_002` | Duplicate entry found | Show business-rule toast/banner; do not auto-retry; keep action disabled until user changes input/state. |
| `BUSINESS_ENTITY_NOT_FOUND` | `BUS_003` | Requested resource not found | Show business-rule toast/banner; do not auto-retry; keep action disabled until user changes input/state. |
| `BUSINESS_INVALID_STATE` | `BUS_001` | Operation not allowed in current state | Show business-rule toast/banner; do not auto-retry; keep action disabled until user changes input/state. |
| `CONCURRENCY_CONFLICT` | `CONC_001` | Resource was modified by another user | Show stale-data dialog and force refresh before retry. |
| `INTERNAL_CONCURRENCY_FAILURE` | `CONC_003` | Internal concurrency failure | Show stale-data dialog and force refresh before retry. |
| `SYSTEM_CONFIGURATION_ERROR` | `SYS_005` | System configuration error | Show non-field error with retry option; log traceId for support. |
| `SYSTEM_DATABASE_ERROR` | `SYS_003` | Database operation failed | Show non-field error with retry option; log traceId for support. |
| `SYSTEM_INTERNAL_ERROR` | `SYS_001` | An internal error occurred | Show non-field error with retry option; log traceId for support. |
| `VALIDATION_INVALID_DATE` | `VAL_005` | Invalid date or time value | Show blocking validation message and keep form editable. |
| `VALIDATION_INVALID_INPUT` | `VAL_001` | Invalid input provided | Show blocking validation message and keep form editable. |
| `VALIDATION_INVALID_REFERENCE` | `VAL_006` | Invalid reference to another resource | Mark referenced selector invalid and refresh dropdown source. |
| `VALIDATION_MISSING_REQUIRED_FIELD` | `VAL_002` | Required field is missing | Inline field validation + prevent submit. |

#### Request DTO Contracts (all endpoint request bodies)

- **`AccountRequest`**
  - `code`: `String` — validation `@NotBlank`
  - `name`: `String` — validation `@NotBlank`
  - `type`: `AccountType` — validation `@NotNull`
  - `parentId`: `Long` — validation `—`
- **`AccrualRequest`**
  - `debitAccountId`: `Long` — validation `@NotNull`
  - `creditAccountId`: `Long` — validation `@NotNull`
  - `amount`: `BigDecimal` — validation `@NotNull; @DecimalMin(value = "0.01")`
  - `entryDate`: `LocalDate` — validation `—`
  - `referenceNumber`: `String` — validation `—`
  - `memo`: `String` — validation `—`
  - `idempotencyKey`: `String` — validation `—`
  - `autoReverseDate`: `LocalDate` — validation `—`
  - `adminOverride`: `Boolean` — validation `—`
- **`BadDebtWriteOffRequest`**
  - `invoiceId`: `Long` — validation `@NotNull`
  - `expenseAccountId`: `Long` — validation `@NotNull`
  - `amount`: `BigDecimal` — validation `@NotNull; @DecimalMin(value = "0.01")`
  - `entryDate`: `LocalDate` — validation `—`
  - `referenceNumber`: `String` — validation `—`
  - `memo`: `String` — validation `—`
  - `idempotencyKey`: `String` — validation `—`
  - `adminOverride`: `Boolean` — validation `—`
- **`ProductCreateRequest`**
  - `brandId`: `Long` — validation `—`
  - `brandName`: `String` — validation `—`
  - `brandCode`: `String` — validation `—`
  - `productName`: `String` — validation `@NotBlank(message = "Product name is required")`
  - `category`: `String` — validation `@NotBlank(message = "Category is required")`
  - `defaultColour`: `String` — validation `—`
  - `sizeLabel`: `String` — validation `—`
  - `unitOfMeasure`: `String` — validation `—`
  - `customSkuCode`: `String` — validation `@JsonProperty(access = JsonProperty.Access.WRITE_ONLY)`
  - `basePrice`: `BigDecimal` — validation `—`
  - `gstRate`: `BigDecimal` — validation `—`
  - `minDiscountPercent`: `BigDecimal` — validation `—`
  - `minSellingPrice`: `BigDecimal` — validation `—`
  - `metadata`: `Map<String, Object>` — validation `—`
- **`BulkVariantRequest`**
  - `brandId`: `Long` — validation `—`
  - `brandName`: `String` — validation `—`
  - `brandCode`: `String` — validation `—`
  - `baseProductName`: `String` — validation `@NotBlank`
  - `category`: `String` — validation `@NotBlank`
  - `colors`: `List<String>` — validation `—`
  - `sizes`: `List<String>` — validation `—`
  - `colorSizeMatrix`: `List<ColorSizeMatrixEntry>` — validation `@Valid`
  - `unitOfMeasure`: `String` — validation `—`
  - `skuPrefix`: `String` — validation `—`
  - `basePrice`: `BigDecimal` — validation `—`
  - `gstRate`: `BigDecimal` — validation `—`
  - `minDiscountPercent`: `BigDecimal` — validation `—`
  - `minSellingPrice`: `BigDecimal` — validation `—`
  - `metadata`: `Map<String, Object>` — validation `—`
- **`ProductUpdateRequest`**
  - `productName`: `String` — validation `—`
  - `category`: `String` — validation `—`
  - `defaultColour`: `String` — validation `—`
  - `sizeLabel`: `String` — validation `—`
  - `unitOfMeasure`: `String` — validation `—`
  - `basePrice`: `BigDecimal` — validation `—`
  - `gstRate`: `BigDecimal` — validation `—`
  - `minDiscountPercent`: `BigDecimal` — validation `—`
  - `minSellingPrice`: `BigDecimal` — validation `—`
  - `metadata`: `Map<String, Object>` — validation `—`
- **`CreditNoteRequest`**
  - `invoiceId`: `Long` — validation `@NotNull`
  - `amount`: `BigDecimal` — validation `@DecimalMin(value = "0.01")`
  - `entryDate`: `LocalDate` — validation `—`
  - `referenceNumber`: `String` — validation `—`
  - `memo`: `String` — validation `—`
  - `idempotencyKey`: `String` — validation `—`
  - `adminOverride`: `Boolean` — validation `—`
- **`AutoSettlementRequest`**
  - `cashAccountId`: `Long` — validation `—`
  - `amount`: `BigDecimal` — validation `@NotNull; @DecimalMin(value = "0.01")`
  - `referenceNumber`: `String` — validation `—`
  - `memo`: `String` — validation `—`
  - `idempotencyKey`: `String` — validation `—`
- **`DebitNoteRequest`**
  - `purchaseId`: `Long` — validation `@NotNull`
  - `amount`: `BigDecimal` — validation `@DecimalMin(value = "0.01")`
  - `entryDate`: `LocalDate` — validation `—`
  - `referenceNumber`: `String` — validation `—`
  - `memo`: `String` — validation `—`
  - `idempotencyKey`: `String` — validation `—`
  - `adminOverride`: `Boolean` — validation `—`
- **`CompanyDefaultAccountsRequest`**
  - `inventoryAccountId`: `Long` — validation `—`
  - `cogsAccountId`: `Long` — validation `—`
  - `revenueAccountId`: `Long` — validation `—`
  - `discountAccountId`: `Long` — validation `—`
  - `taxAccountId`: `Long` — validation `—`
- **`LandedCostRequest`**
  - `rawMaterialPurchaseId`: `Long` — validation `@NotNull`
  - `amount`: `BigDecimal` — validation `@NotNull`
  - `inventoryAccountId`: `Long` — validation `@NotNull`
  - `offsetAccountId`: `Long` — validation `@NotNull`
  - `entryDate`: `LocalDate` — validation `—`
  - `memo`: `String` — validation `—`
  - `referenceNumber`: `String` — validation `—`
  - `idempotencyKey`: `String` — validation `—`
  - `adminOverride`: `Boolean` — validation `—`
- **`InventoryRevaluationRequest`**
  - `inventoryAccountId`: `Long` — validation `@NotNull`
  - `revaluationAccountId`: `Long` — validation `@NotNull`
  - `deltaAmount`: `BigDecimal` — validation `@NotNull`
  - `memo`: `String` — validation `—`
  - `entryDate`: `LocalDate` — validation `—`
  - `referenceNumber`: `String` — validation `—`
  - `idempotencyKey`: `String` — validation `—`
  - `adminOverride`: `Boolean` — validation `—`
- **`WipAdjustmentRequest`**
  - `productionLogId`: `Long` — validation `@NotNull`
  - `amount`: `BigDecimal` — validation `@NotNull`
  - `wipAccountId`: `Long` — validation `@NotNull`
  - `inventoryAccountId`: `Long` — validation `@NotNull`
  - `direction`: `Direction` — validation `@NotNull`
  - `memo`: `String` — validation `—`
  - `entryDate`: `LocalDate` — validation `—`
  - `referenceNumber`: `String` — validation `—`
  - `idempotencyKey`: `String` — validation `—`
  - `adminOverride`: `Boolean` — validation `—`
- **`JournalEntryRequest`**
  - `referenceNumber`: `String` — validation `—`
  - `entryDate`: `LocalDate` — validation `@NotNull`
  - `memo`: `String` — validation `—`
  - `dealerId`: `Long` — validation `—`
  - `supplierId`: `Long` — validation `—`
  - `adminOverride`: `Boolean` — validation `—`
  - `lines`: `List<JournalLineRequest>` — validation `@NotEmpty; @Valid`
  - `currency`: `String` — validation `—`
  - `fxRate`: `BigDecimal` — validation `—`
  - `sourceModule`: `String` — validation `—`
  - `sourceReference`: `String` — validation `—`
  - `journalType`: `String` — validation `—`
- **`JournalEntryReversalRequest`**
  - `reversalDate`: `LocalDate` — validation `—`
  - `voidOnly`: `boolean` — validation `—`
  - `reason`: `String` — validation `—`
  - `memo`: `String` — validation `—`
  - `adminOverride`: `Boolean` — validation `—`
  - `reversalPercentage`: `BigDecimal` — validation `@DecimalMin("0.01"); @DecimalMax("100.00")`
  - `cascadeRelatedEntries`: `boolean` — validation `—`
  - `relatedEntryIds`: `List<Long>` — validation `—`
  - `reasonCode`: `ReversalReasonCode` — validation `—`
  - `approvedBy`: `String` — validation `—`
  - `supportingDocumentRef`: `String` — validation `—`
- **`ManualJournalRequest`**
  - `entryDate`: `LocalDate` — validation `—`
  - `narration`: `String` — validation `—`
  - `idempotencyKey`: `String` — validation `—`
  - `adminOverride`: `Boolean` — validation `—`
  - `lines`: `List<LineRequest>` — validation `—`
- **`MonthEndChecklistUpdateRequest`**
  - `bankReconciled`: `Boolean` — validation `—`
  - `inventoryCounted`: `Boolean` — validation `—`
  - `note`: `String` — validation `—`
- **`PayrollPaymentRequest`**
  - `payrollRunId`: `Long` — validation `@NotNull`
  - `cashAccountId`: `Long` — validation `@NotNull`
  - `expenseAccountId`: `Long` — validation `@NotNull`
  - `amount`: `BigDecimal` — validation `@NotNull; @DecimalMin(value = "0.01")`
  - `referenceNumber`: `String` — validation `—`
  - `memo`: `String` — validation `—`
- **`PayrollBatchPaymentRequest`**
  - `runDate`: `LocalDate` — validation `@NotNull`
  - `cashAccountId`: `Long` — validation `@NotNull`
  - `expenseAccountId`: `Long` — validation `@NotNull`
  - `taxPayableAccountId`: `Long` — validation `—`
  - `pfPayableAccountId`: `Long` — validation `—`
  - `employerTaxExpenseAccountId`: `Long` — validation `—`
  - `employerPfExpenseAccountId`: `Long` — validation `—`
  - `defaultTaxRate`: `BigDecimal` — validation `@DecimalMin("0.00")`
  - `defaultPfRate`: `BigDecimal` — validation `@DecimalMin("0.00")`
  - `employerTaxRate`: `BigDecimal` — validation `@DecimalMin("0.00")`
  - `employerPfRate`: `BigDecimal` — validation `@DecimalMin("0.00")`
  - `referenceNumber`: `String` — validation `—`
  - `memo`: `String` — validation `—`
  - `lines`: `List<PayrollLine>` — validation `@NotEmpty; @Valid`
- **`AccountingPeriodUpsertRequest`**
  - `year`: `int` — validation `@Min(1900); @Max(9999)`
  - `month`: `int` — validation `@Min(1); @Max(12)`
  - `costingMethod`: `CostingMethod` — validation `—`
- **`AccountingPeriodUpdateRequest`**
  - `costingMethod`: `CostingMethod` — validation `@NotNull`
- **`AccountingPeriodCloseRequest`**
  - `force`: `Boolean` — validation `—`
  - `note`: `String` — validation `—`
- **`AccountingPeriodLockRequest`**
  - `reason`: `String` — validation `—`
- **`AccountingPeriodReopenRequest`**
  - `reason`: `String` — validation `—`
- **`DealerReceiptRequest`**
  - `dealerId`: `Long` — validation `@NotNull`
  - `cashAccountId`: `Long` — validation `@NotNull`
  - `amount`: `BigDecimal` — validation `@NotNull; @DecimalMin(value = "0.01")`
  - `referenceNumber`: `String` — validation `—`
  - `memo`: `String` — validation `—`
  - `idempotencyKey`: `String` — validation `—`
  - `allocations`: `List<SettlementAllocationRequest>` — validation `@NotEmpty(message = "Allocations are required for dealer receipts; use settlement endpoints or include allocations"); @Valid`
- **`DealerReceiptSplitRequest`**
  - `dealerId`: `Long` — validation `@NotNull`
  - `incomingLines`: `List<IncomingLine>` — validation `@NotEmpty; @Valid`
  - `referenceNumber`: `String` — validation `—`
  - `memo`: `String` — validation `—`
  - `idempotencyKey`: `String` — validation `—`
- **`BankReconciliationRequest`**
  - `bankAccountId`: `Long` — validation `@NotNull`
  - `statementDate`: `LocalDate` — validation `@NotNull`
  - `statementEndingBalance`: `BigDecimal` — validation `@NotNull`
  - `startDate`: `LocalDate` — validation `—`
  - `endDate`: `LocalDate` — validation `—`
  - `clearedReferences`: `List<String>` — validation `—`
  - `accountingPeriodId`: `Long` — validation `—`
  - `markAsComplete`: `Boolean` — validation `—`
  - `note`: `String` — validation `—`
- **`SalesReturnRequest`**
  - `invoiceId`: `Long` — validation `@NotNull`
  - `reason`: `String` — validation `@NotBlank`
  - `lines`: `List<ReturnLine>` — validation `@NotEmpty; @Valid`
- **`DealerSettlementRequest`**
  - `dealerId`: `Long` — validation `@NotNull`
  - `cashAccountId`: `Long` — validation `—`
  - `discountAccountId`: `Long` — validation `—`
  - `writeOffAccountId`: `Long` — validation `—`
  - `fxGainAccountId`: `Long` — validation `—`
  - `fxLossAccountId`: `Long` — validation `—`
  - `settlementDate`: `LocalDate` — validation `—`
  - `referenceNumber`: `String` — validation `—`
  - `memo`: `String` — validation `—`
  - `idempotencyKey`: `String` — validation `—`
  - `adminOverride`: `Boolean` — validation `—`
  - `allocations`: `List<SettlementAllocationRequest>` — validation `@NotEmpty; @Valid`
  - `payments`: `List<SettlementPaymentRequest>` — validation `@Valid`
- **`SupplierSettlementRequest`**
  - `supplierId`: `Long` — validation `@NotNull`
  - `cashAccountId`: `Long` — validation `@NotNull`
  - `discountAccountId`: `Long` — validation `—`
  - `writeOffAccountId`: `Long` — validation `—`
  - `fxGainAccountId`: `Long` — validation `—`
  - `fxLossAccountId`: `Long` — validation `—`
  - `settlementDate`: `LocalDate` — validation `—`
  - `referenceNumber`: `String` — validation `—`
  - `memo`: `String` — validation `—`
  - `idempotencyKey`: `String` — validation `—`
  - `adminOverride`: `Boolean` — validation `—`
  - `allocations`: `List<SettlementAllocationRequest>` — validation `@NotEmpty; @Valid`
- **`SupplierPaymentRequest`**
  - `supplierId`: `Long` — validation `@NotNull`
  - `cashAccountId`: `Long` — validation `@NotNull`
  - `amount`: `BigDecimal` — validation `@NotNull; @DecimalMin(value = "0.01")`
  - `referenceNumber`: `String` — validation `—`
  - `memo`: `String` — validation `—`
  - `idempotencyKey`: `String` — validation `—`
  - `allocations`: `List<SettlementAllocationRequest>` — validation `@NotEmpty(message = "Allocations are required for supplier payments; use settlement endpoints or include allocations"); @Valid`

#### Response DTO Contracts (all endpoint `data` types)

- **`List<AccountDto>`**
  - `id`: `Long`
  - `publicId`: `UUID`
  - `code`: `String`
  - `name`: `String`
  - `type`: `AccountType`
  - `balance`: `BigDecimal`
- **`AccountDto`**
  - `id`: `Long`
  - `publicId`: `UUID`
  - `code`: `String`
  - `name`: `String`
  - `type`: `AccountType`
  - `balance`: `BigDecimal`
- **`List<AccountHierarchyService.AccountNode>`**
  - `id`: `Long`
  - `code`: `String`
  - `name`: `String`
  - `type`: `String`
  - `balance`: `BigDecimal`
  - `level`: `Integer`
  - `parentId`: `Long`
  - `children`: `List<AccountNode>`
- **`TemporalBalanceService.AccountActivityReport`**
  - `accountCode`: `String`
  - `accountName`: `String`
  - `startDate`: `LocalDate`
  - `endDate`: `LocalDate`
  - `openingBalance`: `BigDecimal`
  - `closingBalance`: `BigDecimal`
  - `totalDebits`: `BigDecimal`
  - `totalCredits`: `BigDecimal`
  - `movements`: `List<AccountMovement>`
- **`BigDecimal`**
  - Primitive/raw payload type (no DTO field list).
- **`TemporalBalanceService.BalanceComparison`**
  - `accountId`: `Long`
  - `date1`: `LocalDate`
  - `balance1`: `BigDecimal`
  - `date2`: `LocalDate`
  - `balance2`: `BigDecimal`
  - `change`: `BigDecimal`
- **`JournalEntryDto`**
  - `id`: `Long`
  - `publicId`: `UUID`
  - `referenceNumber`: `String`
  - `entryDate`: `LocalDate`
  - `memo`: `String`
  - `status`: `String`
  - `dealerId`: `Long`
  - `dealerName`: `String`
  - `supplierId`: `Long`
  - `supplierName`: `String`
  - `accountingPeriodId`: `Long`
  - `accountingPeriodLabel`: `String`
  - `accountingPeriodStatus`: `String`
  - `reversalOfEntryId`: `Long`
  - `reversalEntryId`: `Long`
  - `correctionType`: `String`
  - `correctionReason`: `String`
  - `voidReason`: `String`
  - `lines`: `List<JournalLineDto>`
  - `createdAt`: `Instant`
  - `updatedAt`: `Instant`
  - `postedAt`: `Instant`
  - `createdBy`: `String`
  - `postedBy`: `String`
  - `lastModifiedBy`: `String`
- **`AgingSummaryResponse`**
  - `partnerId`: `Long`
  - `partnerName`: `String`
  - `totalOutstanding`: `BigDecimal`
  - `buckets`: `List<AgingBucketDto>`
- **`byte[]`**
  - Primitive/raw payload type (no DTO field list).
- **`PageResponse<AccountingAuditTrailEntryDto>`**
  - `content`: `List<AccountingAuditTrailEntryDto>`
    - `id`: `Long`
    - `timestamp`: `Instant`
    - `companyId`: `Long`
    - `companyCode`: `String`
    - `actorUserId`: `Long`
    - `actorIdentifier`: `String`
    - `actionType`: `String`
    - `entityType`: `String`
    - `entityId`: `String`
    - `referenceNumber`: `String`
    - `traceId`: `String`
    - `ipAddress`: `String`
    - `beforeState`: `String`
    - `afterState`: `String`
    - `sensitiveOperation`: `boolean`
    - `metadata`: `Map<String, String>`
  - `totalElements`: `long`
  - `totalPages`: `int`
  - `page`: `int` (0-based)
  - `size`: `int`
- **`AuditDigestResponse`**
  - `periodLabel`: `String`
  - `entries`: `List<String>`
- **`String`**
  - Primitive/raw payload type (no DTO field list).
- **`PageResponse<AccountingTransactionAuditListItemDto>`**
  - `content`: `List<AccountingTransactionAuditListItemDto>`
    - `journalEntryId`: `Long`
    - `referenceNumber`: `String`
    - `entryDate`: `LocalDate`
    - `status`: `String`
    - `module`: `String`
    - `transactionType`: `String`
    - `memo`: `String`
    - `dealerId`: `Long`
    - `dealerName`: `String`
    - `supplierId`: `Long`
    - `supplierName`: `String`
    - `totalDebit`: `BigDecimal`
    - `totalCredit`: `BigDecimal`
    - `reversalOfId`: `Long`
    - `reversalEntryId`: `Long`
    - `correctionType`: `String`
    - `consistencyStatus`: `String`
    - `postedAt`: `Instant`
  - `totalElements`: `long`
  - `totalPages`: `int`
  - `page`: `int` (0-based)
  - `size`: `int`
- **`AccountingTransactionAuditDetailDto`**
  - `journalEntryId`: `Long`
  - `journalPublicId`: `UUID`
  - `referenceNumber`: `String`
  - `entryDate`: `LocalDate`
  - `status`: `String`
  - `module`: `String`
  - `transactionType`: `String`
  - `memo`: `String`
  - `dealerId`: `Long`
  - `dealerName`: `String`
  - `supplierId`: `Long`
  - `supplierName`: `String`
  - `accountingPeriodId`: `Long`
  - `accountingPeriodLabel`: `String`
  - `accountingPeriodStatus`: `String`
  - `reversalOfId`: `Long`
  - `reversalEntryId`: `Long`
  - `correctionType`: `String`
  - `correctionReason`: `String`
  - `voidReason`: `String`
  - `totalDebit`: `BigDecimal`
  - `totalCredit`: `BigDecimal`
  - `consistencyStatus`: `String`
  - `consistencyNotes`: `List<String>`
  - `lines`: `List<JournalLineDto>`
  - `linkedDocuments`: `List<LinkedDocument>`
  - `settlementAllocations`: `List<SettlementAllocation>`
  - `eventTrail`: `List<EventTrailItem>`
  - `createdAt`: `Instant`
  - `updatedAt`: `Instant`
  - `postedAt`: `Instant`
  - `createdBy`: `String`
  - `postedBy`: `String`
  - `lastModifiedBy`: `String`
- **`CatalogImportResponse`**
  - `rowsProcessed`: `int`
  - `brandsCreated`: `int`
  - `productsCreated`: `int`
  - `productsUpdated`: `int`
  - `rawMaterialsSeeded`: `int`
  - `errors`: `List<ImportError>`
- **`List<ProductionProductDto>`**
  - `id`: `Long`
  - `publicId`: `UUID`
  - `brandId`: `Long`
  - `brandName`: `String`
  - `brandCode`: `String`
  - `productName`: `String`
  - `category`: `String`
  - `defaultColour`: `String`
  - `sizeLabel`: `String`
  - `unitOfMeasure`: `String`
  - `skuCode`: `String`
  - `active`: `boolean`
  - `basePrice`: `BigDecimal`
  - `gstRate`: `BigDecimal`
  - `minDiscountPercent`: `BigDecimal`
  - `minSellingPrice`: `BigDecimal`
  - `metadata`: `Map<String, Object>`
- **`ProductionProductDto`**
  - `id`: `Long`
  - `publicId`: `UUID`
  - `brandId`: `Long`
  - `brandName`: `String`
  - `brandCode`: `String`
  - `productName`: `String`
  - `category`: `String`
  - `defaultColour`: `String`
  - `sizeLabel`: `String`
  - `unitOfMeasure`: `String`
  - `skuCode`: `String`
  - `active`: `boolean`
  - `basePrice`: `BigDecimal`
  - `gstRate`: `BigDecimal`
  - `minDiscountPercent`: `BigDecimal`
  - `minSellingPrice`: `BigDecimal`
  - `metadata`: `Map<String, Object>`
- **`BulkVariantResponse`**
  - `generated`: `List<VariantItem>`
  - `conflicts`: `List<VariantItem>`
  - `wouldCreate`: `List<VariantItem>`
  - `created`: `List<VariantItem>`
- **`ConfigurationHealthService.ConfigurationHealthReport`**
  - `healthy`: `boolean` (true when no issues are present)
  - `issues`: `List<ConfigurationIssue>`
    - `companyCode`: `String` (tenant/company code where issue was detected)
    - `domain`: `String` (issue category, e.g. `DEFAULT_ACCOUNTS`, `TAX_ACCOUNT`, `PRODUCTION_METADATA`)
    - `reference`: `String` (entity reference such as SKU, `BASE`, or `COMPANY_DEFAULTS`)
    - `message`: `String` (human-readable remediation hint)
- **`Map<String, Object>`**
  - Primitive/raw payload type (no DTO field list).
- **`PartnerSettlementResponse`**
  - `journalEntry`: `JournalEntryDto`
  - `totalApplied`: `BigDecimal`
  - `cashAmount`: `BigDecimal`
  - `totalDiscount`: `BigDecimal`
  - `totalWriteOff`: `BigDecimal`
  - `totalFxGain`: `BigDecimal`
  - `totalFxLoss`: `BigDecimal`
  - `allocations`: `List<Allocation>`
- **`CompanyDefaultAccountsResponse`**
  - `inventoryAccountId`: `Long`
  - `cogsAccountId`: `Long`
  - `revenueAccountId`: `Long`
  - `discountAccountId`: `Long`
  - `taxAccountId`: `Long`
- **`GstReconciliationDto`**
  - `period`: `YearMonth`
  - `periodStart`: `LocalDate`
  - `periodEnd`: `LocalDate`
  - `collected`: `GstComponentSummary`
  - `inputTaxCredit`: `GstComponentSummary`
  - `netLiability`: `GstComponentSummary`
  - `cgst`: `BigDecimal`
  - `sgst`: `BigDecimal`
  - `igst`: `BigDecimal`
  - `total`: `BigDecimal`
- **`GstReturnDto`**
  - `period`: `YearMonth`
  - `periodStart`: `LocalDate`
  - `periodEnd`: `LocalDate`
  - `outputTax`: `BigDecimal`
  - `inputTax`: `BigDecimal`
  - `netPayable`: `BigDecimal`
- **`List<JournalEntryDto>`**
  - `id`: `Long`
  - `publicId`: `UUID`
  - `referenceNumber`: `String`
  - `entryDate`: `LocalDate`
  - `memo`: `String`
  - `status`: `String`
  - `dealerId`: `Long`
  - `dealerName`: `String`
  - `supplierId`: `Long`
  - `supplierName`: `String`
  - `accountingPeriodId`: `Long`
  - `accountingPeriodLabel`: `String`
  - `accountingPeriodStatus`: `String`
  - `reversalOfEntryId`: `Long`
  - `reversalEntryId`: `Long`
  - `correctionType`: `String`
  - `correctionReason`: `String`
  - `voidReason`: `String`
  - `lines`: `List<JournalLineDto>`
  - `createdAt`: `Instant`
  - `updatedAt`: `Instant`
  - `postedAt`: `Instant`
  - `createdBy`: `String`
  - `postedBy`: `String`
  - `lastModifiedBy`: `String`
- **`List<JournalListItemDto>`**
  - `id`: `Long`
  - `referenceNumber`: `String`
  - `entryDate`: `LocalDate`
  - `memo`: `String`
  - `status`: `String`
  - `journalType`: `String`
  - `sourceModule`: `String`
  - `sourceReference`: `String`
  - `totalDebit`: `BigDecimal`
  - `totalCredit`: `BigDecimal`
- **`MonthEndChecklistDto`**
  - `period`: `AccountingPeriodDto`
  - `items`: `List<MonthEndChecklistItemDto>`
  - `readyToClose`: `boolean`
- **`PayrollBatchPaymentResponse`**
  - `payrollRunId`: `Long`
  - `runDate`: `LocalDate`
  - `grossAmount`: `BigDecimal`
  - `totalTaxWithholding`: `BigDecimal`
  - `totalPfWithholding`: `BigDecimal`
  - `totalAdvances`: `BigDecimal`
  - `netPayAmount`: `BigDecimal`
  - `employerTaxAmount`: `BigDecimal`
  - `employerPfAmount`: `BigDecimal`
  - `totalEmployerCost`: `BigDecimal`
  - `payrollJournalId`: `Long`
  - `employerContribJournalId`: `Long`
  - `lines`: `List<LineTotal>`
- **`List<AccountingPeriodDto>`**
  - `id`: `Long`
  - `year`: `int`
  - `month`: `int`
  - `startDate`: `LocalDate`
  - `endDate`: `LocalDate`
  - `label`: `String`
  - `status`: `String`
  - `bankReconciled`: `boolean`
  - `bankReconciledAt`: `Instant`
  - `bankReconciledBy`: `String`
  - `inventoryCounted`: `boolean`
  - `inventoryCountedAt`: `Instant`
  - `inventoryCountedBy`: `String`
  - `closedAt`: `Instant`
  - `closedBy`: `String`
  - `closedReason`: `String`
  - `lockedAt`: `Instant`
  - `lockedBy`: `String`
  - `lockReason`: `String`
  - `reopenedAt`: `Instant`
  - `reopenedBy`: `String`
  - `reopenReason`: `String`
  - `closingJournalEntryId`: `Long`
  - `checklistNotes`: `String`
  - `costingMethod`: `String`
- **`AccountingPeriodDto`**
  - `id`: `Long`
  - `year`: `int`
  - `month`: `int`
  - `startDate`: `LocalDate`
  - `endDate`: `LocalDate`
  - `label`: `String`
  - `status`: `String`
  - `bankReconciled`: `boolean`
  - `bankReconciledAt`: `Instant`
  - `bankReconciledBy`: `String`
  - `inventoryCounted`: `boolean`
  - `inventoryCountedAt`: `Instant`
  - `inventoryCountedBy`: `String`
  - `closedAt`: `Instant`
  - `closedBy`: `String`
  - `closedReason`: `String`
  - `lockedAt`: `Instant`
  - `lockedBy`: `String`
  - `lockReason`: `String`
  - `reopenedAt`: `Instant`
  - `reopenedBy`: `String`
  - `reopenReason`: `String`
  - `closingJournalEntryId`: `Long`
  - `checklistNotes`: `String`
  - `costingMethod`: `String`
- **`BankReconciliationSummaryDto`**
  - `accountId`: `Long`
  - `accountCode`: `String`
  - `accountName`: `String`
  - `statementDate`: `LocalDate`
  - `ledgerBalance`: `BigDecimal`
  - `statementEndingBalance`: `BigDecimal`
  - `outstandingDeposits`: `BigDecimal`
  - `outstandingChecks`: `BigDecimal`
  - `difference`: `BigDecimal`
  - `balanced`: `boolean`
  - `unclearedDeposits`: `List<BankReconciliationItemDto>`
  - `unclearedChecks`: `List<BankReconciliationItemDto>`
- **`ReconciliationService.SubledgerReconciliationReport`**
  - `dealerReconciliation`: `ReconciliationResult`
  - `supplierReconciliation`: `SupplierReconciliationResult`
  - `combinedVariance`: `BigDecimal`
  - `reconciled`: `boolean`
- **`AgingReportService.DealerAgingDetail`**
  - `dealerId`: `Long`
  - `dealerCode`: `String`
  - `dealerName`: `String`
  - `buckets`: `AgingBuckets`
  - `totalOutstanding`: `BigDecimal`
- **`AgingReportService.DealerAgingDetailedReport`**
  - `dealerId`: `Long`
  - `dealerCode`: `String`
  - `dealerName`: `String`
  - `lineItems`: `List<AgingLineItem>`
  - `buckets`: `AgingBuckets`
  - `totalOutstanding`: `BigDecimal`
  - `averageDSO`: `double`
- **`AgingReportService.AgedReceivablesReport`**
  - `asOfDate`: `LocalDate`
  - `dealers`: `List<DealerAgingDetail>`
  - `totalBuckets`: `AgingBuckets`
  - `grandTotal`: `BigDecimal`
- **`AccountHierarchyService.BalanceSheetHierarchy`**
  - `assets`: `List<AccountNode>`
  - `totalAssets`: `BigDecimal`
  - `liabilities`: `List<AccountNode>`
  - `totalLiabilities`: `BigDecimal`
  - `equity`: `List<AccountNode>`
  - `totalEquity`: `BigDecimal`
- **`AgingReportService.DSOReport`**
  - `dealerId`: `Long`
  - `dealerName`: `String`
  - `averageDSO`: `double`
  - `totalOutstanding`: `BigDecimal`
  - `openInvoices`: `int`
  - `overdueInvoices`: `long`
- **`AccountHierarchyService.IncomeStatementHierarchy`**
  - `revenue`: `List<AccountNode>`
  - `totalRevenue`: `BigDecimal`
  - `cogs`: `List<AccountNode>`
  - `totalCogs`: `BigDecimal`
  - `grossProfit`: `BigDecimal`
  - `expenses`: `List<AccountNode>`
  - `totalExpenses`: `BigDecimal`
  - `netIncome`: `BigDecimal`
- **`PartnerStatementResponse`**
  - `partnerId`: `Long`
  - `partnerName`: `String`
  - `fromDate`: `LocalDate`
  - `toDate`: `LocalDate`
  - `openingBalance`: `BigDecimal`
  - `closingBalance`: `BigDecimal`
  - `transactions`: `List<StatementTransactionDto>`
- **`TemporalBalanceService.TrialBalanceSnapshot`**
  - `asOfDate`: `LocalDate`
  - `entries`: `List<TrialBalanceEntry>`
  - `totalDebits`: `BigDecimal`
  - `totalCredits`: `BigDecimal`

#### UI Hints (accounting screens)

- **Dropdown sources**
  - Account dropdowns: `GET /api/v1/accounting/accounts`
  - Dealer dropdowns/search: `GET /api/v1/dealers`, `GET /api/v1/dealers/search?query=`
  - Supplier dropdowns: `GET /api/v1/suppliers`
  - Catalog product selection in accounting context: `GET /api/v1/accounting/catalog/products`
- **Computed fields**
  - GST component split is computed server-side: `taxType=INTRA_STATE` => `cgst+sgst`; `INTER_STATE` => `igst`
  - Settlement totals (`totalApplied`, `totalDiscount`, `totalFxGain/loss`) are computed; render read-only summary cards
  - Statement running balances and period checklist readiness are server computed; never recompute from partial UI data
- **Dependent fields**
  - `sourceState`/`destState` (dealer/supplier/company state codes) decide GST type and tax split
  - In settlement requests, non-zero discount/write-off/fx values require corresponding account IDs (`discountAccountId`, `writeOffAccountId`, etc.)
  - Period close requires checklist controls satisfied unless `force=true` is explicitly used
- **Idempotency**
  - For mutation endpoints supporting replay protection, send `Idempotency-Key` (preferred). Legacy `X-Idempotency-Key` is accepted; mismatches are rejected.

### Product Catalog & Inventory

#### Endpoint Map (catalog)

All responses are wrapped in `ApiResponse<T>`.

| Method | Path | Auth | Request | Response `data` |
|---|---|---|---|---|
| POST | `/api/v1/catalog/brands` | `ROLE_ADMIN/ROLE_ACCOUNTING/ROLE_SALES/ROLE_FACTORY` | `CatalogBrandRequest` | `CatalogBrandDto` |
| GET | `/api/v1/catalog/brands` | `ROLE_ADMIN/ROLE_ACCOUNTING/ROLE_SALES/ROLE_FACTORY` | Query: `active?` | `List<CatalogBrandDto>` |
| GET | `/api/v1/catalog/brands/{brandId}` | `ROLE_ADMIN/ROLE_ACCOUNTING/ROLE_SALES/ROLE_FACTORY` | — | `CatalogBrandDto` |
| PUT | `/api/v1/catalog/brands/{brandId}` | `ROLE_ADMIN/ROLE_ACCOUNTING/ROLE_SALES/ROLE_FACTORY` | `CatalogBrandRequest` | `CatalogBrandDto` |
| DELETE | `/api/v1/catalog/brands/{brandId}` | `ROLE_ADMIN/ROLE_ACCOUNTING/ROLE_SALES/ROLE_FACTORY` | — | `CatalogBrandDto` (inactive) |
| POST | `/api/v1/catalog/products` | `ROLE_ADMIN/ROLE_ACCOUNTING/ROLE_SALES/ROLE_FACTORY` | `CatalogProductRequest` | `CatalogProductDto` |
| GET | `/api/v1/catalog/products` | `ROLE_ADMIN/ROLE_ACCOUNTING/ROLE_SALES/ROLE_FACTORY` | Query: `brandId?`, `color?`, `size?`, `active?`, `page`, `pageSize` | `PageResponse<CatalogProductDto>` |
| GET | `/api/v1/catalog/products/{productId}` | `ROLE_ADMIN/ROLE_ACCOUNTING/ROLE_SALES/ROLE_FACTORY` | — | `CatalogProductDto` |
| PUT | `/api/v1/catalog/products/{productId}` | `ROLE_ADMIN/ROLE_ACCOUNTING/ROLE_SALES/ROLE_FACTORY` | `CatalogProductRequest` | `CatalogProductDto` |
| DELETE | `/api/v1/catalog/products/{productId}` | `ROLE_ADMIN/ROLE_ACCOUNTING/ROLE_SALES/ROLE_FACTORY` | — | `CatalogProductDto` (inactive) |
| POST | `/api/v1/catalog/products/bulk` | `ROLE_ADMIN/ROLE_ACCOUNTING/ROLE_SALES/ROLE_FACTORY` | `List<CatalogProductBulkItemRequest>` | `CatalogProductBulkResponse` |

#### User Flows

1. **Brand setup**
   1. `POST /api/v1/catalog/brands` to create brand (name/logo/description)
   2. `GET /api/v1/catalog/brands?active=true` to refresh dropdown options

2. **Product setup (single)**
   1. `GET /api/v1/catalog/brands?active=true` for brand dropdown
   2. `POST /api/v1/catalog/products` with brand + colors + sizes + carton mapping
   3. `GET /api/v1/catalog/products?brandId={id}&page=0&pageSize=20` to refresh list

3. **Product bulk create/update**
   1. Build list payload for `POST /api/v1/catalog/products/bulk`
   2. Inspect `results[]` for per-item `success`, `action`, and `message`
   3. Re-submit failed rows only after validation corrections

4. **Catalog search/filter**
   1. Query `GET /api/v1/catalog/products` with optional `brandId`, `color`, `size`, `active`
   2. Use `page` + `pageSize` for pagination

#### State Machines

- **Brand lifecycle**: `ACTIVE -> INACTIVE` via `DELETE /api/v1/catalog/brands/{brandId}`
- **Product lifecycle**: `ACTIVE -> INACTIVE` via `DELETE /api/v1/catalog/products/{productId}`

#### Error Codes (catalog)

- `BUS_001` (`BUSINESS_INVALID_STATE`): brand inactive but used for product create/update
- `BUS_002` (`BUSINESS_DUPLICATE_ENTRY`): duplicate brand name or duplicate product name within brand
- `BUS_003` (`BUSINESS_ENTITY_NOT_FOUND`): brand/product not found for current company
- `VAL_001` (`VALIDATION_INVALID_INPUT`): invalid payload (missing colors/sizes/carton mapping, invalid GST range, etc.)

#### Data Contracts

- `CatalogBrandRequest`
  - `name` (required), `logoUrl` (optional), `description` (optional), `active` (optional)

- `CatalogBrandDto`
  - `id`, `publicId`, `name`, `code`, `logoUrl`, `description`, `active`

- `CatalogProductRequest`
  - `brandId` (required)
  - `name` (required)
  - `colors: string[]` (required, at least one)
  - `sizes: string[]` (required, at least one)
  - `cartonSizes: [{ size, piecesPerCarton }]` (required; every size must be mapped)
  - `unitOfMeasure` (required)
  - `hsnCode` (required)
  - `gstRate` (required, 0..100)
  - `active` (optional)

- `CatalogProductDto`
  - `id`, `publicId`, `brandId`, `brandName`, `brandCode`
  - `name`, `sku` (auto-generated by backend), `colors`, `sizes`, `cartonSizes`
  - `unitOfMeasure`, `hsnCode`, `gstRate`, `active`

- `CatalogProductBulkItemRequest`
  - `id` (optional), `sku` (optional), `product` (`CatalogProductRequest`, required)

- `CatalogProductBulkResponse`
  - `total`, `succeeded`, `failed`
  - `results[]` with `index`, `success`, `action`, `productId`, `sku`, `message`, `product`

#### UI Hints

- **Brand dropdown**: load from `GET /api/v1/catalog/brands?active=true` and store `brandId`
- **Color input**: multi-select chips/tags (`colors[]`)
- **Size grid**: editable rows for each size with mandatory `piecesPerCarton`; send as `cartonSizes[]`
- **Bulk upload screen**: show row-level status from `results[]`; allow retry only for failed rows
- **Search UX**: expose filters for brand, color, size, active status and server-side pagination controls

#### Inventory & Dispatch Endpoint Map (finished goods)

| Method | Path | Auth | Request | Response `data` |
|---|---|---|---|---|
| GET | `/api/v1/finished-goods` | `ROLE_ADMIN/ROLE_FACTORY/ROLE_SALES/ROLE_ACCOUNTING` | — | `List<FinishedGoodDto>` |
| GET | `/api/v1/finished-goods/{id}` | `ROLE_ADMIN/ROLE_FACTORY/ROLE_SALES/ROLE_ACCOUNTING` | — | `FinishedGoodDto` |
| POST | `/api/v1/finished-goods` | `ROLE_ADMIN/ROLE_FACTORY` | `FinishedGoodRequest` | `FinishedGoodDto` |
| PUT | `/api/v1/finished-goods/{id}` | `ROLE_ADMIN/ROLE_FACTORY` | `FinishedGoodRequest` | `FinishedGoodDto` |
| GET | `/api/v1/finished-goods/{id}/batches` | `ROLE_ADMIN/ROLE_FACTORY/ROLE_SALES` | — | `List<FinishedGoodBatchDto>` |
| POST | `/api/v1/finished-goods/{id}/batches` | `ROLE_ADMIN/ROLE_FACTORY` | `FinishedGoodBatchRequest` | `FinishedGoodBatchDto` |
| GET | `/api/v1/finished-goods/stock-summary` | `ROLE_ADMIN/ROLE_FACTORY/ROLE_SALES/ROLE_ACCOUNTING` | — | `List<StockSummaryDto>` |
| GET | `/api/v1/finished-goods/low-stock?threshold={n}` | `ROLE_ADMIN/ROLE_FACTORY/ROLE_SALES` | query `threshold` (default `100`) | `List<FinishedGoodDto>` |
| GET | `/api/v1/dispatch/pending` | `ROLE_ADMIN/ROLE_FACTORY/ROLE_SALES` | — | `List<PackagingSlipDto>` |
| GET | `/api/v1/dispatch/preview/{slipId}` | `ROLE_ADMIN/ROLE_FACTORY` | — | `DispatchPreviewDto` |
| GET | `/api/v1/dispatch/slip/{slipId}` | `ROLE_ADMIN/ROLE_FACTORY/ROLE_SALES` | — | `PackagingSlipDto` |
| GET | `/api/v1/dispatch/order/{orderId}` | `ROLE_ADMIN/ROLE_FACTORY/ROLE_SALES` | — | `PackagingSlipDto` |
| PATCH | `/api/v1/dispatch/slip/{slipId}/status?status={value}` | `ROLE_ADMIN/ROLE_FACTORY` | query `status` | `PackagingSlipDto` |
| POST | `/api/v1/dispatch/backorder/{slipId}/cancel?reason={text}` | `ROLE_ADMIN/ROLE_FACTORY` | optional query `reason` | `PackagingSlipDto` |
| POST | `/api/v1/dispatch/confirm` | `ROLE_ADMIN/ROLE_FACTORY` + `dispatch.confirm` | `DispatchConfirmationRequest` | `DispatchConfirmationResponse` |

#### Inventory & Dispatch User Flows

1. **Reserve → preview → confirm dispatch**
   1. Sales confirmation reserves stock and creates/updates packaging slip.
   2. Factory loads slips via `GET /api/v1/dispatch/pending`.
   3. Factory opens `GET /api/v1/dispatch/preview/{slipId}` for suggested ship qty and shortages.
   4. Factory submits shipped quantities using `POST /api/v1/dispatch/confirm`.
   5. UI refreshes with `GET /api/v1/dispatch/slip/{slipId}` or response payload.

2. **Backorder cancellation**
   1. For a `BACKORDER` slip, call `POST /api/v1/dispatch/backorder/{slipId}/cancel`.
   2. Backend releases reserved quantities and marks slip `CANCELLED`.
   3. Refresh order slip state from `GET /api/v1/dispatch/order/{orderId}`.

3. **Stock monitoring**
   1. Load rollup via `GET /api/v1/finished-goods/stock-summary`.
   2. Show warning list via `GET /api/v1/finished-goods/low-stock?threshold=...`.

#### Packaging Slip State Machine

- `PENDING` → `RESERVED` (inventory reserved)
- `RESERVED` → `DISPATCHED` (dispatch confirmed)
- `RESERVED`/`PENDING_*` → `BACKORDER` (shortage remains)
- `BACKORDER` → `CANCELLED` (cancel backorder endpoint)
- Terminal states: `DISPATCHED`, `CANCELLED`

#### Inventory/Dispatch Error Handling Notes

- `VALIDATION_INVALID_INPUT`: missing slip/line confirmation, invalid shipped qty.
- `VALIDATION_INVALID_STATE`: invalid slip transition, dispatched slip mutation attempt, insufficient reserved stock.
- `CONCURRENCY_CONFLICT`: duplicate/open backorder slip race; frontend should refetch slip and retry once.

#### Backend Decomposition Note

Backend now routes finished-goods logic through focused services: catalog, stock, reservation, and dispatch. API contracts above are backward-compatible; frontend behavior does not need endpoint changes.

### Sales & Dealers

#### GST Fields

- Dealer create/update payloads support:
  - `gstNumber` (15-char GSTIN, optional)
  - `stateCode` (2-char Indian state code, optional)
  - `gstRegistrationType` (`REGULAR | COMPOSITION | UNREGISTERED`, optional; defaults to `UNREGISTERED`)

#### Sales Invoice GST Component Exposure

- Invoice line DTO now includes component fields:
  - `cgstAmount`
  - `sgstAmount`
  - `igstAmount`

These values are populated during dispatch confirmation and returned in invoice APIs.

### Purchasing & Suppliers

#### GST Fields

- Supplier create/update payloads support:
  - `gstNumber` (15-char GSTIN, optional)
  - `stateCode` (2-char Indian state code, optional)
  - `gstRegistrationType` (`REGULAR | COMPOSITION | UNREGISTERED`, optional; defaults to `UNREGISTERED`)

#### Purchase GST Component Exposure

- Raw material purchase line response now includes:
  - `cgstAmount`
  - `sgstAmount`
  - `igstAmount`

GST components are computed per line using company state vs supplier state:
- Same state => CGST + SGST split
- Different state => IGST

### HR & Payroll
_To be documented_

### Reports
_To be documented_
