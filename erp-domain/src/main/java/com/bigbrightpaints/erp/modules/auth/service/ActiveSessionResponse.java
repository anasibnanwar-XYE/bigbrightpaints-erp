package com.bigbrightpaints.erp.modules.auth.service;

public record ActiveSessionResponse(
    String sessionId,
    boolean current,
    String createdAt,
    String lastSeenAt,
    String expiresAt,
    String authScopeCode,
    String deviceName,
    String userAgent) {}
