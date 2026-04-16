package com.bigbrightpaints.erp.core.config;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.CommandLineRunner;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Profile;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.security.AuthScopeService;
import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccount;
import com.bigbrightpaints.erp.modules.auth.domain.UserAccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.dto.TenantOnboardingRequest;
import com.bigbrightpaints.erp.modules.company.service.TenantOnboardingService;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodRepository;
import com.bigbrightpaints.erp.modules.invoice.domain.Invoice;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceRepository;
import com.bigbrightpaints.erp.modules.production.domain.ProductionBrand;
import com.bigbrightpaints.erp.modules.production.domain.ProductionBrandRepository;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProduct;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProductRepository;
import com.bigbrightpaints.erp.modules.rbac.domain.Role;
import com.bigbrightpaints.erp.modules.rbac.domain.RoleRepository;
import com.bigbrightpaints.erp.modules.rbac.service.RoleService;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderRepository;
import com.bigbrightpaints.erp.modules.sales.dto.DealerRequest;
import com.bigbrightpaints.erp.modules.sales.dto.SalesOrderDto;
import com.bigbrightpaints.erp.modules.sales.dto.SalesOrderItemRequest;
import com.bigbrightpaints.erp.modules.sales.dto.SalesOrderRequest;
import com.bigbrightpaints.erp.modules.sales.service.SalesFulfillmentService;
import com.bigbrightpaints.erp.modules.sales.service.SalesService;

@Configuration
@Profile("uat-seed")
public class UatSeedDataInitializer {

  private static final Logger log = LoggerFactory.getLogger(UatSeedDataInitializer.class);
  private static final BigDecimal TENANT_DEFAULT_GST_RATE = new BigDecimal("18");
  private static final BigDecimal DEALER_CREDIT_LIMIT = new BigDecimal("500000");
  private static final BigDecimal PRIMARY_BATCH_QUANTITY = new BigDecimal("240");
  private static final BigDecimal SECONDARY_BATCH_QUANTITY = new BigDecimal("120");
  private static final BigDecimal PRIMARY_BATCH_UNIT_COST = new BigDecimal("82.50");
  private static final BigDecimal SECONDARY_BATCH_UNIT_COST = new BigDecimal("84.75");
  private static final BigDecimal DRAFT_ORDER_QUANTITY = new BigDecimal("15");
  private static final BigDecimal FULFILL_ORDER_QUANTITY = new BigDecimal("24");
  private static final BigDecimal UNIT_SELLING_PRICE = new BigDecimal("132.00");
  private static final String MANUFACTURING_TEMPLATE = "MANUFACTURING";
  private static final String PLATFORM_ADMIN_DISPLAY_NAME = "Platform Admin";
  private static final String TENANT_ADMIN_DISPLAY_NAME = "Tenant Admin";
  private static final String ACCOUNTING_DISPLAY_NAME = "Accounting User";
  private static final String SALES_DISPLAY_NAME = "Sales User";
  private static final String FACTORY_DISPLAY_NAME = "Factory User";
  private static final String DEALER_DISPLAY_NAME = "Dealer User";
  private static final String BRAND_CODE = "UAT-BRAND";
  private static final String BRAND_NAME = "UAT Brand";
  private static final String PRODUCT_SKU = "UAT-PAINT-20L";
  private static final String PRODUCT_NAME = "UAT Exterior Paint 20L";
  private static final String PRIMARY_BATCH_CODE = "UAT-BATCH-001";
  private static final String SECONDARY_BATCH_CODE = "UAT-BATCH-002";
  private static final String DEALER_CODE = "UAT-DEALER-01";
  private static final String DEALER_NAME = "UAT Dealer One";
  private static final String DEALER_GST = "27ABCDE1234F1Z5";
  private static final String DEALER_STATE_CODE = "27";
  private static final String DRAFT_ORDER_IDEMPOTENCY_KEY = "uat-draft-order-v1";
  private static final String FULFILL_ORDER_IDEMPOTENCY_KEY = "uat-fulfilled-order-v1";

