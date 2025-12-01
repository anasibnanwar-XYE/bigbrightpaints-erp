# DealerPortalControllerApi

All URIs are relative to *http://localhost:8081*

|Method | HTTP request | Description|
|------------- | ------------- | -------------|
|[**getDashboard**](#getdashboard) | **GET** /api/v1/dealer-portal/dashboard | |
|[**getMyAging**](#getmyaging) | **GET** /api/v1/dealer-portal/aging | |
|[**getMyInvoices**](#getmyinvoices) | **GET** /api/v1/dealer-portal/invoices | |
|[**getMyLedger**](#getmyledger) | **GET** /api/v1/dealer-portal/ledger | |

# **getDashboard**
> ApiResponseMapStringObject getDashboard()


### Example

```typescript
import {
    DealerPortalControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new DealerPortalControllerApi(configuration);

const { status, data } = await apiInstance.getDashboard();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiResponseMapStringObject**

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

# **getMyAging**
> ApiResponseMapStringObject getMyAging()


### Example

```typescript
import {
    DealerPortalControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new DealerPortalControllerApi(configuration);

const { status, data } = await apiInstance.getMyAging();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiResponseMapStringObject**

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

# **getMyInvoices**
> ApiResponseMapStringObject getMyInvoices()


### Example

```typescript
import {
    DealerPortalControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new DealerPortalControllerApi(configuration);

const { status, data } = await apiInstance.getMyInvoices();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiResponseMapStringObject**

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

# **getMyLedger**
> ApiResponseMapStringObject getMyLedger()


### Example

```typescript
import {
    DealerPortalControllerApi,
    Configuration
} from '@bigbright/erp-api-client';

const configuration = new Configuration();
const apiInstance = new DealerPortalControllerApi(configuration);

const { status, data } = await apiInstance.getMyLedger();
```

### Parameters
This endpoint does not have any parameters.


### Return type

**ApiResponseMapStringObject**

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

