package com.bigbrightpaints.erp.core.auditaccess;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import java.util.Map;

import org.junit.jupiter.api.Test;

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
}
