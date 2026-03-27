# Core Infrastructure: Configuration Classes

This document covers all configuration classes in `com.bigbrightpaints.erp.core.config`.

## Overview

Configuration classes handle Spring Boot application configuration, data initialization, and runtime settings management. They provide properties bindings, async configuration, OpenAPI setup, and system settings.

---

## AsyncConfig

| Field | Value |
|-------|-------|
| **Name** | AsyncConfig |
| **Type** | Configuration (Spring @Configuration) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/AsyncConfig.java |
| **Responsibility** | Configures async task executor with company context propagation |
| **Use when** | Understanding @Async method behavior |
| **Do not use when** | N/A (infrastructure) |
| **Public methods** | `@Bean Executor taskExecutor()` |
| **Callers** | Spring @Async infrastructure |
| **Dependencies** | CompanyContextTaskDecorator |
| **Side effects** | Creates thread pool: core=4, max=10, queue=100 |
| **Invariants protected** | CompanyContext and SecurityContext propagate to async threads |
| **Status** | Canonical |

### Thread Pool Configuration

```properties
erp.async.core-pool-size=4
erp.async.max-pool-size=10
erp.async.queue-capacity=100
erp.async.thread-name-prefix=erp-async-
```

---

## CompanyContextTaskDecorator

| Field | Value |
|-------|-------|
| **Name** | CompanyContextTaskDecorator |
| **Type** | Utility (implements TaskDecorator) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/CompanyContextTaskDecorator.java |
| **Responsibility** | Propagates CompanyContext and SecurityContext to async threads |
| **Use when** | Understanding why @Async methods have company context |
| **Do not use when** | N/A (infrastructure) |
| **Public methods** | `Runnable decorate(Runnable runnable)` |
| **Callers** | AsyncConfig |
| **Dependencies** | CompanyContextHolder, SecurityContextHolder |
| **Side effects** | Clears context after task completion to prevent leaks |
| **Invariants protected** | Context isolation between tasks |
| **Status** | Canonical |

---

## DataInitializer

| Field | Value |
|-------|-------|
| **Name** | DataInitializer |
| **Type** | Configuration (Spring @Configuration) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/DataInitializer.java |
| **Responsibility** | Seeds default users, companies, roles, and accounts for dev/test environments |
| **Use when** | Understanding bootstrap user/company creation |
| **Do not use when** | Production (disabled in prod profile) |
| **Public methods** | `@Bean CommandLineRunner seedDefaultUser(...)` |
| **Callers** | Spring Boot startup |
| **Dependencies** | UserAccountRepository, CompanyRepository, RoleRepository, AccountRepository, PasswordEncoder |
| **Side effects** | Creates BBP company, admin/super-admin users, default accounts |
| **Invariants protected** | Idempotent seeding; password validation |
| **Status** | Scoped (dev/test only) |

### Configuration Properties

```properties
erp.seed.super-admin.email=
erp.seed.super-admin.password=
erp.seed.super-admin.company-code=SKE
erp.seed.dev-admin.email=
erp.seed.dev-admin.password=
```

### Default Accounts Seeded

| Code | Name | Type |
|------|------|------|
| CASH | Cash | ASSET |
| AR | Accounts Receivable | ASSET |
| AP | Accounts Payable | LIABILITY |
| INV | Inventory | ASSET |
| COGS | Cost of Goods Sold | COGS |
| REV | Revenue | REVENUE |
| GST-IN | GST Input Tax | ASSET |
| GST-OUT | GST Output Tax | LIABILITY |
| GST-PAY | GST Payable | LIABILITY |
| DISC | Discounts | EXPENSE |
| WIP | Work in Progress | ASSET |
| OPEX | Operating Expenses | EXPENSE |

---

## BbpSampleDataInitializer

| Field | Value |
|-------|-------|
| **Name** | BbpSampleDataInitializer |
| **Type** | Configuration (Spring @Configuration) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/BbpSampleDataInitializer.java |
| **Responsibility** | Seeds BBP-specific raw materials, finished goods, and batches for demo/testing |
| **Use when** | Understanding BBP demo data |
| **Do not use when** | Production |
| **Public methods** | `@Bean CommandLineRunner seedBbpSamples(...)` |
| **Callers** | Spring Boot startup |
| **Dependencies** | Multiple repositories |
| **Side effects** | Creates BBP brand, raw materials (RM-RESIN, RM-TIO2, RM-SOLV, RM-CAN), finished goods (BBP-ENAMEL-WH-1L, BBP-ENAMEL-WH-4L) |
| **Invariants protected** | Only runs if company code "BBP" exists |
| **Status** | Scoped (dev/test only) |

