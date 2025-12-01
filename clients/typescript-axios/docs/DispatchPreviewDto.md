# DispatchPreviewDto


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**packagingSlipId** | **number** |  | [optional] [default to undefined]
**slipNumber** | **string** |  | [optional] [default to undefined]
**status** | **string** |  | [optional] [default to undefined]
**salesOrderId** | **number** |  | [optional] [default to undefined]
**salesOrderNumber** | **string** |  | [optional] [default to undefined]
**dealerName** | **string** |  | [optional] [default to undefined]
**dealerCode** | **string** |  | [optional] [default to undefined]
**createdAt** | **string** |  | [optional] [default to undefined]
**totalOrderedAmount** | **number** |  | [optional] [default to undefined]
**totalAvailableAmount** | **number** |  | [optional] [default to undefined]
**lines** | [**Array&lt;LinePreview&gt;**](LinePreview.md) |  | [optional] [default to undefined]

## Example

```typescript
import { DispatchPreviewDto } from '@bigbright/erp-api-client';

const instance: DispatchPreviewDto = {
    packagingSlipId,
    slipNumber,
    status,
    salesOrderId,
    salesOrderNumber,
    dealerName,
    dealerCode,
    createdAt,
    totalOrderedAmount,
    totalAvailableAmount,
    lines,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
