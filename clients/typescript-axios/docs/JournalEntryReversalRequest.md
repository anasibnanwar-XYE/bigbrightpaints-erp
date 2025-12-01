# JournalEntryReversalRequest


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**reversalDate** | **string** |  | [optional] [default to undefined]
**voidOnly** | **boolean** |  | [optional] [default to undefined]
**reason** | **string** |  | [optional] [default to undefined]
**memo** | **string** |  | [optional] [default to undefined]
**adminOverride** | **boolean** |  | [optional] [default to undefined]
**reversalPercentage** | **number** |  | [optional] [default to undefined]
**cascadeRelatedEntries** | **boolean** |  | [optional] [default to undefined]
**relatedEntryIds** | **Array&lt;number&gt;** |  | [optional] [default to undefined]
**reasonCode** | **string** |  | [optional] [default to undefined]
**approvedBy** | **string** |  | [optional] [default to undefined]
**supportingDocumentRef** | **string** |  | [optional] [default to undefined]
**partialReversal** | **boolean** |  | [optional] [default to undefined]
**effectivePercentage** | **number** |  | [optional] [default to undefined]

## Example

```typescript
import { JournalEntryReversalRequest } from '@bigbright/erp-api-client';

const instance: JournalEntryReversalRequest = {
    reversalDate,
    voidOnly,
    reason,
    memo,
    adminOverride,
    reversalPercentage,
    cascadeRelatedEntries,
    relatedEntryIds,
    reasonCode,
    approvedBy,
    supportingDocumentRef,
    partialReversal,
    effectivePercentage,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
