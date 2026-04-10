package com.bigbrightpaints.erp.modules.accounting.service;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryStatus;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalLineRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocation;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocationRepository;
import com.bigbrightpaints.erp.modules.accounting.dto.AccountingTransactionAuditListItemDto;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.invoice.domain.Invoice;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchase;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseRepository;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

final class AccountingAuditTrailTransactionQueryService {

  private static final JournalTotals ZERO_TOTALS =
      new JournalTotals(BigDecimal.ZERO, BigDecimal.ZERO);

  private final CompanyContextService companyContextService;
  private final JournalEntryRepository journalEntryRepository;
  private final JournalLineRepository journalLineRepository;
  private final PartnerSettlementAllocationRepository settlementAllocationRepository;
  private final InvoiceRepository invoiceRepository;
  private final RawMaterialPurchaseRepository rawMaterialPurchaseRepository;
  private final AccountingAuditTrailClassifier classifier;

  AccountingAuditTrailTransactionQueryService(
      CompanyContextService companyContextService,
      JournalEntryRepository journalEntryRepository,
      JournalLineRepository journalLineRepository,
      PartnerSettlementAllocationRepository settlementAllocationRepository,
      InvoiceRepository invoiceRepository,
      RawMaterialPurchaseRepository rawMaterialPurchaseRepository,
      AccountingAuditTrailClassifier classifier) {
    this.companyContextService = companyContextService;
    this.journalEntryRepository = journalEntryRepository;
    this.journalLineRepository = journalLineRepository;
    this.settlementAllocationRepository = settlementAllocationRepository;
    this.invoiceRepository = invoiceRepository;
    this.rawMaterialPurchaseRepository = rawMaterialPurchaseRepository;
    this.classifier = classifier;
  }

