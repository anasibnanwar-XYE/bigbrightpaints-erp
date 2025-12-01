# DispatchConfirmationRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**packagingSlipId** | **number** |  | [default to undefined]
**lines** | [**Array&lt;LineConfirmation&gt;**](LineConfirmation.md) |  | [default to undefined]
**notes** | **string** |  | [optional] [default to undefined]
**confirmedBy** | **string** |  | [optional] [default to undefined]

## Example

```typescript
import { DispatchConfirmationRequest } from '@bigbright/erp-api-client';

const instance: DispatchConfirmationRequest = {
    packagingSlipId,
    lines,
    notes,
    confirmedBy,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
