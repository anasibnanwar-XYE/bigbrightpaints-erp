package com.bigbrightpaints.erp.modules.auth.service;

import java.util.Map;

public record SecurityEventResponse(
    String type,
    String eventType,
    String actor,
    String targetUserId,
    String sessionId,
    String companyCode,
    String authScopeCode,
    String outcome,
    String reason,
    String createdAt,
    Map<String, String> metadata) {}
