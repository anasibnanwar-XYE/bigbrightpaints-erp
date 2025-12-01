# AgedReceivablesReport


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**asOfDate** | **string** |  | [optional] [default to undefined]
**dealers** | [**Array&lt;DealerAgingDetail&gt;**](DealerAgingDetail.md) |  | [optional] [default to undefined]
**totalBuckets** | [**AgingBuckets**](AgingBuckets.md) |  | [optional] [default to undefined]
**grandTotal** | **number** |  | [optional] [default to undefined]

## Example

```typescript
import { AgedReceivablesReport } from '@bigbright/erp-api-client';

const instance: AgedReceivablesReport = {
    asOfDate,
    dealers,
    totalBuckets,
    grandTotal,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
