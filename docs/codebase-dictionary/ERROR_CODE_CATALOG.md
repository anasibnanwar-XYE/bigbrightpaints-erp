# Error Code Catalog - BigBright ERP

This document lists all `ErrorCode` enum values organized by category.

---

## Overview

Error codes provide consistent error responses across the application without exposing internal implementation details. Each error code has:
- **Code**: Unique identifier (e.g., `AUTH_001`)
- **Default Message**: User-friendly description
- **HTTP Status**: Determined by prefix

### HTTP Status Mapping

| Error Code Prefix | HTTP Status | Description |
|-------------------|-------------|-------------|
| `AUTH_*` | 401 UNAUTHORIZED | Authentication/Authorization failures |
| `VAL_*` | 400 BAD REQUEST | Validation errors |
| `BUS_*` | 409 CONFLICT | Business rule violations |
| `CONC_*` | 409 CONFLICT | Concurrency conflicts |
| `DATA_*` | 409 CONFLICT | Data integrity issues |
| `SYS_*` | 503 SERVICE UNAVAILABLE | System errors |
| `INT_*` | 502 BAD GATEWAY | Integration failures |
| `FILE_*` | 422 UNPROCESSABLE ENTITY | File operation errors |
| `ERR_*` | 500 INTERNAL SERVER ERROR | Unknown errors |

---

## Authentication & Authorization (AUTH_XXX)

Range: 1000-1999 | HTTP Status: 401 UNAUTHORIZED

| Code | Enum | Default Message | Common Causes |
|------|------|-----------------|---------------|
| `AUTH_001` | `AUTH_INVALID_CREDENTIALS` | Invalid credentials provided | Wrong email/password |
| `AUTH_002` | `AUTH_TOKEN_EXPIRED` | Authentication token has expired | Token TTL exceeded |
| `AUTH_003` | `AUTH_TOKEN_INVALID` | Invalid authentication token | Malformed/tampered token |
| `AUTH_004` | `AUTH_INSUFFICIENT_PERMISSIONS` | Insufficient permissions for this operation | Missing role/authority |
| `AUTH_005` | `AUTH_ACCOUNT_LOCKED` | Account is locked | Too many failed attempts |
| `AUTH_006` | `AUTH_ACCOUNT_DISABLED` | Account is disabled | Admin deactivated |
| `AUTH_007` | `AUTH_MFA_REQUIRED` | Multi-factor authentication required | MFA enabled but not provided |
| `AUTH_008` | `AUTH_MFA_INVALID` | Invalid MFA code | Wrong TOTP/recovery code |
| `AUTH_009` | `AUTH_PASSWORD_POLICY_VIOLATION` | Password does not meet security requirements | Complexity requirements |
| `AUTH_010` | `AUTH_SESSION_EXPIRED` | Session has expired | Session timeout |

### Usage Examples

```java
// Invalid credentials
throw new ApplicationException(ErrorCode.AUTH_INVALID_CREDENTIALS);

// Account locked
throw new AuthSecurityContractException(
    HttpStatus.UNAUTHORIZED,
    ErrorCode.AUTH_ACCOUNT_LOCKED.getCode(),
    "Account locked for 15 minutes due to too many failed attempts"
);

// MFA required
throw new MfaRequiredException(ErrorCode.AUTH_MFA_REQUIRED);
```

---

## Business Logic Errors (BUS_XXX)

Range: 2000-2999 | HTTP Status: 409 CONFLICT

| Code | Enum | Default Message | Common Causes |
|------|------|-----------------|---------------|
| `BUS_001` | `BUSINESS_INVALID_STATE` | Operation not allowed in current state | Invalid status transition |
| `BUS_002` | `BUSINESS_DUPLICATE_ENTRY` | Duplicate entry found | Unique constraint |
| `BUS_003` | `BUSINESS_ENTITY_NOT_FOUND` | Requested resource not found | Invalid ID/reference |
| `BUS_004` | `BUSINESS_CONSTRAINT_VIOLATION` | Business rule violation | Domain invariant |
| `BUS_005` | `BUSINESS_INSUFFICIENT_FUNDS` | Insufficient funds for operation | Credit/balance check |
| `BUS_006` | `BUSINESS_LIMIT_EXCEEDED` | Operation limit exceeded | Credit limit, rate limit |
| `BUS_007` | `BUSINESS_INVALID_OPERATION` | Invalid operation | Unsupported action |
| `BUS_008` | `BUSINESS_DEPENDENCY_EXISTS` | Cannot delete due to existing dependencies | Foreign key reference |
| `BUS_009` | `RETURN_EXCEEDS_OUTSTANDING` | Return exceeds outstanding amount | Return quantity > remaining |
| `BUS_010` | `MODULE_DISABLED` | The requested module is disabled for this tenant | Feature gating |

