package com.bigbrightpaints.erp.modules.admin.service;

import java.time.Instant;
import java.util.Map;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.security.SecurityActorResolver;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.admin.domain.ChangelogEntry;
import com.bigbrightpaints.erp.modules.admin.domain.ChangelogEntryRepository;
import com.bigbrightpaints.erp.modules.admin.dto.ChangelogEntryRequest;
import com.bigbrightpaints.erp.modules.admin.dto.ChangelogEntryResponse;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

@Service
public class ChangelogService {

  private static final int MAX_PAGE_SIZE = 100;

  private final ChangelogEntryRepository changelogEntryRepository;
  private final AuditService auditService;

  public ChangelogService(
      ChangelogEntryRepository changelogEntryRepository, AuditService auditService) {
    this.changelogEntryRepository = changelogEntryRepository;
    this.auditService = auditService;
  }

  @Transactional
  public ChangelogEntryResponse create(ChangelogEntryRequest request) {
    ChangelogEntry entry = new ChangelogEntry();
    applyRequest(entry, request);
    entry.setCreatedBy(SecurityActorResolver.resolveActorWithSystemProcessFallback());
    if (entry.getPublishedAt() == null) {
      entry.setPublishedAt(CompanyTime.now());
    }
    ChangelogEntry saved = changelogEntryRepository.save(entry);

    auditService.logAuthSuccess(
        AuditEvent.DATA_CREATE,
        SecurityActorResolver.resolveActorWithSystemProcessFallback(),
        null,
        Map.of(
            "action", "changelog_create",
            "changelogEntryId", saved.getId().toString(),
            "version", saved.getVersionLabel()));
    return toResponse(saved);
  }

  @Transactional
  public ChangelogEntryResponse update(Long id, ChangelogEntryRequest request) {
    ChangelogEntry entry = requireEntry(id);
    applyRequest(entry, request);
    entry.setUpdatedAt(CompanyTime.now());

    auditService.logAuthSuccess(
        AuditEvent.DATA_UPDATE,
        SecurityActorResolver.resolveActorWithSystemProcessFallback(),
        null,
        Map.of(
            "action", "changelog_update",
            "changelogEntryId", entry.getId().toString(),
            "version", entry.getVersionLabel()));
    return toResponse(entry);
  }

  @Transactional
  public void softDelete(Long id) {
    ChangelogEntry entry = requireEntry(id);
    Instant now = CompanyTime.now();
    entry.setDeleted(true);
    entry.setDeletedAt(now);
    entry.setUpdatedAt(now);

    auditService.logAuthSuccess(
        AuditEvent.DATA_DELETE,
        SecurityActorResolver.resolveActorWithSystemProcessFallback(),
        null,
        Map.of(
            "action", "changelog_soft_delete",
            "changelogEntryId", entry.getId().toString(),
            "version", entry.getVersionLabel()));
  }

  @Transactional(readOnly = true)
  public PageResponse<ChangelogEntryResponse> list(int page, int size) {
    int safePage = Math.max(page, 0);
    int safeSize = Math.max(1, Math.min(size, MAX_PAGE_SIZE));

    PageRequest pageable =
        PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "publishedAt", "id"));

    Page<ChangelogEntry> result =
        changelogEntryRepository.findByDeletedFalseOrderByPublishedAtDescIdDesc(pageable);
    return PageResponse.of(
        result.map(this::toResponse).getContent(), result.getTotalElements(), safePage, safeSize);
  }

  @Transactional(readOnly = true)
  public ChangelogEntryResponse latestHighlighted() {
    return changelogEntryRepository
        .findFirstByHighlightedTrueAndDeletedFalseOrderByPublishedAtDescIdDesc()
        .map(this::toResponse)
        .orElseThrow(
            () ->
                new ApplicationException(
                    ErrorCode.BUSINESS_ENTITY_NOT_FOUND, "No highlighted changelog entry found"));
  }

  private ChangelogEntry requireEntry(Long id) {
    return changelogEntryRepository
        .findByIdAndDeletedFalse(id)
        .orElseThrow(
            () ->
                new ApplicationException(
                    ErrorCode.BUSINESS_ENTITY_NOT_FOUND, "Changelog entry not found: " + id));
  }

  private void applyRequest(ChangelogEntry entry, ChangelogEntryRequest request) {
    String version = ValidationUtils.requireNotBlank(request.version(), "version");
    String title = ValidationUtils.requireNotBlank(request.title(), "title");
    String body = ValidationUtils.requireNotBlank(request.body(), "body");

    entry.setVersionLabel(version);
    entry.setTitle(title);
    entry.setBody(body);
    entry.setHighlighted(Boolean.TRUE.equals(request.isHighlighted()));
    if (StringUtils.hasText(entry.getCreatedBy())) {
      entry.setPublishedAt(CompanyTime.now());
    }
  }

  private ChangelogEntryResponse toResponse(ChangelogEntry entry) {
    return new ChangelogEntryResponse(
        entry.getId(),
        entry.getVersionLabel(),
        entry.getTitle(),
        entry.getBody(),
        entry.getPublishedAt(),
        entry.getCreatedBy(),
        entry.isHighlighted());
  }
}
