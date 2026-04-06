package com.bigbrightpaints.erp.modules.accounting.service;

import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalLineRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocationRepository;
import com.bigbrightpaints.erp.modules.accounting.dto.AccountingTransactionAuditDetailDto;
import com.bigbrightpaints.erp.modules.accounting.dto.AccountingTransactionAuditListItemDto;
import com.bigbrightpaints.erp.modules.accounting.event.AccountingEventRepository;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlipRepository;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseRepository;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

@Service
public class AccountingAuditTrailService {

  private final AccountingAuditTrailClassifier classifier;
  private final SettlementAuditMemoDecoder settlementAuditMemoDecoder;
  private final AccountingAuditTrailReferenceChainService referenceChainService;
  private final AccountingAuditTrailTransactionQueryService transactionQueryService;
  private final AccountingAuditTrailTransactionDetailService transactionDetailService;

  public AccountingAuditTrailService(
      CompanyContextService companyContextService,
      JournalEntryRepository journalEntryRepository,
      JournalLineRepository journalLineRepository,
      AccountingEventRepository accountingEventRepository,
      PartnerSettlementAllocationRepository settlementAllocationRepository,
      InvoiceRepository invoiceRepository,
      RawMaterialPurchaseRepository rawMaterialPurchaseRepository,
      PackagingSlipRepository packagingSlipRepository) {
    this.classifier = new AccountingAuditTrailClassifier();
    this.settlementAuditMemoDecoder = new SettlementAuditMemoDecoder();
    this.referenceChainService =
        new AccountingAuditTrailReferenceChainService(
            invoiceRepository, settlementAllocationRepository, packagingSlipRepository);
    this.transactionQueryService =
        new AccountingAuditTrailTransactionQueryService(
            companyContextService,
            journalEntryRepository,
            journalLineRepository,
            settlementAllocationRepository,
            invoiceRepository,
            rawMaterialPurchaseRepository,
            classifier);
    this.transactionDetailService =
        new AccountingAuditTrailTransactionDetailService(
            companyContextService,
            journalEntryRepository,
            accountingEventRepository,
            settlementAllocationRepository,
            invoiceRepository,
            rawMaterialPurchaseRepository,
            referenceChainService,
            classifier,
            settlementAuditMemoDecoder);
  }

  @Transactional(readOnly = true)
  public PageResponse<AccountingTransactionAuditListItemDto> listTransactions(
      java.time.LocalDate from,
      java.time.LocalDate to,
      String module,
      String status,
      String referenceNumber,
      int page,
      int size) {
    return transactionQueryService.listTransactions(
        from, to, module, status, referenceNumber, page, size);
  }

  @Transactional(readOnly = true)
  public AccountingTransactionAuditDetailDto transactionDetail(Long journalEntryId) {
    return transactionDetailService.transactionDetail(journalEntryId);
  }

  SettlementAuditMemoDecoder.DecodedSettlementAuditMemo decodeSettlementAuditMemo(
      com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocation allocation) {
    return settlementAuditMemoDecoder.decode(allocation);
  }

  AccountingAuditTrailClassifier.ConsistencyResult assessConsistency(
      com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry entry,
      java.util.List<com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocation>
          allocations,
      java.math.BigDecimal totalDebit,
      java.math.BigDecimal totalCredit) {
    return classifier.assessConsistency(entry, allocations, totalDebit, totalCredit);
  }

  java.util.List<String> moduleReferencePrefixes(String module) {
    return classifier.moduleReferencePrefixes(module);
  }

  String deriveTransactionType(
      com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry entry,
      com.bigbrightpaints.erp.modules.invoice.domain.Invoice invoice,
      com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchase purchase,
      java.util.List<com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocation>
          allocations) {
    return classifier.deriveTransactionType(entry, invoice, purchase, allocations);
  }

  String deriveModule(String transactionType, String referenceNumber) {
    return classifier.deriveModule(transactionType, referenceNumber);
  }

  com.bigbrightpaints.erp.shared.dto.LinkedBusinessReferenceDto resolveDrivingDocument(
      com.bigbrightpaints.erp.modules.invoice.domain.Invoice invoice,
      com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchase purchase,
      java.util.List<com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocation>
          allocations) {
    return referenceChainService.resolveDrivingDocument(invoice, purchase, allocations);
  }

  java.util.List<com.bigbrightpaints.erp.shared.dto.LinkedBusinessReferenceDto> buildReferenceChain(
      com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry entry,
      com.bigbrightpaints.erp.modules.invoice.domain.Invoice invoice,
      com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchase purchase,
      java.util.List<com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocation>
          allocations,
      com.bigbrightpaints.erp.shared.dto.LinkedBusinessReferenceDto drivingDocument) {
    return referenceChainService.buildReferenceChain(
        entry, invoice, purchase, allocations, drivingDocument);
  }

  void appendSettlementReferences(
      java.util.List<com.bigbrightpaints.erp.shared.dto.LinkedBusinessReferenceDto> chain,
      com.bigbrightpaints.erp.modules.company.domain.Company company,
      com.bigbrightpaints.erp.modules.invoice.domain.Invoice invoice,
      com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchase purchase) {
    referenceChainService.appendSettlementReferences(chain, company, invoice, purchase);
  }

  int resolveCurrentSalesOrderInvoiceCount(
      com.bigbrightpaints.erp.modules.invoice.domain.Invoice invoice) {
    return referenceChainService.resolveCurrentSalesOrderInvoiceCount(invoice);
  }

  java.util.Map<Long, com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchase>
      findPurchasesByJournalEntryIds(
          com.bigbrightpaints.erp.modules.company.domain.Company company,
          java.util.List<Long> journalEntryIds) {
    return transactionQueryService.findPurchasesByJournalEntryIds(company, journalEntryIds);
  }

  org.springframework.data.jpa.domain.Specification<
          com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry>
      byStatus(String status) {
    return transactionQueryService.byStatus(status);
  }

  org.springframework.data.jpa.domain.Specification<
          com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry>
      byReference(String reference) {
    return transactionQueryService.byReference(reference);
  }

  org.springframework.data.jpa.domain.Specification<
          com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry>
      byModule(String module) {
    return transactionQueryService.byModule(module);
  }
}
