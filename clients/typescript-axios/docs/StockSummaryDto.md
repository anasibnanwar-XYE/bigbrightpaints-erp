# StockSummaryDto


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **number** |  | [optional] [default to undefined]
**publicId** | **string** |  | [optional] [default to undefined]
**code** | **string** |  | [optional] [default to undefined]
**name** | **string** |  | [optional] [default to undefined]
**currentStock** | **number** |  | [optional] [default to undefined]
**reservedStock** | **number** |  | [optional] [default to undefined]
**availableStock** | **number** |  | [optional] [default to undefined]
**weightedAverageCost** | **number** |  | [optional] [default to undefined]
**totalMaterials** | **number** |  | [optional] [default to undefined]
**lowStockMaterials** | **number** |  | [optional] [default to undefined]
**criticalStockMaterials** | **number** |  | [optional] [default to undefined]
**totalBatches** | **number** |  | [optional] [default to undefined]

## Example

```typescript
import { StockSummaryDto } from '@bigbright/erp-api-client';

const instance: StockSummaryDto = {
    id,
    publicId,
    code,
    name,
    currentStock,
    reservedStock,
    availableStock,
    weightedAverageCost,
    totalMaterials,
    lowStockMaterials,
    criticalStockMaterials,
    totalBatches,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
