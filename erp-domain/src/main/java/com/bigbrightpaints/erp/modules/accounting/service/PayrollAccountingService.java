package com.bigbrightpaints.erp.modules.accounting.service;

import java.time.LocalDate;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.config.SystemSettingsService;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalReferenceMappingRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocationRepository;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.PayrollPaymentRequest;
import com.bigbrightpaints.erp.modules.accounting.event.AccountingEventStore;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRunLineRepository;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRunRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovementRepository;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceRepository;
import com.bigbrightpaints.erp.modules.invoice.service.InvoiceSettlementPolicy;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.SupplierRepository;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;

import jakarta.persistence.EntityManager;

@Service
public class PayrollAccountingService extends AccountingCoreEngineCore {

  private final JournalEntryService journalEntryService;

  @Autowired
  public PayrollAccountingService(
      CompanyContextService companyContextService,
      AccountRepository accountRepository,
      JournalEntryRepository journalEntryRepository,
      DealerLedgerService dealerLedgerService,
      SupplierLedgerService supplierLedgerService,
      PayrollRunRepository payrollRunRepository,
      PayrollRunLineRepository payrollRunLineRepository,
      AccountingPeriodService accountingPeriodService,
      ReferenceNumberService referenceNumberService,
      ApplicationEventPublisher eventPublisher,
      CompanyClock companyClock,
      CompanyEntityLookup companyEntityLookup,
      PartnerSettlementAllocationRepository settlementAllocationRepository,
      RawMaterialPurchaseRepository rawMaterialPurchaseRepository,
      InvoiceRepository invoiceRepository,
      RawMaterialMovementRepository rawMaterialMovementRepository,
      RawMaterialBatchRepository rawMaterialBatchRepository,
      FinishedGoodBatchRepository finishedGoodBatchRepository,
      DealerRepository dealerRepository,
      SupplierRepository supplierRepository,
      InvoiceSettlementPolicy invoiceSettlementPolicy,
      JournalReferenceResolver journalReferenceResolver,
      JournalReferenceMappingRepository journalReferenceMappingRepository,
      EntityManager entityManager,
      SystemSettingsService systemSettingsService,
      AuditService auditService,
      AccountingEventStore accountingEventStore,
      JournalEntryService journalEntryService) {
    super(
        companyContextService,
        accountRepository,
        journalEntryRepository,
        dealerLedgerService,
        supplierLedgerService,
        payrollRunRepository,
        payrollRunLineRepository,
        accountingPeriodService,
        referenceNumberService,
        eventPublisher,
        companyClock,
        companyEntityLookup,
        settlementAllocationRepository,
        rawMaterialPurchaseRepository,
        invoiceRepository,
        rawMaterialMovementRepository,
        rawMaterialBatchRepository,
        finishedGoodBatchRepository,
        dealerRepository,
        supplierRepository,
        invoiceSettlementPolicy,
        journalReferenceResolver,
        journalReferenceMappingRepository,
        entityManager,
        systemSettingsService,
        auditService,
        accountingEventStore);
    this.journalEntryService = journalEntryService;
  }

  public JournalEntryDto postPayrollRun(
      String runNumber,
      Long runId,
      LocalDate postingDate,
      String memo,
      List<JournalEntryRequest.JournalLineRequest> lines) {
    return super.postPayrollRun(runNumber, runId, postingDate, memo, lines);
  }

  public JournalEntryDto recordPayrollPayment(PayrollPaymentRequest request) {
    return super.recordPayrollPayment(request);
  }

  @Override
  public JournalEntryDto createJournalEntry(JournalEntryRequest request) {
    return journalEntryService.createJournalEntry(request);
  }

  @Override
  public JournalEntryDto createStandardJournal(
      com.bigbrightpaints.erp.modules.accounting.dto.JournalCreationRequest request) {
    return journalEntryService.createStandardJournal(request);
  }
}
