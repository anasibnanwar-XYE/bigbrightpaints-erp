package com.bigbrightpaints.erp.modules.auth.web;

import java.util.List;

public record MeResponse(
        String email,
        String displayName,
        String companyId,
        boolean mfaEnabled,
        List<String> roles,
        List<String> permissions
) {}
