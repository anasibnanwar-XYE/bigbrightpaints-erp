package com.bigbrightpaints.erp.modules.auth.web;

import com.bigbrightpaints.erp.modules.auth.domain.MfaFactorTypes;

import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;

public record MfaActivateRequest(
    @NotBlank @Pattern(regexp = "\\d{6}", message = "MFA code must be 6 digits") String code,
    String factorType) {

  @AssertTrue(message = "Unsupported MFA factor type")
  public boolean hasSupportedFactorType() {
    return MfaFactorTypes.isSupportedTotpFactor(factorType);
  }
}
