package com.bigbrightpaints.erp.core.exception;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.HttpStatusCode;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.util.StringUtils;
import org.springframework.validation.FieldError;
import org.springframework.web.HttpMediaTypeNotAcceptableException;
import org.springframework.web.HttpMediaTypeNotSupportedException;
import org.springframework.web.HttpRequestMethodNotSupportedException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.MissingRequestHeaderException;
import org.springframework.web.bind.MissingServletRequestParameterException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.ServletWebRequest;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.multipart.MaxUploadSizeExceededException;
import org.springframework.web.servlet.NoHandlerFoundException;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;
import org.springframework.web.servlet.resource.NoResourceFoundException;

import com.fasterxml.jackson.databind.exc.UnrecognizedPropertyException;

import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.web.RequestTraceContext;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;

@ControllerAdvice
@Order(Ordered.HIGHEST_PRECEDENCE + 10)
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {
  private static final Logger logger = LoggerFactory.getLogger(GlobalExceptionHandler.class);
  private static final String CATALOG_ITEM_PATH = "/api/v1/catalog/items";
  private static final String CATALOG_ITEM_OPERATION = "catalog-item";
  private static final List<String> CATALOG_CONFLICT_RESPONSE_DETAIL_ALLOWLIST =
      List.of("generated", "conflicts", "wouldCreate", "created", "operation");
  static final Set<String> SETTLEMENT_FAILURE_DETAIL_ALLOWLIST =
      SettlementExceptionHandler.SETTLEMENT_FAILURE_DETAIL_ALLOWLIST;
  private final SettlementExceptionHandler settlementExceptionHandler =
      new SettlementExceptionHandler();
  private final AuditExceptionRoutingService auditExceptionRoutingService =
      new AuditExceptionRoutingService(settlementExceptionHandler);

  @Autowired(required = false)
  private AuditService auditService;

  @Value("${spring.profiles.active:prod}")
  private String activeProfile;

  private boolean isProductionMode() {
    if (!StringUtils.hasText(activeProfile)) {
      return true;
    }
    for (String profile : activeProfile.split(",")) {
      String normalized = profile.trim();
      if ("prod".equalsIgnoreCase(normalized) || "production".equalsIgnoreCase(normalized)) {
        return true;
      }
    }
    return false;
  }

  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleApplicationException(
      ApplicationException ex, HttpServletRequest request) {
    return buildApplicationExceptionResponse(ex, request, determineHttpStatus(ex.getErrorCode()));
  }

  public ResponseEntity<ApiResponse<Map<String, Object>>> buildApplicationExceptionResponse(
      ApplicationException ex, HttpServletRequest request, HttpStatus status) {
    return buildApplicationExceptionResponse(
        ex, request, status, resolveResponseDetails(ex, request, ex.getDetails()));
  }

  public ResponseEntity<ApiResponse<Map<String, Object>>> buildApplicationExceptionResponse(
      ApplicationException ex,
      HttpServletRequest request,
      HttpStatus status,
      Map<String, Object> responseDetails) {
    String traceId = RequestTraceContext.traceId();
    logger.error("Application error [{}] - Code: {}", traceId, ex.getErrorCode().getCode(), ex);
    Map<String, Object> data = new HashMap<>();
    data.put("code", ex.getErrorCode().getCode());
    data.put("message", ex.getUserMessage());
    data.put("reason", ex.getUserMessage());
    data.put("traceId", traceId);
    data.put("timestamp", LocalDateTime.now());
    data.put("path", request.getRequestURI());
    if (responseDetails != null && !responseDetails.isEmpty()) {
      data.put("details", responseDetails);
    }
    auditExceptionRoutingService.routeApplicationException(auditService, request, traceId, ex);
    return ResponseEntity.status(status).body(ApiResponse.failure(ex.getUserMessage(), data));
  }

  @ExceptionHandler(CreditLimitExceededException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleCreditLimitExceeded(
      CreditLimitExceededException ex, HttpServletRequest request) {
    String traceId = RequestTraceContext.traceId();
    logger.warn("Credit limit exceeded [{}]", traceId);
    Map<String, Object> data = new HashMap<>();
    data.put("code", ex.getErrorCode().getCode());
    data.put("message", ex.getUserMessage());
    data.put("reason", ex.getUserMessage());
    data.put("traceId", traceId);
    data.put("timestamp", LocalDateTime.now());
    data.put("path", request.getRequestURI());
    Map<String, Object> details = resolveResponseDetails(ex, request, ex.getDetails());
    if (!details.isEmpty()) {
      data.put("details", details);
    }
    auditExceptionRoutingService.routeApplicationException(auditService, request, traceId, ex);
    return ResponseEntity.status(HttpStatus.UNPROCESSABLE_ENTITY)
        .body(ApiResponse.failure(ex.getUserMessage(), data));
  }

  @Override
  protected ResponseEntity<Object> handleMethodArgumentNotValid(
      MethodArgumentNotValidException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String traceId = RequestTraceContext.traceId();
    logger.warn("Validation error [{}]", traceId);
    Map<String, String> fieldErrors = new LinkedHashMap<>();
    for (FieldError error : ex.getBindingResult().getFieldErrors()) {
      fieldErrors.putIfAbsent(error.getField(), error.getDefaultMessage());
    }
    String reason = buildValidationReason(fieldErrors);
    Map<String, Object> data = new HashMap<>();
    data.put("code", ErrorCode.VALIDATION_INVALID_INPUT.getCode());
    data.put("message", reason);
    data.put("reason", reason);
    data.put("traceId", traceId);
    data.put("errors", fieldErrors);
    data.put("path", resolveRequestPath(request));
    return ResponseEntity.badRequest().body(ApiResponse.failure(reason, data));
  }

  @Override
  protected ResponseEntity<Object> handleHttpMessageNotReadable(
      HttpMessageNotReadableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    String traceId = RequestTraceContext.traceId();
    String reason = "Failed to read request";
    String responseReason = reason;
    String detail = resolveMostSpecificMessage(ex);
    logger.warn("Malformed request [{}]", traceId);
    Map<String, Object> data = new HashMap<>();
    data.put("code", ErrorCode.VALIDATION_INVALID_INPUT.getCode());
    data.put("message", reason);
    data.put("reason", reason);
    data.put("traceId", traceId);
    data.put("path", resolveRequestPath(request));
    UnrecognizedPropertyException unknownProperty =
        findCause(ex, UnrecognizedPropertyException.class);
    if (unknownProperty != null) {
      String fieldPath = resolveUnknownPropertyPath(unknownProperty);
      responseReason = "Unsupported field: " + fieldPath;
      Map<String, String> fieldErrors = new LinkedHashMap<>();
      fieldErrors.put(fieldPath, "Unsupported field");
      data.put("message", responseReason);
      data.put("reason", responseReason);
      data.put("errors", fieldErrors);
    }
    HttpServletRequest servletRequest =
        request instanceof ServletWebRequest servletWebRequest
            ? servletWebRequest.getRequest()
            : null;
    auditExceptionRoutingService.routeMalformedRequest(
        auditService, servletRequest, traceId, reason, detail);
    return ResponseEntity.badRequest().body(ApiResponse.failure(responseReason, data));
  }

  private static <T extends Throwable> T findCause(Throwable throwable, Class<T> type) {
    Throwable current = throwable;
    while (current != null) {
      if (type.isInstance(current)) {
        return type.cast(current);
      }
      current = current.getCause();
    }
    return null;
  }

  private static String resolveUnknownPropertyPath(UnrecognizedPropertyException ex) {
    List<String> path =
        ex.getPath().stream()
            .map(
                reference -> {
                  if (StringUtils.hasText(reference.getFieldName())) {
                    return reference.getFieldName();
                  }
                  return reference.getIndex() >= 0 ? "[" + reference.getIndex() + "]" : "";
                })
            .filter(StringUtils::hasText)
            .toList();
    String property = ex.getPropertyName();
    if (path.isEmpty()) {
      return property;
    }
    String joined = String.join(".", path);
    return joined.endsWith("." + property) || joined.equals(property)
        ? joined
        : joined + "." + property;
  }

  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotSupported(
      HttpMediaTypeNotSupportedException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return buildFrameworkError(
        HttpStatus.UNSUPPORTED_MEDIA_TYPE,
        ErrorCode.VALIDATION_INVALID_INPUT,
        "Unsupported media type",
        request);
  }

  @Override
  protected ResponseEntity<Object> handleHttpMediaTypeNotAcceptable(
      HttpMediaTypeNotAcceptableException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return buildFrameworkError(
        HttpStatus.NOT_ACCEPTABLE,
        ErrorCode.VALIDATION_INVALID_INPUT,
        "Requested response media type is not available",
        request);
  }

  @Override
  protected ResponseEntity<Object> handleHttpRequestMethodNotSupported(
      HttpRequestMethodNotSupportedException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    headers.setAllow(
        ex.getSupportedHttpMethods() == null ? Set.of() : ex.getSupportedHttpMethods());
    return buildFrameworkError(
        HttpStatus.METHOD_NOT_ALLOWED,
        ErrorCode.VALIDATION_INVALID_INPUT,
        "HTTP method is not supported for this endpoint",
        request);
  }

  @Override
  protected ResponseEntity<Object> handleMissingServletRequestParameter(
      MissingServletRequestParameterException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return buildFrameworkError(
        HttpStatus.BAD_REQUEST,
        ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
        "Required request parameter is missing",
        request);
  }

  @ExceptionHandler(MissingRequestHeaderException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleMissingRequestHeader(
      MissingRequestHeaderException ex, HttpServletRequest request) {
    return buildServletFrameworkError(
        HttpStatus.BAD_REQUEST,
        ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
        "Required request header is missing",
        request);
  }

  @Override
  protected ResponseEntity<Object> handleNoHandlerFoundException(
      NoHandlerFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    return buildFrameworkError(
        HttpStatus.NOT_FOUND,
        ErrorCode.BUSINESS_ENTITY_NOT_FOUND,
        "Requested resource was not found",
        request);
  }

  @Override
  protected ResponseEntity<Object> handleNoResourceFoundException(
      NoResourceFoundException ex, HttpHeaders headers, HttpStatusCode status, WebRequest request) {
    return buildFrameworkError(
        HttpStatus.NOT_FOUND,
        ErrorCode.BUSINESS_ENTITY_NOT_FOUND,
        "Requested resource was not found",
        request);
  }

  @Override
  protected ResponseEntity<Object> handleMaxUploadSizeExceededException(
      MaxUploadSizeExceededException ex,
      HttpHeaders headers,
      HttpStatusCode status,
      WebRequest request) {
    return buildFrameworkError(
        HttpStatus.PAYLOAD_TOO_LARGE,
        ErrorCode.FILE_SIZE_EXCEEDED,
        "Request body is too large",
        request);
  }

  @ExceptionHandler(ConstraintViolationException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleConstraintViolation(
      ConstraintViolationException ex, HttpServletRequest request) {
    String traceId = RequestTraceContext.traceId();
    logger.warn("Constraint violation [{}]", traceId, ex);
    Map<String, String> violations = new LinkedHashMap<>();
    ex.getConstraintViolations()
        .forEach(
            violation -> {
              String rawPath =
                  violation.getPropertyPath() != null
                      ? violation.getPropertyPath().toString()
                      : "value";
              String field = rawPath;
              int dot = rawPath.lastIndexOf('.');
              if (dot >= 0 && dot < rawPath.length() - 1) {
                field = rawPath.substring(dot + 1);
              }
              violations.putIfAbsent(field, violation.getMessage());
            });
    String reason = buildValidationReason(violations);
    Map<String, Object> data = new HashMap<>();
    data.put("code", ErrorCode.VALIDATION_INVALID_INPUT.getCode());
    data.put("message", reason);
    data.put("reason", reason);
    data.put("traceId", traceId);
    data.put("path", request.getRequestURI());
    if (!violations.isEmpty()) {
      data.put("errors", violations);
    }
    return ResponseEntity.badRequest().body(ApiResponse.failure(reason, data));
  }

  @ExceptionHandler(IllegalArgumentException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleIllegalArgument(
      IllegalArgumentException ex, HttpServletRequest request) {
    String traceId = RequestTraceContext.traceId();
    logger.warn("Illegal argument [{}]", traceId);
    String reason = resolveIllegalArgumentMessage(ex.getMessage());
    Map<String, Object> data = new HashMap<>();
    data.put("code", ErrorCode.VALIDATION_INVALID_INPUT.getCode());
    data.put("message", reason);
    data.put("reason", reason);
    data.put("traceId", traceId);
    data.put("path", request.getRequestURI());
    return ResponseEntity.badRequest().body(ApiResponse.failure(reason, data));
  }

  private ResponseEntity<Object> buildFrameworkError(
      HttpStatus status, ErrorCode code, String message, WebRequest request) {
    String traceId = RequestTraceContext.traceId();
    logger.warn("Safe framework request error [{}] - status: {}", traceId, status.value());
    Map<String, Object> data = new HashMap<>();
    data.put("code", code.getCode());
    data.put("message", message);
    data.put("reason", message);
    data.put("traceId", traceId);
    data.put("path", resolveRequestPath(request));
    return ResponseEntity.status(status).body(ApiResponse.failure(message, data));
  }

  private ResponseEntity<ApiResponse<Map<String, Object>>> buildServletFrameworkError(
      HttpStatus status, ErrorCode code, String message, HttpServletRequest request) {
    String traceId = RequestTraceContext.traceId();
    logger.warn("Safe framework request error [{}] - status: {}", traceId, status.value());
    Map<String, Object> data = new HashMap<>();
    data.put("code", code.getCode());
    data.put("message", message);
    data.put("reason", message);
    data.put("traceId", traceId);
    data.put("path", request.getRequestURI());
    return ResponseEntity.status(status).body(ApiResponse.failure(message, data));
  }

  private HttpStatus determineHttpStatus(ErrorCode errorCode) {
    if (errorCode == null) return HttpStatus.INTERNAL_SERVER_ERROR;
    if (errorCode == ErrorCode.BUSINESS_ENTITY_NOT_FOUND || errorCode == ErrorCode.FILE_NOT_FOUND)
      return HttpStatus.NOT_FOUND;
    if (errorCode == ErrorCode.AUTH_MFA_REQUIRED) return HttpStatus.PRECONDITION_REQUIRED;
    if (errorCode == ErrorCode.MODULE_DISABLED) return HttpStatus.FORBIDDEN;
    if (errorCode == ErrorCode.SYSTEM_RATE_LIMIT_EXCEEDED) return HttpStatus.TOO_MANY_REQUESTS;
    String prefix = errorCode.getCode().split("_")[0];
    return switch (prefix) {
      case "AUTH" -> HttpStatus.UNAUTHORIZED;
      case "VAL" -> HttpStatus.BAD_REQUEST;
      case "BUS", "CONC", "DATA" -> HttpStatus.CONFLICT;
      case "SYS" -> HttpStatus.SERVICE_UNAVAILABLE;
      case "INT" -> HttpStatus.BAD_GATEWAY;
      case "FILE" -> HttpStatus.UNPROCESSABLE_ENTITY;
      default -> HttpStatus.INTERNAL_SERVER_ERROR;
    };
  }

  private String buildValidationReason(Map<String, String> errors) {
    if (errors == null || errors.isEmpty()) return "Validation failed";
    StringBuilder builder = new StringBuilder("Validation failed: ");
    int count = 0;
    for (Map.Entry<String, String> entry : errors.entrySet()) {
      if (count > 0) builder.append("; ");
      builder.append(entry.getKey()).append(" ").append(entry.getValue());
      count++;
      if (count >= 3) break;
    }
    if (errors.size() > 3) builder.append("; and ").append(errors.size() - 3).append(" more");
    return builder.toString();
  }

  private Map<String, Object> resolveResponseDetails(
      ApplicationException ex, HttpServletRequest request, Map<String, Object> details) {
    if (details == null || details.isEmpty()) return Map.of();
    if (!isProductionMode()) return details;
    return sanitizeCatalogConflictDetails(ex, request, details);
  }

  private Map<String, Object> sanitizeCatalogConflictDetails(
      ApplicationException ex, HttpServletRequest request, Map<String, Object> details) {
    if (!isCatalogConflictDetailsSafeToExpose(ex, request, details)) return Map.of();
    Map<String, Object> sanitized = new LinkedHashMap<>();
    for (String key : CATALOG_CONFLICT_RESPONSE_DETAIL_ALLOWLIST) {
      if (details.containsKey(key)) sanitized.put(key, details.get(key));
    }
    return sanitized;
  }

  private boolean isCatalogConflictDetailsSafeToExpose(
      ApplicationException ex, HttpServletRequest request, Map<String, Object> details) {
    if (ex == null || request == null || ex.getErrorCode() != ErrorCode.CONCURRENCY_CONFLICT)
      return false;
    return matchesSafeConflictContract(request, details, CATALOG_ITEM_PATH, CATALOG_ITEM_OPERATION);
  }

  private boolean matchesSafeConflictContract(
      HttpServletRequest request, Map<String, Object> details, String path, String operation) {
    if (!matchesEndpoint(request, path)) return false;
    return operation.equals(details.get("operation"));
  }

  private boolean matchesEndpoint(HttpServletRequest request, String endpointPath) {
    for (String path : resolveNormalizedRequestPaths(request)) {
      if (matchesEndpointPath(path, endpointPath)) return true;
    }
    return false;
  }

  private List<String> resolveNormalizedRequestPaths(HttpServletRequest request) {
    if (request == null) return List.of();
    String servletPath = normalizeEndpointPath(request.getServletPath());
    String pathInfo = normalizeEndpointPath(request.getPathInfo());
    String combined = normalizeEndpointPath(joinServletPathAndPathInfo(servletPath, pathInfo));
    String requestUri =
        normalizeEndpointPath(stripContextPath(request.getRequestURI(), request.getContextPath()));
    return List.of(combined, servletPath, pathInfo, requestUri);
  }

  private String joinServletPathAndPathInfo(String servletPath, String pathInfo) {
    if (!StringUtils.hasText(pathInfo)) return servletPath;
    if (!StringUtils.hasText(servletPath) || "/".equals(servletPath)) return pathInfo;
    return servletPath + pathInfo;
  }

  private String stripContextPath(String requestUri, String contextPath) {
    if (!StringUtils.hasText(requestUri)) return "";
    if (StringUtils.hasText(contextPath) && requestUri.startsWith(contextPath)) {
      String stripped = requestUri.substring(contextPath.length());
      return StringUtils.hasText(stripped) ? stripped : "/";
    }
    return requestUri;
  }

  private boolean matchesEndpointPath(String normalizedPath, String endpointPath) {
    if (!StringUtils.hasText(normalizedPath) || !StringUtils.hasText(endpointPath)) return false;
    return normalizedPath.equals(endpointPath) || normalizedPath.endsWith(endpointPath);
  }

  private String normalizeEndpointPath(String value) {
    if (!StringUtils.hasText(value)) return "";
    String normalized = value.trim();
    if (!normalized.startsWith("/")) normalized = "/" + normalized;
    while (normalized.length() > 1 && normalized.endsWith("/")) {
      normalized = normalized.substring(0, normalized.length() - 1);
    }
    return normalized;
  }

  private String resolveIllegalArgumentMessage(String message) {
    if (!StringUtils.hasText(message)) return "Invalid input provided";
    String trimmed = message.trim();
    if (!trimmed.startsWith("No enum constant ")) return trimmed;
    String token = trimmed.substring("No enum constant ".length()).trim();
    if (!StringUtils.hasText(token) || token.endsWith(".")) return "Invalid option provided";
    int lastDot = token.lastIndexOf('.');
    String invalidValue =
        lastDot >= 0 && lastDot < token.length() - 1 ? token.substring(lastDot + 1) : token;
    return StringUtils.hasText(invalidValue)
        ? "Invalid option '" + invalidValue + "'"
        : "Invalid option provided";
  }

  private String resolveMostSpecificMessage(Throwable ex) {
    if (ex == null) return null;
    Throwable current = ex;
    while (current.getCause() != null && current.getCause() != current) {
      current = current.getCause();
    }
    return current.getMessage();
  }

  private String resolveRequestPath(WebRequest request) {
    if (request instanceof ServletWebRequest servletWebRequest) {
      return servletWebRequest.getRequest().getRequestURI();
    }
    return null;
  }
}
