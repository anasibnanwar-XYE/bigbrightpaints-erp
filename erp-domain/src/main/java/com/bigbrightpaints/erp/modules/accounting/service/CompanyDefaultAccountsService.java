package com.bigbrightpaints.erp.modules.accounting.service;

import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;

/**
 * Manages company-wide default accounts for automatic postings.
 * Ensures correct account types are used to prevent mis-mapping (e.g., revenue to cash).
 */
@Service
public class CompanyDefaultAccountsService {

    private final CompanyContextService companyContextService;
    private final CompanyEntityLookup companyEntityLookup;
    private final CompanyRepository companyRepository;

    public CompanyDefaultAccountsService(CompanyContextService companyContextService,
                                         CompanyEntityLookup companyEntityLookup,
                                         CompanyRepository companyRepository) {
        this.companyContextService = companyContextService;
        this.companyEntityLookup = companyEntityLookup;
        this.companyRepository = companyRepository;
    }

    public DefaultAccounts requireDefaults() {
        Company company = companyContextService.requireCurrentCompany();
        if (company.getDefaultInventoryAccountId() == null ||
                company.getDefaultCogsAccountId() == null ||
                company.getDefaultRevenueAccountId() == null ||
                company.getDefaultTaxAccountId() == null) {
            throw new IllegalStateException("Company default accounts are not configured for " + company.getCode());
        }
        return new DefaultAccounts(
                company.getDefaultInventoryAccountId(),
                company.getDefaultCogsAccountId(),
                company.getDefaultRevenueAccountId(),
                company.getDefaultDiscountAccountId(),
                company.getDefaultTaxAccountId()
        );
    }

    @Transactional
    public DefaultAccounts updateDefaults(Long inventoryAccountId,
                                          Long cogsAccountId,
                                          Long revenueAccountId,
                                          Long discountAccountId,
                                          Long taxAccountId) {
        Company company = companyContextService.requireCurrentCompany();
        if (inventoryAccountId != null) {
            Account account = companyEntityLookup.requireAccount(company, inventoryAccountId);
            requireType(account, AccountType.ASSET, "inventory");
            company.setDefaultInventoryAccountId(account.getId());
        }
        if (cogsAccountId != null) {
            Account account = companyEntityLookup.requireAccount(company, cogsAccountId);
            requireType(account, AccountType.COGS, "COGS");
            company.setDefaultCogsAccountId(account.getId());
        }
        if (revenueAccountId != null) {
            Account account = companyEntityLookup.requireAccount(company, revenueAccountId);
            requireType(account, AccountType.REVENUE, "revenue");
            company.setDefaultRevenueAccountId(account.getId());
        }
        if (discountAccountId != null) {
            Account account = companyEntityLookup.requireAccount(company, discountAccountId);
            if (!(AccountType.REVENUE.equals(account.getType()) || AccountType.EXPENSE.equals(account.getType()))) {
                throw new IllegalArgumentException("Discount account must be revenue or expense");
            }
            company.setDefaultDiscountAccountId(account.getId());
        }
        if (taxAccountId != null) {
            Account account = companyEntityLookup.requireAccount(company, taxAccountId);
            requireType(account, AccountType.LIABILITY, "tax");
            company.setDefaultTaxAccountId(account.getId());
        }
        companyRepository.save(company);
        return new DefaultAccounts(
                company.getDefaultInventoryAccountId(),
                company.getDefaultCogsAccountId(),
                company.getDefaultRevenueAccountId(),
                company.getDefaultDiscountAccountId(),
                company.getDefaultTaxAccountId()
        );
    }

    private void requireType(Account account, AccountType expected, String purpose) {
        if (!expected.equals(account.getType())) {
            throw new IllegalArgumentException("Account " + account.getCode() + " is not a valid " + purpose + " account (expected type " + expected + ")");
        }
    }

    public record DefaultAccounts(Long inventoryAccountId,
                                  Long cogsAccountId,
                                  Long revenueAccountId,
                                  Long discountAccountId,
                                  Long taxAccountId) {}
}