  @Bean
  CommandLineRunner seedUatRuntimeData(
      RoleService roleService,
      RoleRepository roleRepository,
      CompanyRepository companyRepository,
      UserAccountRepository userAccountRepository,
      TenantOnboardingService tenantOnboardingService,
      PasswordEncoder passwordEncoder,
      AuthScopeService authScopeService,
      AccountRepository accountRepository,
      DealerRepository dealerRepository,
      ProductionBrandRepository productionBrandRepository,
      ProductionProductRepository productionProductRepository,
      FinishedGoodRepository finishedGoodRepository,
      FinishedGoodBatchRepository finishedGoodBatchRepository,
      SalesService salesService,
      SalesFulfillmentService salesFulfillmentService,
      SalesOrderRepository salesOrderRepository,
      InvoiceRepository invoiceRepository,
      @Value("${erp.uat-seed.enabled:true}") boolean enabled,
      @Value("${erp.uat-seed.platform-admin.email}") String platformAdminEmail,
      @Value("${erp.uat-seed.platform-admin.password}") String platformAdminPassword,
      @Value("${erp.uat-seed.platform-admin.scope-code:PLATFORM}") String platformScopeCode,
      @Value("${erp.uat-seed.tenant.code}") String tenantCode,
      @Value("${erp.uat-seed.tenant.name}") String tenantName,
      @Value("${erp.uat-seed.tenant.timezone}") String tenantTimezone,
      @Value("${erp.uat-seed.tenant.user-password}") String tenantUserPassword,
      @Value("${erp.uat-seed.tenant.max-active-users:500}") long tenantMaxActiveUsers,
      @Value("${erp.uat-seed.tenant.max-api-requests:5000}") long tenantMaxApiRequests,
      @Value("${erp.uat-seed.tenant.max-concurrent-requests:200}")
          long tenantMaxConcurrentRequests,
      @Value("${erp.uat-seed.tenant.users.admin-email}") String tenantAdminEmail,
      @Value("${erp.uat-seed.tenant.users.accounting-email}") String accountingEmail,
      @Value("${erp.uat-seed.tenant.users.sales-email}") String salesEmail,
      @Value("${erp.uat-seed.tenant.users.factory-email}") String factoryEmail,
      @Value("${erp.uat-seed.tenant.users.dealer-email}") String dealerEmail) {
    return args -> {
      if (!enabled) {
        log.info("UAT runtime seed disabled; set erp.uat-seed.enabled=true to seed local actors.");
        return;
      }

      roleService.synchronizeSystemRoles();
      Map<String, Role> roles = loadRequiredRoles(roleRepository);
      String normalizedPlatformScopeCode = authScopeService.updatePlatformScopeCode(platformScopeCode);
      ensurePlatformAdmin(
          userAccountRepository,
          passwordEncoder,
          normalizedPlatformScopeCode,
          platformAdminEmail,
          platformAdminPassword,
          roles.get("ROLE_ADMIN"),
          roles.get("ROLE_SUPER_ADMIN"));

      Company tenant =
          ensureTenant(
              companyRepository,
              tenantOnboardingService,
              tenantName,
              tenantCode,
              tenantTimezone,
              tenantMaxActiveUsers,
              tenantMaxApiRequests,
              tenantMaxConcurrentRequests,
              tenantAdminEmail);

      UserAccount tenantAdmin =
          ensureScopedUser(
              userAccountRepository,
              passwordEncoder,
              tenant,
              tenantAdminEmail,
              TENANT_ADMIN_DISPLAY_NAME,
              tenantUserPassword,
              List.of(roles.get("ROLE_ADMIN")));
      UserAccount accountingUser =
          ensureScopedUser(
              userAccountRepository,
              passwordEncoder,
              tenant,
              accountingEmail,
              ACCOUNTING_DISPLAY_NAME,
              tenantUserPassword,
              List.of(roles.get("ROLE_ACCOUNTING")));
      UserAccount salesUser =
          ensureScopedUser(
              userAccountRepository,
              passwordEncoder,
              tenant,
              salesEmail,
              SALES_DISPLAY_NAME,
              tenantUserPassword,
              List.of(roles.get("ROLE_SALES")));
      UserAccount factoryUser =
          ensureScopedUser(
              userAccountRepository,
              passwordEncoder,
              tenant,
              factoryEmail,
              FACTORY_DISPLAY_NAME,
              tenantUserPassword,
              List.of(roles.get("ROLE_FACTORY")));
      UserAccount dealerUser =
          ensureScopedUser(
              userAccountRepository,
              passwordEncoder,
              tenant,
              dealerEmail,
              DEALER_DISPLAY_NAME,
              tenantUserPassword,
              List.of(roles.get("ROLE_DEALER")));
      synchronizeTenantAdminPointers(companyRepository, tenant, tenantAdmin);

      SeedFixture fixture =
          withCompanyContext(
              tenant.getCode(),
              () ->
                  ensureBusinessFixture(
                      tenant,
                      accountRepository,
                      dealerRepository,
                      productionBrandRepository,
                      productionProductRepository,
                      finishedGoodRepository,
                      finishedGoodBatchRepository,
                      salesService,
                      salesFulfillmentService,
                      salesOrderRepository,
                      invoiceRepository,
                      dealerUser));

      log.info(
          "UAT runtime seed ready for company {} with users [{} | {} | {} | {} | {}], dealerId={},"
              + " finishedGoodId={}, batches=[{}, {}], draftOrderId={}, fulfilledOrderId={},"
              + " invoiceId={}",
          tenant.getCode(),
          tenantAdmin.getEmail(),
          accountingUser.getEmail(),
          salesUser.getEmail(),
          factoryUser.getEmail(),
          dealerUser.getEmail(),
          fixture.dealer().getId(),
          fixture.finishedGood().getId(),
          fixture.primaryBatch().getId(),
          fixture.secondaryBatch().getId(),
          fixture.draftOrder().getId(),
          fixture.fulfilledOrder().getId(),
          fixture.invoice() != null ? fixture.invoice().getId() : null);
    };
  }

