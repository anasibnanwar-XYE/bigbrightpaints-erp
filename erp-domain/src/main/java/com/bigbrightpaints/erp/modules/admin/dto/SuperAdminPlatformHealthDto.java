package com.bigbrightpaints.erp.modules.admin.dto;

import java.time.Instant;
import java.util.List;
import java.util.Map;

public record SuperAdminPlatformHealthDto(
    Instant checkedAt,
    String overallStatus,
    String traceId,
    Component appReadiness,
    Component database,
    Component rabbitMq,
    Component queue,
    Component email,
    Component sentry,
    Component datadog,
    Component backup,
    Component failedJobs,
    List<Component> components,
    RedactionPolicy redactionPolicy) {

  public record Component(
      String name,
      String status,
      String reasonCode,
      Instant checkedAt,
      String traceId,
      Map<String, Object> metrics) {}

  public record RedactionPolicy(
      boolean degradedDetailsRedacted, List<String> exposedFields, List<String> hiddenFields) {}
}
