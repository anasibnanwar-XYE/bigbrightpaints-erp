package com.bigbrightpaints.erp.modules.auth.service;

import com.bigbrightpaints.erp.core.idempotency.IdempotencyUtils;

public final class AuthTokenDigests {

  private static final String REFRESH_TOKEN_SCOPE = "refresh-token";
  private static final String PASSWORD_RESET_TOKEN_SCOPE = "password-reset-token";
  private static final String TENANT_ACTIVATION_TOKEN_SCOPE = "tenant-activation:v1";

  public static final String DIGEST_ALGORITHM = "SHA-256";
  public static final int DIGEST_VERSION = 1;

  private AuthTokenDigests() {}

  static String refreshTokenDigest(String token) {
    return digest(REFRESH_TOKEN_SCOPE, token);
  }

  public static String passwordResetTokenDigest(String token) {
    return digest(PASSWORD_RESET_TOKEN_SCOPE, token);
  }

  public static String tenantActivationTokenDigest(String token) {
    return digest(TENANT_ACTIVATION_TOKEN_SCOPE, token);
  }

  private static String digest(String scope, String token) {
    return IdempotencyUtils.sha256Hex(scope + ":" + (token == null ? "" : token));
  }
}
