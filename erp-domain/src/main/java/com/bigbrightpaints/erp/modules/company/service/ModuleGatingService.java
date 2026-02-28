package com.bigbrightpaints.erp.modules.company.service;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyModule;
import java.util.LinkedHashSet;
import java.util.Set;
import org.springframework.stereotype.Service;

@Service
public class ModuleGatingService {

    private final CompanyContextService companyContextService;

    public ModuleGatingService(CompanyContextService companyContextService) {
        this.companyContextService = companyContextService;
    }

    public boolean isEnabledForCurrentCompany(CompanyModule module) {
        if (module == null || module.isCore()) {
            return true;
        }
        Company company = companyContextService.requireCurrentCompany();
        return resolveEnabledGatableModules(company).contains(module.name());
    }

    public Set<String> resolveEnabledGatableModules(Company company) {
        if (company == null) {
            return new LinkedHashSet<>(CompanyModule.defaultEnabledGatableModuleNames());
        }
        return CompanyModule.normalizeEnabledGatableModuleNames(company.getEnabledModules());
    }

    public Set<String> normalizeRequestedEnabledModules(Set<String> requestedModules) {
        return CompanyModule.normalizeEnabledGatableModuleNames(requestedModules);
    }
}