---

## BenchmarkDataInitializer

| Field | Value |
|-------|-------|
| **Name** | BenchmarkDataInitializer |
| **Type** | Configuration (Spring @Configuration) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/BenchmarkDataInitializer.java |
| **Responsibility** | Creates clean master data for BBP without journal entries for benchmark testing |
| **Use when** | Running COGS/inventory costing benchmarks |
| **Do not use when** | Regular development (use MockDataInitializer) |
| **Public methods** | `@Bean CommandLineRunner seedBenchmarkData(...)` |
| **Callers** | Spring Boot startup |
| **Dependencies** | Multiple repositories |
| **Side effects** | Creates BBP company, suppliers, dealers, accounts, products |
| **Invariants protected** | NO pre-seeded journal entries or inventory batches |
| **Status** | Scoped (benchmark profile only) |

### Products Created

| SKU | Type | Description |
|-----|------|-------------|
| RM-WB | Raw Material | White Base |
| RM-TIO2 | Raw Material | Titanium Dioxide |
| RM-BINDER | Raw Material | Acrylic Binder |
| RM-COLORANT | Raw Material | Color Pigment |
| PK-5L-CAN | Packaging | 5L Metal Can |
| PK-10L-CAN | Packaging | 10L Metal Can |
| PK-LABEL | Packaging | Product Label |
| SF-PEE | Semi-Finished | Premium Enamel (Bulk) |
| FG-PEE-5L | Finished Good | Premium Enamel 5L |
| FG-PEE-10L | Finished Good | Premium Enamel 10L |

---

## MockDataInitializer

| Field | Value |
|-------|-------|
| **Name** | MockDataInitializer |
| **Type** | Configuration (Spring @Configuration) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/MockDataInitializer.java |
| **Responsibility** | Seeds complete training environment with mock company, users, inventory, and journals |
| **Use when** | Setting up training/testing environment |
| **Do not use when** | Production or benchmark testing |
| **Public methods** | `@Bean CommandLineRunner seedMockData(...)` |
| **Callers** | Spring Boot startup |
| **Dependencies** | Multiple repositories, AccountingService |
| **Side effects** | Creates MOCK company with dealer, supplier, finished goods, batches, and sample journals |
| **Invariants protected** | Complete training data set with FIFO/LIFO examples |
| **Status** | Scoped (mock profile only) |

---

## CriticalFixtureInitializer

| Field | Value |
|-------|-------|
| **Name** | CriticalFixtureInitializer |
| **Type** | Configuration (Spring @Configuration) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/CriticalFixtureInitializer.java |
| **Responsibility** | Seeds critical fixtures for all companies via CriticalFixtureService |
| **Use when** | Understanding company-specific fixture requirements |
| **Do not use when** | Production |
| **Public methods** | `@Bean CommandLineRunner seedCriticalFixtures(...)` |
| **Callers** | Spring Boot startup |
| **Dependencies** | CompanyRepository, CriticalFixtureService |
| **Side effects** | Calls CriticalFixtureService.seedCompanyFixtures for each company |
| **Invariants protected** | Critical fixtures exist for all companies |
| **Status** | Scoped (test/mock/dev only) |

---

## ValidationSeedDataInitializer

| Field | Value |
|-------|-------|
| **Name** | ValidationSeedDataInitializer |
| **Type** | Configuration (Spring @Configuration) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/ValidationSeedDataInitializer.java |
| **Responsibility** | Seeds validation actors with strong password policy enforcement |
| **Use when** | Setting up validation testing actors |
| **Do not use when** | Production |
| **Public methods** | `@Bean CommandLineRunner seedValidationActors(...)` |
| **Callers** | Spring Boot startup |
| **Dependencies** | Multiple repositories, PasswordPolicy |
| **Side effects** | Creates MOCK, RIVAL, SKE companies with role-specific users |
| **Invariants protected** | Password policy enforced; mock profile required |
| **Status** | Scoped (validation-seed profile only) |

### Validation Actors Created

| Email | Role | Company |
|-------|------|---------|
| validation.admin@example.com | ADMIN, ACCOUNTING, SALES | MOCK |
| validation.accounting@example.com | ACCOUNTING | MOCK |
| validation.sales@example.com | SALES | MOCK |
| validation.factory@example.com | FACTORY | MOCK |
| validation.dealer@example.com | DEALER | MOCK |
| validation.rival.dealer@example.com | DEALER | RIVAL |
| validation.rival.admin@example.com | ADMIN | RIVAL |
| validation.superadmin@example.com | ADMIN, SUPER_ADMIN | SKE, MOCK |

