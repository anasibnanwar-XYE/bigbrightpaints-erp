package com.bigbrightpaints.erp.modules.auth.web;

import java.util.List;

public record MfaRecoveryCodesResponse(boolean enabled, List<String> recoveryCodes) {}
