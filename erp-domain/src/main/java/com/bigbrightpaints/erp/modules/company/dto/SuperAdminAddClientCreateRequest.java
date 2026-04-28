package com.bigbrightpaints.erp.modules.company.dto;

import java.math.BigDecimal;
import java.util.Set;

import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;

public record SuperAdminAddClientCreateRequest(
    @Valid @NotNull Company company,
    @Valid @NotNull Owner owner,
    @Valid @NotNull Commercial commercial,
    @Valid @NotNull Quotas quotas,
    @Valid @NotNull Modules modules,
    @Valid @NotNull Support support,
    @NotNull CreateMode createMode) {

  public enum CreateMode {
    DRAFT,
    SEND_ACTIVATION
  }

  public record Company(
      @NotBlank @Size(max = 160) String name,
      @NotBlank
          @Size(min = 2, max = 32)
          @Pattern(regexp = "^[A-Z0-9][A-Z0-9_-]*$", message = "code must be uppercase code-safe")
          String code,
      @NotBlank @Size(max = 64) String timezone,
      @Size(min = 2, max = 2) String stateCode,
      @NotBlank @Size(min = 3, max = 3) String baseCurrency,
      @DecimalMin(value = "0.0", inclusive = true) BigDecimal defaultGstRate,
      @Size(max = 64) String coaTemplateCode) {}

  public record Owner(
      @Email @NotBlank @Size(max = 255) String email,
      @NotBlank @Size(max = 160) String displayName,
      @Size(max = 32) String phone) {}

  public record Commercial(
      @NotBlank @Size(max = 64) String planId,
      @NotBlank @Size(max = 32) String billingStatus,
      @Min(0) Integer trialDays,
      @NotBlank @Size(max = 32) String supportTier) {}

  public record Quotas(
      @Min(0) Long maxActiveUsers,
      @Min(0) Long maxApiRequests,
      @Min(0) Long maxStorageBytes,
      @Min(0) Long maxConcurrentRequests,
      Boolean softLimitEnabled,
      Boolean hardLimitEnabled) {}

  public record Modules(@NotNull Set<@NotBlank @Size(max = 64) String> enabled) {}

  public record Support(
      @Size(max = 4000) String notes, Set<@NotBlank @Size(max = 64) String> tags) {}
}
