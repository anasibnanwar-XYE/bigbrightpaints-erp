# FinishedGoodControllerApi

All URIs are relative to *http://localhost:8081*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**createFinishedGood**](#createfinishedgood) | **POST** /api/v1/finished-goods | |
|[**getFinishedGood**](#getfinishedgood) | **GET** /api/v1/finished-goods/{id} | |
|[**getLowStockItems**](#getlowstockitems) | **GET** /api/v1/finished-goods/low-stock | |
|[**getStockSummary**](#getstocksummary) | **GET** /api/v1/finished-goods/stock-summary | |
|[**listBatches**](#listbatches) | **GET** /api/v1/finished-goods/{id}/batches | |
|[**listFinishedGoods**](#listfinishedgoods) | **GET** /api/v1/finished-goods | |
|[**registerBatch**](#registerbatch) | **POST** /api/v1/finished-goods/{id}/batches | |
|[**updateFinishedGood**](#updatefinishedgood) | **PUT** /api/v1/finished-goods/{id} | |

# **createFinishedGood**
> ApiResponseFinishedGoodDto createFinishedGood(finishedGoodRequest)


### Example

```typescript
import {
    FinishedGoodControllerApi,
    Configuration,
    FinishedGoodRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new FinishedGoodControllerApi(configuration);

let finishedGoodRequest: FinishedGoodRequest; //

const { status, data } = await apiInstance.createFinishedGood(
    finishedGoodRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **finishedGoodRequest** | **FinishedGoodRequest**|  | |


### Return type

**ApiResponseFinishedGoodDto**

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

# **getFinishedGood**
> ApiResponseFinishedGoodDto getFinishedGood()


### Example

```typescript
import {
    FinishedGoodControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new FinishedGoodControllerApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.getFinishedGood(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponseFinishedGoodDto**

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

# **getLowStockItems**
> ApiResponseListFinishedGoodDto getLowStockItems()


### Example

```typescript
import {
    FinishedGoodControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new FinishedGoodControllerApi(configuration);

let threshold: number; // (optional) (default to 100)

const { status, data } = await apiInstance.getLowStockItems(
    threshold
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **threshold** | [**number**] |  | (optional) defaults to 100|


### Return type

**ApiResponseListFinishedGoodDto**

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

# **getStockSummary**
> ApiResponseListStockSummaryDto getStockSummary()


### Example

```typescript
import {
    FinishedGoodControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new FinishedGoodControllerApi(configuration);

const { status, data } = await apiInstance.getStockSummary();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiResponseListStockSummaryDto**

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

# **listBatches**
> ApiResponseListFinishedGoodBatchDto listBatches()


### Example

```typescript
import {
    FinishedGoodControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new FinishedGoodControllerApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.listBatches(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponseListFinishedGoodBatchDto**

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

# **listFinishedGoods**
> ApiResponseListFinishedGoodDto listFinishedGoods()


### Example

```typescript
import {
    FinishedGoodControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new FinishedGoodControllerApi(configuration);

const { status, data } = await apiInstance.listFinishedGoods();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiResponseListFinishedGoodDto**

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

# **registerBatch**
> ApiResponseFinishedGoodBatchDto registerBatch(finishedGoodBatchRequest)


### Example

```typescript
import {
    FinishedGoodControllerApi,
    Configuration,
    FinishedGoodBatchRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new FinishedGoodControllerApi(configuration);

let id: number; // (default to undefined)
let finishedGoodBatchRequest: FinishedGoodBatchRequest; //

const { status, data } = await apiInstance.registerBatch(
    id,
    finishedGoodBatchRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **finishedGoodBatchRequest** | **FinishedGoodBatchRequest**|  | |
| **id** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponseFinishedGoodBatchDto**

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

# **updateFinishedGood**
> ApiResponseFinishedGoodDto updateFinishedGood(finishedGoodRequest)


### Example

```typescript
import {
    FinishedGoodControllerApi,
    Configuration,
    FinishedGoodRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new FinishedGoodControllerApi(configuration);

let id: number; // (default to undefined)
let finishedGoodRequest: FinishedGoodRequest; //

const { status, data } = await apiInstance.updateFinishedGood(
    id,
    finishedGoodRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **finishedGoodRequest** | **FinishedGoodRequest**|  | |
| **id** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponseFinishedGoodDto**

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

