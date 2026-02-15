# COMPANY Module Map

`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company` + related tests + SQL.

## Files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/controller/CompanyController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/controller/MultiCompanyController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/domain/Company.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/domain/CompanyRepository.java | JPA repository for Company persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/dto/CompanyDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/dto/CompanyRequest.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/service/CompanyContextService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/service/CompanyService.java | business service logic for module workflows
erp-domain/src/main/resources/db/migration/V114__dealer_company_name.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V30__company_payroll_account_defaults.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V34__company_gst_defaults.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V83__company_default_accounts.sql | Flyway schema migration file
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/company/CompanyControllerIT.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/company/service/CompanyServiceTest.java | module test coverage

## Entrypoint files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/controller/CompanyController.java | Company CRUD API
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/controller/MultiCompanyController.java | Company switch/context API

## High-risk files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/service/CompanyService.java | Tenant-boundary critical logic
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/service/CompanyContextService.java | Context resolution and tenant scoping
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/company/domain/CompanyRepository.java | Tenant isolation at persistence boundary
