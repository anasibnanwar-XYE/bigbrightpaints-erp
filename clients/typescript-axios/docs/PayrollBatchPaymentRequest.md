# PayrollBatchPaymentRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**runDate** | **string** |  | [default to undefined]
**cashAccountId** | **number** |  | [default to undefined]
**expenseAccountId** | **number** |  | [default to undefined]
**taxPayableAccountId** | **number** |  | [optional] [default to undefined]
**pfPayableAccountId** | **number** |  | [optional] [default to undefined]
**employerTaxExpenseAccountId** | **number** |  | [optional] [default to undefined]
**employerPfExpenseAccountId** | **number** |  | [optional] [default to undefined]
**defaultTaxRate** | **number** |  | [optional] [default to undefined]
**defaultPfRate** | **number** |  | [optional] [default to undefined]
**employerTaxRate** | **number** |  | [optional] [default to undefined]
**employerPfRate** | **number** |  | [optional] [default to undefined]
**referenceNumber** | **string** |  | [optional] [default to undefined]
**memo** | **string** |  | [optional] [default to undefined]
**lines** | [**Array&lt;PayrollLine&gt;**](PayrollLine.md) |  | [default to undefined]

## Example

```typescript
import { PayrollBatchPaymentRequest } from '@bigbright/erp-api-client';

const instance: PayrollBatchPaymentRequest = {
    runDate,
    cashAccountId,
    expenseAccountId,
    taxPayableAccountId,
    pfPayableAccountId,
    employerTaxExpenseAccountId,
    employerPfExpenseAccountId,
    defaultTaxRate,
    defaultPfRate,
    employerTaxRate,
    employerPfRate,
    referenceNumber,
    memo,
    lines,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
