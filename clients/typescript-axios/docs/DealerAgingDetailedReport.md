# DealerAgingDetailedReport


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**dealerId** | **number** |  | [optional] [default to undefined]
**dealerCode** | **string** |  | [optional] [default to undefined]
**dealerName** | **string** |  | [optional] [default to undefined]
**lineItems** | [**Array&lt;AgingLineItem&gt;**](AgingLineItem.md) |  | [optional] [default to undefined]
**buckets** | [**AgingBuckets**](AgingBuckets.md) |  | [optional] [default to undefined]
**totalOutstanding** | **number** |  | [optional] [default to undefined]
**averageDSO** | **number** |  | [optional] [default to undefined]

## Example

```typescript
import { DealerAgingDetailedReport } from '@bigbright/erp-api-client';

const instance: DealerAgingDetailedReport = {
    dealerId,
    dealerCode,
    dealerName,
    lineItems,
    buckets,
    totalOutstanding,
    averageDSO,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
