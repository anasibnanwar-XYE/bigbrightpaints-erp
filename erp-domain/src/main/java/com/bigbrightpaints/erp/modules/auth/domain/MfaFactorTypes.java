package com.bigbrightpaints.erp.modules.auth.domain;

public final class MfaFactorTypes {

  public static final String TOTP = "TOTP";
  public static final String AUTHENTICATOR = "AUTHENTICATOR";

  private MfaFactorTypes() {}

  public static boolean isSupportedTotpAlias(String factorType) {
    if (factorType == null || factorType.isBlank()) {
      return true;
    }
    String normalized = factorType.trim();
    return TOTP.equalsIgnoreCase(normalized) || AUTHENTICATOR.equalsIgnoreCase(normalized);
  }
}
