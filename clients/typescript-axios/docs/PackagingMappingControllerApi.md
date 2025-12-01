# PackagingMappingControllerApi

All URIs are relative to *http://localhost:8081*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**createMapping**](#createmapping) | **POST** /api/v1/factory/packaging-mappings | |
|[**deactivateMapping**](#deactivatemapping) | **DELETE** /api/v1/factory/packaging-mappings/{id} | |
|[**listActiveMappings**](#listactivemappings) | **GET** /api/v1/factory/packaging-mappings/active | |
|[**listMappings**](#listmappings) | **GET** /api/v1/factory/packaging-mappings | |
|[**updateMapping**](#updatemapping) | **PUT** /api/v1/factory/packaging-mappings/{id} | |

# **createMapping**
> ApiResponsePackagingSizeMappingDto createMapping(packagingSizeMappingRequest)


### Example

```typescript
import {
    PackagingMappingControllerApi,
    Configuration,
    PackagingSizeMappingRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new PackagingMappingControllerApi(configuration);

let packagingSizeMappingRequest: PackagingSizeMappingRequest; //

const { status, data } = await apiInstance.createMapping(
    packagingSizeMappingRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **packagingSizeMappingRequest** | **PackagingSizeMappingRequest**|  | |


### Return type

**ApiResponsePackagingSizeMappingDto**

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

# **deactivateMapping**
> ApiResponseVoid deactivateMapping()


### Example

```typescript
import {
    PackagingMappingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new PackagingMappingControllerApi(configuration);

let id: number; // (default to undefined)

const { status, data } = await apiInstance.deactivateMapping(
    id
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **id** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponseVoid**

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

# **listActiveMappings**
> ApiResponseListPackagingSizeMappingDto listActiveMappings()


### Example

```typescript
import {
    PackagingMappingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new PackagingMappingControllerApi(configuration);

const { status, data } = await apiInstance.listActiveMappings();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiResponseListPackagingSizeMappingDto**

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

# **listMappings**
> ApiResponseListPackagingSizeMappingDto listMappings()


### Example

```typescript
import {
    PackagingMappingControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new PackagingMappingControllerApi(configuration);

const { status, data } = await apiInstance.listMappings();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiResponseListPackagingSizeMappingDto**

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

# **updateMapping**
> ApiResponsePackagingSizeMappingDto updateMapping(packagingSizeMappingRequest)


### Example

```typescript
import {
    PackagingMappingControllerApi,
    Configuration,
    PackagingSizeMappingRequest
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new PackagingMappingControllerApi(configuration);

let id: number; // (default to undefined)
let packagingSizeMappingRequest: PackagingSizeMappingRequest; //

const { status, data } = await apiInstance.updateMapping(
    id,
    packagingSizeMappingRequest
);
```

### Parameters

|Name | Type | Description  | Notes|
|------------- | ------------- | ------------- | -------------|
| **packagingSizeMappingRequest** | **PackagingSizeMappingRequest**|  | |
| **id** | [**number**] |  | defaults to undefined|


### Return type

**ApiResponsePackagingSizeMappingDto**

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

