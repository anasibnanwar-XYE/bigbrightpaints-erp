package com.bigbrightpaints.erp.modules.accounting.service;

import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

@Service
public class CompanyAccountingSettingsService {

    private final CompanyContextService companyContextService;
    private final CompanyRepository companyRepository;
    private final CompanyEntityLookup companyEntityLookup;

    public CompanyAccountingSettingsService(CompanyContextService companyContextService,
                                            CompanyRepository companyRepository,
                                            CompanyEntityLookup companyEntityLookup) {
        this.companyContextService = companyContextService;
        this.companyRepository = companyRepository;
        this.companyEntityLookup = companyEntityLookup;
    }

    public PayrollAccountDefaults requirePayrollDefaults() {
        Company company = companyContextService.requireCurrentCompany();
        Account expense = company.getPayrollExpenseAccount();
        Account cash = company.getPayrollCashAccount();
        if (expense == null || cash == null) {
            throw new IllegalStateException("Payroll account defaults are not configured for company " + company.getCode());
        }
        return new PayrollAccountDefaults(expense.getId(), cash.getId());
    }

    @Transactional
    public void updatePayrollDefaults(Long expenseAccountId, Long cashAccountId) {
        Company company = companyContextService.requireCurrentCompany();
        if (expenseAccountId != null) {
            Account expense = companyEntityLookup.requireAccount(company, expenseAccountId);
            company.setPayrollExpenseAccount(expense);
        }
        if (cashAccountId != null) {
            Account cash = companyEntityLookup.requireAccount(company, cashAccountId);
            company.setPayrollCashAccount(cash);
        }
        companyRepository.save(company);
    }

    public record PayrollAccountDefaults(Long expenseAccountId, Long cashAccountId) {}

    public TaxAccountConfiguration requireTaxAccounts() {
        Company company = companyContextService.requireCurrentCompany();
        if (company.getGstInputTaxAccountId() == null || company.getGstOutputTaxAccountId() == null) {
            throw new IllegalStateException("GST tax accounts not configured for company " + company.getCode());
        }
        return new TaxAccountConfiguration(
                company.getGstInputTaxAccountId(),
                company.getGstOutputTaxAccountId(),
                company.getGstPayableAccountId()
        );
    }

    public record TaxAccountConfiguration(Long inputTaxAccountId,
                                          Long outputTaxAccountId,
                                          Long payableAccountId) {}
}
