package com.bigbrightpaints.erp.core.config;

import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.rbac.domain.Role;
import com.bigbrightpaints.erp.modules.rbac.domain.RoleRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import java.util.List;
import java.util.Locale;

@Configuration
public class DataInitializer {

    private static final Logger log = LoggerFactory.getLogger(DataInitializer.class);
    private static final String DEFAULT_SUPER_ADMIN_COMPANY_CODE = "SKE";
    private static final String SUPER_ADMIN_DISPLAY_NAME = "Platform Super Admin";

    @Bean
    @Profile({"dev", "seed"})
    CommandLineRunner seedDefaultUser(UserAccountRepository userRepository,
                                      CompanyRepository companyRepository,
                                      RoleRepository roleRepository,
                                      AccountRepository accountRepository,
                                      PasswordEncoder passwordEncoder,
                                      @Value("${erp.seed.super-admin.email:}") String superAdminEmail,
                                      @Value("${erp.seed.super-admin.password:}") String superAdminPassword,
                                      @Value("${erp.seed.super-admin.company-code:SKE}") String superAdminCompanyCode) {
        return args -> {
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
                Role role = new Role();
                role.setName("ROLE_ADMIN");
                role.setDescription("Platform administrator");
                return roleRepository.save(role);
            });
            Role superAdminRole = roleRepository.findByName("ROLE_SUPER_ADMIN").orElseGet(() -> {
                Role role = new Role();
                role.setName("ROLE_SUPER_ADMIN");
                role.setDescription("Platform super administrator");
                return roleRepository.save(role);
            });

            Company superAdminCompany = seedConfiguredSuperAdmin(
                    userRepository,
                    companyRepository,
                    passwordEncoder,
                    adminRole,
                    superAdminRole,
                    superAdminEmail,
                    superAdminPassword,
                    superAdminCompanyCode);

            Company company = companyRepository.findByCodeIgnoreCase("BBP")
                    .orElseGet(() -> {
                        Company c = new Company();
                        c.setName("Big Bright Paints");
                        c.setCode("BBP");
                        c.setTimezone("UTC");
                        c.setBaseCurrency("INR");
                        return companyRepository.save(c);
                    });

            userRepository.findByEmailIgnoreCase("admin@bbp.dev").orElseGet(() -> {
                UserAccount user = new UserAccount(
                        "admin@bbp.dev",
                        passwordEncoder.encode("ChangeMe123!"),
                        "Dev Admin");
                user.addCompany(company);
                user.addRole(adminRole);
                return userRepository.save(user);
            });

