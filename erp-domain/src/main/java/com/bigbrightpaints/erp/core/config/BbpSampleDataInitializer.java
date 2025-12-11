package com.bigbrightpaints.erp.core.config;

import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialRepository;
import com.bigbrightpaints.erp.modules.production.domain.ProductionBrand;
import com.bigbrightpaints.erp.modules.production.domain.ProductionBrandRepository;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProduct;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProductRepository;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

/**
 * Seeds a handful of BBP raw materials and finished goods for demo/testing only.
 * Runs in dev/test profiles and only if company code "BBP" exists.
 */
@Configuration
@Profile({"dev", "test"})
public class BbpSampleDataInitializer {

    @Bean
    CommandLineRunner seedBbpSamples(CompanyRepository companyRepository,
                                     AccountRepository accountRepository,
                                     ProductionBrandRepository brandRepository,
                                     ProductionProductRepository productRepository,
                                     FinishedGoodRepository finishedGoodRepository,
                                     RawMaterialRepository rawMaterialRepository,
                                     RawMaterialBatchRepository rawMaterialBatchRepository) {
        return args -> {
            Optional<Company> maybeCompany = companyRepository.findByCodeIgnoreCase("BBP");
            if (maybeCompany.isEmpty()) {
                return;
            }
            Company company = maybeCompany.get();
            Map<String, Account> accounts = loadAccounts(accountRepository, company);
            ProductionBrand brand = brandRepository.findByCompanyAndCodeIgnoreCase(company, "BBP")
                    .orElseGet(() -> {
                        ProductionBrand b = new ProductionBrand();
                        b.setCompany(company);
                        b.setCode("BBP");
                        b.setName("Big Bright Paints");
                        return brandRepository.save(b);
                    });

            // Seed raw materials with batches
            RawMaterial resin = ensureRawMaterial(company, rawMaterialRepository, accounts.get("INV"), "RM-RESIN", "Acrylic Resin", "KG");
            RawMaterial tio2 = ensureRawMaterial(company, rawMaterialRepository, accounts.get("INV"), "RM-TIO2", "Titanium Dioxide", "KG");
            RawMaterial solvent = ensureRawMaterial(company, rawMaterialRepository, accounts.get("INV"), "RM-SOLV", "Solvent Mix", "L");
            RawMaterial can = ensureRawMaterial(company, rawMaterialRepository, accounts.get("INV"), "RM-CAN", "Metal Can 1L", "UNIT");

            seedBatch(rawMaterialBatchRepository, rawMaterialRepository, resin, "RM-RESIN-B1", new BigDecimal("500"), new BigDecimal("6.50"), "KG");
            seedBatch(rawMaterialBatchRepository, rawMaterialRepository, tio2, "RM-TIO2-B1", new BigDecimal("300"), new BigDecimal("4.25"), "KG");
            seedBatch(rawMaterialBatchRepository, rawMaterialRepository, solvent, "RM-SOLV-B1", new BigDecimal("1000"), new BigDecimal("1.80"), "L");
            seedBatch(rawMaterialBatchRepository, rawMaterialRepository, can, "RM-CAN-B1", new BigDecimal("800"), new BigDecimal("1.10"), "UNIT");

            // Seed finished goods/products with default accounts
            ensureFinishedGood(productRepository, finishedGoodRepository, accounts, brand,
                    "BBP-ENAMEL-WH-1L", "Enamel White 1L", "WHITE", "1L", "L",
                    new BigDecimal("420.00"), new BigDecimal("18.00"));

            ensureFinishedGood(productRepository, finishedGoodRepository, accounts, brand,
                    "BBP-ENAMEL-WH-4L", "Enamel White 4L", "WHITE", "4L", "L",
                    new BigDecimal("1480.00"), new BigDecimal("18.00"));
        };
    }

    private Map<String, Account> loadAccounts(AccountRepository repo, Company company) {
        Map<String, Account> map = new HashMap<>();
        repo.findByCompanyOrderByCodeAsc(company).forEach(acc -> map.put(acc.getCode().toUpperCase(), acc));
        return map;
    }

