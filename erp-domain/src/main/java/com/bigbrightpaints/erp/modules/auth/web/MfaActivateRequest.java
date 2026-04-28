package com.bigbrightpaints.erp.modules.auth.web;

import org.springframework.util.StringUtils;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaActivateRequest(
    @NotBlank @Pattern(regexp = "\\d{6}", message = "MFA code must be 6 digits") String code,
    String factorType) {

  @AssertTrue(message = "Unsupported MFA factor type")
  public boolean hasSupportedFactorType() {
    return !StringUtils.hasText(factorType)
        || "totp".equalsIgnoreCase(factorType)
        || "authenticator".equalsIgnoreCase(factorType);
  }
}
