# IncomeStatementHierarchy


## Properties

Name | Type | Description | Notes
------------ | ------------- | ------------- | -------------
**revenue** | [**Array&lt;AccountNode&gt;**](AccountNode.md) |  | [optional] [default to undefined]
**totalRevenue** | **number** |  | [optional] [default to undefined]
**cogs** | [**Array&lt;AccountNode&gt;**](AccountNode.md) |  | [optional] [default to undefined]
**totalCogs** | **number** |  | [optional] [default to undefined]
**grossProfit** | **number** |  | [optional] [default to undefined]
**expenses** | [**Array&lt;AccountNode&gt;**](AccountNode.md) |  | [optional] [default to undefined]
**totalExpenses** | **number** |  | [optional] [default to undefined]
**netIncome** | **number** |  | [optional] [default to undefined]

## Example

```typescript
import { IncomeStatementHierarchy } from '@bigbright/erp-api-client';

const instance: IncomeStatementHierarchy = {
    revenue,
    totalRevenue,
    cogs,
    totalCogs,
    grossProfit,
    expenses,
    totalExpenses,
    netIncome,
};
```

[[Back to Model list]](../README.md#documentation-for-models) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to README]](../README.md)
