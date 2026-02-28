package com.bigbrightpaints.erp.modules.company.service;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import org.springframework.stereotype.Service;

@Service
public class CompanyContextService {

    private final CompanyRepository companyRepository;

    public CompanyContextService(CompanyRepository companyRepository) {
        this.companyRepository = companyRepository;
    }

    public Company requireCurrentCompany() {
        String code = CompanyContextHolder.getCompanyCode();
        if (code == null) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState("No active company in context");
        }
        return companyRepository.findByCodeIgnoreCase(code)
                .orElseThrow(() -> com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Company not found: " + code));
    }
}
