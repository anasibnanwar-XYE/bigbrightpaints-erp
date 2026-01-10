# API ↔ Portal Matrix

This document is the **manual source of truth** for:
- endpoint → portal responsibility
- endpoint → auth (role/permission)
- endpoint → deprecation/alias mapping

Maintained by: `tasks/debugging/task-02-endpoint-and-portal-matrix.md`.

## Portals (allowed set)
- **Admin**: users/roles/settings/company configuration, ops diagnostics
- **Accounting**: accounting + inventory + purchasing/AP + HR/payroll (posting, reconciliation, close)
- **Sales**: dealer creation, orders, dispatch confirmation trigger, invoices
- **Manufacturing**: factory + production (logs, packing, batch dispatch)
- **Dealer**: read-only dealer self-service (ledger/invoices/orders/outstanding)

## Auth interpretation
- `permitAll (SecurityConfig)`: public endpoint (must be safe to expose).
- `authenticated() (no @PreAuthorize found)`: any authenticated user can call it (treat as **security review required** unless explicitly intended).
- `denyAll()`: endpoint is intentionally disabled (document in Notes).
- Otherwise: the exact `@PreAuthorize(...)` expression from code.

Company scoping: `CompanyContextFilter` enforces `X-Company-Id` or JWT `cid` and validates company access; `CompanyContextService` applies company scoping. Dealer endpoints note explicit self-scope enforcement.

## Deprecated endpoints ledger
| Endpoint | Status | Canonical replacement | Proof required | Tests required | Notes |
|---|---|---|---|---|---|
| `POST /api/v1/dispatch/confirm` | deprecated | `POST /api/v1/sales/dispatch/confirm` | 0 hits in access logs for 2 releases + client inventory shows canonical endpoint only. | `OpenApiSnapshotIT` + `SalesControllerIT` | Legacy alias of `POST /api/v1/sales/dispatch/confirm` (canonical). |
| `GET /api/v1/hr/payroll-runs` | deprecated | `GET /api/v1/payroll/runs` | 0 hits in access logs for 2 releases + client inventory shows canonical endpoint only. | `OpenApiSnapshotIT` + `HrControllerIT` | Legacy alias of `GET /api/v1/payroll/runs` (canonical). |
| `POST /api/v1/hr/payroll-runs` | deprecated | `POST /api/v1/payroll/runs` | 0 hits in access logs for 2 releases + client inventory shows canonical endpoint only. | `OpenApiSnapshotIT` + `HrControllerIT` | Legacy alias of `POST /api/v1/payroll/runs` (canonical). |
| `POST /api/v1/orchestrator/dispatch/{orderId}` | deprecated | `POST /api/v1/orchestrator/dispatch` | 0 hits in access logs for 2 releases + callers migrated to body-based dispatch. | `OpenApiSnapshotIT` + `OrchestratorControllerIT` | Alias of `POST /api/v1/orchestrator/dispatch` with `{orderId}` in path. |
| `GET /api/v1/sales/dealers` | deprecated | `GET /api/v1/dealers` | 0 hits in access logs for 2 releases + portals updated to canonical endpoint. | `OpenApiSnapshotIT` + `SalesControllerIT` | Alias of `GET /api/v1/dealers` (canonical). |
| `GET /api/v1/sales/dealers/search` | deprecated | `GET /api/v1/dealers/search` | 0 hits in access logs for 2 releases + portals updated to canonical endpoint. | `OpenApiSnapshotIT` + `SalesControllerIT` | Alias of `GET /api/v1/dealers/search` (canonical). |

---

## Endpoint matrix

