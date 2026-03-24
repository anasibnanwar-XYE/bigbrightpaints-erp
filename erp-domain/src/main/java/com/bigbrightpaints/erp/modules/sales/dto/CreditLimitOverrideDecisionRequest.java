package com.bigbrightpaints.erp.modules.sales.dto;

import java.time.Instant;

public record CreditLimitOverrideDecisionRequest(String reason, Instant expiresAt) {}