            seedDefaultAccounts(superAdminCompany, accountRepository);
            if (superAdminCompany != null) {
                setCompanyDefaultAccounts(superAdminCompany, companyRepository, accountRepository);
            }
            seedDefaultAccounts(company, accountRepository);
            setCompanyDefaultAccounts(company, companyRepository, accountRepository);
        };
    }

    private Company seedConfiguredSuperAdmin(UserAccountRepository userRepository,
                                             CompanyRepository companyRepository,
                                             PasswordEncoder passwordEncoder,
                                             Role adminRole,
                                             Role superAdminRole,
                                             String configuredEmail,
                                             String configuredPassword,
                                             String configuredCompanyCode) {
        if (!StringUtils.hasText(configuredEmail)) {
            log.info("Super-admin seed skipped: set erp.seed.super-admin.email to enable bootstrap");
            return null;
        }
        String normalizedEmail = configuredEmail.trim().toLowerCase(Locale.ROOT);
        String normalizedCompanyCode = normalizeCompanyCode(configuredCompanyCode);
        UserAccount existingSuperAdmin = userRepository.findByEmailIgnoreCase(normalizedEmail).orElse(null);
        if (existingSuperAdmin == null && !StringUtils.hasText(configuredPassword)) {
            throw new IllegalStateException(
                    "erp.seed.super-admin.password is required when bootstrap super-admin user does not exist");
        }
        Company superAdminCompany = companyRepository.findByCodeIgnoreCase(normalizedCompanyCode)
                .orElseGet(() -> {
                    Company company = new Company();
                    company.setName(normalizedCompanyCode);
                    company.setCode(normalizedCompanyCode);
                    company.setTimezone("UTC");
                    company.setBaseCurrency("INR");
                    company.setDefaultGstRate(java.math.BigDecimal.ZERO);
                    return companyRepository.save(company);
                });

        UserAccount superAdmin = existingSuperAdmin == null
                ? new UserAccount(
                        normalizedEmail,
                        passwordEncoder.encode(configuredPassword),
                        SUPER_ADMIN_DISPLAY_NAME)
                : existingSuperAdmin;
        if (existingSuperAdmin == null) {
            superAdmin.setMustChangePassword(true);
        }
        superAdmin.setDisplayName(SUPER_ADMIN_DISPLAY_NAME);
        superAdmin.addCompany(superAdminCompany);
        superAdmin.addRole(superAdminRole);
        superAdmin.addRole(adminRole);
        userRepository.save(superAdmin);
        return superAdminCompany;
    }

    private String normalizeCompanyCode(String configuredCompanyCode) {
        if (!StringUtils.hasText(configuredCompanyCode)) {
            return DEFAULT_SUPER_ADMIN_COMPANY_CODE;
        }
        return configuredCompanyCode.trim().toUpperCase(Locale.ROOT);
    }

    private void seedDefaultAccounts(Company company, AccountRepository accountRepository) {
        if (company == null) {
            return;
        }
        List<AccountSeed> seeds = List.of(
                new AccountSeed("CASH", "Cash", AccountType.ASSET),
                new AccountSeed("AR", "Accounts Receivable", AccountType.ASSET),
                new AccountSeed("AP", "Accounts Payable", AccountType.LIABILITY),
                new AccountSeed("INV", "Inventory", AccountType.ASSET),
                new AccountSeed("COGS", "Cost of Goods Sold", AccountType.COGS),
                new AccountSeed("REV", "Revenue", AccountType.REVENUE),
                new AccountSeed("GST-IN", "GST Input Tax", AccountType.ASSET),
                new AccountSeed("GST-OUT", "GST Output Tax", AccountType.LIABILITY),
                new AccountSeed("GST-PAY", "GST Payable", AccountType.LIABILITY),
                new AccountSeed("DISC", "Discounts", AccountType.EXPENSE),
                new AccountSeed("WIP", "Work in Progress", AccountType.ASSET),
                new AccountSeed("OPEX", "Operating Expenses", AccountType.EXPENSE)
        );
        for (AccountSeed seed : seeds) {
            accountRepository.findByCompanyAndCodeIgnoreCase(company, seed.code())
                    .orElseGet(() -> {
                        Account account = new Account();
                        account.setCompany(company);
                        account.setCode(seed.code());
                        account.setName(seed.name());
                        account.setType(seed.type());
                        return accountRepository.save(account);
                    });
        }
    }

    private void setCompanyDefaultAccounts(Company company,
                                          CompanyRepository companyRepository,
                                          AccountRepository accountRepository) {
        // Only set if missing to avoid overriding user-configured values
        if (company.getDefaultInventoryAccountId() == null) {
            accountRepository.findByCompanyAndCodeIgnoreCase(company, "INV")
                    .ifPresent(a -> company.setDefaultInventoryAccountId(a.getId()));
        }
        if (company.getDefaultCogsAccountId() == null) {
            accountRepository.findByCompanyAndCodeIgnoreCase(company, "COGS")
                    .ifPresent(a -> company.setDefaultCogsAccountId(a.getId()));
        }
        if (company.getDefaultRevenueAccountId() == null) {
            accountRepository.findByCompanyAndCodeIgnoreCase(company, "REV")
                    .ifPresent(a -> company.setDefaultRevenueAccountId(a.getId()));
        }
        if (company.getDefaultDiscountAccountId() == null) {
            accountRepository.findByCompanyAndCodeIgnoreCase(company, "DISC")
                    .ifPresent(a -> company.setDefaultDiscountAccountId(a.getId()));
        }
        if (company.getDefaultTaxAccountId() == null) {
            accountRepository.findByCompanyAndCodeIgnoreCase(company, "GST-OUT")
                    .ifPresent(a -> company.setDefaultTaxAccountId(a.getId()));
        }
        if (company.getGstInputTaxAccountId() == null) {
            accountRepository.findByCompanyAndCodeIgnoreCase(company, "GST-IN")
                    .ifPresent(a -> company.setGstInputTaxAccountId(a.getId()));
        }
        if (company.getGstOutputTaxAccountId() == null) {
            accountRepository.findByCompanyAndCodeIgnoreCase(company, "GST-OUT")
                    .ifPresent(a -> company.setGstOutputTaxAccountId(a.getId()));
        }
        if (company.getGstPayableAccountId() == null) {
            accountRepository.findByCompanyAndCodeIgnoreCase(company, "GST-PAY")
                    .ifPresent(a -> company.setGstPayableAccountId(a.getId()));
        }
        companyRepository.save(company);
    }

    private record AccountSeed(String code, String name, AccountType type) {}
}