  private Map<String, Role> loadRequiredRoles(RoleRepository roleRepository) {
    return List.of(
            "ROLE_ADMIN",
            "ROLE_SUPER_ADMIN",
            "ROLE_ACCOUNTING",
            "ROLE_SALES",
            "ROLE_FACTORY",
            "ROLE_DEALER")
        .stream()
        .collect(
            HashMap::new,
            (map, roleName) ->
                map.put(
                    roleName,
                    roleRepository
                        .findByName(roleName)
                        .orElseThrow(
                            () ->
                                new IllegalStateException(
                                    "Required role missing after synchronization: " + roleName))),
            HashMap::putAll);
  }

  private void ensurePlatformAdmin(
      UserAccountRepository userAccountRepository,
      PasswordEncoder passwordEncoder,
      String platformScopeCode,
      String email,
      String rawPassword,
      Role adminRole,
      Role superAdminRole) {
    String normalizedEmail = normalizeEmail(email);
    UserAccount user =
        userAccountRepository
            .findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(normalizedEmail, platformScopeCode)
            .orElseGet(
                () ->
                    new UserAccount(
                        normalizedEmail,
                        platformScopeCode,
                        passwordEncoder.encode(rawPassword),
                        PLATFORM_ADMIN_DISPLAY_NAME));
    user.setEmail(normalizedEmail);
    user.setAuthScopeCode(platformScopeCode);
    user.setDisplayName(PLATFORM_ADMIN_DISPLAY_NAME);
    user.setPasswordHash(passwordEncoder.encode(rawPassword));
    user.setEnabled(true);
    user.setMustChangePassword(false);
    user.setFailedLoginAttempts(0);
    user.setLockedUntil(null);
    user.clearCompany();
    user.addRole(adminRole);
    user.addRole(superAdminRole);
    userAccountRepository.save(user);
  }

  private Company ensureTenant(
      CompanyRepository companyRepository,
      TenantOnboardingService tenantOnboardingService,
      String tenantName,
      String tenantCode,
      String tenantTimezone,
      long maxActiveUsers,
      long maxApiRequests,
      long maxConcurrentRequests,
      String tenantAdminEmail) {
    Company existing = companyRepository.findByCodeIgnoreCase(tenantCode).orElse(null);
    if (existing != null) {
      existing.setName(tenantName);
      existing.setTimezone(tenantTimezone);
      existing.setBaseCurrency("INR");
      return companyRepository.save(existing);
    }

    tenantOnboardingService.onboardTenant(
        new TenantOnboardingRequest(
            tenantName,
            tenantCode,
            tenantTimezone,
            TENANT_DEFAULT_GST_RATE,
            maxActiveUsers,
            maxApiRequests,
            0L,
            maxConcurrentRequests,
            false,
            true,
            normalizeEmail(tenantAdminEmail),
            TENANT_ADMIN_DISPLAY_NAME,
            MANUFACTURING_TEMPLATE));

    return companyRepository
        .findByCodeIgnoreCase(tenantCode)
        .orElseThrow(() -> new IllegalStateException("Seeded tenant not found: " + tenantCode));
  }