### Shared (All portals)
| Endpoint | Portal | Auth (roles/permissions) | Notes | Code anchor |
|---|---|---|---|---|
| `POST /api/v1/auth/login` | Admin/Accounting/Sales/Manufacturing/Dealer | `permitAll (SecurityConfig)` |  | `AuthController#login` |
| `POST /api/v1/auth/logout` | Admin/Accounting/Sales/Manufacturing/Dealer | `isAuthenticated()` |  | `AuthController#logout` |
| `GET /api/v1/auth/me` | Admin/Accounting/Sales/Manufacturing/Dealer | `isAuthenticated()` |  | `AuthController#me` |
| `POST /api/v1/auth/mfa/activate` | Admin/Accounting/Sales/Manufacturing/Dealer | `isAuthenticated()` |  | `MfaController#activate` |
| `POST /api/v1/auth/mfa/disable` | Admin/Accounting/Sales/Manufacturing/Dealer | `isAuthenticated()` |  | `MfaController#disable` |
| `POST /api/v1/auth/mfa/setup` | Admin/Accounting/Sales/Manufacturing/Dealer | `isAuthenticated()` |  | `MfaController#setup` |
| `GET /api/v1/auth/profile` | Admin/Accounting/Sales/Manufacturing/Dealer | `isAuthenticated()` |  | `UserProfileController#profile` |
| `PUT /api/v1/auth/profile` | Admin/Accounting/Sales/Manufacturing/Dealer | `isAuthenticated()` |  | `UserProfileController#update` |
| `POST /api/v1/auth/password/change` | Admin/Accounting/Sales/Manufacturing/Dealer | `isAuthenticated()` |  | `AuthController#changePassword` |
| `POST /api/v1/auth/password/forgot` | Admin/Accounting/Sales/Manufacturing/Dealer | `permitAll (SecurityConfig)` |  | `AuthController#forgotPassword` |
| `POST /api/v1/auth/password/reset` | Admin/Accounting/Sales/Manufacturing/Dealer | `permitAll (SecurityConfig)` |  | `AuthController#resetPassword` |
| `POST /api/v1/auth/refresh-token` | Admin/Accounting/Sales/Manufacturing/Dealer | `permitAll (SecurityConfig)` |  | `AuthController#refresh` |
| `GET /api/v1/companies` | Admin/Accounting/Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_SALES')` |  | `CompanyController#list` |
| `POST /api/v1/companies` | Admin | `denyAll()` | Disabled: tenant company creation blocked. | `CompanyController#create` |
| `DELETE /api/v1/companies/{id}` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `CompanyController#delete` |
| `PUT /api/v1/companies/{id}` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `CompanyController#update` |
| `POST /api/v1/multi-company/companies/switch` | Admin/Accounting/Sales/Manufacturing/Dealer | `isAuthenticated()` |  | `MultiCompanyController#switchCompany` |
| `GET /api/v1/orchestrator/dashboard/admin` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `DashboardController#adminDashboard` |
| `GET /api/v1/orchestrator/dashboard/factory` | Admin/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `DashboardController#factoryDashboard` |
| `GET /api/v1/orchestrator/dashboard/finance` | Admin/Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `DashboardController#financeDashboard` |
| `POST /api/v1/orchestrator/dispatch` | Admin/Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES')` |  | `OrchestratorController#dispatchOrder` |
| `POST /api/v1/orchestrator/dispatch/{orderId}` | Admin/Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES')` | Alias of `POST /api/v1/orchestrator/dispatch` with `{orderId}` in body. | `OrchestratorController#dispatchOrderAlias` |
| `POST /api/v1/orchestrator/factory/dispatch/{batchId}` | Admin/Manufacturing | `hasAuthority('ROLE_ADMIN') or (hasAuthority('ROLE_FACTORY') and hasAuthority('factory.dispatch'))` |  | `OrchestratorController#dispatch` |
| `GET /api/v1/orchestrator/health/events` | Admin | `authenticated() (no @PreAuthorize found)` | Security review required: no method-level guard. | `OrchestratorController#eventHealth` |
| `GET /api/v1/orchestrator/health/integrations` | Admin | `authenticated() (no @PreAuthorize found)` | Security review required: no method-level guard. | `OrchestratorController#integrationsHealth` |
| `POST /api/v1/orchestrator/orders/{orderId}/approve` | Admin/Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES')` |  | `OrchestratorController#approveOrder` |
| `POST /api/v1/orchestrator/orders/{orderId}/fulfillment` | Admin/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `OrchestratorController#fulfillOrder` |
| `POST /api/v1/orchestrator/payroll/run` | Admin/Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING') and hasAuthority('payroll.run')` |  | `OrchestratorController#runPayroll` |
| `GET /api/v1/orchestrator/traces/{traceId}` | Admin | `authenticated() (no @PreAuthorize found)` | Security review required: no method-level guard. | `OrchestratorController#trace` |

### Admin
| Endpoint | Portal | Auth (roles/permissions) | Notes | Code anchor |
|---|---|---|---|---|
| `GET /api/integration/health` | Admin | `permitAll (SecurityConfig)` |  | `IntegrationHealthController#health` |
| `GET /api/v1/admin/approvals` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `AdminSettingsController#approvals` |
| `POST /api/v1/admin/notify` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `AdminSettingsController#notifyUser` |
| `GET /api/v1/admin/roles` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `RoleController#listRoles` |
| `POST /api/v1/admin/roles` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `RoleController#createRole` |
| `GET /api/v1/admin/roles/{roleKey}` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `RoleController#getRoleByKey` |
| `GET /api/v1/admin/settings` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `AdminSettingsController#getSettings` |
| `PUT /api/v1/admin/settings` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `AdminSettingsController#updateSettings` |
| `GET /api/v1/admin/users` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `AdminUserController#list` |
| `POST /api/v1/admin/users` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `AdminUserController#create` |
| `DELETE /api/v1/admin/users/{id}` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `AdminUserController#delete` |
| `PUT /api/v1/admin/users/{id}` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `AdminUserController#update` |
| `PATCH /api/v1/admin/users/{id}/mfa/disable` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `AdminUserController#disableMfa` |
| `PATCH /api/v1/admin/users/{id}/suspend` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `AdminUserController#suspend` |
| `PATCH /api/v1/admin/users/{id}/unsuspend` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `AdminUserController#unsuspend` |
| `GET /api/v1/demo/ping` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `DemoController#ping` |
| `GET /api/v1/portal/dashboard` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `PortalInsightsController#dashboard` |
| `GET /api/v1/portal/operations` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `PortalInsightsController#operations` |
| `GET /api/v1/portal/workforce` | Admin | `hasAuthority('ROLE_ADMIN')` |  | `PortalInsightsController#workforce` |