  PageResponse<AccountingTransactionAuditListItemDto> listTransactions(
      LocalDate from,
      LocalDate to,
      String module,
      String status,
      String referenceNumber,
      int page,
      int size) {
    Company company = companyContextService.requireCurrentCompany();
    LocalDate end = to != null ? to : CompanyTime.today();
    LocalDate start = from != null ? from : end.minusDays(30);
    int safePage = Math.max(page, 0);
    int safeSize = Math.max(1, Math.min(size, 200));

    Specification<JournalEntry> spec =
        Specification.where(byCompany(company))
            .and(byEntryDateRange(start, end))
            .and(byStatus(status))
            .and(byReference(referenceNumber))
            .and(byModule(module));

    Page<JournalEntry> data =
        journalEntryRepository.findAll(
            spec,
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "entryDate", "id")));

    List<Long> journalIds = data.getContent().stream().map(JournalEntry::getId).toList();
    if (journalIds.isEmpty()) {
      return PageResponse.of(List.of(), data.getTotalElements(), safePage, safeSize);
    }
    Map<Long, JournalTotals> totalsByJournal =
        journalLineRepository
            .summarizeTotalsByCompanyAndJournalEntryIds(company, journalIds)
            .stream()
            .collect(
                Collectors.toMap(
                    JournalLineRepository.JournalEntryLineTotals::getJournalEntryId,
                    row ->
                        new JournalTotals(
                            row.getTotalDebit() != null ? row.getTotalDebit() : BigDecimal.ZERO,
                            row.getTotalCredit() != null
                                ? row.getTotalCredit()
                                : BigDecimal.ZERO)));
    Map<Long, Invoice> invoiceByJournal =
        invoiceRepository.findByCompanyAndJournalEntry_IdIn(company, journalIds).stream()
            .filter(
                invoice ->
                    invoice.getJournalEntry() != null && invoice.getJournalEntry().getId() != null)
            .collect(
                Collectors.toMap(
                    invoice -> invoice.getJournalEntry().getId(),
                    invoice -> invoice,
                    (left, right) -> left));
    Map<Long, RawMaterialPurchase> purchaseByJournal =
        findPurchasesByJournalEntryIds(company, journalIds);
    Map<Long, List<PartnerSettlementAllocation>> allocationsByJournal =
        settlementAllocationRepository
            .findByCompanyAndJournalEntry_IdIn(company, journalIds)
            .stream()
            .collect(Collectors.groupingBy(allocation -> allocation.getJournalEntry().getId()));

    List<AccountingTransactionAuditListItemDto> rows =
        data.getContent().stream()
            .map(
                entry -> {
                  JournalTotals totals = totalsByJournal.getOrDefault(entry.getId(), ZERO_TOTALS);
                  BigDecimal totalDebit = totals.totalDebit();
                  BigDecimal totalCredit = totals.totalCredit();
                  Invoice invoice = invoiceByJournal.get(entry.getId());
                  RawMaterialPurchase purchase = purchaseByJournal.get(entry.getId());
                  List<PartnerSettlementAllocation> allocations =
                      allocationsByJournal.getOrDefault(entry.getId(), List.of());
                  String transactionType =
                      classifier.deriveTransactionType(entry, invoice, purchase, allocations);
                  String resolvedModule =
                      classifier.deriveModule(transactionType, entry.getReferenceNumber());
                  String consistency =
                      classifier
                          .assessConsistency(entry, allocations, totalDebit, totalCredit)
                          .status();
                  return new AccountingTransactionAuditListItemDto(
                      entry.getId(),
                      entry.getReferenceNumber(),
                      entry.getEntryDate(),
                      entry.getStatus() != null ? entry.getStatus().name() : null,
                      resolvedModule,
                      transactionType,
                      entry.getMemo(),
                      entry.getDealer() != null ? entry.getDealer().getId() : null,
                      entry.getDealer() != null ? entry.getDealer().getName() : null,
                      entry.getSupplier() != null ? entry.getSupplier().getId() : null,
                      entry.getSupplier() != null ? entry.getSupplier().getName() : null,
                      totalDebit,
                      totalCredit,
                      entry.getReversalOf() != null ? entry.getReversalOf().getId() : null,
                      entry.getReversalEntry() != null ? entry.getReversalEntry().getId() : null,
                      entry.getCorrectionType() != null ? entry.getCorrectionType().name() : null,
                      consistency,
                      entry.getPostedAt());
                })
            .toList();

    return PageResponse.of(rows, data.getTotalElements(), safePage, safeSize);
  }

  Map<Long, RawMaterialPurchase> findPurchasesByJournalEntryIds(
      Company company, List<Long> journalEntryIds) {
    return rawMaterialPurchaseRepository
        .findByCompanyAndJournalEntry_IdIn(company, journalEntryIds)
        .stream()
        .filter(
            purchase ->
                purchase.getJournalEntry() != null && purchase.getJournalEntry().getId() != null)
        .collect(
            Collectors.toMap(
                purchase -> purchase.getJournalEntry().getId(),
                purchase -> purchase,
                (left, right) -> left));
  }

  private Specification<JournalEntry> byCompany(Company company) {
    return (root, query, cb) -> cb.equal(root.get("company"), company);
  }

  private Specification<JournalEntry> byEntryDateRange(LocalDate from, LocalDate to) {
    return (root, query, cb) -> cb.between(root.get("entryDate"), from, to);
  }

  Specification<JournalEntry> byStatus(String status) {
    return (root, query, cb) -> {
      if (!StringUtils.hasText(status)) {
        return cb.conjunction();
      }
      JournalEntryStatus normalizedStatus;
      try {
        normalizedStatus = JournalEntryStatus.valueOf(status.trim().toUpperCase(Locale.ROOT));
      } catch (IllegalArgumentException ex) {
        throw ValidationUtils.invalidInput("Invalid accounting audit status: " + status.trim());
      }
      return cb.equal(root.get("status"), normalizedStatus);
    };
  }

  Specification<JournalEntry> byReference(String reference) {
    return (root, query, cb) -> {
      if (!StringUtils.hasText(reference)) {
        return cb.conjunction();
      }
      return cb.like(
          cb.upper(root.get("referenceNumber")),
          "%" + reference.trim().toUpperCase(Locale.ROOT) + "%");
    };
  }

  Specification<JournalEntry> byModule(String module) {
    return (root, query, cb) -> {
      if (!StringUtils.hasText(module)) {
        return cb.conjunction();
      }
      List<String> prefixes =
          classifier.moduleReferencePrefixes(module.trim().toUpperCase(Locale.ROOT));
      if (prefixes.isEmpty()) {
        return cb.conjunction();
      }
      List<jakarta.persistence.criteria.Predicate> predicates = new java.util.ArrayList<>();
      for (String prefix : prefixes) {
        predicates.add(cb.like(cb.upper(root.get("referenceNumber")), prefix + "%"));
      }
      return cb.or(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
    };
  }

  private record JournalTotals(BigDecimal totalDebit, BigDecimal totalCredit) {}
}