  private UserAccount ensureScopedUser(
      UserAccountRepository userAccountRepository,
      PasswordEncoder passwordEncoder,
      Company company,
      String email,
      String displayName,
      String rawPassword,
      List<Role> roles) {
    String normalizedEmail = normalizeEmail(email);
    String scopeCode = company.getCode();
    UserAccount user =
        userAccountRepository
            .findByEmailIgnoreCaseAndAuthScopeCodeIgnoreCase(normalizedEmail, scopeCode)
            .orElseGet(
                () ->
                    new UserAccount(
                        normalizedEmail,
                        scopeCode,
                        passwordEncoder.encode(rawPassword),
                        displayName));
    user.setEmail(normalizedEmail);
    user.setAuthScopeCode(scopeCode);
    user.setDisplayName(displayName);
    user.setPasswordHash(passwordEncoder.encode(rawPassword));
    user.setEnabled(true);
    user.setMustChangePassword(false);
    user.setFailedLoginAttempts(0);
    user.setLockedUntil(null);
    user.setCompany(company);
    user.getRoles().clear();
    roles.forEach(user::addRole);
    return userAccountRepository.save(user);
  }

  private void synchronizeTenantAdminPointers(
      CompanyRepository companyRepository, Company company, UserAccount tenantAdmin) {
    company.setMainAdminUserId(tenantAdmin.getId());
    company.setOnboardingAdminUserId(tenantAdmin.getId());
    company.setOnboardingAdminEmail(tenantAdmin.getEmail());
    if (company.getOnboardingCompletedAt() == null) {
      company.setOnboardingCompletedAt(Instant.now());
    }
    companyRepository.save(company);
  }

  private SeedFixture ensureBusinessFixture(
      Company company,
      AccountRepository accountRepository,
      DealerRepository dealerRepository,
      ProductionBrandRepository productionBrandRepository,
      ProductionProductRepository productionProductRepository,
      FinishedGoodRepository finishedGoodRepository,
      FinishedGoodBatchRepository finishedGoodBatchRepository,
      SalesService salesService,
      SalesFulfillmentService salesFulfillmentService,
      SalesOrderRepository salesOrderRepository,
      InvoiceRepository invoiceRepository,
      UserAccount dealerUser) {
    Map<String, Account> accounts = resolveAccounts(company, accountRepository);
    Dealer dealer = ensureDealer(company, salesService, dealerRepository, dealerUser);
    ProductionBrand brand = ensureBrand(company, productionBrandRepository);
    ProductionProduct product =
        ensureProductionProduct(company, brand, accounts, productionProductRepository);
    FinishedGood finishedGood =
        ensureFinishedGood(company, product, accounts, finishedGoodRepository);
    boolean fulfilledOrderAlreadyIssued =
        salesOrderRepository
            .findByCompanyAndIdempotencyKey(company, FULFILL_ORDER_IDEMPOTENCY_KEY)
            .map(SalesOrder::hasInvoiceIssued)
            .orElse(false);
    FinishedGoodBatch primaryBatch =
        ensureBatchStock(
            company,
            finishedGood,
            PRIMARY_BATCH_CODE,
            fulfilledOrderAlreadyIssued
                ? PRIMARY_BATCH_QUANTITY.subtract(FULFILL_ORDER_QUANTITY)
                : PRIMARY_BATCH_QUANTITY,
            PRIMARY_BATCH_UNIT_COST,
            finishedGoodBatchRepository);
    FinishedGoodBatch secondaryBatch =
        ensureBatchStock(
            company,
            finishedGood,
            SECONDARY_BATCH_CODE,
            SECONDARY_BATCH_QUANTITY,
            SECONDARY_BATCH_UNIT_COST,
            finishedGoodBatchRepository);
    normalizeFinishedGoodStock(finishedGood, finishedGoodRepository, finishedGoodBatchRepository);

    SalesOrder draftOrder =
        ensureDraftOrder(salesService, salesOrderRepository, dealer, finishedGood, company);
    SalesOrder fulfilledOrder =
        ensureFulfilledOrder(
            salesService,
            salesFulfillmentService,
            salesOrderRepository,
            dealer,
            finishedGood,
            company);
    Invoice invoice =
        invoiceRepository.findByCompanyAndSalesOrderId(company, fulfilledOrder.getId()).orElse(null);
    return new SeedFixture(
        dealer,
        product,
        finishedGood,
        primaryBatch,
        secondaryBatch,
        draftOrder,
        fulfilledOrder,
        invoice);
  }