### Accounting
| Endpoint | Portal | Auth (roles/permissions) | Notes | Code anchor |
|---|---|---|---|---|
| `GET /api/v1/accounting/accounts` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#accounts` |
| `POST /api/v1/accounting/accounts` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#createAccount` |
| `GET /api/v1/accounting/accounts/tree` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#getChartOfAccountsTree` |
| `GET /api/v1/accounting/accounts/tree/{type}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#getAccountTreeByType` |
| `GET /api/v1/accounting/accounts/{accountId}/activity` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#getAccountActivity` |
| `GET /api/v1/accounting/accounts/{accountId}/balance/as-of` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#getBalanceAsOf` |
| `GET /api/v1/accounting/accounts/{accountId}/balance/compare` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#compareBalances` |
| `GET /api/v1/accounting/trial-balance/as-of` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#getTrialBalanceAsOf` |
| `POST /api/v1/accounting/accruals` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#postAccrual` |
| `GET /api/v1/accounting/aging/dealers/{dealerId}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#dealerAging` |
| `GET /api/v1/accounting/aging/dealers/{dealerId}/pdf` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#dealerAgingPdf` |
| `GET /api/v1/accounting/aging/suppliers/{supplierId}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#supplierAging` |
| `GET /api/v1/accounting/aging/suppliers/{supplierId}/pdf` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#supplierAgingPdf` |
| `GET /api/v1/accounting/audit/digest` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#auditDigest` |
| `GET /api/v1/accounting/audit/digest.csv` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#auditDigestCsv` |
| `POST /api/v1/accounting/bad-debts/write-off` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#writeOffBadDebt` |
| `POST /api/v1/accounting/catalog/import` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingCatalogController#importCatalog` |
| `GET /api/v1/accounting/catalog/products` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingCatalogController#listProducts` |
| `POST /api/v1/accounting/catalog/products` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingCatalogController#createProduct` |
| `POST /api/v1/accounting/catalog/products/bulk-variants` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingCatalogController#createVariants` |
| `PUT /api/v1/accounting/catalog/products/{id}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingCatalogController#updateProduct` |
| `POST /api/v1/accounting/credit-notes` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#postCreditNote` |
| `POST /api/v1/accounting/debit-notes` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#postDebitNote` |
| `GET /api/v1/accounting/default-accounts` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#defaultAccounts` |
| `PUT /api/v1/accounting/default-accounts` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#updateDefaultAccounts` |
| `GET /api/v1/accounting/configuration/health` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingConfigurationController#health` |
| `GET /api/v1/accounting/gst/return` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#generateGstReturn` |
| `GET /api/v1/accounting/onboarding/account-suggestions` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#accountSuggestions` |
| `GET /api/v1/accounting/onboarding/brands` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#listBrands` |
| `POST /api/v1/accounting/onboarding/brands` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#upsertBrand` |
| `PUT /api/v1/accounting/onboarding/brands/{brandId}` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#updateBrand` |
| `GET /api/v1/accounting/onboarding/categories` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#listCategories` |
| `POST /api/v1/accounting/onboarding/categories` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#upsertCategory` |
| `PUT /api/v1/accounting/onboarding/categories/{categoryId}` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#updateCategory` |
| `GET /api/v1/accounting/onboarding/products` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#listProducts` |
| `POST /api/v1/accounting/onboarding/products` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#upsertProduct` |
| `PUT /api/v1/accounting/onboarding/products/{productId}` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#updateProduct` |
| `POST /api/v1/accounting/onboarding/products/variants` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#createVariants` |
| `GET /api/v1/accounting/onboarding/raw-materials` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#listRawMaterials` |
| `POST /api/v1/accounting/onboarding/raw-materials` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#upsertRawMaterial` |
| `PUT /api/v1/accounting/onboarding/raw-materials/{id}` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#updateRawMaterial` |
| `GET /api/v1/accounting/onboarding/suppliers` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#listSuppliers` |
| `POST /api/v1/accounting/onboarding/suppliers` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#upsertSupplier` |
| `PUT /api/v1/accounting/onboarding/suppliers/{id}` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#updateSupplier` |
| `GET /api/v1/accounting/onboarding/dealers` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#listDealers` |
| `POST /api/v1/accounting/onboarding/dealers` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#upsertDealer` |
| `PUT /api/v1/accounting/onboarding/dealers/{dealerId}` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). | `OnboardingController#updateDealer` |
| `POST /api/v1/accounting/onboarding/opening-stock` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). Financially significant: requires full evidence chain (see Task 03). | `OnboardingController#openingStock` |
| `POST /api/v1/accounting/onboarding/opening-balances/dealers` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). Financially significant: requires full evidence chain (see Task 03). | `OnboardingController#openingReceivable` |
| `POST /api/v1/accounting/onboarding/opening-balances/suppliers` | Accounting | `hasAuthority('ROLE_ADMIN') and hasAuthority('portal:accounting') and hasAuthority('onboarding.manage')` | Onboarding (admin-only). Financially significant: requires full evidence chain (see Task 03). | `OnboardingController#openingPayable` |
| `POST /api/v1/accounting/inventory/landed-cost` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#recordLandedCost` |
| `POST /api/v1/accounting/inventory/revaluation` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#revalueInventory` |
| `POST /api/v1/accounting/inventory/wip-adjustment` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#adjustWip` |
| `GET /api/v1/accounting/journal-entries` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#journalEntries` |
| `POST /api/v1/accounting/journal-entries` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#createJournalEntry` |
| `POST /api/v1/accounting/journal-entries/{entryId}/cascade-reverse` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#recordDealerReceipt` |
| `POST /api/v1/accounting/journal-entries/{entryId}/reverse` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#reverseJournalEntry` |
| `GET /api/v1/accounting/month-end/checklist` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#checklist` |
| `POST /api/v1/accounting/month-end/checklist/{periodId}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#updateChecklist` |
| `POST /api/v1/accounting/payroll/payments` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#recordPayrollPayment` |
| `POST /api/v1/accounting/payroll/payments/batch` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `PayrollController#processBatchPayment` |
| `GET /api/v1/accounting/periods` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#listPeriods` |
| `POST /api/v1/accounting/periods/{periodId}/close` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#closePeriod` |
| `POST /api/v1/accounting/periods/{periodId}/lock` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#lockPeriod` |
| `POST /api/v1/accounting/periods/{periodId}/reopen` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#reopenPeriod` |
| `GET /api/v1/accounting/raw-materials` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_FACTORY')` |  | `RawMaterialController#listRawMaterials` |
| `POST /api/v1/accounting/raw-materials` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_FACTORY')` | Financially significant: requires full evidence chain (see Task 03). | `RawMaterialController#createRawMaterial` |
| `DELETE /api/v1/accounting/raw-materials/{id}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_FACTORY')` |  | `RawMaterialController#deleteRawMaterial` |
| `PUT /api/v1/accounting/raw-materials/{id}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_FACTORY')` |  | `RawMaterialController#updateRawMaterial` |
| `POST /api/v1/accounting/receipts/dealer` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#recordDealerReceipt` |
| `POST /api/v1/accounting/receipts/dealer/hybrid` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#recordDealerHybridReceipt` |
| `GET /api/v1/accounting/reports/aging/receivables` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#getAgedReceivables` |
| `GET /api/v1/accounting/reports/aging/dealer/{dealerId}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#getDealerAging` |
| `GET /api/v1/accounting/reports/aging/dealer/{dealerId}/detailed` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#getDealerAgingDetailed` |
| `GET /api/v1/accounting/reports/dso/dealer/{dealerId}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#getDealerDSO` |
| `GET /api/v1/accounting/reports/balance-sheet/hierarchy` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#getBalanceSheetHierarchy` |
| `GET /api/v1/accounting/reports/income-statement/hierarchy` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#getIncomeStatementHierarchy` |
| `GET /api/v1/accounting/reports/aged-debtors` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `ReportController#agedDebtors` |
| `GET /api/v1/accounting/sales/returns` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_SALES')` |  | `AccountingController#listSalesReturns` |
| `POST /api/v1/accounting/sales/returns` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#recordSalesReturn` |
| `POST /api/v1/accounting/settlements/dealers` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#settleDealer` |
| `POST /api/v1/accounting/settlements/suppliers` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#settleSupplier` |
| `GET /api/v1/accounting/statements/dealers/{dealerId}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#dealerStatement` |
| `GET /api/v1/accounting/statements/dealers/{dealerId}/pdf` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#dealerStatementPdf` |
| `GET /api/v1/accounting/statements/suppliers/{supplierId}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#supplierStatement` |
| `GET /api/v1/accounting/statements/suppliers/{supplierId}/pdf` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `AccountingController#supplierStatementPdf` |
| `POST /api/v1/accounting/suppliers/payments` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `AccountingController#recordSupplierPayment` |
| `POST /api/v1/hr/attendance/bulk-mark` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrController#bulkMarkAttendance` |
| `GET /api/v1/hr/attendance/date/{date}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrController#attendanceByDate` |
| `GET /api/v1/hr/attendance/employee/{employeeId}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrController#employeeAttendance` |
| `POST /api/v1/hr/attendance/mark/{employeeId}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrController#markAttendance` |
| `GET /api/v1/hr/attendance/summary` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrController#attendanceSummary` |
| `GET /api/v1/hr/attendance/today` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrController#attendanceToday` |
| `GET /api/v1/hr/employees` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrController#employees` |
| `POST /api/v1/hr/employees` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrController#createEmployee` |
| `DELETE /api/v1/hr/employees/{id}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrController#deleteEmployee` |
| `PUT /api/v1/hr/employees/{id}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrController#updateEmployee` |
| `GET /api/v1/hr/leave-requests` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrController#leaveRequests` |
| `POST /api/v1/hr/leave-requests` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrController#createLeaveRequest` |
| `PATCH /api/v1/hr/leave-requests/{id}/status` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrController#updateLeaveStatus` |
| `GET /api/v1/hr/payroll-runs` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Legacy alias of `GET /api/v1/payroll/runs` (canonical). | `HrController#payrollRuns` |
| `POST /api/v1/hr/payroll-runs` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Legacy alias of `POST /api/v1/payroll/runs` (canonical). | `HrController#createPayrollRun` |
| `GET /api/v1/inventory/adjustments` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `InventoryAdjustmentController#listAdjustments` |
| `POST /api/v1/inventory/adjustments` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `InventoryAdjustmentController#createAdjustment` |
| `POST /api/v1/inventory/opening-stock` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_FACTORY')` |  | `OpeningStockImportController#importOpeningStock` |
| `GET /api/v1/payroll/runs` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrPayrollController#listPayrollRuns` |
| `POST /api/v1/payroll/runs` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `HrPayrollController#createPayrollRun` |
| `GET /api/v1/payroll/runs/monthly` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrPayrollController#listMonthlyPayrollRuns` |
| `POST /api/v1/payroll/runs/monthly` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `HrPayrollController#createMonthlyPayrollRun` |
| `GET /api/v1/payroll/runs/weekly` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrPayrollController#listWeeklyPayrollRuns` |
| `POST /api/v1/payroll/runs/weekly` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `HrPayrollController#createWeeklyPayrollRun` |
| `GET /api/v1/payroll/runs/{id}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrPayrollController#getPayrollRun` |
| `POST /api/v1/payroll/runs/{id}/approve` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `HrPayrollController#approvePayroll` |
| `POST /api/v1/payroll/runs/{id}/calculate` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `HrPayrollController#calculatePayroll` |
| `GET /api/v1/payroll/runs/{id}/lines` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrPayrollController#getPayrollRunLines` |
| `POST /api/v1/payroll/runs/{id}/mark-paid` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `HrPayrollController#markAsPaid` |
| `POST /api/v1/payroll/runs/{id}/post` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `HrPayrollController#postPayroll` |
| `GET /api/v1/payroll/summary/current-month` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrPayrollController#getCurrentMonthPaySummary` |
| `GET /api/v1/payroll/summary/current-week` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrPayrollController#getCurrentWeekPaySummary` |
| `GET /api/v1/payroll/summary/monthly` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrPayrollController#getMonthlyPaySummary` |
| `GET /api/v1/payroll/summary/weekly` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `HrPayrollController#getWeeklyPaySummary` |
| `GET /api/v1/purchasing/raw-material-purchases` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `RawMaterialPurchaseController#listPurchases` |
| `POST /api/v1/purchasing/raw-material-purchases` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `RawMaterialPurchaseController#createPurchase` |
| `POST /api/v1/purchasing/raw-material-purchases/returns` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` | Financially significant: requires full evidence chain (see Task 03). | `RawMaterialPurchaseController#recordPurchaseReturn` |
| `GET /api/v1/purchasing/raw-material-purchases/{id}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `RawMaterialPurchaseController#getPurchase` |
| `GET /api/v1/reports/account-statement` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `ReportController#accountStatement` |
| `GET /api/v1/reports/balance-sheet` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `ReportController#balanceSheet` |
| `GET /api/v1/reports/balance-warnings` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `ReportController#balanceWarnings` |
| `GET /api/v1/reports/cash-flow` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `ReportController#cashFlow` |
| `GET /api/v1/reports/inventory-reconciliation` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `ReportController#inventoryReconciliation` |
| `GET /api/v1/reports/inventory-valuation` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `ReportController#inventoryValuation` |
| `GET /api/v1/reports/monthly-production-costs` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `ReportController#monthlyProductionCosts` |
| `GET /api/v1/reports/production-logs/{id}/cost-breakdown` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `ReportController#costBreakdown` |
| `GET /api/v1/reports/profit-loss` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `ReportController#profitLoss` |
| `GET /api/v1/reports/reconciliation-dashboard` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `ReportController#reconciliationDashboard` |
| `GET /api/v1/reports/trial-balance` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `ReportController#trialBalance` |
| `GET /api/v1/reports/wastage` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `ReportController#wastageReport` |
| `GET /api/v1/suppliers` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_FACTORY')` |  | `SupplierController#listSuppliers` |
| `POST /api/v1/suppliers` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `SupplierController#createSupplier` |
| `GET /api/v1/suppliers/{id}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_FACTORY')` |  | `SupplierController#getSupplier` |
| `PUT /api/v1/suppliers/{id}` | Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `SupplierController#updateSupplier` |

