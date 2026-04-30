package com.bigbrightpaints.erp.modules.auth.web;

import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.modules.auth.domain.MfaFactorTypes;

import jakarta.validation.constraints.AssertTrue;

public record MfaDisableRequest(String code, String recoveryCode, String factorType) {

  @AssertTrue(message = "Provide either code or recoveryCode")
  public boolean hasVerifier() {
    return StringUtils.hasText(code) || StringUtils.hasText(recoveryCode);
  }

  @AssertTrue(message = "Unsupported MFA factor type")
  public boolean hasSupportedFactorType() {
    return MfaFactorTypes.isSupportedTotpAlias(factorType);
  }
}
