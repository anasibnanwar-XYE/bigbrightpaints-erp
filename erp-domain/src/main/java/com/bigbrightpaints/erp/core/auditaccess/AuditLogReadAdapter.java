package com.bigbrightpaints.erp.core.auditaccess;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.EnumSet;
import java.util.Locale;
import java.util.Map;
import java.util.Set;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditLogRepository;
import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.auditaccess.dto.AuditFeedItemDto;
import com.bigbrightpaints.erp.modules.company.domain.Company;

import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.MapJoin;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;

@Component
public class AuditLogReadAdapter {

  private final AuditLogRepository auditLogRepository;
  private final AuditEventClassifier auditEventClassifier;
  private final AuditVisibilityPolicy auditVisibilityPolicy;

  public AuditLogReadAdapter(
      AuditLogRepository auditLogRepository,
      AuditEventClassifier auditEventClassifier,
      AuditVisibilityPolicy auditVisibilityPolicy) {
    this.auditLogRepository = auditLogRepository;
    this.auditEventClassifier = auditEventClassifier;
    this.auditVisibilityPolicy = auditVisibilityPolicy;
  }

  @Transactional(readOnly = true)
  public AuditFeedSlice queryTenantCompanyFeed(Company company, AuditFeedFilter filter) {
    int fetchLimit = filter.fetchLimit();
    Specification<AuditLog> spec =
        Specification.where(auditVisibilityPolicy.tenantCompanyVisibility(company.getId()))
            .and(byOccurredRange(filter.from(), filter.to()))
            .and(byModule(filter.normalizedModule()))
            .and(byActor(filter.normalizedActor()))
            .and(byStatus(filter.normalizedStatus()))
            .and(byAction(filter.normalizedAction()))
            .and(byEntityType(filter.normalizedEntityType()))
            .and(byReference(filter.normalizedReference()));
    Page<AuditLog> page = auditLogRepository.findAll(spec, firstPage(fetchLimit));
    return new AuditFeedSlice(
        page.getContent().stream().map(log -> toDto(log, company.getCode())).toList(),
        page.getTotalElements());
  }

  @Transactional(readOnly = true)
  public AuditFeedSlice queryPlatformFeed(AuditFeedFilter filter) {
    int safePage = filter.safePage();
    int safeSize = filter.safeSize();
    Specification<AuditLog> spec =
        Specification.where(auditVisibilityPolicy.platformVisibility())
            .and(byOccurredRange(filter.from(), filter.to()))
            .and(byModule(filter.normalizedModule()))
            .and(byActor(filter.normalizedActor()))
            .and(byStatus(filter.normalizedStatus()))
            .and(byAction(filter.normalizedAction()))
            .and(byEntityType(filter.normalizedEntityType()))
            .and(byReference(filter.normalizedReference()));
    Page<AuditLog> page = auditLogRepository.findAll(spec, PageRequest.of(safePage, safeSize, sort()));
    return new AuditFeedSlice(page.getContent().stream().map(log -> toDto(log, null)).toList(), page.getTotalElements());
  }

  private AuditFeedItemDto toDto(AuditLog log, String currentCompanyCode) {
    Map<String, String> metadata = metadata(log);
    String companyCode =
        currentCompanyCode != null
            ? currentCompanyCode
            : firstNonBlank(
                metadata.get("targetCompanyCode"),
                auditVisibilityPolicy.resolveCompanyCode(log.getCompanyId()));
    String entityType = entityTypeFor(log);
    String entityId = entityIdFor(log);
    return new AuditFeedItemDto(
        log.getId(),
        "AUDIT_LOG",
        auditEventClassifier.categoryFor(log),
        log.getTimestamp() != null ? log.getTimestamp().atZone(java.time.ZoneOffset.UTC).toInstant() : null,
        log.getCompanyId(),
        companyCode,
        auditEventClassifier.moduleFor(log),
        log.getEventType() != null ? log.getEventType().name() : null,
        log.getStatus() != null ? log.getStatus().name() : null,
        parseLong(firstNonBlank(metadata.get("actorUserId"), log.getUserId())),
        firstNonBlank(log.getUsername(), log.getUserId()),
        auditEventClassifier.subjectUserId(metadata),
        auditEventClassifier.subjectIdentifier(metadata),
        entityType,
        entityId,
        referenceNumberFor(log),
        log.getRequestMethod(),
        log.getRequestPath(),
        log.getTraceId(),
        metadata);
  }

