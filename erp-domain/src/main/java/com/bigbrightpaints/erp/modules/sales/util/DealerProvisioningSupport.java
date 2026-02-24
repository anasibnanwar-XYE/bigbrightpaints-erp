package com.bigbrightpaints.erp.modules.sales.util;

import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;

import java.text.Normalizer;
import java.util.Locale;
import java.util.Optional;

public final class DealerProvisioningSupport {

    public static final String ACTIVE_STATUS = "ACTIVE";
    public static final String INACTIVE_STATUS = "INACTIVE";

    private DealerProvisioningSupport() {
    }

    public static Account createReceivableAccount(Company company,
                                                  Dealer dealer,
                                                  AccountRepository accountRepository) {
        String baseCode = "AR-" + dealer.getCode();
        String code = baseCode;
        int attempt = 1;
        while (accountRepository.findByCompanyAndCodeIgnoreCase(company, code).isPresent()) {
            code = baseCode + "-" + attempt++;
        }
        Account account = new Account();
        account.setCompany(company);
        account.setCode(code);
        account.setName(dealer.getName() + " Receivable");
        account.setType(AccountType.ASSET);
        resolveControlAccount(company, "AR", AccountType.ASSET, accountRepository).ifPresent(account::setParent);
        return accountRepository.save(account);
    }

    public static String generateDealerCode(String input, Company company, DealerRepository dealerRepository) {
        String normalized = Normalizer.normalize(input, Normalizer.Form.NFD)
                .replaceAll("\\p{M}", "")
                .replaceAll("[^A-Za-z0-9]", "")
                .toUpperCase(Locale.ROOT);
        String base = normalized.isEmpty() ? "DEALER" : normalized;
        String code = base;
        int attempt = 1;
        while (dealerRepository.findByCompanyAndCodeIgnoreCase(company, code).isPresent()) {
            code = base + attempt++;
        }
        return code;
    }

    private static Optional<Account> resolveControlAccount(Company company,
                                                           String code,
                                                           AccountType expectedType,
                                                           AccountRepository accountRepository) {
        return accountRepository.findByCompanyAndCodeIgnoreCase(company, code)
                .filter(account -> account.getType() == expectedType);
    }
}