---

## EmailProperties

| Field | Value |
|-------|-------|
| **Name** | EmailProperties |
| **Type** | Configuration Properties |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/EmailProperties.java |
| **Responsibility** | Binds email configuration properties from application.yml |
| **Use when** | Configuring email sending behavior |
| **Do not use when** | N/A |
| **Public methods** | Getters/setters for all properties |
| **Callers** | SystemSettingsService, email services |
| **Dependencies** | None |
| **Side effects** | None |
| **Invariants protected** | None |
| **Status** | Canonical |

### Properties

| Property | Default | Description |
|----------|---------|-------------|
| erp.mail.enabled | false | Enable email sending |
| erp.mail.from-address | noreply@bigbrightpaints.com | Sender address |
| erp.mail.base-url | http://localhost:3004 | Base URL for links |
| erp.mail.send-credentials | true | Send credential emails |
| erp.mail.send-password-reset | true | Send password reset emails |

---

## GitHubProperties

| Field | Value |
|-------|-------|
| **Name** | GitHubProperties |
| **Type** | Configuration Properties |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/GitHubProperties.java |
| **Responsibility** | Binds GitHub API configuration for issue tracking integration |
| **Use when** | Integrating with GitHub API |
| **Do not use when** | N/A |
| **Public methods** | Getters/setters; `boolean isConfigured()` |
| **Callers** | GitHub integration services |
| **Dependencies** | None |
| **Side effects** | None |
| **Invariants protected** | `isConfigured()` validates all required fields present |
| **Status** | Canonical |

### Properties

| Property | Description |
|----------|-------------|
| erp.github.enabled | Enable GitHub integration |
| erp.github.token | Personal access token |
| erp.github.repo-owner | Repository owner |
| erp.github.repo-name | Repository name |

---

## LicensingProperties

| Field | Value |
|-------|-------|
| **Name** | LicensingProperties |
| **Type** | Configuration Properties |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/LicensingProperties.java |
| **Responsibility** | Binds CryptoLens licensing configuration |
| **Use when** | Understanding license enforcement |
| **Do not use when** | N/A |
| **Public methods** | Getters/setters; `boolean hasLicenseKey()` |
| **Callers** | LicensingGuard |
| **Dependencies** | None |
| **Side effects** | None |
| **Invariants protected** | License key presence check |
| **Status** | Canonical |

### Properties

| Property | Default | Description |
|----------|---------|-------------|
| erp.licensing.product-id | 31720 | CryptoLens product ID |
| erp.licensing.license-key | null | License key string |
| erp.licensing.algorithm | SKM15 | Signing algorithm |
| erp.licensing.enforce | false | Fail-fast on missing license |
| erp.licensing.access-token | null | CryptoLens API token |

---

## JacksonConfig

| Field | Value |
|-------|-------|
| **Name** | JacksonConfig |
| **Type** | Configuration (Spring @Configuration) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/JacksonConfig.java |
| **Responsibility** | Configures Jackson ObjectMapper with JavaTimeModule |
| **Use when** | Understanding JSON serialization behavior |
| **Do not use when** | N/A |
| **Public methods** | `@Bean ObjectMapper objectMapper()` |
| **Callers** | Spring MVC, all JSON serialization |
| **Dependencies** | JavaTimeModule |
| **Side effects** | None |
| **Invariants protected** | Java 8 time types serialize correctly |
| **Status** | Canonical |

---

## OpenApiConfig

| Field | Value |
|-------|-------|
| **Name** | OpenApiConfig |
| **Type** | Configuration (Spring @Configuration) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/OpenApiConfig.java |
| **Responsibility** | Configures OpenAPI/Swagger documentation |
| **Use when** | Understanding API documentation setup |
| **Do not use when** | N/A |
| **Public methods** | `@Bean OpenAPI erpOpenApi()` |
| **Callers** | SpringDoc |
| **Dependencies** | None |
| **Side effects** | None |
| **Invariants protected** | None |
| **Status** | Canonical |

---

## OpenApiTaggingConfig

