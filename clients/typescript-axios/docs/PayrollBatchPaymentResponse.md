# PayrollBatchPaymentResponse


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**payrollRunId** | **number** |  | [optional] [default to undefined]
**runDate** | **string** |  | [optional] [default to undefined]
**grossAmount** | **number** |  | [optional] [default to undefined]
**totalTaxWithholding** | **number** |  | [optional] [default to undefined]
**totalPfWithholding** | **number** |  | [optional] [default to undefined]
**totalAdvances** | **number** |  | [optional] [default to undefined]
**netPayAmount** | **number** |  | [optional] [default to undefined]
**employerTaxAmount** | **number** |  | [optional] [default to undefined]
**employerPfAmount** | **number** |  | [optional] [default to undefined]
**totalEmployerCost** | **number** |  | [optional] [default to undefined]
**payrollJournalId** | **number** |  | [optional] [default to undefined]
**employerContribJournalId** | **number** |  | [optional] [default to undefined]
**lines** | [**Array&lt;LineTotal&gt;**](LineTotal.md) |  | [optional] [default to undefined]

## Example

```typescript
import { PayrollBatchPaymentResponse } from '@bigbright/erp-api-client';

const instance: PayrollBatchPaymentResponse = {
    payrollRunId,
    runDate,
    grossAmount,
    totalTaxWithholding,
    totalPfWithholding,
    totalAdvances,
    netPayAmount,
    employerTaxAmount,
    employerPfAmount,
    totalEmployerCost,
    payrollJournalId,
    employerContribJournalId,
    lines,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