  private Map<String, Account> resolveAccounts(Company company, AccountRepository accountRepository) {
    return Map.of(
        "INV", requireAccount(company, "INV", accountRepository),
        "COGS", requireAccount(company, "COGS", accountRepository),
        "REV", requireAccount(company, "REV", accountRepository),
        "DISC", requireAccount(company, "DISC", accountRepository),
        "GST-OUT", requireAccount(company, "GST-OUT", accountRepository));
  }

  private Account requireAccount(
      Company company, String code, AccountRepository accountRepository) {
    return accountRepository
        .findByCompanyAndCodeIgnoreCase(company, code)
        .orElseThrow(
            () ->
                new IllegalStateException(
                    "Required account missing for seeded company " + company.getCode() + ": " + code));
  }

  private Dealer ensureDealer(
      Company company,
      SalesService salesService,
      DealerRepository dealerRepository,
      UserAccount dealerUser) {
    salesService.createDealer(
        new DealerRequest(
            DEALER_NAME,
            DEALER_CODE,
            dealerUser.getEmail(),
            "+91-9000000001",
            DEALER_CREDIT_LIMIT,
            DEALER_GST,
            DEALER_STATE_CODE,
            com.bigbrightpaints.erp.modules.accounting.domain.GstRegistrationType.REGULAR));
    Dealer dealer =
        dealerRepository
            .findByCompanyAndCodeIgnoreCase(company, DEALER_CODE)
            .orElseThrow(
                () ->
                    new IllegalStateException(
                        "Seeded dealer not found for company " + company.getCode()));
    dealer.setPortalUser(dealerUser);
    dealer.setCompanyName(DEALER_NAME);
    dealer.setAddress("Local UAT Dealer Address");
    dealer.setRegion("WEST");
    dealer.setStatus("ACTIVE");
    return dealerRepository.save(dealer);
  }

  private ProductionBrand ensureBrand(
      Company company, ProductionBrandRepository productionBrandRepository) {
    ProductionBrand brand =
        productionBrandRepository
            .findByCompanyAndCodeIgnoreCase(company, BRAND_CODE)
            .orElseGet(ProductionBrand::new);
    brand.setCompany(company);
    brand.setCode(BRAND_CODE);
    brand.setName(BRAND_NAME);
    brand.setDescription("UAT runtime seeded brand");
    brand.setActive(true);
    return productionBrandRepository.save(brand);
  }

  private ProductionProduct ensureProductionProduct(
      Company company,
      ProductionBrand brand,
      Map<String, Account> accounts,
      ProductionProductRepository productionProductRepository) {
    ProductionProduct product =
        productionProductRepository
            .findByCompanyAndSkuCodeIgnoreCase(company, PRODUCT_SKU)
            .orElseGet(ProductionProduct::new);
    product.setCompany(company);
    product.setBrand(brand);
    product.setSkuCode(PRODUCT_SKU);
    product.setProductName(PRODUCT_NAME);
    product.setCategory("FINISHED_GOOD");
    product.setDefaultColour("Neutral White");
    product.setSizeLabel("20L");
    product.setUnitOfMeasure("UNIT");
    product.setProductFamilyName("UAT Exterior Range");
    product.setHsnCode("3209");
    product.setActive(true);
    product.setBasePrice(UNIT_SELLING_PRICE);
    product.setGstRate(TENANT_DEFAULT_GST_RATE);
    product.setMinDiscountPercent(BigDecimal.ZERO);
    product.setMinSellingPrice(UNIT_SELLING_PRICE);
    product.setColors(new java.util.LinkedHashSet<>(List.of("Neutral White")));
    product.setSizes(new java.util.LinkedHashSet<>(List.of("20L")));
    product.setCartonSizes(Map.of("20L", 1));
    Map<String, Object> metadata =
        product.getMetadata() == null ? new HashMap<>() : new HashMap<>(product.getMetadata());
    metadata.put("fgValuationAccountId", accounts.get("INV").getId());
    metadata.put("fgCogsAccountId", accounts.get("COGS").getId());
    metadata.put("fgRevenueAccountId", accounts.get("REV").getId());
    metadata.put("fgDiscountAccountId", accounts.get("DISC").getId());
    metadata.put("fgTaxAccountId", accounts.get("GST-OUT").getId());
    product.setMetadata(metadata);
    return productionProductRepository.save(product);
  }

