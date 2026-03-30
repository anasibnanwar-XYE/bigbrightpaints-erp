package com.bigbrightpaints.erp.modules.accounting.controller;

import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

import jakarta.servlet.http.HttpServletRequest;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.core.auditaccess.AuditAccessService;
import com.bigbrightpaints.erp.core.auditaccess.AuditControllerSupport;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.auditaccess.dto.AuditFeedItemDto;
import com.bigbrightpaints.erp.core.security.PortalRoleActionMatrix;
import com.bigbrightpaints.erp.modules.accounting.dto.AccountingTransactionAuditDetailDto;
import com.bigbrightpaints.erp.modules.accounting.dto.AccountingTransactionAuditListItemDto;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

@RestController
@RequestMapping("/api/v1/accounting/audit")
public class AccountingAuditController extends AuditControllerSupport {

  private final AuditAccessService auditAccessService;

  public AccountingAuditController(AuditAccessService auditAccessService) {
    this.auditAccessService = auditAccessService;
  }

  @ExceptionHandler(ApplicationException.class)
  public ResponseEntity<ApiResponse<Map<String, Object>>> handleApplicationException(
      ApplicationException ex, HttpServletRequest request) {
    String traceId = UUID.randomUUID().toString();
    Map<String, Object> errorData = new HashMap<>();
    errorData.put("code", ex.getErrorCode().getCode());
    errorData.put("message", ex.getUserMessage());
    errorData.put("reason", ex.getUserMessage());
    errorData.put("path", request != null ? request.getRequestURI() : null);
    errorData.put("traceId", traceId);
    Map<String, Object> details = ex.getDetails();
    if (!details.isEmpty()) {
      errorData.put("details", details);
    }
    return ResponseEntity.status(determineHttpStatus(ex.getErrorCode()))
        .body(ApiResponse.failure(ex.getUserMessage(), errorData));
  }

  @GetMapping("/events")
  @PreAuthorize(PortalRoleActionMatrix.TENANT_ADMIN_OR_ACCOUNTING_ONLY)
  public ResponseEntity<ApiResponse<PageResponse<AuditFeedItemDto>>> listEvents(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) String module,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String actor,
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) String reference,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(
        ApiResponse.success(
            auditAccessService.queryAccountingFeed(
                buildFilter(from, to, module, action, status, actor, entityType, reference, page, size))));
  }

  @GetMapping("/transactions")
  @PreAuthorize(PortalRoleActionMatrix.TENANT_ADMIN_OR_ACCOUNTING_ONLY)
  public ResponseEntity<ApiResponse<PageResponse<AccountingTransactionAuditListItemDto>>> transactionAudit(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) String module,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String reference,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(
        ApiResponse.success(
            auditAccessService.queryAccountingTransactions(
                parseOptionalDate(from, "from"),
                parseOptionalDate(to, "to"),
                module,
                status,
                reference,
                page,
                size)));
  }

  @GetMapping("/transactions/{journalEntryId}")
  @PreAuthorize(PortalRoleActionMatrix.TENANT_ADMIN_OR_ACCOUNTING_ONLY)
  public ResponseEntity<ApiResponse<AccountingTransactionAuditDetailDto>> transactionAuditDetail(
      @PathVariable Long journalEntryId) {
    return ResponseEntity.ok(
        ApiResponse.success(auditAccessService.getAccountingTransactionDetail(journalEntryId)));
  }

  private HttpStatus determineHttpStatus(ErrorCode errorCode) {
    if (errorCode == null) {
      return HttpStatus.INTERNAL_SERVER_ERROR;
    }
    if (errorCode == ErrorCode.BUSINESS_ENTITY_NOT_FOUND || errorCode == ErrorCode.FILE_NOT_FOUND) {
      return HttpStatus.NOT_FOUND;
    }
    if (errorCode == ErrorCode.AUTH_MFA_REQUIRED) {
      return HttpStatus.PRECONDITION_REQUIRED;
    }
    if (errorCode == ErrorCode.MODULE_DISABLED) {
      return HttpStatus.FORBIDDEN;
    }
    if (errorCode == ErrorCode.SYSTEM_RATE_LIMIT_EXCEEDED) {
      return HttpStatus.TOO_MANY_REQUESTS;
    }
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
}
