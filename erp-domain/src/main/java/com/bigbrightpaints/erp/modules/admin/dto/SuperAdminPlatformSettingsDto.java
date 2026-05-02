package com.bigbrightpaints.erp.modules.admin.dto;

import java.util.List;

public record SuperAdminPlatformSettingsDto(
    Access access, Mail mail, Workflow workflow, Security security) {

  public record Access(List<String> allowedOrigins, AuthCode authCode) {}

  public record AuthCode(boolean configured, String value, String updatePolicy) {}

  public record Mail(
      boolean enabled,
      String fromAddress,
      String baseUrl,
      boolean sendCredentials,
      boolean sendPasswordReset) {}

  public record Workflow(
      boolean autoApprovalEnabled, boolean periodLockEnforced, boolean exportApprovalRequired) {}

  public record Security(String secretDisplayPolicy, String auditPolicy) {}
}