### Accounting/Manufacturing
| Endpoint | Portal | Auth (roles/permissions) | Notes | Code anchor |
|---|---|---|---|---|
| `GET /api/v1/raw-material-batches/{rawMaterialId}` | Accounting/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_FACTORY')` |  | `RawMaterialController#batches` |
| `POST /api/v1/raw-material-batches/{rawMaterialId}` | Accounting/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_FACTORY')` |  | `RawMaterialController#createBatch` |
| `POST /api/v1/raw-materials/intake` | Accounting/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_FACTORY')` |  | `RawMaterialController#intake` |
| `GET /api/v1/raw-materials/stock` | Accounting/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_FACTORY')` |  | `RawMaterialController#stockSummary` |
| `GET /api/v1/raw-materials/stock/inventory` | Accounting/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_FACTORY')` |  | `RawMaterialController#inventory` |
| `GET /api/v1/raw-materials/stock/low-stock` | Accounting/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING','ROLE_FACTORY')` |  | `RawMaterialController#lowStock` |

### Accounting/Sales/Manufacturing
| Endpoint | Portal | Auth (roles/permissions) | Notes | Code anchor |
|---|---|---|---|---|
| `GET /api/v1/finished-goods` | Accounting/Sales/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY','ROLE_SALES','ROLE_ACCOUNTING')` |  | `FinishedGoodController#listFinishedGoods` |
| `GET /api/v1/finished-goods/low-stock` | Accounting/Sales/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY','ROLE_SALES')` |  | `FinishedGoodController#getLowStockItems` |
| `GET /api/v1/finished-goods/stock-summary` | Accounting/Sales/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY','ROLE_SALES','ROLE_ACCOUNTING')` |  | `FinishedGoodController#getStockSummary` |
| `GET /api/v1/finished-goods/{id}` | Accounting/Sales/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY','ROLE_SALES','ROLE_ACCOUNTING')` |  | `FinishedGoodController#getFinishedGood` |
| `PUT /api/v1/finished-goods/{id}` | Accounting/Sales/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `FinishedGoodController#updateFinishedGood` |
| `GET /api/v1/finished-goods/{id}/batches` | Accounting/Sales/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY','ROLE_SALES')` |  | `FinishedGoodController#listBatches` |
| `POST /api/v1/finished-goods/{id}/batches` | Accounting/Sales/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `FinishedGoodController#registerBatch` |

