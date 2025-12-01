# TrialBalanceSnapshot


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**asOfDate** | **string** |  | [optional] [default to undefined]
**entries** | [**Array&lt;TrialBalanceEntry&gt;**](TrialBalanceEntry.md) |  | [optional] [default to undefined]
**totalDebits** | **number** |  | [optional] [default to undefined]
**totalCredits** | **number** |  | [optional] [default to undefined]

## Example

```typescript
import { TrialBalanceSnapshot } from '@bigbright/erp-api-client';

const instance: TrialBalanceSnapshot = {
    asOfDate,
    entries,
    totalDebits,
    totalCredits,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
