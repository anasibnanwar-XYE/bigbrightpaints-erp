# PackagingSlipDto


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**id** | **number** |  | [optional] [default to undefined]
**publicId** | **string** |  | [optional] [default to undefined]
**salesOrderId** | **number** |  | [optional] [default to undefined]
**orderNumber** | **string** |  | [optional] [default to undefined]
**dealerName** | **string** |  | [optional] [default to undefined]
**slipNumber** | **string** |  | [optional] [default to undefined]
**status** | **string** |  | [optional] [default to undefined]
**createdAt** | **string** |  | [optional] [default to undefined]
**confirmedAt** | **string** |  | [optional] [default to undefined]
**confirmedBy** | **string** |  | [optional] [default to undefined]
**dispatchedAt** | **string** |  | [optional] [default to undefined]
**dispatchNotes** | **string** |  | [optional] [default to undefined]
**journalEntryId** | **number** |  | [optional] [default to undefined]
**cogsJournalEntryId** | **number** |  | [optional] [default to undefined]
**lines** | [**Array&lt;PackagingSlipLineDto&gt;**](PackagingSlipLineDto.md) |  | [optional] [default to undefined]

## Example

```typescript
import { PackagingSlipDto } from '@bigbright/erp-api-client';

const instance: PackagingSlipDto = {
    id,
    publicId,
    salesOrderId,
    orderNumber,
    dealerName,
    slipNumber,
    status,
    createdAt,
    confirmedAt,
    confirmedBy,
    dispatchedAt,
    dispatchNotes,
    journalEntryId,
    cogsJournalEntryId,
    lines,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