### Sales
| Endpoint | Portal | Auth (roles/permissions) | Notes | Code anchor |
|---|---|---|---|---|
| `GET /api/v1/dealers` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES')` |  | `DealerController#listDealers` |
| `POST /api/v1/dealers` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES')` |  | `DealerController#createDealer` |
| `GET /api/v1/dealers/search` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES')` |  | `DealerController#searchDealers` |
| `PUT /api/v1/dealers/{dealerId}` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES')` |  | `DealerController#updateDealer` |
| `GET /api/v1/dealers/{dealerId}/aging` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES','ROLE_DEALER')` | Dealer self-scope enforced via `DealerPortalService.verifyDealerAccess`. | `DealerController#dealerAging` |
| `POST /api/v1/dealers/{dealerId}/dunning/hold` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES','ROLE_ACCOUNTING')` |  | `DealerController#holdIfOverdue` |
| `GET /api/v1/dealers/{dealerId}/invoices` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES','ROLE_DEALER')` | Dealer self-scope enforced via `DealerPortalService.verifyDealerAccess`. | `DealerController#dealerInvoices` |
| `GET /api/v1/dealers/{dealerId}/ledger` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES','ROLE_DEALER')` | Dealer self-scope enforced via `DealerPortalService.verifyDealerAccess`. | `DealerController#dealerLedger` |
| `GET /api/v1/invoices/dealers/{dealerId}` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES','ROLE_ACCOUNTING')` |  | `InvoiceController#dealerInvoices` |
| `GET /api/v1/invoices` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES','ROLE_ACCOUNTING')` |  | `InvoiceController#listInvoices` |
| `GET /api/v1/invoices/{id}` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES','ROLE_ACCOUNTING')` |  | `InvoiceController#getInvoice` |
| `POST /api/v1/invoices/{id}/email` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES','ROLE_ACCOUNTING')` |  | `InvoiceController#sendInvoiceEmail` |
| `GET /api/v1/invoices/{id}/pdf` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES','ROLE_ACCOUNTING')` |  | `InvoiceController#downloadInvoicePdf` |
| `GET /api/v1/sales/credit-requests` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES')` |  | `SalesController#creditRequests` |
| `POST /api/v1/sales/credit-requests` | Sales | `hasAnyAuthority('ROLE_SALES','ROLE_ADMIN')` |  | `SalesController#createCreditRequest` |
| `PUT /api/v1/sales/credit-requests/{id}` | Sales | `hasAnyAuthority('ROLE_SALES','ROLE_ADMIN')` |  | `SalesController#updateCreditRequest` |
| `GET /api/v1/sales/dealers` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES')` | Alias of `GET /api/v1/dealers` (canonical). | `SalesController#listDealers` |
| `GET /api/v1/sales/dealers/search` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES')` | Alias of `GET /api/v1/dealers/search` (canonical). | `SalesController#searchDealers` |
| `POST /api/v1/sales/dispatch/confirm` | Sales | `hasAnyAuthority('ROLE_SALES','ROLE_ACCOUNTING','ROLE_ADMIN')` | Financially significant: requires full evidence chain (see Task 03). | `SalesController#confirmDispatch` |
| `GET /api/v1/sales/orders` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES','ROLE_FACTORY')` |  | `SalesController#orders` |
| `POST /api/v1/sales/orders` | Sales | `hasAnyAuthority('ROLE_SALES','ROLE_ADMIN')` |  | `SalesController#createOrder` |
| `DELETE /api/v1/sales/orders/{id}` | Sales | `hasAnyAuthority('ROLE_SALES','ROLE_ADMIN')` |  | `SalesController#deleteOrder` |
| `PUT /api/v1/sales/orders/{id}` | Sales | `hasAnyAuthority('ROLE_SALES','ROLE_ADMIN')` |  | `SalesController#updateOrder` |
| `POST /api/v1/sales/orders/{id}/cancel` | Sales | `hasAnyAuthority('ROLE_SALES','ROLE_ADMIN')` |  | `SalesController#cancelOrder` |
| `POST /api/v1/sales/orders/{id}/confirm` | Sales | `hasAnyAuthority('ROLE_SALES','ROLE_ADMIN')` |  | `SalesController#confirmOrder` |
| `PATCH /api/v1/sales/orders/{id}/status` | Sales | `hasAnyAuthority('ROLE_SALES','ROLE_ADMIN')` |  | `SalesController#updateStatus` |
| `GET /api/v1/sales/promotions` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES','ROLE_DEALER')` |  | `SalesController#promotions` |
| `POST /api/v1/sales/promotions` | Sales | `hasAnyAuthority('ROLE_SALES','ROLE_ADMIN')` |  | `SalesController#createPromotion` |
| `DELETE /api/v1/sales/promotions/{id}` | Sales | `hasAnyAuthority('ROLE_SALES','ROLE_ADMIN')` |  | `SalesController#deletePromotion` |
| `PUT /api/v1/sales/promotions/{id}` | Sales | `hasAnyAuthority('ROLE_SALES','ROLE_ADMIN')` |  | `SalesController#updatePromotion` |
| `GET /api/v1/sales/targets` | Sales | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES')` |  | `SalesController#targets` |
| `POST /api/v1/sales/targets` | Sales | `hasAnyAuthority('ROLE_SALES','ROLE_ADMIN')` |  | `SalesController#createTarget` |
| `DELETE /api/v1/sales/targets/{id}` | Sales | `hasAnyAuthority('ROLE_SALES','ROLE_ADMIN')` |  | `SalesController#deleteTarget` |
| `PUT /api/v1/sales/targets/{id}` | Sales | `hasAnyAuthority('ROLE_SALES','ROLE_ADMIN')` |  | `SalesController#updateTarget` |