### Usage Examples

```java
// Invalid state transition
throw new ApplicationException(ErrorCode.BUSINESS_INVALID_STATE,
    "Cannot confirm order in status: " + order.getStatus());

// Credit limit exceeded
throw new CreditLimitExceededException(
    "Order amount exceeds credit limit. Available: " + available);

// Entity not found
throw new ApplicationException(ErrorCode.BUSINESS_ENTITY_NOT_FOUND,
    "Order not found")
    .withDetail("orderId", orderId);
```

---

## Validation Errors (VAL_XXX)

Range: 3000-3999 | HTTP Status: 400 BAD REQUEST

| Code | Enum | Default Message | Common Causes |
|------|------|-----------------|---------------|
| `VAL_001` | `VALIDATION_INVALID_INPUT` | Invalid input provided | Malformed request |
| `VAL_002` | `VALIDATION_MISSING_REQUIRED_FIELD` | Required field is missing | Null/empty required field |
| `VAL_003` | `VALIDATION_INVALID_FORMAT` | Invalid data format | Pattern mismatch |
| `VAL_004` | `VALIDATION_OUT_OF_RANGE` | Value is out of acceptable range | Min/max violation |
| `VAL_005` | `VALIDATION_INVALID_DATE` | Invalid date or time value | Invalid date format |
| `VAL_006` | `VALIDATION_INVALID_REFERENCE` | Invalid reference to another resource | FK not found |
| `VAL_007` | `VALIDATION_INVALID_STATE` | Invalid state for requested operation | Status check |

### Usage Examples

```java
// Validation with field errors
@Valid public class OrderRequest {
    @NotBlank(message = "Order number is required")
    private String orderNumber;
    
    @Positive(message = "Quantity must be positive")
    private BigDecimal quantity;
}

// Manual validation
throw new ApplicationException(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
    "Dealer ID is required for order creation");
```

---

## System Errors (SYS_XXX)

Range: 4000-4999 | HTTP Status: 503 SERVICE UNAVAILABLE

| Code | Enum | Default Message | Common Causes |
|------|------|-----------------|---------------|
| `SYS_001` | `SYSTEM_INTERNAL_ERROR` | An internal error occurred | Unexpected exception |
| `SYS_002` | `SYSTEM_SERVICE_UNAVAILABLE` | Service temporarily unavailable | Dependency down |
| `SYS_003` | `SYSTEM_DATABASE_ERROR` | Database operation failed | DB connection/query error |
| `SYS_004` | `SYSTEM_EXTERNAL_SERVICE_ERROR` | External service error | Third-party failure |
| `SYS_005` | `SYSTEM_CONFIGURATION_ERROR` | System configuration error | Missing/invalid config |
| `SYS_006` | `SYSTEM_RATE_LIMIT_EXCEEDED` | Rate limit exceeded | Too many requests |
| `SYS_007` | `SYSTEM_MAINTENANCE_MODE` | System is under maintenance | Maintenance window |

### Usage Examples

```java
// Internal error (logged but not exposed)
log.error("Unexpected error processing order", e);
throw new ApplicationException(ErrorCode.SYSTEM_INTERNAL_ERROR);

// Rate limit exceeded
throw new ApplicationException(ErrorCode.SYSTEM_RATE_LIMIT_EXCEEDED,
    "Rate limit of " + limit + " requests per minute exceeded");
```

---

## Integration Errors (INT_XXX)

Range: 5000-5999 | HTTP Status: 502 BAD GATEWAY

| Code | Enum | Default Message | Common Causes |
|------|------|-----------------|---------------|
| `INT_001` | `INTEGRATION_CONNECTION_FAILED` | Failed to connect to external service | Network issue |
| `INT_002` | `INTEGRATION_TIMEOUT` | External service timeout | Slow response |
| `INT_003` | `INTEGRATION_INVALID_RESPONSE` | Invalid response from external service | Malformed response |
| `INT_004` | `INTEGRATION_AUTHENTICATION_FAILED` | External service authentication failed | Invalid credentials |

### Usage Examples

```java
// Connection failure
throw new ApplicationException(ErrorCode.INTEGRATION_CONNECTION_FAILED,
    "Failed to connect to payment gateway")
    .withDetail("service", "payment-gateway");

// Timeout
throw new ApplicationException(ErrorCode.INTEGRATION_TIMEOUT,
    "External service did not respond within " + timeout + "ms");
```

---

## File Operations (FILE_XXX)

Range: 6000-6999 | HTTP Status: 422 UNPROCESSABLE ENTITY

| Code | Enum | Default Message | Common Causes |
|------|------|-----------------|---------------|
| `FILE_001` | `FILE_NOT_FOUND` | File not found | Invalid path |
| `FILE_002` | `FILE_UPLOAD_FAILED` | File upload failed | Storage error |
| `FILE_003` | `FILE_INVALID_TYPE` | Invalid file type | Unsupported format |
| `FILE_004` | `FILE_SIZE_EXCEEDED` | File size limit exceeded | File too large |

