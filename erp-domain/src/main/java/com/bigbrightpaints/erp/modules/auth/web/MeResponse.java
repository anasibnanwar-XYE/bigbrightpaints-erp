package com.bigbrightpaints.erp.modules.auth.web;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.util.List;

public record MeResponse(
        String email,
        String displayName,
        String companyCode,
        boolean mfaEnabled,
        boolean mustChangePassword,
        List<String> roles,
        List<String> permissions
) {
    @Deprecated
    @JsonProperty("companyId")
    public String legacyCompanyId() {
        return companyCode;
    }
}