### Sales/Accounting
| Endpoint | Portal | Auth (roles/permissions) | Notes | Code anchor |
|---|---|---|---|---|
| `GET /api/v1/credit/override-requests` | Sales/Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `CreditLimitOverrideController#listRequests` |
| `POST /api/v1/credit/override-requests/{id}/approve` | Sales/Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `CreditLimitOverrideController#approveRequest` |
| `POST /api/v1/credit/override-requests/{id}/reject` | Sales/Accounting | `hasAnyAuthority('ROLE_ADMIN','ROLE_ACCOUNTING')` |  | `CreditLimitOverrideController#rejectRequest` |

### Sales/Manufacturing
| Endpoint | Portal | Auth (roles/permissions) | Notes | Code anchor |
|---|---|---|---|---|
| `POST /api/v1/credit/override-requests` | Sales/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_SALES','ROLE_FACTORY')` |  | `CreditLimitOverrideController#createRequest` |
| `POST /api/v1/dispatch/backorder/{slipId}/cancel` | Sales/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `DispatchController#cancelBackorder` |
| `POST /api/v1/dispatch/confirm` | Sales/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY') and hasAuthority('dispatch.confirm')` | Legacy alias of `POST /api/v1/sales/dispatch/confirm` (canonical). | `DispatchController#confirmDispatch` |
| `GET /api/v1/dispatch/order/{orderId}` | Sales/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY','ROLE_SALES')` |  | `DispatchController#getPackagingSlipByOrder` |
| `GET /api/v1/dispatch/pending` | Sales/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY','ROLE_SALES')` |  | `DispatchController#getPendingSlips` |
| `GET /api/v1/dispatch/preview/{slipId}` | Sales/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `DispatchController#getDispatchPreview` |
| `GET /api/v1/dispatch/slip/{slipId}` | Sales/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY','ROLE_SALES')` |  | `DispatchController#getPackagingSlip` |
| `PATCH /api/v1/dispatch/slip/{slipId}/status` | Sales/Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `DispatchController#updateSlipStatus` |