  private FinishedGood ensureFinishedGood(
      Company company,
      ProductionProduct product,
      Map<String, Account> accounts,
      FinishedGoodRepository finishedGoodRepository) {
    FinishedGood finishedGood =
        finishedGoodRepository
            .findByCompanyAndProductCodeIgnoreCase(company, product.getSkuCode())
            .orElseGet(FinishedGood::new);
    finishedGood.setCompany(company);
    finishedGood.setProductCode(product.getSkuCode());
    finishedGood.setName(product.getProductName());
    finishedGood.setUnit("UNIT");
    finishedGood.setCostingMethod("FIFO");
    finishedGood.setValuationAccountId(accounts.get("INV").getId());
    finishedGood.setCogsAccountId(accounts.get("COGS").getId());
    finishedGood.setRevenueAccountId(accounts.get("REV").getId());
    finishedGood.setDiscountAccountId(accounts.get("DISC").getId());
    finishedGood.setTaxAccountId(accounts.get("GST-OUT").getId());
    finishedGood.setLowStockThreshold(new BigDecimal("25"));
    if (finishedGood.getCurrentStock() == null) {
      finishedGood.setCurrentStock(BigDecimal.ZERO);
    }
    if (finishedGood.getReservedStock() == null) {
      finishedGood.setReservedStock(BigDecimal.ZERO);
    }
    return finishedGoodRepository.save(finishedGood);
  }

  private FinishedGoodBatch ensureBatchStock(
      Company company,
      FinishedGood finishedGood,
      String batchCode,
      BigDecimal targetQuantity,
      BigDecimal unitCost,
      FinishedGoodBatchRepository finishedGoodBatchRepository) {
    FinishedGoodBatch batch =
        finishedGoodBatchRepository
            .findByFinishedGoodAndBatchCode(finishedGood, batchCode)
            .orElseGet(FinishedGoodBatch::new);
    batch.setFinishedGood(finishedGood);
    batch.setBatchCode(batchCode);
    batch.setUnitCost(unitCost);
    batch.setManufacturedAt(CompanyTime.now(company).minusSeconds(3600));
    batch.setExpiryDate(LocalDate.now().plusMonths(18));
    BigDecimal currentAvailable =
        Optional.ofNullable(batch.getQuantityAvailable()).orElse(BigDecimal.ZERO);
    BigDecimal currentTotal = Optional.ofNullable(batch.getQuantityTotal()).orElse(BigDecimal.ZERO);
    BigDecimal requiredIncrease = targetQuantity.subtract(currentAvailable);
    if (requiredIncrease.compareTo(BigDecimal.ZERO) > 0) {
      batch.setQuantityAvailable(currentAvailable.add(requiredIncrease));
      batch.setQuantityTotal(currentTotal.max(currentAvailable).add(requiredIncrease));
    } else {
      batch.setQuantityAvailable(currentAvailable);
      batch.setQuantityTotal(currentTotal.max(currentAvailable));
    }
    return finishedGoodBatchRepository.save(batch);
  }

  private void normalizeFinishedGoodStock(
      FinishedGood finishedGood,
      FinishedGoodRepository finishedGoodRepository,
      FinishedGoodBatchRepository finishedGoodBatchRepository) {
    BigDecimal available =
        finishedGoodBatchRepository.findByFinishedGoodOrderByManufacturedAtAsc(finishedGood).stream()
            .map(FinishedGoodBatch::getQuantityAvailable)
            .filter(java.util.Objects::nonNull)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    finishedGood.setReservedStock(BigDecimal.ZERO);
    finishedGood.setCurrentStock(available);
    finishedGoodRepository.save(finishedGood);
  }

