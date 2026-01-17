package com.bigbrightpaints.erp.core.config;

import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialRepository;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProduct;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProductRepository;
import com.bigbrightpaints.erp.modules.rbac.domain.Role;
import com.bigbrightpaints.erp.modules.rbac.domain.RoleRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

@Configuration
public class DataInitializer {

    @Bean
    @Profile({"dev", "seed"})
    CommandLineRunner seedDefaultUser(UserAccountRepository userRepository,
                                      CompanyRepository companyRepository,
                                      RoleRepository roleRepository,
                                      AccountRepository accountRepository,
                                      RawMaterialRepository rawMaterialRepository,
                                      FinishedGoodRepository finishedGoodRepository,
                                      ProductionProductRepository productionProductRepository,
                                      PasswordEncoder passwordEncoder) {
        return args -> {
            Company company = companyRepository.findByCodeIgnoreCase("BBP")
                    .orElseGet(() -> {
                        Company c = new Company();
                        c.setName("Big Bright Paints");
                        c.setCode("BBP");
                        c.setTimezone("UTC");
                        return companyRepository.save(c);
                    });
            Role adminRole = roleRepository.findByName("ROLE_ADMIN").orElseGet(() -> {
                Role role = new Role();
                role.setName("ROLE_ADMIN");
                role.setDescription("Platform administrator");
                return roleRepository.save(role);
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

            List<Company> companies = companyRepository.findAll();
            if (companies.isEmpty()) {
                return;
            }
            for (Company tenant : companies) {
                seedDefaultAccounts(tenant, accountRepository);
                setCompanyDefaultAccounts(tenant, companyRepository, accountRepository);
                backfillInventoryDefaults(tenant,
                        accountRepository,
                        rawMaterialRepository,
                        finishedGoodRepository,
                        productionProductRepository);
            }
        };
    }

    private void seedDefaultAccounts(Company company, AccountRepository accountRepository) {
        if (company == null) {
            return;
        }
        List<AccountSeed> seeds = List.of(
                new AccountSeed("1000", "Cash", AccountType.ASSET),
                new AccountSeed("1100", "Accounts Receivable", AccountType.ASSET),
                new AccountSeed("1200", "Inventory", AccountType.ASSET),
                new AccountSeed("2000", "Accounts Payable", AccountType.LIABILITY),
                new AccountSeed("4000", "Revenue", AccountType.REVENUE),
                new AccountSeed("5000", "Cost of Goods Sold", AccountType.COGS),
                new AccountSeed("6000", "Operating Expenses", AccountType.EXPENSE),
                new AccountSeed("AR", "Accounts Receivable", AccountType.ASSET),
                new AccountSeed("AP", "Accounts Payable", AccountType.LIABILITY),
                new AccountSeed("INV", "Inventory", AccountType.ASSET),
                new AccountSeed("REV", "Revenue", AccountType.REVENUE),
                new AccountSeed("COGS", "Cost of Goods Sold", AccountType.COGS),
                new AccountSeed("GST-IN", "GST Input Tax", AccountType.ASSET),
                new AccountSeed("GST-OUT", "GST Output Tax", AccountType.LIABILITY),
                new AccountSeed("GST-PAY", "GST Payable", AccountType.LIABILITY),
                new AccountSeed("DISC", "Discounts", AccountType.EXPENSE),
                new AccountSeed("WIP", "Work in Progress", AccountType.ASSET)
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
            accountRepository.findByCompanyAndCodeIgnoreCase(company, "1200")
                    .or(() -> accountRepository.findByCompanyAndCodeIgnoreCase(company, "INV"))
                    .ifPresent(a -> company.setDefaultInventoryAccountId(a.getId()));
        }
        if (company.getDefaultCogsAccountId() == null) {
            accountRepository.findByCompanyAndCodeIgnoreCase(company, "5000")
                    .or(() -> accountRepository.findByCompanyAndCodeIgnoreCase(company, "COGS"))
                    .ifPresent(a -> company.setDefaultCogsAccountId(a.getId()));
        }
        if (company.getDefaultRevenueAccountId() == null) {
            accountRepository.findByCompanyAndCodeIgnoreCase(company, "4000")
                    .or(() -> accountRepository.findByCompanyAndCodeIgnoreCase(company, "REV"))
                    .ifPresent(a -> company.setDefaultRevenueAccountId(a.getId()));
        }
        if (company.getDefaultDiscountAccountId() == null) {
            accountRepository.findByCompanyAndCodeIgnoreCase(company, "DISC")
                    .ifPresent(a -> company.setDefaultDiscountAccountId(a.getId()));
        }
        if (company.getDefaultTaxAccountId() == null) {
            accountRepository.findByCompanyAndCodeIgnoreCase(company, "2000")
                    .or(() -> accountRepository.findByCompanyAndCodeIgnoreCase(company, "GST-OUT"))
                    .or(() -> accountRepository.findByCompanyAndCodeIgnoreCase(company, "GST-PAY"))
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

    private void backfillInventoryDefaults(Company company,
                                           AccountRepository accountRepository,
                                           RawMaterialRepository rawMaterialRepository,
                                           FinishedGoodRepository finishedGoodRepository,
                                           ProductionProductRepository productionProductRepository) {
        if (company == null) {
            return;
        }
        Long defaultInventoryId = company.getDefaultInventoryAccountId();
        Long defaultCogsId = company.getDefaultCogsAccountId();
        Long defaultRevenueId = company.getDefaultRevenueAccountId();
        Long defaultDiscountId = company.getDefaultDiscountAccountId();
        Long defaultTaxId = company.getDefaultTaxAccountId();
        Long wipAccountId = accountRepository.findByCompanyAndCodeIgnoreCase(company, "WIP")
                .map(Account::getId)
                .orElse(defaultInventoryId);

        List<RawMaterial> materials = rawMaterialRepository.findByCompanyOrderByNameAsc(company);
        boolean materialsUpdated = false;
        for (RawMaterial material : materials) {
            if (material.getInventoryAccountId() == null && defaultInventoryId != null) {
                material.setInventoryAccountId(defaultInventoryId);
                materialsUpdated = true;
            }
        }
        if (materialsUpdated) {
            rawMaterialRepository.saveAll(materials);
        }

        List<FinishedGood> finishedGoods = finishedGoodRepository.findByCompanyOrderByProductCodeAsc(company);
        boolean finishedGoodsUpdated = false;
        for (FinishedGood finishedGood : finishedGoods) {
            boolean updated = false;
            if (finishedGood.getValuationAccountId() == null && defaultInventoryId != null) {
                finishedGood.setValuationAccountId(defaultInventoryId);
                updated = true;
            }
            if (finishedGood.getCogsAccountId() == null && defaultCogsId != null) {
                finishedGood.setCogsAccountId(defaultCogsId);
                updated = true;
            }
            if (finishedGood.getRevenueAccountId() == null && defaultRevenueId != null) {
                finishedGood.setRevenueAccountId(defaultRevenueId);
                updated = true;
            }
            if (finishedGood.getDiscountAccountId() == null && defaultDiscountId != null) {
                finishedGood.setDiscountAccountId(defaultDiscountId);
                updated = true;
            }
            if (finishedGood.getTaxAccountId() == null && defaultTaxId != null) {
                finishedGood.setTaxAccountId(defaultTaxId);
                updated = true;
            }
            if (updated) {
                finishedGoodsUpdated = true;
            }
        }
        if (finishedGoodsUpdated) {
            finishedGoodRepository.saveAll(finishedGoods);
        }

        List<ProductionProduct> products = productionProductRepository.findByCompanyOrderByProductNameAsc(company);
        boolean productsUpdated = false;
        for (ProductionProduct product : products) {
            if (isRawMaterialCategory(product.getCategory())) {
                continue;
            }
            Map<String, Object> metadata = product.getMetadata() == null
                    ? new HashMap<>()
                    : new HashMap<>(product.getMetadata());
            boolean updated = false;
            updated |= ensureMetadataLong(metadata, "fgValuationAccountId", defaultInventoryId);
            updated |= ensureMetadataLong(metadata, "fgCogsAccountId", defaultCogsId);
            updated |= ensureMetadataLong(metadata, "fgRevenueAccountId", defaultRevenueId);
            updated |= ensureMetadataLong(metadata, "fgDiscountAccountId", defaultDiscountId);
            updated |= ensureMetadataLong(metadata, "fgTaxAccountId", defaultTaxId);
            updated |= ensureMetadataLong(metadata, "wipAccountId", wipAccountId);
            updated |= ensureMetadataLong(metadata, "semiFinishedAccountId",
                    hasLongValue(metadata.get("fgValuationAccountId")) ? metadata.get("fgValuationAccountId") : defaultInventoryId);
            if (updated) {
                product.setMetadata(metadata);
                productsUpdated = true;
            }
        }
        if (productsUpdated) {
            productionProductRepository.saveAll(products);
        }
    }

    private boolean ensureMetadataLong(Map<String, Object> metadata, String key, Object value) {
        if (metadata == null || hasLongValue(metadata.get(key)) || !hasLongValue(value)) {
            return false;
        }
        metadata.put(key, value);
        return true;
    }

    private boolean hasLongValue(Object value) {
        if (value instanceof Number number) {
            return number.longValue() > 0;
        }
        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Long.parseLong(stringValue.trim()) > 0;
            } catch (NumberFormatException ignored) {
                return false;
            }
        }
        return false;
    }

    private boolean isRawMaterialCategory(String category) {
        if (category == null || category.isBlank()) {
            return false;
        }
        String normalized = category.trim().toUpperCase();
        return RAW_MATERIAL_CATEGORIES.contains(normalized);
    }

    private static final Set<String> RAW_MATERIAL_CATEGORIES = Set.of(
            "RAW_MATERIAL",
            "RAW MATERIAL",
            "RAW-MATERIAL"
    );

    private record AccountSeed(String code, String name, AccountType type) {}
}
