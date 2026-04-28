package com.bigbrightpaints.erp.modules.auth.web;

import org.springframework.util.StringUtils;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record LoginRequest(
    @Email @NotBlank String email,
    @NotBlank String password,
    @NotBlank String companyCode,
    String mfaCode,
    String recoveryCode,
    String factorType) {

  public LoginRequest(
      String email, String password, String companyCode, String mfaCode, String recoveryCode) {
    this(email, password, companyCode, mfaCode, recoveryCode, null);
  }

  @AssertTrue(message = "Unsupported MFA factor type")
  public boolean hasSupportedFactorType() {
    return !StringUtils.hasText(factorType)
        || "totp".equalsIgnoreCase(factorType)
        || "authenticator".equalsIgnoreCase(factorType);
  }
}