| Field | Value |
|-------|-------|
| **Name** | OpenApiTaggingConfig |
| **Type** | Configuration (Spring @Configuration) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/OpenApiTaggingConfig.java |
| **Responsibility** | Auto-tags API endpoints by module based on path prefix |
| **Use when** | Understanding API documentation organization |
| **Do not use when** | N/A |
| **Public methods** | `@Bean OpenApiCustomizer moduleTagCustomizer()` |
| **Callers** | SpringDoc |
| **Dependencies** | None |
| **Side effects** | None |
| **Invariants protected** | Consistent API tagging |
| **Status** | Canonical |

### Path-to-Tag Mapping

| Path Prefix | Tag |
|-------------|-----|
| /api/v1/admin/*, /api/v1/auth/*, /api/v1/companies/* | ADMIN |
| /api/v1/accounting/*, /api/v1/reports/*, /api/v1/inventory/* | ACCOUNTING |
| /api/v1/factory/*, /api/v1/production/* | FACTORY_PRODUCTION |
| /api/v1/sales/*, /api/v1/invoices/* | SALES |
| /api/v1/dealers/*, /api/v1/dealer-portal/* | DEALERS |

---

## SmtpPropertiesValidator

| Field | Value |
|-------|-------|
| **Name** | SmtpPropertiesValidator |
| **Type** | Configuration (Spring @Configuration) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/SmtpPropertiesValidator.java |
| **Responsibility** | Validates SMTP configuration on startup in production |
| **Use when** | Understanding production startup validation |
| **Do not use when** | N/A |
| **Public methods** | `@PostConstruct void validateSmtp()` |
| **Callers** | Spring container |
| **Dependencies** | Spring Environment |
| **Side effects** | Throws IllegalStateException on missing SMTP config |
| **Invariants protected** | Production requires valid SMTP configuration |
| **Status** | Canonical |

---

## SystemSetting

| Field | Value |
|-------|-------|
| **Name** | SystemSetting |
| **Type** | Entity (JPA) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/SystemSetting.java |
| **Responsibility** | Key-value store for runtime-tunable system settings |
| **Use when** | Persisting system-level configuration |
| **Do not use when** | Company-scoped settings |
| **Public methods** | Constructor, getters/setters |
| **Callers** | SystemSettingsRepository, SystemSettingsService |
| **Dependencies** | None |
| **Side effects** | Database writes |
| **Invariants protected** | Key uniqueness |
| **Status** | Canonical |

---

## SystemSettingsRepository

| Field | Value |
|-------|-------|
| **Name** | SystemSettingsRepository |
| **Type** | Repository (Spring Data JPA) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/SystemSettingsRepository.java |
| **Responsibility** | JPA repository for SystemSetting with atomic increment |
| **Use when** | Accessing/persisting system settings |
| **Do not use when** | N/A |
| **Public methods** | `void incrementLongSetting(String key)` (native UPSERT) |
| **Callers** | SystemSettingsService |
| **Dependencies** | SystemSetting entity |
| **Side effects** | Database writes |
| **Invariants protected** | Atomic counter increment |
| **Status** | Canonical |

---

## SystemSettingsService

| Field | Value |
|-------|-------|
| **Name** | SystemSettingsService |
| **Type** | Service (Spring Bean) |
| **Module** | core |
| **Package** | com.bigbrightpaints.erp.core.config |
| **File** | erp-domain/src/main/java/com/bigbrightpaints/erp/core/config/SystemSettingsService.java |
| **Responsibility** | Manages runtime-tunable settings with CORS validation |
| **Use when** | Reading/updating system settings |
| **Do not use when** | Company-scoped settings |
| **Public methods** | `List<String> getAllowedOrigins()`<br>`void setAllowedOrigins(List<String> origins)`<br>`boolean isAutoApprovalEnabled()`<br>`boolean isPeriodLockEnforced()`<br>`boolean isExportApprovalRequired()`<br>`SystemSettingsDto snapshot()`<br>`SystemSettingsDto update(SystemSettingsUpdateRequest request)`<br>`CorsConfiguration buildCorsConfiguration()` |
| **Callers** | AdminController, CorsConfig |
| **Dependencies** | EmailProperties, SystemSettingsRepository, Environment |
| **Side effects** | Database writes for persistence |
| **Invariants protected** | Production CORS validation (no wildcards, https-only); Tailscale HTTP support |
| **Status** | Canonical |

### Managed Settings

| Key | Default | Description |
|-----|---------|-------------|
| cors.allowed-origins | http://localhost:3002 | Allowed CORS origins |
| auto-approval.enabled | true | Auto-approve qualifying orders |
| period-lock.enforced | true | Enforce accounting period locks |
| export.require-approval | false | Require approval for exports |