  String entityTypeFor(AuditLog log) {
    Map<String, String> metadata = metadata(log);
    return firstNonBlank(log.getResourceType(), metadata.get("resourceType"), metadata.get("entityType"));
  }

  String entityIdFor(AuditLog log) {
    Map<String, String> metadata = metadata(log);
    return firstNonBlank(log.getResourceId(), metadata.get("resourceId"), metadata.get("entityId"));
  }

  String referenceNumberFor(AuditLog log) {
    return firstNonBlank(metadata(log).get("referenceNumber"), entityIdFor(log));
  }

  private PageRequest firstPage(int fetchLimit) {
    return PageRequest.of(0, fetchLimit, sort());
  }

  private Sort sort() {
    return Sort.by(Sort.Direction.DESC, "timestamp", "id");
  }

  private Specification<AuditLog> byOccurredRange(LocalDate from, LocalDate to) {
    return (root, query, cb) -> {
      if (from == null && to == null) {
        return cb.conjunction();
      }
      LocalDateTime start = from != null ? from.atStartOfDay() : null;
      LocalDateTime end = to != null ? to.plusDays(1).atStartOfDay() : null;
      if (start != null && end != null) {
        return cb.and(
            cb.greaterThanOrEqualTo(root.get("timestamp"), start),
            cb.lessThan(root.get("timestamp"), end));
      }
      if (start != null) {
        return cb.greaterThanOrEqualTo(root.get("timestamp"), start);
      }
      return cb.lessThan(root.get("timestamp"), end);
    };
  }

  private Specification<AuditLog> byModule(String module) {
    if (!StringUtils.hasText(module)) {
      return null;
    }
    String normalizedModule = module.trim().toUpperCase(Locale.ROOT);
    return (root, query, cb) -> {
      Predicate knownPath = knownModulePathPredicate(root.get("requestPath"), cb);
      Predicate pathMatch = modulePathPredicate(root.get("requestPath"), cb, normalizedModule);
      Predicate resourceTypePresent =
          cb.or(
              hasText(root.get("resourceType"), cb),
              metadataValueHasText(root, query, cb, "resourceType"));
      Predicate resourceTypeMatch =
          cb.or(
              equalsIgnoreCase(root.get("resourceType"), normalizedModule, cb),
              metadataValueEquals(root, query, cb, "resourceType", normalizedModule));
      Predicate categoryMatch = categoryPredicate(root.get("eventType"), cb, normalizedModule);

      ArrayList<Predicate> matches = new ArrayList<>();
      if (pathMatch != null) {
        matches.add(pathMatch);
      }
      matches.add(cb.and(cb.not(knownPath), resourceTypeMatch));
      if (categoryMatch != null) {
        matches.add(cb.and(cb.not(knownPath), cb.not(resourceTypePresent), categoryMatch));
      }
      return cb.or(matches.toArray(Predicate[]::new));
    };
  }

  private Specification<AuditLog> byActor(String actor) {
    if (!StringUtils.hasText(actor)) {
      return null;
    }
    String normalizedActor = actor.trim().toLowerCase(java.util.Locale.ROOT);
    return (root, query, cb) ->
        cb.or(
            cb.equal(cb.lower(root.get("username").as(String.class)), normalizedActor),
            cb.equal(cb.lower(root.get("userId").as(String.class)), normalizedActor));
  }

  private Specification<AuditLog> byStatus(String status) {
    if (!StringUtils.hasText(status)) {
      return null;
    }
    String normalizedStatus = status.trim().toUpperCase(java.util.Locale.ROOT);
    return (root, query, cb) -> cb.equal(root.get("status").as(String.class), normalizedStatus);
  }

  private Specification<AuditLog> byAction(String action) {
    if (!StringUtils.hasText(action)) {
      return null;
    }
    String normalizedAction = action.trim().toUpperCase(java.util.Locale.ROOT);
    return (root, query, cb) -> cb.equal(root.get("eventType").as(String.class), normalizedAction);
  }

