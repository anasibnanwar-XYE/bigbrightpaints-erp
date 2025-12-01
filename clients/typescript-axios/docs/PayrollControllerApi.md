# PayrollControllerApi

All URIs are relative to *http://localhost:8081*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**processBatchPayment**](#processbatchpayment) | **POST** /api/v1/accounting/payroll/payments/batch | |

# **processBatchPayment**
> ApiResponsePayrollBatchPaymentResponse processBatchPayment(payrollBatchPaymentRequest)


### Example

```typescript
import {
    PayrollControllerApi,
    Configuration,
    PayrollBatchPaymentRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new PayrollControllerApi(configuration);

let payrollBatchPaymentRequest: PayrollBatchPaymentRequest; //

const { status, data } = await apiInstance.processBatchPayment(
    payrollBatchPaymentRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **payrollBatchPaymentRequest** | **PayrollBatchPaymentRequest**|  | |


### Return type

**ApiResponsePayrollBatchPaymentResponse**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: application/json
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