    private RawMaterial ensureRawMaterial(Company company,
                                          RawMaterialRepository repo,
                                          Account inventory,
                                          String sku,
                                          String name,
                                          String unit) {
        return repo.findByCompanyAndSku(company, sku)
                .orElseGet(() -> {
                    RawMaterial rm = new RawMaterial();
                    rm.setCompany(company);
                    rm.setSku(sku);
                    rm.setName(name);
                    rm.setUnitType(unit);
                    rm.setInventoryAccountId(inventory != null ? inventory.getId() : null);
                    rm.setReorderLevel(new BigDecimal("50"));
                    rm.setMinStock(new BigDecimal("50"));
                    rm.setMaxStock(new BigDecimal("5000"));
                    return repo.save(rm);
                });
    }

    private void seedBatch(RawMaterialBatchRepository batchRepo,
                           RawMaterialRepository rmRepo,
                           RawMaterial rm,
                           String batchCode,
                           BigDecimal qty,
                           BigDecimal cost,
                           String unit) {
        if (rm == null) {
            return;
        }
        RawMaterialBatch batch = new RawMaterialBatch();
        batch.setRawMaterial(rm);
        batch.setBatchCode(batchCode);
        batch.setQuantity(qty);
        batch.setUnit(unit);
        batch.setCostPerUnit(cost);
        batchRepo.save(batch);
        rm.setCurrentStock(rm.getCurrentStock().add(qty));
        rmRepo.save(rm);
    }

    private void ensureFinishedGood(ProductionProductRepository productRepo,
                                    FinishedGoodRepository fgRepo,
                                    Map<String, Account> accounts,
                                    ProductionBrand brand,
                                    String sku,
                                    String productName,
                                    String color,
                                    String sizeLabel,
                                    String uom,
                                    BigDecimal basePrice,
                                    BigDecimal gstRate) {
        Map<String, Object> metadata = new HashMap<>();
        maybePut(metadata, "fgValuationAccountId", accountId(accounts, "INV"));
        maybePut(metadata, "fgCogsAccountId", accountId(accounts, "COGS"));
        maybePut(metadata, "fgRevenueAccountId", accountId(accounts, "REV"));
        maybePut(metadata, "fgDiscountAccountId", accountId(accounts, "DISC"));
        maybePut(metadata, "fgTaxAccountId", accountId(accounts, "GST-OUT"));
        maybePut(metadata, "wipAccountId", accountId(accounts, "WIP"));
        maybePut(metadata, "semiFinishedAccountId", accountId(accounts, "INV"));

        ProductionProduct product = productRepo.findByCompanyAndSkuCode(brand.getCompany(), sku)
                .orElseGet(() -> {
                    ProductionProduct p = new ProductionProduct();
                    p.setCompany(brand.getCompany());
                    p.setBrand(brand);
                    p.setSkuCode(sku);
                    p.setCategory("FINISHED_GOOD");
                    p.setUnitOfMeasure(uom);
                    p.setActive(true);
                    return p;
                });
        product.setProductName(productName);
        product.setDefaultColour(color);
        product.setSizeLabel(sizeLabel);
        product.setBasePrice(basePrice);
        product.setGstRate(gstRate);
        product.setMinDiscountPercent(BigDecimal.ZERO);
        product.setMinSellingPrice(basePrice);
        product.setMetadata(metadata);
        productRepo.save(product);

        fgRepo.findByCompanyAndProductCode(brand.getCompany(), sku)
                .orElseGet(() -> {
                    FinishedGood fg = new FinishedGood();
                    fg.setCompany(brand.getCompany());
                    fg.setProductCode(sku);
                    fg.setName(productName);
                    fg.setUnit(uom);
                    fg.setCostingMethod("FIFO");
                    fg.setValuationAccountId(accountId(accounts, "INV"));
                    fg.setCogsAccountId(accountId(accounts, "COGS"));
                    fg.setRevenueAccountId(accountId(accounts, "REV"));
                    fg.setDiscountAccountId(accountId(accounts, "DISC"));
                    fg.setTaxAccountId(accountId(accounts, "GST-OUT"));
                    fg.setCurrentStock(BigDecimal.ZERO);
                    fg.setReservedStock(BigDecimal.ZERO);
                    return fgRepo.save(fg);
                });
    }

    private void maybePut(Map<String, Object> map, String key, Long value) {
        if (value != null) {
            map.put(key, value);
        }
    }

    private Long accountId(Map<String, Account> accounts, String code) {
        if (accounts == null) {
            return null;
        }
        return accounts.getOrDefault(code.toUpperCase(), null) != null
                ? accounts.get(code.toUpperCase()).getId()
                : null;
    }
}