  private Specification<AuditLog> byEntityType(String entityType) {
    if (!StringUtils.hasText(entityType)) {
      return null;
    }
    String normalizedEntityType = entityType.trim().toLowerCase(java.util.Locale.ROOT);
    return (root, query, cb) ->
        cb.or(
            equalsIgnoreCase(root.get("resourceType"), normalizedEntityType, cb),
            metadataValueEquals(root, query, cb, "resourceType", normalizedEntityType),
            metadataValueEquals(root, query, cb, "entityType", normalizedEntityType));
  }

  private Specification<AuditLog> byReference(String reference) {
    if (!StringUtils.hasText(reference)) {
      return null;
    }
    String normalizedReference = reference.trim().toLowerCase(java.util.Locale.ROOT);
    return (root, query, cb) ->
        cb.or(
            equalsIgnoreCase(root.get("resourceId"), normalizedReference, cb),
            equalsIgnoreCase(root.get("traceId"), normalizedReference, cb),
            metadataValueEquals(root, query, cb, "resourceId", normalizedReference),
            metadataValueEquals(root, query, cb, "entityId", normalizedReference),
            metadataValueEquals(root, query, cb, "referenceNumber", normalizedReference));
  }

  private Predicate equalsIgnoreCase(Path<String> path, String value, CriteriaBuilder cb) {
    return cb.equal(cb.lower(path.as(String.class)), value.toLowerCase(Locale.ROOT));
  }

  private Predicate hasText(Path<String> path, CriteriaBuilder cb) {
    return cb.and(cb.isNotNull(path), cb.notEqual(cb.trim(path), ""));
  }

  private Predicate modulePathPredicate(
      Path<String> path, CriteriaBuilder cb, String normalizedModule) {
    return switch (normalizedModule) {
      case "SUPERADMIN" -> pathEqualsOrStartsWith(path, cb, "/api/v1/superadmin");
      case "ADMIN" -> pathEqualsOrStartsWith(path, cb, "/api/v1/admin");
      case "ACCOUNTING" -> pathEqualsOrStartsWith(path, cb, "/api/v1/accounting");
      case "AUTH" -> pathEqualsOrStartsWith(path, cb, "/api/v1/auth");
      case "CHANGELOG" -> pathEqualsOrStartsWith(path, cb, "/api/v1/changelog");
      case "COMPANIES" -> pathEqualsOrStartsWith(path, cb, "/api/v1/companies");
      default -> null;
    };
  }

  private Predicate knownModulePathPredicate(Path<String> path, CriteriaBuilder cb) {
    return cb.or(
        pathEqualsOrStartsWith(path, cb, "/api/v1/superadmin"),
        pathEqualsOrStartsWith(path, cb, "/api/v1/admin"),
        pathEqualsOrStartsWith(path, cb, "/api/v1/accounting"),
        pathEqualsOrStartsWith(path, cb, "/api/v1/auth"),
        pathEqualsOrStartsWith(path, cb, "/api/v1/changelog"),
        pathEqualsOrStartsWith(path, cb, "/api/v1/companies"));
  }

  private Predicate categoryPredicate(
      Path<AuditEvent> eventTypePath, CriteriaBuilder cb, String normalizedModule) {
    Set<AuditEvent> categoryEvents = categoryEvents(normalizedModule);
    if (categoryEvents == null || categoryEvents.isEmpty()) {
      return null;
    }
    return eventTypePath.in(categoryEvents);
  }

