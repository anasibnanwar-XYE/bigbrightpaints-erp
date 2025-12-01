# DispatchConfirmationResponse


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**packagingSlipId** | **number** |  | [optional] [default to undefined]
**slipNumber** | **string** |  | [optional] [default to undefined]
**status** | **string** |  | [optional] [default to undefined]
**confirmedAt** | **string** |  | [optional] [default to undefined]
**confirmedBy** | **string** |  | [optional] [default to undefined]
**totalOrderedAmount** | **number** |  | [optional] [default to undefined]
**totalShippedAmount** | **number** |  | [optional] [default to undefined]
**totalBackorderAmount** | **number** |  | [optional] [default to undefined]
**journalEntryId** | **number** |  | [optional] [default to undefined]
**cogsJournalEntryId** | **number** |  | [optional] [default to undefined]
**lines** | [**Array&lt;LineResult&gt;**](LineResult.md) |  | [optional] [default to undefined]
**backorderSlipId** | **number** |  | [optional] [default to undefined]

## Example

```typescript
import { DispatchConfirmationResponse } from '@bigbright/erp-api-client';

const instance: DispatchConfirmationResponse = {
    packagingSlipId,
    slipNumber,
    status,
    confirmedAt,
    confirmedBy,
    totalOrderedAmount,
    totalShippedAmount,
    totalBackorderAmount,
    journalEntryId,
    cogsJournalEntryId,
    lines,
    backorderSlipId,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
