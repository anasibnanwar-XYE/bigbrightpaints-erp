# Core Infrastructure: Exception Handling

This document covers all exception classes in `com.bigbrightpaints.erp.core.exception`.

## Overview

Exception classes provide structured error handling with error codes, user-friendly messages, and audit integration. They support validation errors, business rule violations, security exceptions, and integration failures.

---

## ErrorCode

| Field | Value |
|-------|-------|
| **Name** | ErrorCode |
| **Type** | Enum |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.exception |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/exception/ErrorCode.java |
| **Responsibility** | Defines all error codes with unique identifiers and default messages |
| **Use when** | Throwing or handling application errors |
| **Do not use when** | N/A |
| **Public methods** | `String getCode()`<br>`String getDefaultMessage()` |
| **Callers** | ApplicationException, GlobalExceptionHandler, all services |
| **Dependencies** | None |
| **Side effects** | None |
| **Invariants protected** | Consistent error code format |
| **Status** | Canonical |

### Error Code Categories

| Prefix | Range | Category |
|--------|-------|----------|
| AUTH_XXX | 1000-1999 | Authentication & Authorization |
| BUS_XXX | 2000-2999 | Business Logic Errors |
| VAL_XXX | 3000-3999 | Validation Errors |
| SYS_XXX | 4000-4999 | System Errors |
| INT_XXX | 5000-5999 | Integration Errors |
| FILE_XXX | 6000-6999 | File Operations |
| CONC_XXX | 7000-7999 | Concurrency Errors |
| DATA_XXX | 8000-8999 | Data Integrity Errors |
| ERR_XXX | 9999 | Unknown Error |

### Common Error Codes

| Code | Enum | Default Message |
|------|------|-----------------|
| AUTH_001 | AUTH_INVALID_CREDENTIALS | Invalid credentials provided |
| AUTH_004 | AUTH_INSUFFICIENT_PERMISSIONS | Insufficient permissions for this operation |
| AUTH_005 | AUTH_ACCOUNT_LOCKED | Account is locked |
| AUTH_007 | AUTH_MFA_REQUIRED | Multi-factor authentication required |
| BUS_001 | BUSINESS_INVALID_STATE | Operation not allowed in current state |
| BUS_003 | BUSINESS_ENTITY_NOT_FOUND | Requested resource not found |
| BUS_006 | BUSINESS_LIMIT_EXCEEDED | Operation limit exceeded |
| VAL_001 | VALIDATION_INVALID_INPUT | Invalid input provided |
| VAL_002 | VALIDATION_MISSING_REQUIRED_FIELD | Required field is missing |
| SYS_001 | SYSTEM_INTERNAL_ERROR | An internal error occurred |
| SYS_006 | SYSTEM_RATE_LIMIT_EXCEEDED | Rate limit exceeded |
| CONC_001 | CONCURRENCY_CONFLICT | Resource was modified by another user |

---

## ApplicationException

| Field | Value |
|-------|-------|
| **Name** | ApplicationException |
| **Type** | Runtime Exception |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.exception |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/exception/ApplicationException.java |
| **Responsibility** | Base exception for all application-specific errors with error codes and details |
| **Use when** | Throwing business/validation errors |
| **Do not use when** | Framework exceptions (use native types) |
| **Public methods** | `ApplicationException(ErrorCode errorCode)`<br>`ApplicationException(ErrorCode errorCode, String userMessage)`<br>`ApplicationException(ErrorCode errorCode, String userMessage, Throwable cause)`<br>`ApplicationException(ErrorCode errorCode, Throwable cause)`<br>`ApplicationException withDetail(String key, Object value)`<br>`ErrorCode getErrorCode()`<br>`Map<String, Object> getDetails()`<br>`String getUserMessage()`<br>`Map<String, Object> toErrorResponse(boolean includeDetails)` |
| **Callers** | All services throwing business errors |
| **Dependencies** | ErrorCode |
| **Side effects** | None |
| **Invariants protected** | Consistent error structure; chainable detail building |
| **Status** | Canonical |

### Usage Pattern

```java
throw new ApplicationException(ErrorCode.BUSINESS_ENTITY_NOT_FOUND, "Order not found")
    .withDetail("orderId", orderId)
    .withDetail("companyCode", company.getCode());
```

---

## AuthSecurityContractException

