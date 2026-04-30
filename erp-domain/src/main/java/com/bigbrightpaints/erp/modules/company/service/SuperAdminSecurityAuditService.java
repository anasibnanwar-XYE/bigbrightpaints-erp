package com.bigbrightpaints.erp.modules.company.service;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Set;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditLogRepository;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.auditaccess.AuditAccessService;
import com.bigbrightpaints.erp.core.auditaccess.AuditFeedFilter;
import com.bigbrightpaints.erp.core.auditaccess.dto.AuditFeedItemDto;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.observability.TelemetryPrivacySanitizer;
import com.bigbrightpaints.erp.core.security.SecurityActorResolver;
import com.bigbrightpaints.erp.modules.company.domain.SuperAdminSecurityRemediation;
import com.bigbrightpaints.erp.modules.company.domain.SuperAdminSecurityRemediationRepository;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminSecurityAuditDtos;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

@Service
public class SuperAdminSecurityAuditService {

  private static final Set<AuditEvent> REMEDIABLE_EVENTS =
      Set.of(AuditEvent.SECURITY_ALERT, AuditEvent.LOGIN_FAILURE, AuditEvent.ACCESS_DENIED);
  private static final Set<String> OPEN_REMEDIATION_STATUSES = Set.of("OPEN", "ACKNOWLEDGED");
  private static final Set<String> FORBIDDEN_REASON_MARKERS =
      Set.of(
          "password",
          "token",
          "secret",
          "bearer",
          "jwt",
          "invoice",
          "ledger",
          "inventory",
          "salary",
          "payroll",
          "vendor",
          "customer",
          "gst return",
          "request body",
          "response body");

  private final AuditAccessService auditAccessService;
  private final AuditLogRepository auditLogRepository;
  private final SuperAdminSecurityRemediationRepository remediationRepository;
  private final AuditService auditService;

  public SuperAdminSecurityAuditService(
      AuditAccessService auditAccessService,
      AuditLogRepository auditLogRepository,
      SuperAdminSecurityRemediationRepository remediationRepository,
      AuditService auditService) {
    this.auditAccessService = auditAccessService;
    this.auditLogRepository = auditLogRepository;
    this.remediationRepository = remediationRepository;
    this.auditService = auditService;
  }

  @Transactional(readOnly = true)
  public PageResponse<SuperAdminSecurityAuditDtos.SecurityEventResponse> listSecurityEvents(
      AuditFeedFilter filter) {
    return toSecurityEvents(auditAccessService.queryPlatformSecurityFeed(filter, false));
  }

  @Transactional(readOnly = true)
  public PageResponse<SuperAdminSecurityAuditDtos.SecurityEventResponse> listSuspiciousEvents(
      AuditFeedFilter filter) {
    return toSecurityEvents(auditAccessService.queryPlatformSecurityFeed(filter, true));
  }

  @Transactional
  public SuperAdminSecurityAuditDtos.RemediationResponse acknowledge(
      Long eventId, SuperAdminSecurityAuditDtos.RemediationRequest request) {
    return transition(eventId, "ACKNOWLEDGED", request);
  }

  @Transactional
  public SuperAdminSecurityAuditDtos.RemediationResponse resolve(
      Long eventId, SuperAdminSecurityAuditDtos.RemediationRequest request) {
    return transition(eventId, "RESOLVED", request);
  }

  @Transactional
  public SuperAdminSecurityAuditDtos.RemediationResponse reopen(
      Long eventId, SuperAdminSecurityAuditDtos.RemediationRequest request) {
    return transition(eventId, "OPEN", request);
  }

  public long countSecurityEvents() {
    return auditLogRepository.countByEventTypeIn(
        List.of(
            AuditEvent.LOGIN_SUCCESS,
            AuditEvent.LOGIN_FAILURE,
            AuditEvent.LOGOUT,
            AuditEvent.TOKEN_REFRESH,
            AuditEvent.TOKEN_REVOKED,
            AuditEvent.PASSWORD_CHANGED,
            AuditEvent.PASSWORD_RESET_REQUESTED,
            AuditEvent.PASSWORD_RESET_COMPLETED,
            AuditEvent.ACCESS_DENIED,
            AuditEvent.SECURITY_ALERT));
  }

  public long countSuspiciousEvents() {
    return auditLogRepository.countByEventType(AuditEvent.SECURITY_ALERT);
  }

  public long countOpenRemediations() {
    return remediationRepository.countByStatusIn(OPEN_REMEDIATION_STATUSES);
  }

  public long countResolvedRemediations() {
    return remediationRepository.countByStatusIn(Set.of("RESOLVED"));
  }

  private PageResponse<SuperAdminSecurityAuditDtos.SecurityEventResponse> toSecurityEvents(
      PageResponse<AuditFeedItemDto> page) {
    List<Long> ids = page.content().stream().map(AuditFeedItemDto::sourceId).toList();
    Map<Long, SuperAdminSecurityRemediation> remediations =
        ids.isEmpty()
            ? Map.of()
            : remediationRepository.findByAuditEventIdIn(ids).stream()
                .collect(
                    Collectors.toMap(
                        SuperAdminSecurityRemediation::getAuditEventId,
                        Function.identity(),
                        (left, right) -> left,
                        LinkedHashMap::new));
    return PageResponse.of(
        page.content().stream()
            .map(item -> toSecurityEvent(item, remediations.get(item.sourceId())))
            .toList(),
        page.totalElements(),
        page.page(),
        page.size());
  }