  private Set<AuditEvent> categoryEvents(String normalizedModule) {
    return switch (normalizedModule) {
      case "AUTH" ->
          EnumSet.of(
              AuditEvent.LOGIN_SUCCESS,
              AuditEvent.LOGIN_FAILURE,
              AuditEvent.LOGOUT,
              AuditEvent.TOKEN_REFRESH,
              AuditEvent.TOKEN_REVOKED,
              AuditEvent.PASSWORD_CHANGED,
              AuditEvent.PASSWORD_RESET_REQUESTED,
              AuditEvent.PASSWORD_RESET_COMPLETED,
              AuditEvent.MFA_ENROLLED,
              AuditEvent.MFA_ACTIVATED,
              AuditEvent.MFA_DISABLED,
              AuditEvent.MFA_SUCCESS,
              AuditEvent.MFA_FAILURE,
              AuditEvent.MFA_RECOVERY_CODE_USED);
      case "SECURITY" ->
          EnumSet.of(
              AuditEvent.ACCESS_GRANTED, AuditEvent.ACCESS_DENIED, AuditEvent.SECURITY_ALERT);
      case "ADMIN" ->
          EnumSet.of(
              AuditEvent.USER_CREATED,
              AuditEvent.USER_UPDATED,
              AuditEvent.USER_DELETED,
              AuditEvent.USER_ACTIVATED,
              AuditEvent.USER_DEACTIVATED,
              AuditEvent.USER_LOCKED,
              AuditEvent.USER_UNLOCKED,
              AuditEvent.PERMISSION_CHANGED,
              AuditEvent.ROLE_ASSIGNED,
              AuditEvent.ROLE_REMOVED,
              AuditEvent.CONFIGURATION_CHANGED);
      case "DATA" ->
          EnumSet.of(
              AuditEvent.DATA_CREATE,
              AuditEvent.DATA_READ,
              AuditEvent.DATA_UPDATE,
              AuditEvent.DATA_DELETE,
              AuditEvent.DATA_EXPORT,
              AuditEvent.SENSITIVE_DATA_ACCESSED);
      case "COMPLIANCE" ->
          EnumSet.of(
              AuditEvent.AUDIT_LOG_ACCESSED,
              AuditEvent.AUDIT_LOG_EXPORTED,
              AuditEvent.COMPLIANCE_CHECK,
              AuditEvent.DATA_RETENTION_ACTION);
      case "SYSTEM" ->
          EnumSet.of(
              AuditEvent.SYSTEM_STARTUP,
              AuditEvent.SYSTEM_SHUTDOWN,
              AuditEvent.INTEGRATION_SUCCESS,
              AuditEvent.INTEGRATION_FAILURE);
      case "BUSINESS" ->
          EnumSet.of(
              AuditEvent.REFERENCE_GENERATED,
              AuditEvent.ORDER_NUMBER_GENERATED,
              AuditEvent.JOURNAL_ENTRY_POSTED,
              AuditEvent.JOURNAL_ENTRY_REVERSED,
              AuditEvent.DISPATCH_CONFIRMED,
              AuditEvent.SETTLEMENT_RECORDED,
              AuditEvent.PAYROLL_POSTED,
              AuditEvent.TRANSACTION_CREATED,
              AuditEvent.TRANSACTION_APPROVED,
              AuditEvent.TRANSACTION_REJECTED,
              AuditEvent.PAYMENT_PROCESSED,
              AuditEvent.REFUND_ISSUED);
      default -> null;
    };
  }

  private Predicate metadataValueEquals(
      Root<AuditLog> root, CriteriaQuery<?> query, CriteriaBuilder cb, String key, String value) {
    var subquery = query.subquery(Integer.class);
    Root<AuditLog> correlatedRoot = subquery.correlate(root);
    MapJoin<AuditLog, String, String> metadata = correlatedRoot.joinMap("metadata");
    subquery
        .select(cb.literal(1))
        .where(
            cb.equal(metadata.key(), key),
            cb.equal(cb.lower(metadata.value()), value.toLowerCase(Locale.ROOT)));
    return cb.exists(subquery);
  }

  private Predicate metadataValueHasText(
      Root<AuditLog> root, CriteriaQuery<?> query, CriteriaBuilder cb, String key) {
    var subquery = query.subquery(Integer.class);
    Root<AuditLog> correlatedRoot = subquery.correlate(root);
    MapJoin<AuditLog, String, String> metadata = correlatedRoot.joinMap("metadata");
    subquery
        .select(cb.literal(1))
        .where(
            cb.equal(metadata.key(), key),
            cb.isNotNull(metadata.value()),
            cb.notEqual(cb.trim(metadata.value()), ""));
    return cb.exists(subquery);
  }

  private Predicate pathEqualsOrStartsWith(Path<String> path, CriteriaBuilder cb, String prefix) {
    return cb.or(cb.equal(path, prefix), cb.like(path, prefix + "/%"));
  }

  private Map<String, String> metadata(AuditLog log) {
    return log.getMetadata() == null ? Map.of() : Map.copyOf(log.getMetadata());
  }

  private String firstNonBlank(String... values) {
    for (String value : values) {
      if (StringUtils.hasText(value)) {
        return value.trim();
      }
    }
    return null;
  }

  private Long parseLong(String value) {
    if (!StringUtils.hasText(value)) {
      return null;
    }
    try {
      return Long.valueOf(value.trim());
    } catch (NumberFormatException ex) {
      return null;
    }
  }
}
