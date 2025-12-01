# DispatchControllerApi

All URIs are relative to *http://localhost:8081*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**cancelBackorder**](#cancelbackorder) | **POST** /api/v1/dispatch/backorder/{slipId}/cancel | |
|[**confirmDispatch1**](#confirmdispatch1) | **POST** /api/v1/dispatch/confirm | |
|[**getDispatchPreview**](#getdispatchpreview) | **GET** /api/v1/dispatch/preview/{slipId} | |
|[**getPackagingSlip**](#getpackagingslip) | **GET** /api/v1/dispatch/slip/{slipId} | |
|[**getPackagingSlipByOrder**](#getpackagingslipbyorder) | **GET** /api/v1/dispatch/order/{orderId} | |
|[**getPendingSlips**](#getpendingslips) | **GET** /api/v1/dispatch/pending | |
|[**updateSlipStatus**](#updateslipstatus) | **PATCH** /api/v1/dispatch/slip/{slipId}/status | |

# **cancelBackorder**
> ApiResponsePackagingSlipDto cancelBackorder()


### Example

```typescript
import {
    DispatchControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new DispatchControllerApi(configuration);

let slipId: number; // (default to undefined)
let reason: string; // (optional) (default to undefined)

const { status, data } = await apiInstance.cancelBackorder(
    slipId,
    reason
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **slipId** | [**number**] |  | defaults to undefined|
| **reason** | [**string**] |  | (optional) defaults to undefined|


### Return type

**ApiResponsePackagingSlipDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **confirmDispatch1**
> ApiResponseDispatchConfirmationResponse confirmDispatch1(dispatchConfirmationRequest)


### Example

```typescript
import {
    DispatchControllerApi,
    Configuration,
    DispatchConfirmationRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new DispatchControllerApi(configuration);

let dispatchConfirmationRequest: DispatchConfirmationRequest; //

const { status, data } = await apiInstance.confirmDispatch1(
    dispatchConfirmationRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **dispatchConfirmationRequest** | **DispatchConfirmationRequest**|  | |


### Return type

**ApiResponseDispatchConfirmationResponse**

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

# **getDispatchPreview**
> ApiResponseDispatchPreviewDto getDispatchPreview()


### Example

```typescript
import {
    DispatchControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new DispatchControllerApi(configuration);

let slipId: number; // (default to undefined)

const { status, data } = await apiInstance.getDispatchPreview(
    slipId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **slipId** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponseDispatchPreviewDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getPackagingSlip**
> ApiResponsePackagingSlipDto getPackagingSlip()


### Example

```typescript
import {
    DispatchControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new DispatchControllerApi(configuration);

let slipId: number; // (default to undefined)

const { status, data } = await apiInstance.getPackagingSlip(
    slipId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **slipId** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponsePackagingSlipDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getPackagingSlipByOrder**
> ApiResponsePackagingSlipDto getPackagingSlipByOrder()


### Example

```typescript
import {
    DispatchControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new DispatchControllerApi(configuration);

let orderId: number; // (default to undefined)

const { status, data } = await apiInstance.getPackagingSlipByOrder(
    orderId
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **orderId** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponsePackagingSlipDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **getPendingSlips**
> ApiResponseListPackagingSlipDto getPendingSlips()


### Example

```typescript
import {
    DispatchControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new DispatchControllerApi(configuration);

const { status, data } = await apiInstance.getPendingSlips();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiResponseListPackagingSlipDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

# **updateSlipStatus**
> ApiResponsePackagingSlipDto updateSlipStatus()


### Example

```typescript
import {
    DispatchControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new DispatchControllerApi(configuration);

let slipId: number; // (default to undefined)
let status: string; // (default to undefined)

const { status, data } = await apiInstance.updateSlipStatus(
    slipId,
    status
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **slipId** | [**number**] |  | defaults to undefined|
| **status** | [**string**] |  | defaults to undefined|


### Return type

**ApiResponsePackagingSlipDto**

### Authorization

No authorization required

### HTTP request headers

 - **Content-Type**: Not defined
 - **Accept**: */*


### HTTP response details
| Status code | Description | Response headers |
|-------------|-------------|------------------|
|**200** | OK |  -  |

[[Back to top]](#) [[Back to API list]](../README.md#documentation-for-api-endpoints) [[Back to Model list]](../README.md#documentation-for-models) [[Back to README]](../README.md)

