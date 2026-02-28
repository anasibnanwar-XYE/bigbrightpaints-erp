package com.bigbrightpaints.erp.modules.company.dto;

import java.util.Collections;
import java.util.LinkedHashSet;
import java.util.Set;

public record CompanyEnabledModulesDto(Long companyId, String companyCode, Set<String> enabledModules) {

    public CompanyEnabledModulesDto {
        enabledModules = enabledModules == null
                ? Set.of()
                : Collections.unmodifiableSet(new LinkedHashSet<>(enabledModules));
    }
}