  private SuperAdminSecurityAuditDtos.SecurityEventResponse toSecurityEvent(
      AuditFeedItemDto item, SuperAdminSecurityRemediation remediation) {
    return new SuperAdminSecurityAuditDtos.SecurityEventResponse(
        item.sourceId(),
        item.action(),
        item.category(),
        severity(item),
        item.occurredAt(),
        item.companyId(),
        item.companyCode(),
        safeActor(item.actorIdentifier()),
        item.requestMethod(),
        item.requestPath(),
        item.traceId(),
        item.metadata(),
        toResponse(remediation));
  }

  private SuperAdminSecurityAuditDtos.RemediationResponse transition(
      Long eventId, String nextStatus, SuperAdminSecurityAuditDtos.RemediationRequest request) {
    AuditLog event =
        auditLogRepository
            .findById(eventId)
            .orElseThrow(
                () ->
                    new ApplicationException(
                        ErrorCode.BUSINESS_ENTITY_NOT_FOUND, "Security event not found"));
    if (!REMEDIABLE_EVENTS.contains(event.getEventType())) {
      throw new ApplicationException(
          ErrorCode.BUSINESS_INVALID_STATE, "Audit event is not remediable");
    }
    String reason = validateReason(request == null ? null : request.reason());
    SuperAdminSecurityRemediation remediation =
        remediationRepository
            .lockByAuditEventId(eventId)
            .orElseGet(
                () -> {
                  SuperAdminSecurityRemediation created = new SuperAdminSecurityRemediation();
                  created.setAuditEventId(eventId);
                  created.setStatus("OPEN");
                  created.setSeverity(severity(event));
                  return remediationRepository.saveAndFlush(created);
                });
    String previousStatus = remediation.getStatus();
    remediation.setStatus(nextStatus);
    remediation.setSeverity(severity(event));
    remediation.setReason(reason);
    remediation.setUpdatedBy(currentActor());
    Long auditEventId =
        auditRequired(
            "security-event-remediation-" + nextStatus.toLowerCase(Locale.ROOT),
            Map.of(
                "resourceType",
                "SECURITY_EVENT",
                "resourceId",
                String.valueOf(eventId),
                "remediationId",
                String.valueOf(remediation.getId()),
                "previousStatus",
                previousStatus,
                "newStatus",
                nextStatus,
                "reasonText",
                reason));
    remediation.setLastAuditEventId(auditEventId);
    return toResponse(remediationRepository.saveAndFlush(remediation));
  }

  private Long auditRequired(String reason, Map<String, String> metadata) {
    Map<String, String> auditMetadata = new LinkedHashMap<>();
    auditMetadata.put("operation", reason);
    auditMetadata.putAll(metadata);
    AuditLog auditLog =
        auditService.logAuthSuccessRequired(
            AuditEvent.CONFIGURATION_CHANGED, currentActor(), null, auditMetadata);
    if (auditLog == null || auditLog.getId() == null) {
      throw new ApplicationException(
          ErrorCode.BUSINESS_INVALID_STATE, "Security remediation audit event was not persisted");
    }
    return auditLog.getId();
  }

  private SuperAdminSecurityAuditDtos.RemediationResponse toResponse(
      SuperAdminSecurityRemediation remediation) {
    if (remediation == null) {
      return null;
    }
    return new SuperAdminSecurityAuditDtos.RemediationResponse(
        remediation.getId(),
        remediation.getAuditEventId(),
        remediation.getStatus(),
        remediation.getSeverity(),
        remediation.getReason(),
        remediation.getUpdatedBy(),
        remediation.getCreatedAt(),
        remediation.getUpdatedAt(),
        remediation.getLastAuditEventId());
  }

  private String severity(AuditFeedItemDto item) {
    if (item == null || item.metadata() == null) {
      return "MEDIUM";
    }
    return severity(item.action(), item.metadata().get("alertType"));
  }

  private String severity(AuditLog event) {
    return severity(event.getEventType() == null ? null : event.getEventType().name(), null);
  }

  private String severity(String action, String alertType) {
    String normalizedAction = action == null ? "" : action.toUpperCase(Locale.ROOT);
    String normalizedAlert = alertType == null ? "" : alertType.toUpperCase(Locale.ROOT);
    if (normalizedAlert.contains("BRUTE_FORCE")
        || normalizedAlert.contains("SUSPICIOUS")
        || "SECURITY_ALERT".equals(normalizedAction)) {
      return "HIGH";
    }
    if ("LOGIN_FAILURE".equals(normalizedAction) || "ACCESS_DENIED".equals(normalizedAction)) {
      return "MEDIUM";
    }
    return "LOW";
  }

  private String safeActor(String actorIdentifier) {
    if (!StringUtils.hasText(actorIdentifier)) {
      return null;
    }
    String normalized = actorIdentifier.trim().toLowerCase(Locale.ROOT);
    return "hash:" + TelemetryPrivacySanitizer.pseudonymousHash("actor", normalized);
  }

  private String validateReason(String rawReason) {
    if (!StringUtils.hasText(rawReason)) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, "reason is required");
    }
    String reason = rawReason.trim();
    if (reason.length() > 300) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_OUT_OF_RANGE, "reason must be at most 300 characters");
    }
    String lower = reason.toLowerCase(Locale.ROOT);
    if (FORBIDDEN_REASON_MARKERS.stream().anyMatch(lower::contains)) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "reason must not contain secrets or private tenant business data");
    }
    return reason;
  }

  private String currentActor() {
    String actor = SecurityActorResolver.resolveActorOrUnknown();
    if (!StringUtils.hasText(actor)
        || SecurityActorResolver.UNKNOWN_AUTH_ACTOR.equals(actor)
        || SecurityActorResolver.SYSTEM_PROCESS_ACTOR.equals(actor)) {
      throw new ApplicationException(
          ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS,
          "Authenticated Super Admin actor is required for security remediation");
    }
    return actor;
  }
}
