package com.bigbrightpaints.erp.core.security;

import java.util.UUID;

import io.jsonwebtoken.Claims;

public interface AuthSessionIntrospectionService {

  UUID currentSessionIdFromClaims(Claims claims);

  boolean isSessionActive(UUID userPublicId, String authScopeCode, UUID sessionId);
}