  private SalesOrder ensureDraftOrder(
      SalesService salesService,
      SalesOrderRepository salesOrderRepository,
      Dealer dealer,
      FinishedGood finishedGood,
      Company company) {
    SalesOrderDto orderDto =
        salesService.createOrder(
            new SalesOrderRequest(
                dealer.getId(),
                DRAFT_ORDER_QUANTITY.multiply(UNIT_SELLING_PRICE),
                "INR",
                "UAT seeded draft order",
                List.of(
                    new SalesOrderItemRequest(
                        finishedGood.getId(),
                        PRODUCT_SKU,
                        PRODUCT_NAME,
                        DRAFT_ORDER_QUANTITY,
                        UNIT_SELLING_PRICE,
                        TENANT_DEFAULT_GST_RATE)),
                "NONE",
                TENANT_DEFAULT_GST_RATE,
                Boolean.FALSE,
                DRAFT_ORDER_IDEMPOTENCY_KEY,
                "CREDIT",
                "NET_30"));
    return salesOrderRepository
        .findByCompanyAndId(company, orderDto.id())
        .orElseThrow(() -> new IllegalStateException("Seeded draft order not found"));
  }

  private SalesOrder ensureFulfilledOrder(
      SalesService salesService,
      SalesFulfillmentService salesFulfillmentService,
      SalesOrderRepository salesOrderRepository,
      Dealer dealer,
      FinishedGood finishedGood,
      Company company) {
    SalesOrderDto orderDto =
        salesService.createOrder(
            new SalesOrderRequest(
                dealer.getId(),
                FULFILL_ORDER_QUANTITY.multiply(UNIT_SELLING_PRICE),
                "INR",
                "UAT seeded fulfilled order",
                List.of(
                    new SalesOrderItemRequest(
                        finishedGood.getId(),
                        PRODUCT_SKU,
                        PRODUCT_NAME,
                        FULFILL_ORDER_QUANTITY,
                        UNIT_SELLING_PRICE,
                        TENANT_DEFAULT_GST_RATE)),
                "NONE",
                TENANT_DEFAULT_GST_RATE,
                Boolean.FALSE,
                FULFILL_ORDER_IDEMPOTENCY_KEY,
                "CREDIT",
                "NET_30"));

    SalesOrder order =
        salesOrderRepository
            .findByCompanyAndId(company, orderDto.id())
            .orElseThrow(() -> new IllegalStateException("Seeded fulfillment order not found"));
    if ("DRAFT".equalsIgnoreCase(order.getStatus())) {
      salesService.confirmOrder(order.getId());
      order =
          salesOrderRepository
              .findByCompanyAndId(company, order.getId())
              .orElseThrow(() -> new IllegalStateException("Confirmed order not found"));
    }
    if (!order.hasInvoiceIssued()
        && !"SHIPPED".equalsIgnoreCase(order.getStatus())
        && !"FULFILLED".equalsIgnoreCase(order.getStatus())
        && !"COMPLETED".equalsIgnoreCase(order.getStatus())) {
      salesFulfillmentService.fulfillOrder(order.getId());
    }
    return salesOrderRepository
        .findByCompanyAndId(company, order.getId())
        .orElseThrow(() -> new IllegalStateException("Fulfilled order not found after seeding"));
  }

  private <T> T withCompanyContext(String companyCode, java.util.function.Supplier<T> supplier) {
    CompanyContextHolder.setCompanyCode(companyCode);
    try {
      return supplier.get();
    } finally {
      CompanyContextHolder.clear();
    }
  }

  private String normalizeEmail(String email) {
    if (!StringUtils.hasText(email)) {
      throw new IllegalStateException("Seed email must not be blank");
    }
    return email.trim().toLowerCase(Locale.ROOT);
  }

  private record SeedFixture(
      Dealer dealer,
      ProductionProduct product,
      FinishedGood finishedGood,
      FinishedGoodBatch primaryBatch,
      FinishedGoodBatch secondaryBatch,
      SalesOrder draftOrder,
      SalesOrder fulfilledOrder,
      Invoice invoice) {}
}