### Manufacturing
| Endpoint | Portal | Auth (roles/permissions) | Notes | Code anchor |
|---|---|---|---|---|
| `POST /api/v1/factory/cost-allocation` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` | Financially significant: requires full evidence chain (see Task 03). | `FactoryController#allocateCosts` |
| `GET /api/v1/factory/dashboard` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `FactoryController#dashboard` |
| `POST /api/v1/finished-goods` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `FinishedGoodController#createFinishedGood` |
| `POST /api/v1/factory/pack` | Manufacturing | `hasAnyAuthority('ROLE_FACTORY','ROLE_ACCOUNTING','ROLE_ADMIN')` | Financially significant: requires full evidence chain (see Task 03). | `PackingController#packBulkToSizes` |
| `GET /api/v1/factory/bulk-batches/{finishedGoodId}` | Manufacturing | `hasAnyAuthority('ROLE_FACTORY','ROLE_ACCOUNTING','ROLE_ADMIN')` |  | `PackingController#listBulkBatches` |
| `GET /api/v1/factory/bulk-batches/{parentBatchId}/children` | Manufacturing | `hasAnyAuthority('ROLE_FACTORY','ROLE_ACCOUNTING','ROLE_ADMIN')` |  | `PackingController#listChildBatches` |
| `GET /api/v1/factory/packaging-mappings` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `PackagingMappingController#listMappings` |
| `POST /api/v1/factory/packaging-mappings` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN')` |  | `PackagingMappingController#createMapping` |
| `GET /api/v1/factory/packaging-mappings/active` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `PackagingMappingController#listActiveMappings` |
| `DELETE /api/v1/factory/packaging-mappings/{id}` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN')` |  | `PackagingMappingController#deactivateMapping` |
| `PUT /api/v1/factory/packaging-mappings/{id}` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN')` |  | `PackagingMappingController#updateMapping` |
| `POST /api/v1/factory/packing-records` | Manufacturing | `authenticated() (no @PreAuthorize found)` | Financially significant: requires full evidence chain (see Task 03). Security review required: no method-level guard. | `PackingController#recordPacking` |
| `POST /api/v1/factory/packing-records/{productionLogId}/complete` | Manufacturing | `authenticated() (no @PreAuthorize found)` | Financially significant: requires full evidence chain (see Task 03). Security review required: no method-level guard. | `PackingController#completePacking` |
| `GET /api/v1/factory/production-batches` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `FactoryController#batches` |
| `POST /api/v1/factory/production-batches` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` | Financially significant: requires full evidence chain (see Task 03). | `FactoryController#logBatch` |
| `GET /api/v1/factory/production-logs/{productionLogId}/packing-history` | Manufacturing | `authenticated() (no @PreAuthorize found)` | Security review required: no method-level guard. | `PackingController#packingHistory` |
| `GET /api/v1/factory/production-plans` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `FactoryController#plans` |
| `POST /api/v1/factory/production-plans` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` | Financially significant: requires full evidence chain (see Task 03). | `FactoryController#createPlan` |
| `DELETE /api/v1/factory/production-plans/{id}` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `FactoryController#deletePlan` |
| `PUT /api/v1/factory/production-plans/{id}` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `FactoryController#updatePlan` |
| `PATCH /api/v1/factory/production-plans/{id}/status` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `FactoryController#updatePlanStatus` |
| `GET /api/v1/factory/production/logs` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `ProductionLogController#list` |
| `POST /api/v1/factory/production/logs` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `ProductionLogController#create` |
| `GET /api/v1/factory/production/logs/{id}` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `ProductionLogController#detail` |
| `GET /api/v1/factory/tasks` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `FactoryController#tasks` |
| `POST /api/v1/factory/tasks` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` | Financially significant: requires full evidence chain (see Task 03). | `FactoryController#createTask` |
| `PUT /api/v1/factory/tasks/{id}` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY')` |  | `FactoryController#updateTask` |
| `GET /api/v1/factory/unpacked-batches` | Manufacturing | `authenticated() (no @PreAuthorize found)` | Security review required: no method-level guard. | `PackingController#listUnpackedBatches` |
| `GET /api/v1/production/brands` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY','ROLE_SALES','ROLE_ACCOUNTING')` |  | `ProductionCatalogController#listBrands` |
| `GET /api/v1/production/brands/{brandId}/products` | Manufacturing | `hasAnyAuthority('ROLE_ADMIN','ROLE_FACTORY','ROLE_SALES','ROLE_ACCOUNTING')` |  | `ProductionCatalogController#listBrandProducts` |

### Dealer
| Endpoint | Portal | Auth (roles/permissions) | Notes | Code anchor |
|---|---|---|---|---|
| `GET /api/v1/dealer-portal/aging` | Dealer | `hasAuthority('ROLE_DEALER')` |  | `DealerPortalController#getMyAging` |
| `GET /api/v1/dealer-portal/dashboard` | Dealer | `hasAuthority('ROLE_DEALER')` |  | `DealerPortalController#getDashboard` |
| `GET /api/v1/dealer-portal/invoices` | Dealer | `hasAuthority('ROLE_DEALER')` |  | `DealerPortalController#getMyInvoices` |
| `GET /api/v1/dealer-portal/invoices/{invoiceId}/pdf` | Dealer | `hasAuthority('ROLE_DEALER')` |  | `DealerPortalController#getMyInvoicePdf` |
| `GET /api/v1/dealer-portal/ledger` | Dealer | `hasAuthority('ROLE_DEALER')` |  | `DealerPortalController#getMyLedger` |
| `GET /api/v1/dealer-portal/orders` | Dealer | `hasAuthority('ROLE_DEALER')` |  | `DealerPortalController#getMyOrders` |