| Field | Value |
|-------|-------|
| **Name** | AuthSecurityContractException |
| **Type** | Runtime Exception |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.exception |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/exception/AuthSecurityContractException.java |
| **Responsibility** | Carries explicit HTTP status plus frontend-safe auth/security error contract |
| **Use when** | Auth/security errors requiring specific HTTP status codes |
| **Do not use when** | General business errors (use ApplicationException) |
| **Public methods** | `AuthSecurityContractException(HttpStatus httpStatus, String code, String userMessage)`<br>`AuthSecurityContractException withDetail(String key, Object value)`<br>`HttpStatus getHttpStatus()`<br>`String getCode()`<br>`String getUserMessage()`<br>`Map<String, Object> getDetails()` |
| **Callers** | Auth services, security filters |
| **Dependencies** | HttpStatus (Spring) |
| **Side effects** | None |
| **Invariants protected** | Explicit HTTP status for auth errors |
| **Status** | Canonical |

---

## CreditLimitExceededException

| Field | Value |
|-------|-------|
| **Name** | CreditLimitExceededException |
| **Type** | Runtime Exception (extends ApplicationException) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.exception |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/exception/CreditLimitExceededException.java |
| **Responsibility** | Specialized exception for credit limit violations |
| **Use when** | Dealer credit limit exceeded |
| **Do not use when** | General business limit errors |
| **Public methods** | `CreditLimitExceededException(String userMessage)` |
| **Callers** | CreditService, SalesService |
| **Dependencies** | ApplicationException, ErrorCode.BUSINESS_LIMIT_EXCEEDED |
| **Side effects** | None |
| **Invariants protected** | Consistent error code for credit limits |
| **Status** | Canonical |

---

## GlobalExceptionHandler

| Field | Value |
|-------|-------|
| **Name** | GlobalExceptionHandler |
| **Type** | Controller Advice (Spring @ControllerAdvice) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.exception |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/exception/GlobalExceptionHandler.java |
| **Responsibility** | Primary exception handler for validation and application errors |
| **Use when** | Understanding how exceptions become API responses |
| **Do not use when** | N/A |
| **Public methods** | `ResponseEntity<ApiResponse<Map<String, Object>>> handleApplicationException(ApplicationException ex, HttpServletRequest request)`<br>`ResponseEntity<Object> handleMethodArgumentNotValid(MethodArgumentNotValidException ex, ...)`<br>`ResponseEntity<Object> handleHttpMessageNotReadable(HttpMessageNotReadableException ex, ...)`<br>`ResponseEntity<ApiResponse<Map<String, Object>>> handleConstraintViolation(ConstraintViolationException ex, ...)`<br>`ResponseEntity<ApiResponse<Map<String, Object>>> handleIllegalArgument(IllegalArgumentException ex, ...)` |
| **Callers** | Spring MVC |
| **Dependencies** | AuditService, AuditExceptionRoutingService, SettlementExceptionHandler |
| **Side effects** | Audit logging for integration failures |
| **Invariants protected** | Consistent error response format; validation error aggregation |
| **Status** | Canonical |

### HTTP Status Mapping

| Error Code Prefix | HTTP Status |
|-------------------|-------------|
| AUTH_* | 401 UNAUTHORIZED |
| VAL_* | 400 BAD REQUEST |
| BUS_* | 409 CONFLICT |
| CONC_* | 409 CONFLICT |
| DATA_* | 409 CONFLICT |
| SYS_* | 503 SERVICE_UNAVAILABLE |
| INT_* | 502 BAD_GATEWAY |
| FILE_* | 422 UNPROCESSABLE_ENTITY |

### Error Response Structure

```json
{
  "success": false,
  "message": "Validation failed",
  "data": {
    "code": "VAL_001",
    "message": "Validation failed: field1 is required; field2 invalid format",
    "reason": "Validation failed...",
    "traceId": "uuid",
    "errors": {
      "field1": "is required",
      "field2": "invalid format"
    }
  }
}
```

---

## CoreFallbackExceptionHandler

