package com.bigbrightpaints.erp.modules.auth.web;

import java.util.List;

public record AuthResponse(
    String tokenType,
    String accessToken,
    String refreshToken,
    long expiresIn,
    String companyCode,
    String scopeType,
    String displayName,
    boolean mustChangePassword,
    List<String> roles) {

  public AuthResponse(
      String tokenType,
      String accessToken,
      String refreshToken,
      long expiresIn,
      String companyCode,
      String displayName,
      boolean mustChangePassword) {
    this(
        tokenType,
        accessToken,
        refreshToken,
        expiresIn,
        companyCode,
        "TENANT",
        displayName,
        mustChangePassword,
        List.of());
  }
}
