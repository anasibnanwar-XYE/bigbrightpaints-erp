# AccountActivityReport


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**accountCode** | **string** |  | [optional] [default to undefined]
**accountName** | **string** |  | [optional] [default to undefined]
**startDate** | **string** |  | [optional] [default to undefined]
**endDate** | **string** |  | [optional] [default to undefined]
**openingBalance** | **number** |  | [optional] [default to undefined]
**closingBalance** | **number** |  | [optional] [default to undefined]
**totalDebits** | **number** |  | [optional] [default to undefined]
**totalCredits** | **number** |  | [optional] [default to undefined]
**movements** | [**Array&lt;AccountMovement&gt;**](AccountMovement.md) |  | [optional] [default to undefined]

## Example

```typescript
import { AccountActivityReport } from '@bigbright/erp-api-client';

const instance: AccountActivityReport = {
    accountCode,
    accountName,
    startDate,
    endDate,
    openingBalance,
    closingBalance,
    totalDebits,
    totalCredits,
    movements,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