| Field | Value |
|-------|-------|
| **Name** | CoreFallbackExceptionHandler |
| **Type** | Controller Advice (Spring @ControllerAdvice) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.exception |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/exception/CoreFallbackExceptionHandler.java |
| **Responsibility** | Fallback exception handler for security, authentication, and unhandled errors |
| **Use when** | Understanding how unhandled exceptions become API responses |
| **Do not use when** | N/A |
| **Public methods** | `ResponseEntity<ApiResponse<Map<String, Object>>> handleCreditLimitExceeded(CreditLimitExceededException ex, ...)`<br>`ResponseEntity<ApiResponse<MfaChallengeResponse>> handleMfaRequired(MfaRequiredException ex, ...)`<br>`ResponseEntity<ApiResponse<Map<String, Object>>> handleInvalidMfa(InvalidMfaException ex, ...)`<br>`ResponseEntity<ApiResponse<Map<String, Object>>> handleAuthenticationException(AuthenticationException ex, ...)`<br>`ResponseEntity<ApiResponse<Map<String, Object>>> handleAuthSecurityContract(AuthSecurityContractException ex, ...)`<br>`ResponseEntity<ApiResponse<Map<String, Object>>> handleAccessDenied(AccessDeniedException ex, ...)`<br>`ResponseEntity<ApiResponse<Map<String, Object>>> handleDataIntegrityViolation(DataIntegrityViolationException ex, ...)`<br>`ResponseEntity<ApiResponse<Map<String, Object>>> handleIllegalState(IllegalStateException ex, ...)`<br>`ResponseEntity<ApiResponse<Map<String, Object>>> handleRuntime(RuntimeException ex, ...)`<br>`ResponseEntity<ApiResponse<Map<String, Object>>> handleGenericException(Exception ex, ...)` |
| **Callers** | Spring MVC |
| **Dependencies** | PortalRoleActionMatrix |
| **Side effects** | None |
| **Invariants protected** | No stack traces in production; graceful error responses |
| **Status** | Canonical |

### Exception Priority

`GlobalExceptionHandler` (HIGHEST_PRECEDENCE) → `CoreFallbackExceptionHandler` (LOWEST_PRECEDENCE)

---

## SettlementExceptionHandler

| Field | Value |
|-------|-------|
| **Name** | SettlementExceptionHandler |
| **Type** | Utility (helper class) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.exception |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/exception/SettlementExceptionHandler.java |
| **Responsibility** | Routes settlement failures to audit with proper metadata |
| **Use when** | Handling accounting settlement failures |
| **Do not use when** | Non-settlement errors |
| **Public methods** | `boolean isSettlementRequest(HttpServletRequest request)`<br>`Map<String, String> buildSettlementFailureMetadata(HttpServletRequest request, String traceId, ApplicationException ex)` |
| **Callers** | AuditExceptionRoutingService |
| **Dependencies** | IntegrationFailureAlertRoutingPolicy, IntegrationFailureMetadataSchema |
| **Side effects** | None |
| **Invariants protected** | Settlement failure metadata schema compliance |
| **Status** | Canonical |

### Settlement Failure Detail Allowlist

| Key | Description |
|-----|-------------|
| idempotencyKey | Request idempotency key |
| partnerType | DEALER or SUPPLIER |
| partnerId | Partner entity ID |
| invoiceId | Invoice ID (if applicable) |
| purchaseId | Purchase ID (if applicable) |
| outstandingAmount | Amount outstanding |
| appliedAmount | Amount being applied |
| allocationCount | Number of allocations |

---

## AuditExceptionRoutingService

| Field | Value |
|-------|-------|
| **Name** | AuditExceptionRoutingService |
| **Type** | Service |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.exception |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/exception/AuditExceptionRoutingService.java |
| **Responsibility** | Routes application exceptions to audit service with integration failure metadata |
| **Use when** | Understanding how exceptions become audit events |
| **Do not use when** | N/A |
| **Public methods** | `void routeApplicationException(AuditService auditService, HttpServletRequest request, String traceId, ApplicationException ex)`<br>`void routeMalformedRequest(AuditService auditService, HttpServletRequest request, String traceId, String reason, String detail)` |
| **Callers** | GlobalExceptionHandler |
| **Dependencies** | SettlementExceptionHandler, AuditService |
| **Side effects** | Audit log entries |
| **Invariants protected** | Integration failure routing policy compliance |
| **Status** | Canonical |

### Routing Logic

1. **Settlement Requests**: Routes to AuditService as INTEGRATION_FAILURE with settlement metadata
2. **Malformed Requests**: Routes with MALFORMED_REQUEST_PAYLOAD failure code
3. **Other Requests**: No special routing
