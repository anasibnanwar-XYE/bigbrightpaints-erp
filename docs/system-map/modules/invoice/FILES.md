# INVOICE Module Map

`erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice` + related tests + SQL.

## Files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/controller/InvoiceController.java | REST API entrypoint for module endpoints
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/domain/Invoice.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/domain/InvoiceLine.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/domain/InvoiceRepository.java | JPA repository for Invoice persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/domain/InvoiceSequence.java | domain entity for module persistence state
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/domain/InvoiceSequenceRepository.java | JPA repository for InvoiceSequence persistence access
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/dto/InvoiceDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/dto/InvoiceLineDto.java | request/response DTO definitions
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/service/InvoiceNumberService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/service/InvoicePdfService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/service/InvoiceService.java | business service logic for module workflows
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/service/InvoiceSettlementPolicy.java | business service logic for module workflows
erp-domain/src/main/resources/db/migration/V107__invoice_line_tax_discount_columns.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V12__invoices.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V26__invoice_journal_link.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V56__invoice_purchase_outstanding.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V59__invoice_payment_refs.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V89__packaging_slip_invoice_link.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration/V90__packaging_slip_invoice_unique.sql | Flyway schema migration file
erp-domain/src/main/resources/db/migration_v2/V3__sales_invoice.sql | Flyway schema migration file
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/invoice/service/InvoiceServiceTest.java | module test coverage
erp-domain/src/test/java/com/bigbrightpaints/erp/modules/invoice/service/InvoiceSettlementPolicyTest.java | module test coverage

## Entrypoint files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/controller/InvoiceController.java | Invoice CRUD and retrieval API
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/service/InvoiceService.java | Invoice orchestration entrypoint

## High-risk files
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/service/InvoiceService.java | Invoice lifecycle and accounting linkage
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/service/InvoiceSettlementPolicy.java | Settlement math and policy
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/service/InvoicePdfService.java | Invoice document generation
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/domain/InvoiceRepository.java | Invoice persistence and uniqueness
erp-domain/src/main/java/com/bigbrightpaints/erp/modules/invoice/domain/Invoice.java | Core monetary totals and status