### Usage Examples

```java
// Invalid file type
throw new ApplicationException(ErrorCode.FILE_INVALID_TYPE,
    "File type '" + contentType + "' is not supported. Allowed: PDF, PNG, JPEG");

// File size exceeded
throw new ApplicationException(ErrorCode.FILE_SIZE_EXCEEDED,
    "File size " + size + " exceeds maximum allowed size of " + maxSize);
```

---

## Concurrency Errors (CONC_XXX)

Range: 7000-7999 | HTTP Status: 409 CONFLICT

| Code | Enum | Default Message | Common Causes |
|------|------|-----------------|---------------|
| `CONC_001` | `CONCURRENCY_CONFLICT` | Resource was modified by another user | Optimistic lock failure |
| `CONC_002` | `CONCURRENCY_LOCK_TIMEOUT` | Could not acquire resource lock | Pessimistic lock timeout |
| `CONC_003` | `INTERNAL_CONCURRENCY_FAILURE` | Internal concurrency failure | Thread contention |

### Usage Examples

```java
// Optimistic lock failure
catch (ObjectOptimisticLockingFailureException e) {
    throw new ApplicationException(ErrorCode.CONCURRENCY_CONFLICT,
        "Order was modified by another user. Please refresh and try again.");
}

// Idempotency conflict
throw new ApplicationException(ErrorCode.CONCURRENCY_CONFLICT,
    "Request with this idempotency key already processed with different payload");
```

---

## Data Integrity Errors (DATA_XXX)

Range: 8000-8999 | HTTP Status: 409 CONFLICT

| Code | Enum | Default Message | Common Causes |
|------|------|-----------------|---------------|
| `DATA_001` | `DUPLICATE_ENTITY` | Duplicate entity found | Unique constraint violation |

### Usage Examples

```java
// Duplicate entity
catch (DataIntegrityViolationException e) {
    throw new ApplicationException(ErrorCode.DUPLICATE_ENTITY,
        "An order with number '" + orderNumber + "' already exists");
}
```

---

## Unknown Errors (ERR_XXX)

Range: 9999 | HTTP Status: 500 INTERNAL SERVER ERROR

| Code | Enum | Default Message | Common Causes |
|------|------|-----------------|---------------|
| `ERR_999` | `UNKNOWN_ERROR` | An unexpected error occurred | Unhandled exception |

### Usage Examples

```java
// Fallback for unexpected errors
catch (Exception e) {
    log.error("Unexpected error", e);
    throw new ApplicationException(ErrorCode.UNKNOWN_ERROR);
}
```

---

## Error Response Format

All errors follow a consistent response format:

```json
{
  "success": false,
  "message": "Operation failed",
  "data": {
    "code": "AUTH_005",
    "message": "Account is locked",
    "reason": "Account locked for 15 minutes due to 5 failed login attempts",
    "traceId": "550e8400-e29b-41d4-a716-446655440000",
    "errors": {
      "lockDuration": "15 minutes",
      "failedAttempts": 5
    }
  }
}
```

---

## Module-Specific Error Handling

### Accounting Module
- Uses `SettlementExceptionHandler` for settlement failures
- Routes to audit with `AuditExceptionRoutingService`

### Auth Module
- Uses `AuthSecurityContractException` for explicit HTTP status
- MFA exceptions: `MfaRequiredException`, `InvalidMfaException`

### Sales Module
- Credit limit: `CreditLimitExceededException` (extends `ApplicationException`)

---

## Adding New Error Codes

When adding new error codes, follow these guidelines:

1. **Choose the right category** based on the error type
2. **Use descriptive codes** that indicate the error type
3. **Provide clear default messages** that help users understand the issue
4. **Document the error** in this catalog
5. **Add appropriate logging** for debugging

```java
// Example: Adding a new error code
BUSINESS_CYCLE_DETECTED("BUS_011", "Circular reference detected in business entity"),
```

---

## Summary Table

| Category | Prefix | Range | Count |
|----------|--------|-------|-------|
| Authentication | AUTH_ | 1000-1999 | 10 |
| Business Logic | BUS_ | 2000-2999 | 10 |
| Validation | VAL_ | 3000-3999 | 7 |
| System | SYS_ | 4000-4999 | 7 |
| Integration | INT_ | 5000-5999 | 4 |
| File Operations | FILE_ | 6000-6999 | 4 |
| Concurrency | CONC_ | 7000-7999 | 3 |
| Data Integrity | DATA_ | 8000-8999 | 1 |
| Unknown | ERR_ | 9999 | 1 |

**Total: 47 Error Codes**
