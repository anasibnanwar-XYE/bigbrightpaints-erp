package com.bigbrightpaints.erp.modules.company.controller;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bigbrightpaints.erp.core.auditaccess.AuditAccessService;
import com.bigbrightpaints.erp.core.auditaccess.AuditControllerSupport;
import com.bigbrightpaints.erp.core.auditaccess.dto.AuditFeedItemDto;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.security.PortalRoleActionMatrix;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminSecurityAuditDtos;
import com.bigbrightpaints.erp.modules.company.service.SuperAdminSecurityAuditService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

import io.swagger.v3.oas.annotations.Hidden;
import jakarta.validation.Valid;

@RestController
@RequestMapping("/api/v1/superadmin/audit")
@PreAuthorize(PortalRoleActionMatrix.SUPER_ADMIN_ONLY)
public class SuperAdminAuditController extends AuditControllerSupport {

  private final AuditAccessService auditAccessService;
  private final SuperAdminSecurityAuditService securityAuditService;

  public SuperAdminAuditController(
      AuditAccessService auditAccessService, SuperAdminSecurityAuditService securityAuditService) {
    this.auditAccessService = auditAccessService;
    this.securityAuditService = securityAuditService;
  }

  @GetMapping("/platform-events")
  public ResponseEntity<ApiResponse<PageResponse<AuditFeedItemDto>>> listPlatformEvents(
      @RequestParam(required = false) String from,
      @RequestParam(required = false) String to,
      @RequestParam(required = false) String module,
      @RequestParam(required = false) String action,
      @RequestParam(required = false) String status,
      @RequestParam(required = false) String actor,
      @RequestParam(required = false) String entityType,
      @RequestParam(required = false) String reference,
      @RequestParam(required = false) Long tenantId,
      @RequestParam(required = false) String category,
      @RequestParam(defaultValue = "0") int page,
      @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(
        ApiResponse.success(
            auditAccessService.queryPlatformFeed(
                buildFilter(
                    from,
                    to,
                    module,
                    action,
                    status,
                    actor,
                    entityType,
                    reference,
                    tenantId,
                    category,
                    page,
                    size))));
  }

  @GetMapping("/security-events")
  public ResponseEntity<
          ApiResponse<PageResponse<SuperAdminSecurityAuditDtos.SecurityEventResponse>>>
      listSecurityEvents(
          @RequestParam(required = false) String from,
          @RequestParam(required = false) String to,
          @RequestParam(required = false) String action,
          @RequestParam(required = false) String status,
          @RequestParam(required = false) String actor,
          @RequestParam(required = false) String entityType,
          @RequestParam(required = false) String reference,
          @RequestParam(required = false) Long tenantId,
          @RequestParam(required = false) String category,
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(
        ApiResponse.success(
            securityAuditService.listSecurityEvents(
                buildFilter(
                    from,
                    to,
                    null,
                    action,
                    status,
                    actor,
                    entityType,
                    reference,
                    tenantId,
                    category,
                    page,
                    size))));
  }

  @GetMapping("/suspicious-events")
  public ResponseEntity<
          ApiResponse<PageResponse<SuperAdminSecurityAuditDtos.SecurityEventResponse>>>
      listSuspiciousEvents(
          @RequestParam(required = false) String from,
          @RequestParam(required = false) String to,
          @RequestParam(required = false) String status,
          @RequestParam(required = false) String actor,
          @RequestParam(required = false) String reference,
          @RequestParam(required = false) Long tenantId,
          @RequestParam(defaultValue = "0") int page,
          @RequestParam(defaultValue = "50") int size) {
    return ResponseEntity.ok(
        ApiResponse.success(
            securityAuditService.listSuspiciousEvents(
                buildFilter(
                    from,
                    to,
                    "SECURITY",
                    "SECURITY_ALERT",
                    status,
                    actor,
                    null,
                    reference,
                    tenantId,
                    "SECURITY",
                    page,
                    size))));
  }

  @PostMapping("/suspicious-events/{eventId}/acknowledge")
  public ResponseEntity<ApiResponse<SuperAdminSecurityAuditDtos.RemediationResponse>> acknowledge(
      @PathVariable Long eventId,
      @Valid @RequestBody SuperAdminSecurityAuditDtos.RemediationRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Security event acknowledged", securityAuditService.acknowledge(eventId, request)));
  }

  @PostMapping("/suspicious-events/{eventId}/resolve")
  public ResponseEntity<ApiResponse<SuperAdminSecurityAuditDtos.RemediationResponse>> resolve(
      @PathVariable Long eventId,
      @Valid @RequestBody SuperAdminSecurityAuditDtos.RemediationRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Security event resolved", securityAuditService.resolve(eventId, request)));
  }

  @PostMapping("/suspicious-events/{eventId}/reopen")
  public ResponseEntity<ApiResponse<SuperAdminSecurityAuditDtos.RemediationResponse>> reopen(
      @PathVariable Long eventId,
      @Valid @RequestBody SuperAdminSecurityAuditDtos.RemediationRequest request) {
    return ResponseEntity.ok(
        ApiResponse.success(
            "Security event reopened", securityAuditService.reopen(eventId, request)));
  }

  @PutMapping({
    "/platform-events/{eventId}",
    "/security-events/{eventId}",
    "/suspicious-events/{eventId}"
  })
  @Hidden
  public ResponseEntity<ApiResponse<Void>> rejectAuditEventUpdate(@PathVariable Long eventId) {
    throw immutableAuditEventError(eventId);
  }

  @DeleteMapping({
    "/platform-events/{eventId}",
    "/security-events/{eventId}",
    "/suspicious-events/{eventId}"
  })
  @Hidden
  public ResponseEntity<ApiResponse<Void>> rejectAuditEventDelete(@PathVariable Long eventId) {
    throw immutableAuditEventError(eventId);
  }

  private ApplicationException immutableAuditEventError(Long eventId) {
    return new ApplicationException(
            ErrorCode.BUSINESS_INVALID_OPERATION,
            "Audit and security events are immutable; append remediation instead")
        .withDetail("eventId", eventId);
  }
}
