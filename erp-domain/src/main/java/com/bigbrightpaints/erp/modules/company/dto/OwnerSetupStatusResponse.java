package com.bigbrightpaints.erp.modules.company.dto;

import java.time.Instant;
import java.util.List;

public record OwnerSetupStatusResponse(
    Long tenantId,
    String companyCode,
    String tenantStatus,
    boolean setupRequired,
    List<Step> steps,
    String nextStep,
    List<String> roleOptions,
    Instant completedAt,
    Long auditEventId) {

  public record Step(
      String key, String label, boolean required, boolean completed, Instant completedAt) {}
}
