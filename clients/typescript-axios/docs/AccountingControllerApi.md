# AccountingControllerApi

All URIs are relative to *http://localhost:8081*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**accounts**](#accounts) | **GET** /api/v1/accounting/accounts | |
|[**adjustWip**](#adjustwip) | **POST** /api/v1/accounting/inventory/wip-adjustment | |
|[**auditDigest**](#auditdigest) | **GET** /api/v1/accounting/audit/digest | |
|[**auditDigestCsv**](#auditdigestcsv) | **GET** /api/v1/accounting/audit/digest.csv | |
|[**cascadeReverseJournalEntry**](#cascadereversejournalentry) | **POST** /api/v1/accounting/journal-entries/{entryId}/cascade-reverse | |
|[**checklist**](#checklist) | **GET** /api/v1/accounting/month-end/checklist | |
|[**closePeriod**](#closeperiod) | **POST** /api/v1/accounting/periods/{periodId}/close | |
|[**compareBalances**](#comparebalances) | **GET** /api/v1/accounting/accounts/{accountId}/balance/compare | |
|[**createAccount**](#createaccount) | **POST** /api/v1/accounting/accounts | |
|[**createJournalEntry**](#createjournalentry) | **POST** /api/v1/accounting/journal-entries | |
|[**dealerAging1**](#dealeraging1) | **GET** /api/v1/accounting/aging/dealers/{dealerId} | |
|[**dealerAgingPdf**](#dealeragingpdf) | **GET** /api/v1/accounting/aging/dealers/{dealerId}/pdf | |
|[**dealerStatement**](#dealerstatement) | **GET** /api/v1/accounting/statements/dealers/{dealerId} | |
|[**dealerStatementPdf**](#dealerstatementpdf) | **GET** /api/v1/accounting/statements/dealers/{dealerId}/pdf | |
|[**generateGstReturn**](#generategstreturn) | **GET** /api/v1/accounting/gst/return | |
|[**getAccountActivity**](#getaccountactivity) | **GET** /api/v1/accounting/accounts/{accountId}/activity | |
|[**getAccountTreeByType**](#getaccounttreebytype) | **GET** /api/v1/accounting/accounts/tree/{type} | |
|[**getAgedReceivables**](#getagedreceivables) | **GET** /api/v1/accounting/reports/aging/receivables | |
|[**getBalanceAsOf**](#getbalanceasof) | **GET** /api/v1/accounting/accounts/{accountId}/balance/as-of | |
|[**getBalanceSheetHierarchy**](#getbalancesheethierarchy) | **GET** /api/v1/accounting/reports/balance-sheet/hierarchy | |
|[**getChartOfAccountsTree**](#getchartofaccountstree) | **GET** /api/v1/accounting/accounts/tree | |
|[**getDealerAging**](#getdealeraging) | **GET** /api/v1/accounting/reports/aging/dealer/{dealerId} | |
|[**getDealerAgingDetailed**](#getdealeragingdetailed) | **GET** /api/v1/accounting/reports/aging/dealer/{dealerId}/detailed | |
|[**getDealerDSO**](#getdealerdso) | **GET** /api/v1/accounting/reports/dso/dealer/{dealerId} | |
|[**getIncomeStatementHierarchy**](#getincomestatementhierarchy) | **GET** /api/v1/accounting/reports/income-statement/hierarchy | |
|[**getTrialBalanceAsOf**](#gettrialbalanceasof) | **GET** /api/v1/accounting/trial-balance/as-of | |
|[**journalEntries**](#journalentries) | **GET** /api/v1/accounting/journal-entries | |
|[**listPeriods**](#listperiods) | **GET** /api/v1/accounting/periods | |
|[**lockPeriod**](#lockperiod) | **POST** /api/v1/accounting/periods/{periodId}/lock | |
|[**postAccrual**](#postaccrual) | **POST** /api/v1/accounting/accruals | |
|[**postCreditNote**](#postcreditnote) | **POST** /api/v1/accounting/credit-notes | |
|[**postDebitNote**](#postdebitnote) | **POST** /api/v1/accounting/debit-notes | |
|[**reconcileBank**](#reconcilebank) | **POST** /api/v1/accounting/bank-reconciliation | |
|[**recordDealerReceipt**](#recorddealerreceipt) | **POST** /api/v1/accounting/receipts/dealer | |
|[**recordInventoryCount**](#recordinventorycount) | **POST** /api/v1/accounting/inventory/physical-count | |
|[**recordLandedCost**](#recordlandedcost) | **POST** /api/v1/accounting/inventory/landed-cost | |
|[**recordPayrollPayment**](#recordpayrollpayment) | **POST** /api/v1/accounting/payroll/payments | |
|[**recordSalesReturn**](#recordsalesreturn) | **POST** /api/v1/accounting/sales/returns | |
|[**recordSupplierPayment**](#recordsupplierpayment) | **POST** /api/v1/accounting/suppliers/payments | |
|[**reopenPeriod**](#reopenperiod) | **POST** /api/v1/accounting/periods/{periodId}/reopen | |
|[**revalueInventory**](#revalueinventory) | **POST** /api/v1/accounting/inventory/revaluation | |
|[**reverseJournalEntry**](#reversejournalentry) | **POST** /api/v1/accounting/journal-entries/{entryId}/reverse | |
|[**settleDealer**](#settledealer) | **POST** /api/v1/accounting/settlements/dealers | |
|[**settleSupplier**](#settlesupplier) | **POST** /api/v1/accounting/settlements/suppliers | |
|[**supplierAging**](#supplieraging) | **GET** /api/v1/accounting/aging/suppliers/{supplierId} | |
|[**supplierAgingPdf**](#supplieragingpdf) | **GET** /api/v1/accounting/aging/suppliers/{supplierId}/pdf | |
|[**supplierStatement**](#supplierstatement) | **GET** /api/v1/accounting/statements/suppliers/{supplierId} | |
|[**supplierStatementPdf**](#supplierstatementpdf) | **GET** /api/v1/accounting/statements/suppliers/{supplierId}/pdf | |
|[**updateChecklist**](#updatechecklist) | **POST** /api/v1/accounting/month-end/checklist/{periodId} | |
|[**writeOffBadDebt**](#writeoffbaddebt) | **POST** /api/v1/accounting/bad-debts/write-off | |

# **accounts**
> ApiResponseListAccountDto accounts()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

const { status, data } = await apiInstance.accounts();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiResponseListAccountDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **adjustWip**
> ApiResponseJournalEntryDto adjustWip(wipAdjustmentRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    WipAdjustmentRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let wipAdjustmentRequest: WipAdjustmentRequest; //

const { status, data } = await apiInstance.adjustWip(
    wipAdjustmentRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **wipAdjustmentRequest** | **WipAdjustmentRequest**|  | |


### Return type

**ApiResponseJournalEntryDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **auditDigest**
> ApiResponseAuditDigestResponse auditDigest()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let from: string; // (optional) (default to undefined)
let to: string; // (optional) (default to undefined)

const { status, data } = await apiInstance.auditDigest(
    from,
    to
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **from** | [**string**] |  | (optional) defaults to undefined|
| **to** | [**string**] |  | (optional) defaults to undefined|


### Return type

**ApiResponseAuditDigestResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **auditDigestCsv**
> string auditDigestCsv()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let from: string; // (optional) (default to undefined)
let to: string; // (optional) (default to undefined)

const { status, data } = await apiInstance.auditDigestCsv(
    from,
    to
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **from** | [**string**] |  | (optional) defaults to undefined|
| **to** | [**string**] |  | (optional) defaults to undefined|


### Return type

**string**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*, text/csv


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **cascadeReverseJournalEntry**
> ApiResponseListJournalEntryDto cascadeReverseJournalEntry(journalEntryReversalRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    JournalEntryReversalRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let entryId: number; // (default to undefined)
let journalEntryReversalRequest: JournalEntryReversalRequest; //

const { status, data } = await apiInstance.cascadeReverseJournalEntry(
    entryId,
    journalEntryReversalRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **journalEntryReversalRequest** | **JournalEntryReversalRequest**|  | |
| **entryId** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponseListJournalEntryDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **checklist**
> ApiResponseMonthEndChecklistDto checklist()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let periodId: number; // (optional) (default to undefined)

const { status, data } = await apiInstance.checklist(
    periodId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **periodId** | [**number**] |  | (optional) defaults to undefined|


### Return type

**ApiResponseMonthEndChecklistDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **closePeriod**
> ApiResponseAccountingPeriodDto closePeriod()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    AccountingPeriodCloseRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let periodId: number; // (default to undefined)
let accountingPeriodCloseRequest: AccountingPeriodCloseRequest; // (optional)

const { status, data } = await apiInstance.closePeriod(
    periodId,
    accountingPeriodCloseRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **accountingPeriodCloseRequest** | **AccountingPeriodCloseRequest**|  | |
| **periodId** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponseAccountingPeriodDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **compareBalances**
> ApiResponseBalanceComparison compareBalances()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let accountId: number; // (default to undefined)
let date1: string; // (default to undefined)
let date2: string; // (default to undefined)

const { status, data } = await apiInstance.compareBalances(
    accountId,
    date1,
    date2
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **accountId** | [**number**] |  | defaults to undefined|
| **date1** | [**string**] |  | defaults to undefined|
| **date2** | [**string**] |  | defaults to undefined|


### Return type

**ApiResponseBalanceComparison**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **createAccount**
> ApiResponseAccountDto createAccount(accountRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    AccountRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let accountRequest: AccountRequest; //

const { status, data } = await apiInstance.createAccount(
    accountRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **accountRequest** | **AccountRequest**|  | |


### Return type

**ApiResponseAccountDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **createJournalEntry**
> ApiResponseJournalEntryDto createJournalEntry(journalEntryRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    JournalEntryRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let journalEntryRequest: JournalEntryRequest; //

const { status, data } = await apiInstance.createJournalEntry(
    journalEntryRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **journalEntryRequest** | **JournalEntryRequest**|  | |


### Return type

**ApiResponseJournalEntryDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **dealerAging1**
> ApiResponseAgingSummaryResponse dealerAging1()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let dealerId: number; // (default to undefined)
let asOf: string; // (optional) (default to undefined)
let buckets: string; // (optional) (default to undefined)

const { status, data } = await apiInstance.dealerAging1(
    dealerId,
    asOf,
    buckets
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **dealerId** | [**number**] |  | defaults to undefined|
| **asOf** | [**string**] |  | (optional) defaults to undefined|
| **buckets** | [**string**] |  | (optional) defaults to undefined|


### Return type

**ApiResponseAgingSummaryResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **dealerAgingPdf**
> string dealerAgingPdf()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let dealerId: number; // (default to undefined)
let asOf: string; // (optional) (default to undefined)
let buckets: string; // (optional) (default to undefined)

const { status, data } = await apiInstance.dealerAgingPdf(
    dealerId,
    asOf,
    buckets
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **dealerId** | [**number**] |  | defaults to undefined|
| **asOf** | [**string**] |  | (optional) defaults to undefined|
| **buckets** | [**string**] |  | (optional) defaults to undefined|


### Return type

**string**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*, application/pdf


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **dealerStatement**
> ApiResponsePartnerStatementResponse dealerStatement()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let dealerId: number; // (default to undefined)
let from: string; // (optional) (default to undefined)
let to: string; // (optional) (default to undefined)

const { status, data } = await apiInstance.dealerStatement(
    dealerId,
    from,
    to
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **dealerId** | [**number**] |  | defaults to undefined|
| **from** | [**string**] |  | (optional) defaults to undefined|
| **to** | [**string**] |  | (optional) defaults to undefined|


### Return type

**ApiResponsePartnerStatementResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **dealerStatementPdf**
> string dealerStatementPdf()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let dealerId: number; // (default to undefined)
let from: string; // (optional) (default to undefined)
let to: string; // (optional) (default to undefined)

const { status, data } = await apiInstance.dealerStatementPdf(
    dealerId,
    from,
    to
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **dealerId** | [**number**] |  | defaults to undefined|
| **from** | [**string**] |  | (optional) defaults to undefined|
| **to** | [**string**] |  | (optional) defaults to undefined|


### Return type

**string**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*, application/pdf


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **generateGstReturn**
> ApiResponseGstReturnDto generateGstReturn()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let period: string; // (optional) (default to undefined)

const { status, data } = await apiInstance.generateGstReturn(
    period
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **period** | [**string**] |  | (optional) defaults to undefined|


### Return type

**ApiResponseGstReturnDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getAccountActivity**
> ApiResponseAccountActivityReport getAccountActivity()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let accountId: number; // (default to undefined)
let startDate: string; // (default to undefined)
let endDate: string; // (default to undefined)

const { status, data } = await apiInstance.getAccountActivity(
    accountId,
    startDate,
    endDate
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **accountId** | [**number**] |  | defaults to undefined|
| **startDate** | [**string**] |  | defaults to undefined|
| **endDate** | [**string**] |  | defaults to undefined|


### Return type

**ApiResponseAccountActivityReport**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getAccountTreeByType**
> ApiResponseListAccountNode getAccountTreeByType()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let type: string; // (default to undefined)

const { status, data } = await apiInstance.getAccountTreeByType(
    type
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **type** | [**string**] |  | defaults to undefined|


### Return type

**ApiResponseListAccountNode**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getAgedReceivables**
> ApiResponseAgedReceivablesReport getAgedReceivables()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let asOfDate: string; // (optional) (default to undefined)

const { status, data } = await apiInstance.getAgedReceivables(
    asOfDate
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **asOfDate** | [**string**] |  | (optional) defaults to undefined|


### Return type

**ApiResponseAgedReceivablesReport**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getBalanceAsOf**
> ApiResponseBigDecimal getBalanceAsOf()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let accountId: number; // (default to undefined)
let date: string; // (default to undefined)

const { status, data } = await apiInstance.getBalanceAsOf(
    accountId,
    date
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **accountId** | [**number**] |  | defaults to undefined|
| **date** | [**string**] |  | defaults to undefined|


### Return type

**ApiResponseBigDecimal**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getBalanceSheetHierarchy**
> ApiResponseBalanceSheetHierarchy getBalanceSheetHierarchy()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

const { status, data } = await apiInstance.getBalanceSheetHierarchy();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiResponseBalanceSheetHierarchy**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getChartOfAccountsTree**
> ApiResponseListAccountNode getChartOfAccountsTree()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

const { status, data } = await apiInstance.getChartOfAccountsTree();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiResponseListAccountNode**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getDealerAging**
> ApiResponseDealerAgingDetail getDealerAging()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let dealerId: number; // (default to undefined)

const { status, data } = await apiInstance.getDealerAging(
    dealerId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **dealerId** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponseDealerAgingDetail**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getDealerAgingDetailed**
> ApiResponseDealerAgingDetailedReport getDealerAgingDetailed()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let dealerId: number; // (default to undefined)

const { status, data } = await apiInstance.getDealerAgingDetailed(
    dealerId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **dealerId** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponseDealerAgingDetailedReport**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getDealerDSO**
> ApiResponseDSOReport getDealerDSO()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let dealerId: number; // (default to undefined)

const { status, data } = await apiInstance.getDealerDSO(
    dealerId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **dealerId** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponseDSOReport**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getIncomeStatementHierarchy**
> ApiResponseIncomeStatementHierarchy getIncomeStatementHierarchy()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

const { status, data } = await apiInstance.getIncomeStatementHierarchy();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiResponseIncomeStatementHierarchy**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getTrialBalanceAsOf**
> ApiResponseTrialBalanceSnapshot getTrialBalanceAsOf()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let date: string; // (default to undefined)

const { status, data } = await apiInstance.getTrialBalanceAsOf(
    date
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **date** | [**string**] |  | defaults to undefined|


### Return type

**ApiResponseTrialBalanceSnapshot**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **journalEntries**
> ApiResponseListJournalEntryDto journalEntries()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let dealerId: number; // (optional) (default to undefined)
let page: number; // (optional) (default to 0)
let size: number; // (optional) (default to 100)

const { status, data } = await apiInstance.journalEntries(
    dealerId,
    page,
    size
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **dealerId** | [**number**] |  | (optional) defaults to undefined|
| **page** | [**number**] |  | (optional) defaults to 0|
| **size** | [**number**] |  | (optional) defaults to 100|


### Return type

**ApiResponseListJournalEntryDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **listPeriods**
> ApiResponseListAccountingPeriodDto listPeriods()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

const { status, data } = await apiInstance.listPeriods();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiResponseListAccountingPeriodDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **lockPeriod**
> ApiResponseAccountingPeriodDto lockPeriod()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    AccountingPeriodLockRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let periodId: number; // (default to undefined)
let accountingPeriodLockRequest: AccountingPeriodLockRequest; // (optional)

const { status, data } = await apiInstance.lockPeriod(
    periodId,
    accountingPeriodLockRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **accountingPeriodLockRequest** | **AccountingPeriodLockRequest**|  | |
| **periodId** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponseAccountingPeriodDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **postAccrual**
> ApiResponseJournalEntryDto postAccrual(accrualRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    AccrualRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let accrualRequest: AccrualRequest; //

const { status, data } = await apiInstance.postAccrual(
    accrualRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **accrualRequest** | **AccrualRequest**|  | |


### Return type

**ApiResponseJournalEntryDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **postCreditNote**
> ApiResponseJournalEntryDto postCreditNote(creditNoteRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    CreditNoteRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let creditNoteRequest: CreditNoteRequest; //

const { status, data } = await apiInstance.postCreditNote(
    creditNoteRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **creditNoteRequest** | **CreditNoteRequest**|  | |


### Return type

**ApiResponseJournalEntryDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **postDebitNote**
> ApiResponseJournalEntryDto postDebitNote(debitNoteRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    DebitNoteRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let debitNoteRequest: DebitNoteRequest; //

const { status, data } = await apiInstance.postDebitNote(
    debitNoteRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **debitNoteRequest** | **DebitNoteRequest**|  | |


### Return type

**ApiResponseJournalEntryDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **reconcileBank**
> ApiResponseBankReconciliationSummaryDto reconcileBank(bankReconciliationRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    BankReconciliationRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let bankReconciliationRequest: BankReconciliationRequest; //

const { status, data } = await apiInstance.reconcileBank(
    bankReconciliationRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **bankReconciliationRequest** | **BankReconciliationRequest**|  | |


### Return type

**ApiResponseBankReconciliationSummaryDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **recordDealerReceipt**
> ApiResponseJournalEntryDto recordDealerReceipt(dealerReceiptRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    DealerReceiptRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let dealerReceiptRequest: DealerReceiptRequest; //

const { status, data } = await apiInstance.recordDealerReceipt(
    dealerReceiptRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **dealerReceiptRequest** | **DealerReceiptRequest**|  | |


### Return type

**ApiResponseJournalEntryDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **recordInventoryCount**
> ApiResponseInventoryCountResponse recordInventoryCount(inventoryCountRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    InventoryCountRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let inventoryCountRequest: InventoryCountRequest; //

const { status, data } = await apiInstance.recordInventoryCount(
    inventoryCountRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **inventoryCountRequest** | **InventoryCountRequest**|  | |


### Return type

**ApiResponseInventoryCountResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **recordLandedCost**
> ApiResponseJournalEntryDto recordLandedCost(landedCostRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    LandedCostRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let landedCostRequest: LandedCostRequest; //

const { status, data } = await apiInstance.recordLandedCost(
    landedCostRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **landedCostRequest** | **LandedCostRequest**|  | |


### Return type

**ApiResponseJournalEntryDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **recordPayrollPayment**
> ApiResponseJournalEntryDto recordPayrollPayment(payrollPaymentRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    PayrollPaymentRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let payrollPaymentRequest: PayrollPaymentRequest; //

const { status, data } = await apiInstance.recordPayrollPayment(
    payrollPaymentRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **payrollPaymentRequest** | **PayrollPaymentRequest**|  | |


### Return type

**ApiResponseJournalEntryDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **recordSalesReturn**
> ApiResponseJournalEntryDto recordSalesReturn(salesReturnRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    SalesReturnRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let salesReturnRequest: SalesReturnRequest; //

const { status, data } = await apiInstance.recordSalesReturn(
    salesReturnRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **salesReturnRequest** | **SalesReturnRequest**|  | |


### Return type

**ApiResponseJournalEntryDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **recordSupplierPayment**
> ApiResponseJournalEntryDto recordSupplierPayment(supplierPaymentRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    SupplierPaymentRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let supplierPaymentRequest: SupplierPaymentRequest; //

const { status, data } = await apiInstance.recordSupplierPayment(
    supplierPaymentRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **supplierPaymentRequest** | **SupplierPaymentRequest**|  | |


### Return type

**ApiResponseJournalEntryDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **reopenPeriod**
> ApiResponseAccountingPeriodDto reopenPeriod()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    AccountingPeriodReopenRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let periodId: number; // (default to undefined)
let accountingPeriodReopenRequest: AccountingPeriodReopenRequest; // (optional)

const { status, data } = await apiInstance.reopenPeriod(
    periodId,
    accountingPeriodReopenRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **accountingPeriodReopenRequest** | **AccountingPeriodReopenRequest**|  | |
| **periodId** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponseAccountingPeriodDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **revalueInventory**
> ApiResponseJournalEntryDto revalueInventory(inventoryRevaluationRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    InventoryRevaluationRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let inventoryRevaluationRequest: InventoryRevaluationRequest; //

const { status, data } = await apiInstance.revalueInventory(
    inventoryRevaluationRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **inventoryRevaluationRequest** | **InventoryRevaluationRequest**|  | |


### Return type

**ApiResponseJournalEntryDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **reverseJournalEntry**
> ApiResponseJournalEntryDto reverseJournalEntry()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    JournalEntryReversalRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let entryId: number; // (default to undefined)
let journalEntryReversalRequest: JournalEntryReversalRequest; // (optional)

const { status, data } = await apiInstance.reverseJournalEntry(
    entryId,
    journalEntryReversalRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **journalEntryReversalRequest** | **JournalEntryReversalRequest**|  | |
| **entryId** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponseJournalEntryDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **settleDealer**
> ApiResponsePartnerSettlementResponse settleDealer(dealerSettlementRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    DealerSettlementRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let dealerSettlementRequest: DealerSettlementRequest; //

const { status, data } = await apiInstance.settleDealer(
    dealerSettlementRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **dealerSettlementRequest** | **DealerSettlementRequest**|  | |


### Return type

**ApiResponsePartnerSettlementResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **settleSupplier**
> ApiResponsePartnerSettlementResponse settleSupplier(supplierSettlementRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    SupplierSettlementRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let supplierSettlementRequest: SupplierSettlementRequest; //

const { status, data } = await apiInstance.settleSupplier(
    supplierSettlementRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **supplierSettlementRequest** | **SupplierSettlementRequest**|  | |


### Return type

**ApiResponsePartnerSettlementResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **supplierAging**
> ApiResponseAgingSummaryResponse supplierAging()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let supplierId: number; // (default to undefined)
let asOf: string; // (optional) (default to undefined)
let buckets: string; // (optional) (default to undefined)

const { status, data } = await apiInstance.supplierAging(
    supplierId,
    asOf,
    buckets
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **supplierId** | [**number**] |  | defaults to undefined|
| **asOf** | [**string**] |  | (optional) defaults to undefined|
| **buckets** | [**string**] |  | (optional) defaults to undefined|


### Return type

**ApiResponseAgingSummaryResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **supplierAgingPdf**
> string supplierAgingPdf()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let supplierId: number; // (default to undefined)
let asOf: string; // (optional) (default to undefined)
let buckets: string; // (optional) (default to undefined)

const { status, data } = await apiInstance.supplierAgingPdf(
    supplierId,
    asOf,
    buckets
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **supplierId** | [**number**] |  | defaults to undefined|
| **asOf** | [**string**] |  | (optional) defaults to undefined|
| **buckets** | [**string**] |  | (optional) defaults to undefined|


### Return type

**string**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*, application/pdf


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **supplierStatement**
> ApiResponsePartnerStatementResponse supplierStatement()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let supplierId: number; // (default to undefined)
let from: string; // (optional) (default to undefined)
let to: string; // (optional) (default to undefined)

const { status, data } = await apiInstance.supplierStatement(
    supplierId,
    from,
    to
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **supplierId** | [**number**] |  | defaults to undefined|
| **from** | [**string**] |  | (optional) defaults to undefined|
| **to** | [**string**] |  | (optional) defaults to undefined|


### Return type

**ApiResponsePartnerStatementResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **supplierStatementPdf**
> string supplierStatementPdf()


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let supplierId: number; // (default to undefined)
let from: string; // (optional) (default to undefined)
let to: string; // (optional) (default to undefined)

const { status, data } = await apiInstance.supplierStatementPdf(
    supplierId,
    from,
    to
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **supplierId** | [**number**] |  | defaults to undefined|
| **from** | [**string**] |  | (optional) defaults to undefined|
| **to** | [**string**] |  | (optional) defaults to undefined|


### Return type

**string**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*, application/pdf


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **updateChecklist**
> ApiResponseMonthEndChecklistDto updateChecklist(monthEndChecklistUpdateRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    MonthEndChecklistUpdateRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let periodId: number; // (default to undefined)
let monthEndChecklistUpdateRequest: MonthEndChecklistUpdateRequest; //

const { status, data } = await apiInstance.updateChecklist(
    periodId,
    monthEndChecklistUpdateRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **monthEndChecklistUpdateRequest** | **MonthEndChecklistUpdateRequest**|  | |
| **periodId** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponseMonthEndChecklistDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **writeOffBadDebt**
> ApiResponseJournalEntryDto writeOffBadDebt(badDebtWriteOffRequest)


### Example

```typescript
import {
    AccountingControllerApi,
    Configuration,
    BadDebtWriteOffRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new AccountingControllerApi(configuration);

let badDebtWriteOffRequest: BadDebtWriteOffRequest; //

const { status, data } = await apiInstance.writeOffBadDebt(
    badDebtWriteOffRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **badDebtWriteOffRequest** | **BadDebtWriteOffRequest**|  | |


### Return type

**ApiResponseJournalEntryDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**400** | Bad Request |  -  |
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

