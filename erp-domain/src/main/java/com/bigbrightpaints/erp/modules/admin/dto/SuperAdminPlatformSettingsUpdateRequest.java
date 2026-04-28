package com.bigbrightpaints.erp.modules.admin.dto;

import java.util.List;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SuperAdminPlatformSettingsUpdateRequest(
    @Valid AccessUpdate access, @Valid MailUpdate mail, @Valid WorkflowUpdate workflow) {

  public record AccessUpdate(
      List<
              @Size(max = 255, message = "allowedOrigins entries must be at most 255 characters")
              String>
          allowedOrigins,
      @Size(min = 3, max = 32, message = "platformAuthCode must be 3 to 32 characters")
          @Pattern(
              regexp = "^[A-Z0-9][A-Z0-9_-]{2,31}$",
              message =
                  "platformAuthCode must use uppercase letters, digits, underscore, or hyphen")
          String platformAuthCode) {}

  public record MailUpdate(
      Boolean enabled,
      @Email(message = "fromAddress must be a valid email") String fromAddress,
      @Size(max = 500, message = "baseUrl must be at most 500 characters") String baseUrl,
      Boolean sendCredentials,
      Boolean sendPasswordReset) {}

  public record WorkflowUpdate(
      Boolean autoApprovalEnabled, Boolean periodLockEnforced, Boolean exportApprovalRequired) {}
}
