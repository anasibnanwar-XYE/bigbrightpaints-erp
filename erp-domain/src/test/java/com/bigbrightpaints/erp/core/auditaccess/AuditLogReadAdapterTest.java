package com.bigbrightpaints.erp.core.auditaccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.Map;

import org.junit.jupiter.api.Test;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;

import com.bigbrightpaints.erp.core.audit.AuditLog;
import com.bigbrightpaints.erp.core.audit.AuditLogRepository;

class AuditLogReadAdapterTest {

  @Test
  void entityFieldResolvers_preferMetadataResourceKeys() {
    AuditLogReadAdapter adapter =
        new AuditLogReadAdapter(
            mock(AuditLogRepository.class), new AuditEventClassifier(), mock(AuditVisibilityPolicy.class));
    AuditLog auditLog = new AuditLog();
    auditLog.setMetadata(
        Map.of(
            "resourceType", "JOURNAL_ENTRY",
            "resourceId", "17",
            "referenceNumber", "OB-2026-0001"));

    assertThat(adapter.entityTypeFor(auditLog)).isEqualTo("JOURNAL_ENTRY");
    assertThat(adapter.entityIdFor(auditLog)).isEqualTo("17");
    assertThat(adapter.referenceNumberFor(auditLog)).isEqualTo("OB-2026-0001");
  }

  @Test
  void referenceNumberFallsBackToEntityIdWhenExplicitReferenceIsMissing() {
    AuditLogReadAdapter adapter =
        new AuditLogReadAdapter(
            mock(AuditLogRepository.class), new AuditEventClassifier(), mock(AuditVisibilityPolicy.class));
    AuditLog auditLog = new AuditLog();
    auditLog.setMetadata(Map.of("resourceId", "17"));

    assertThat(adapter.entityIdFor(auditLog)).isEqualTo("17");
    assertThat(adapter.referenceNumberFor(auditLog)).isEqualTo("17");
  }

  @Test
  void queryPlatformFeed_resolvesMissingCompanyCodesInOneBatchPerPage() {
    AuditLogRepository auditLogRepository = mock(AuditLogRepository.class);
    AuditVisibilityPolicy auditVisibilityPolicy = mock(AuditVisibilityPolicy.class);
    AuditLogReadAdapter adapter =
        new AuditLogReadAdapter(auditLogRepository, new AuditEventClassifier(), auditVisibilityPolicy);
    Specification<com.bigbrightpaints.erp.core.audit.AuditLog> allowAll =
        (root, query, cb) -> cb.conjunction();
    when(auditVisibilityPolicy.platformVisibility()).thenReturn(allowAll);
    when(auditVisibilityPolicy.resolveCompanyCodes(java.util.Set.of(7L)))
        .thenReturn(Map.of(7L, "TENANT-A"));

    AuditLog fallbackCompanyLog = new AuditLog();
    fallbackCompanyLog.setCompanyId(7L);
    fallbackCompanyLog.setRequestPath("/api/v1/companies/7");
    fallbackCompanyLog.setMetadata(Map.of());

    AuditLog metadataCompanyLog = new AuditLog();
    metadataCompanyLog.setCompanyId(9L);
    metadataCompanyLog.setRequestPath("/api/v1/companies/9");
    metadataCompanyLog.setMetadata(Map.of("targetCompanyCode", "TENANT-B"));

    when(
            auditLogRepository.findAll(
                org.mockito.ArgumentMatchers.<Specification<AuditLog>>any(), any(Pageable.class)))
        .thenReturn(new PageImpl<>(java.util.List.of(fallbackCompanyLog, metadataCompanyLog)));

    AuditFeedSlice slice =
        adapter.queryPlatformFeed(
            new AuditFeedFilter(null, null, null, null, null, null, null, null, 0, 50));

    assertThat(slice.items()).extracting(item -> item.companyCode()).containsExactly("TENANT-A", "TENANT-B");
    verify(auditVisibilityPolicy).resolveCompanyCodes(java.util.Set.of(7L));
  }
}
