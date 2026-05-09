package com.bigbrightpaints.erp.modules.auth.domain;

public final class MfaFactorTypes {

  public static final String TOTP = "TOTP";

  private MfaFactorTypes() {}

  public static boolean isSupportedTotpFactor(String factorType) {
    if (factorType == null || factorType.isBlank()) {
      return true;
    }
    return TOTP.equalsIgnoreCase(factorType.trim());
  }
}
