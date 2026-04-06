package com.bigbrightpaints.erp.modules.accounting.service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.config.SystemSettingsService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.idempotency.IdempotencyReservationService;
import com.bigbrightpaints.erp.core.idempotency.IdempotencyUtils;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriod;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountingPeriodStatus;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryStatus;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryType;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalLine;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalReferenceMapping;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalReferenceMappingRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocationRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerType;
import com.bigbrightpaints.erp.modules.accounting.dto.AccountDto;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalCreationRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalLineDto;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalListItemDto;
import com.bigbrightpaints.erp.modules.accounting.dto.ManualJournalRequest;
import com.bigbrightpaints.erp.modules.accounting.event.AccountingEventStore;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.factory.service.CompanyScopedFactoryLookupService;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRunLineRepository;
import com.bigbrightpaints.erp.modules.hr.domain.PayrollRunRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovementRepository;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceRepository;
import com.bigbrightpaints.erp.modules.invoice.service.InvoiceSettlementPolicy;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.Supplier;
import com.bigbrightpaints.erp.modules.purchasing.domain.SupplierRepository;
import com.bigbrightpaints.erp.modules.purchasing.service.CompanyScopedPurchasingLookupService;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import com.bigbrightpaints.erp.modules.sales.service.CompanyScopedSalesLookupService;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

import jakarta.persistence.EntityManager;

@Service
class JournalPostingService {

  protected static final Logger log = LoggerFactory.getLogger(JournalPostingService.class);

  // Exact zero tolerance enforced for double-entry accounting integrity.
  // All amounts must be properly rounded before posting to ensure perfect balance.
  private static final BigDecimal JOURNAL_BALANCE_TOLERANCE = BigDecimal.ZERO;
  private static final BigDecimal FX_RATE_MIN = new BigDecimal("0.0001");
  private static final BigDecimal FX_RATE_MAX = new BigDecimal("100000");
  private static final BigDecimal FX_ROUNDING_TOLERANCE = new BigDecimal("0.05");
  protected static final BigDecimal ALLOCATION_TOLERANCE = new BigDecimal("0.01");
  private static final Duration IDEMPOTENCY_WAIT_TIMEOUT = Duration.ofSeconds(8);
  private static final long IDEMPOTENCY_WAIT_SLEEP_MILLIS = 50L;
  private static final ThreadLocal<Boolean> SYSTEM_ENTRY_DATE_OVERRIDE =
      ThreadLocal.withInitial(() -> Boolean.FALSE);
  private static final int ACCOUNTING_EVENT_JOURNAL_REFERENCE_MAX_LENGTH = 100;
  private static final int ACCOUNTING_EVENT_ACCOUNT_CODE_MAX_LENGTH = 50;
  private static final int ACCOUNTING_EVENT_DESCRIPTION_MAX_LENGTH = 500;
  private static final String ENTITY_TYPE_DEALER_RECEIPT = "DEALER_RECEIPT";
  private static final String ENTITY_TYPE_DEALER_RECEIPT_SPLIT = "DEALER_RECEIPT_SPLIT";
  protected static final String ENTITY_TYPE_DEALER_SETTLEMENT = "DEALER_SETTLEMENT";
  protected static final String ENTITY_TYPE_SUPPLIER_PAYMENT = "SUPPLIER_PAYMENT";
  protected static final String ENTITY_TYPE_SUPPLIER_SETTLEMENT = "SUPPLIER_SETTLEMENT";
  private static final String ENTITY_TYPE_CREDIT_NOTE = "CREDIT_NOTE";
  private static final String ENTITY_TYPE_DEBIT_NOTE = "DEBIT_NOTE";
  private static final String SETTLEMENT_DISCOUNT_LINE_DESCRIPTION = "settlement discount";
  private static final String SETTLEMENT_WRITE_OFF_LINE_DESCRIPTION = "settlement write-off";
  private static final String SETTLEMENT_FX_LOSS_LINE_DESCRIPTION = "fx loss on settlement";
  private static final String SETTLEMENT_APPLICATION_PREFIX = "[SETTLEMENT-APPLICATION:";
  private static final String INPUT_TAX_LINE_DESCRIPTION_PREFIX = "input tax for ";
  private static final int IDEMPOTENCY_LOG_HASH_LENGTH = 12;

  protected final CompanyContextService companyContextService;
  protected final AccountRepository accountRepository;
  protected final JournalEntryRepository journalEntryRepository;
  protected final DealerLedgerService dealerLedgerService;
  protected final SupplierLedgerService supplierLedgerService;
  protected final PayrollRunRepository payrollRunRepository;
  protected final PayrollRunLineRepository payrollRunLineRepository;
  protected final AccountingPeriodService accountingPeriodService;
  protected final ReferenceNumberService referenceNumberService;
  protected final ApplicationEventPublisher eventPublisher;
  protected final CompanyClock companyClock;
  protected final CompanyScopedAccountingLookupService accountingLookupService;
  protected final CompanyScopedSalesLookupService salesLookupService;
  protected final CompanyScopedPurchasingLookupService purchasingLookupService;
  protected final CompanyScopedFactoryLookupService factoryLookupService;
  protected final PartnerSettlementAllocationRepository settlementAllocationRepository;
  protected final RawMaterialPurchaseRepository rawMaterialPurchaseRepository;
  protected final InvoiceRepository invoiceRepository;
  protected final RawMaterialMovementRepository rawMaterialMovementRepository;
  protected final RawMaterialBatchRepository rawMaterialBatchRepository;
  protected final FinishedGoodBatchRepository finishedGoodBatchRepository;
  protected final DealerRepository dealerRepository;
  protected final SupplierRepository supplierRepository;
  protected final InvoiceSettlementPolicy invoiceSettlementPolicy;
  protected final JournalReferenceResolver journalReferenceResolver;
  protected final JournalReferenceMappingRepository journalReferenceMappingRepository;
  protected final EntityManager entityManager;
  protected final SystemSettingsService systemSettingsService;
  protected final AuditService auditService;
  protected final AccountingEventStore accountingEventStore;
  private final AccountingAuditService accountingAuditService;

  @Autowired(required = false)
  private Environment environment;

  @Autowired(required = false)
  private ObjectProvider<AccountingFacade> accountingFacadeProvider;

  private final IdempotencyReservationService idempotencyReservationService =
      new IdempotencyReservationService();

  @Autowired(required = false)
  protected AccountingComplianceAuditService accountingComplianceAuditService;

  @Autowired(required = false)
  protected ClosedPeriodPostingExceptionService closedPeriodPostingExceptionService;

  /**
   * When true, disables date validation for benchmark mode.
   * This allows posting entries with any date regardless of past/future constraints.
   */
  @Value("${erp.benchmark.skip-date-validation:false}")
  protected boolean skipDateValidation;

  /**
   * When true, journal posting/reversal fails if event-trail persistence fails.
   * This is the staging/predeploy default to prevent silent audit-trail drops.
   */
  @Value("${erp.accounting.event-trail.strict:true}")
  private boolean strictAccountingEventTrail = true;

  @Autowired
  public JournalPostingService(
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
      CompanyScopedAccountingLookupService accountingLookupService,
      CompanyScopedSalesLookupService salesLookupService,
      CompanyScopedPurchasingLookupService purchasingLookupService,
      CompanyScopedFactoryLookupService factoryLookupService,
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
      AccountingAuditService accountingAuditService) {
    this.companyContextService = companyContextService;
    this.accountRepository = accountRepository;
    this.journalEntryRepository = journalEntryRepository;
    this.dealerLedgerService = dealerLedgerService;
    this.supplierLedgerService = supplierLedgerService;
    this.payrollRunRepository = payrollRunRepository;
    this.payrollRunLineRepository = payrollRunLineRepository;
    this.accountingPeriodService = accountingPeriodService;
    this.referenceNumberService = referenceNumberService;
    this.eventPublisher = eventPublisher;
    this.companyClock = companyClock;
    this.accountingLookupService = accountingLookupService;
    this.salesLookupService = salesLookupService;
    this.purchasingLookupService = purchasingLookupService;
    this.factoryLookupService = factoryLookupService;
    this.settlementAllocationRepository = settlementAllocationRepository;
    this.rawMaterialPurchaseRepository = rawMaterialPurchaseRepository;
    this.invoiceRepository = invoiceRepository;
    this.rawMaterialMovementRepository = rawMaterialMovementRepository;
    this.rawMaterialBatchRepository = rawMaterialBatchRepository;
    this.finishedGoodBatchRepository = finishedGoodBatchRepository;
    this.dealerRepository = dealerRepository;
    this.supplierRepository = supplierRepository;
    this.invoiceSettlementPolicy = invoiceSettlementPolicy;
    this.journalReferenceResolver = journalReferenceResolver;
    this.journalReferenceMappingRepository = journalReferenceMappingRepository;
    this.entityManager = entityManager;
    this.systemSettingsService = systemSettingsService;
    this.auditService = auditService;
    this.accountingEventStore = accountingEventStore;
    this.accountingAuditService = accountingAuditService;
  }

  protected JournalPostingService(
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
      com.bigbrightpaints.erp.core.util.CompanyEntityLookup companyEntityLookup,
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
      AccountingAuditService accountingAuditService) {
    this(
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
        CompanyScopedAccountingLookupService.fromLegacy(companyEntityLookup),
        CompanyScopedSalesLookupService.fromLegacy(companyEntityLookup),
        CompanyScopedPurchasingLookupService.fromLegacy(companyEntityLookup),
        CompanyScopedFactoryLookupService.fromLegacy(companyEntityLookup),
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
        accountingEventStore,
        accountingAuditService);
  }

  @Transactional
  protected JournalEntryDto createStandardJournal(JournalCreationRequest request) {
    if (request == null) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, "Journal creation request is required");
    }
    ValidationUtils.requirePositive(request.amount(), "amount");
    if (!StringUtils.hasText(request.narration())) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
          "Narration is required for journal creation");
    }
    if (!StringUtils.hasText(request.sourceModule())) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
          "Source module is required for journal creation");
    }
    if (!StringUtils.hasText(request.sourceReference())) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
          "Source reference is required for journal creation");
    }

    List<JournalEntryRequest.JournalLineRequest> resolvedLines = request.resolvedLines();
    if (resolvedLines == null || resolvedLines.isEmpty()) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "At least one journal line is required");
    }

    BigDecimal totalDebit = BigDecimal.ZERO;
    BigDecimal totalCredit = BigDecimal.ZERO;
    for (JournalEntryRequest.JournalLineRequest line : resolvedLines) {
      if (line.accountId() == null) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
            "Account is required for every journal line");
      }
      BigDecimal debit = line.debit() == null ? BigDecimal.ZERO : line.debit();
      BigDecimal credit = line.credit() == null ? BigDecimal.ZERO : line.credit();
      if (debit.compareTo(BigDecimal.ZERO) < 0 || credit.compareTo(BigDecimal.ZERO) < 0) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT, "Journal line amounts cannot be negative");
      }
      if (debit.compareTo(BigDecimal.ZERO) > 0 && credit.compareTo(BigDecimal.ZERO) > 0) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT,
            "Debit and credit cannot both be non-zero on the same line");
      }
      totalDebit = totalDebit.add(debit);
      totalCredit = totalCredit.add(credit);
    }
    if (totalDebit.compareTo(BigDecimal.ZERO) <= 0 || totalCredit.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "Journal lines must include at least one debit and one credit");
    }
    if (totalDebit.subtract(totalCredit).abs().compareTo(JOURNAL_BALANCE_TOLERANCE) > 0) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT, "Journal entry must balance")
          .withDetail("totalDebit", totalDebit)
          .withDetail("totalCredit", totalCredit);
    }

    LocalDate entryDate =
        request.entryDate() != null
            ? request.entryDate()
            : companyClock.today(companyContextService.requireCurrentCompany());
    String narration = request.narration().trim();
    String sourceModule = request.sourceModule().trim();
    String sourceReference = request.sourceReference().trim();
    String journalType =
        "MANUAL".equalsIgnoreCase(sourceModule)
            ? JournalEntryType.MANUAL.name()
            : JournalEntryType.AUTOMATED.name();
    JournalEntryRequest journalRequest =
        new JournalEntryRequest(
            sourceReference,
            entryDate,
            narration,
            request.dealerId(),
            request.supplierId(),
            Boolean.TRUE.equals(request.adminOverride()),
            resolvedLines,
            null,
            null,
            sourceModule,
            sourceReference,
            journalType,
            request.attachmentReferences());
    return createJournalEntry(journalRequest);
  }

  @Transactional
  protected JournalEntryDto createManualJournal(ManualJournalRequest request) {
    if (request == null) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, "Manual journal request is required");
    }
    if (request.lines() == null || request.lines().isEmpty()) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "Manual journal requires at least one line");
    }
    BigDecimal totalDebit = BigDecimal.ZERO;
    BigDecimal totalCredit = BigDecimal.ZERO;
    List<JournalEntryRequest.JournalLineRequest> lines = new ArrayList<>();
    for (ManualJournalRequest.LineRequest line : request.lines()) {
      if (line == null || line.accountId() == null) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
            "Account is required for manual journal lines");
      }
      BigDecimal amount = ValidationUtils.requirePositive(line.amount(), "amount");
      ManualJournalRequest.EntryType entryType = line.entryType();
      if (entryType == null) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
            "Entry type is required for manual journal lines");
      }
      String lineNarration =
          StringUtils.hasText(line.narration())
              ? line.narration().trim()
              : (StringUtils.hasText(request.narration())
                  ? request.narration().trim()
                  : "Manual journal line");
      BigDecimal debit =
          entryType == ManualJournalRequest.EntryType.DEBIT ? amount : BigDecimal.ZERO;
      BigDecimal credit =
          entryType == ManualJournalRequest.EntryType.CREDIT ? amount : BigDecimal.ZERO;
      totalDebit = totalDebit.add(debit);
      totalCredit = totalCredit.add(credit);
      lines.add(
          new JournalEntryRequest.JournalLineRequest(
              line.accountId(), lineNarration, debit, credit));
    }
    if (totalDebit.subtract(totalCredit).abs().compareTo(JOURNAL_BALANCE_TOLERANCE) > 0) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT, "Manual journal entry must balance")
          .withDetail("totalDebit", totalDebit)
          .withDetail("totalCredit", totalCredit);
    }

    LocalDate entryDate =
        request.entryDate() != null
            ? request.entryDate()
            : companyClock.today(companyContextService.requireCurrentCompany());
    if (!StringUtils.hasText(request.narration())) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, "Manual journal reason is required");
    }
    String narration = request.narration().trim();
    String sourceReference =
        StringUtils.hasText(request.idempotencyKey()) ? request.idempotencyKey().trim() : null;
    JournalEntryRequest journalRequest =
        new JournalEntryRequest(
            null,
            entryDate,
            narration,
            null,
            null,
            Boolean.TRUE.equals(request.adminOverride()),
            lines,
            null,
            null,
            "MANUAL",
            sourceReference,
            JournalEntryType.MANUAL.name(),
            request.attachmentReferences());
    return createJournalEntry(journalRequest);
  }

  @Transactional(readOnly = true)
  public PageResponse<JournalListItemDto> listJournals(
      LocalDate fromDate,
      LocalDate toDate,
      String journalType,
      String sourceModule,
      int page,
      int size) {
    if (fromDate != null && toDate != null && fromDate.isAfter(toDate)) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_DATE, "fromDate cannot be after toDate")
          .withDetail("fromDate", fromDate)
          .withDetail("toDate", toDate);
    }
    JournalEntryType typeFilter = parseJournalTypeFilter(journalType);
    String normalizedSourceModule = normalizeSourceModule(sourceModule);
    Company company = companyContextService.requireCurrentCompany();
    int safePage = Math.max(page, 0);
    int safeSize = Math.max(1, Math.min(size, 200));
    Specification<JournalEntry> spec =
        Specification.where(byJournalCompany(company))
            .and(byJournalEntryDateRange(fromDate, toDate))
            .and(byJournalType(typeFilter))
            .and(byJournalSourceModule(normalizedSourceModule));
    Page<JournalEntry> journalPage =
        journalEntryRepository.findAll(
            spec,
            PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "entryDate", "id")));
    List<JournalListItemDto> content =
        journalPage.getContent().stream().map(this::toJournalListItemDto).toList();
    return PageResponse.of(content, journalPage.getTotalElements(), safePage, safeSize);
  }

  private Specification<JournalEntry> byJournalCompany(Company company) {
    return (root, query, cb) -> cb.equal(root.get("company"), company);
  }

  private Specification<JournalEntry> byJournalEntryDateRange(
      LocalDate fromDate, LocalDate toDate) {
    return (root, query, cb) -> {
      if (fromDate == null && toDate == null) {
        return cb.conjunction();
      }
      if (fromDate != null && toDate != null) {
        return cb.between(root.get("entryDate"), fromDate, toDate);
      }
      if (fromDate != null) {
        return cb.greaterThanOrEqualTo(root.get("entryDate"), fromDate);
      }
      return cb.lessThanOrEqualTo(root.get("entryDate"), toDate);
    };
  }

  private Specification<JournalEntry> byJournalType(JournalEntryType typeFilter) {
    return (root, query, cb) ->
        typeFilter != null ? cb.equal(root.get("journalType"), typeFilter) : cb.conjunction();
  }

  private Specification<JournalEntry> byJournalSourceModule(String normalizedSourceModule) {
    return (root, query, cb) ->
        normalizedSourceModule != null
            ? cb.equal(
                cb.lower(root.get("sourceModule")), normalizedSourceModule.toLowerCase(Locale.ROOT))
            : cb.conjunction();
  }

  @Retryable(
      value = DataIntegrityViolationException.class,
      maxAttempts = 3,
      backoff = @Backoff(delay = 50, maxDelay = 250, multiplier = 2.0))
  @Transactional
  protected JournalEntryDto createJournalEntry(JournalEntryRequest request) {
    Map<String, String> auditMetadata = new HashMap<>();
    if (request != null && request.referenceNumber() != null) {
      auditMetadata.put("requestedReference", request.referenceNumber());
    }
    try {
      if (request == null) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, "Journal entry request is required");
      }
      Company company = companyContextService.requireCurrentCompany();
      List<JournalEntryRequest.JournalLineRequest> lines = request.lines();
      if (company.getId() != null) {
        auditMetadata.put("companyId", company.getId().toString());
      }
      if (lines == null || lines.isEmpty()) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT, "At least one journal line is required");
      }
      String currency = resolveCurrency(request.currency(), company);
      BigDecimal fxRate = resolveFxRate(currency, company, request.fxRate());
      String baseCurrency =
          company.getBaseCurrency() != null && !company.getBaseCurrency().isBlank()
              ? company.getBaseCurrency().trim().toUpperCase()
              : "INR";
      boolean foreignCurrency = !currency.equalsIgnoreCase(baseCurrency);
      JournalEntry entry = new JournalEntry();
      entry.setCompany(company);
      entry.setCurrency(currency);
      entry.setFxRate(fxRate);
      entry.setJournalType(resolveJournalEntryType(request.journalType()));
      entry.setSourceModule(normalizeSourceModule(request.sourceModule()));
      entry.setSourceReference(normalizeSourceReference(request.sourceReference()));
      entry.setReferenceNumber(resolveJournalReference(company, request.referenceNumber()));
      auditMetadata.put("referenceNumber", entry.getReferenceNumber());

      Optional<JournalEntry> duplicate =
          journalEntryRepository.findByCompanyAndReferenceNumber(
              company, entry.getReferenceNumber());

      LocalDate entryDate = request.entryDate();
      if (entryDate == null) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT, "Entry date is required");
      }
      boolean overrideRequested = Boolean.TRUE.equals(request.adminOverride());
      boolean overrideAuthorized = overrideRequested && hasEntryDateOverrideAuthority();
      boolean manualJournal =
          entry.getJournalType() == JournalEntryType.MANUAL
              || "MANUAL".equalsIgnoreCase(entry.getSourceModule());
      if (manualJournal && !StringUtils.hasText(request.memo())) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, "Manual journal reason is required");
      }
      entry.setAttachmentReferences(joinAttachmentReferences(request.attachmentReferences()));
      if (duplicate.isEmpty()) {
        AccountingPeriod postingPeriod;
        if (systemSettingsService.isPeriodLockEnforced()) {
          validateEntryDate(company, entryDate, overrideRequested, overrideAuthorized);
          postingPeriod =
              accountingPeriodService.requirePostablePeriod(
                  company,
                  entryDate,
                  resolvePostingDocumentType(entry),
                  resolvePostingDocumentReference(entry),
                  request.memo(),
                  overrideAuthorized);
        } else {
          validateEntryDate(company, entryDate, overrideRequested, overrideAuthorized);
          postingPeriod = accountingPeriodService.ensurePeriod(company, entryDate);
        }
        if (postingPeriod == null) {
          throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT,
              "Accounting period is required for journal posting");
        }
        entry.setAccountingPeriod(postingPeriod);
      }
      entry.setEntryDate(entryDate);
      entry.setMemo(request.memo());
      entry.setStatus(JournalEntryStatus.POSTED);
      Dealer dealer = null;
      Account dealerReceivableAccount = null;
      Supplier supplier = null;
      Account supplierPayableAccount = null;
      if (request.dealerId() != null) {
        dealer = requireDealer(company, request.dealerId());
        dealerReceivableAccount = dealer.getReceivableAccount();
        entry.setDealer(dealer);
      }
      if (request.supplierId() != null) {
        supplier = requireSupplier(company, request.supplierId());
        supplierPayableAccount = supplier.getPayableAccount();
        entry.setSupplier(supplier);
      }
      Map<Account, BigDecimal> accountDeltas = new HashMap<>();
      BigDecimal dealerLedgerDebitTotal = BigDecimal.ZERO;
      BigDecimal dealerLedgerCreditTotal = BigDecimal.ZERO;
      BigDecimal supplierLedgerDebitTotal = BigDecimal.ZERO;
      BigDecimal supplierLedgerCreditTotal = BigDecimal.ZERO;
      int dealerArLines = 0;
      int supplierApLines = 0;
      List<Long> sortedAccountIds =
          lines.stream()
              .map(JournalEntryRequest.JournalLineRequest::accountId)
              .filter(Objects::nonNull)
              .distinct()
              .sorted()
              .toList();
      Map<Long, Account> lockedAccounts = new HashMap<>();
      for (Long accountId : sortedAccountIds) {
        Account account =
            accountRepository
                .lockByCompanyAndId(company, accountId)
                .orElseThrow(
                    () ->
                        new ApplicationException(
                            ErrorCode.VALIDATION_INVALID_REFERENCE, "Account not found"));
        lockedAccounts.put(accountId, account);
      }
      Dealer dealerContext = dealer;
      Supplier supplierContext = supplier;
      boolean hasReceivableAccount = false;
      boolean hasPayableAccount = false;
      List<Account> distinctAccounts = lockedAccounts.values().stream().distinct().toList();
      Map<Long, Set<Long>> dealerOwnerByReceivableAccountId = new HashMap<>();
      Map<Long, Set<Long>> supplierOwnerByPayableAccountId = new HashMap<>();
      List<Account> receivableAccounts =
          distinctAccounts.stream().filter(this::isReceivableAccount).toList();
      if (!receivableAccounts.isEmpty()) {
        hasReceivableAccount = true;
        List<Dealer> dealerOwners =
            dealerRepository.findByCompanyAndReceivableAccountIn(company, receivableAccounts);
        for (Dealer owner : dealerOwners) {
          if (owner.getReceivableAccount() == null
              || owner.getReceivableAccount().getId() == null
              || owner.getId() == null) {
            continue;
          }
          dealerOwnerByReceivableAccountId
              .computeIfAbsent(owner.getReceivableAccount().getId(), ignored -> new HashSet<>())
              .add(owner.getId());
        }
      }
      List<Account> payableAccounts =
          distinctAccounts.stream().filter(this::isPayableAccount).toList();
      if (!payableAccounts.isEmpty()) {
        hasPayableAccount = true;
        List<Supplier> supplierOwners =
            supplierRepository.findByCompanyAndPayableAccountIn(company, payableAccounts);
        for (Supplier owner : supplierOwners) {
          if (owner.getPayableAccount() == null
              || owner.getPayableAccount().getId() == null
              || owner.getId() == null) {
            continue;
          }
          supplierOwnerByPayableAccountId
              .computeIfAbsent(owner.getPayableAccount().getId(), ignored -> new HashSet<>())
              .add(owner.getId());
        }
      }
      for (Account account : distinctAccounts) {
        Long accountId = account.getId();
        if (accountId == null) {
          continue;
        }
        Set<Long> dealerOwnerIds = dealerOwnerByReceivableAccountId.get(accountId);
        if (dealerOwnerIds != null && !dealerOwnerIds.isEmpty()) {
          if (dealerContext == null) {
            throw new ApplicationException(
                ErrorCode.VALIDATION_INVALID_REFERENCE,
                "Dealer receivable account " + account.getCode() + " requires a dealer context");
          }
          Long dealerContextId = dealerContext.getId();
          if (dealerContextId == null || !dealerOwnerIds.contains(dealerContextId)) {
            throw new ApplicationException(
                ErrorCode.VALIDATION_INVALID_REFERENCE,
                "Dealer receivable account "
                    + account.getCode()
                    + " requires matching dealer context");
          }
        }
        Set<Long> supplierOwnerIds = supplierOwnerByPayableAccountId.get(accountId);
        if (supplierOwnerIds != null && !supplierOwnerIds.isEmpty()) {
          if (supplierContext == null) {
            throw new ApplicationException(
                ErrorCode.VALIDATION_INVALID_REFERENCE,
                "Supplier payable account " + account.getCode() + " requires a supplier context");
          }
          Long supplierContextId = supplierContext.getId();
          if (supplierContextId == null || !supplierOwnerIds.contains(supplierContextId)) {
            throw new ApplicationException(
                ErrorCode.VALIDATION_INVALID_REFERENCE,
                "Supplier payable account "
                    + account.getCode()
                    + " requires matching supplier context");
          }
        }
      }
      if (hasReceivableAccount && hasPayableAccount) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT,
            "Journal entry cannot combine AR and AP accounts; split into separate entries");
      }
      if (hasReceivableAccount && dealerContext == null) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT, "Posting to AR requires a dealer context");
      }
      if (hasPayableAccount && supplierContext == null) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT, "Posting to AP requires a supplier context");
      }
      if (dealerContext != null && hasReceivableAccount && dealerReceivableAccount == null) {
        dealerReceivableAccount = requireDealerReceivable(dealerContext);
      }
      if (supplierContext != null && hasPayableAccount && supplierPayableAccount == null) {
        supplierPayableAccount = requireSupplierPayable(supplierContext);
      }
      BigDecimal totalBaseDebit = BigDecimal.ZERO;
      BigDecimal totalBaseCredit = BigDecimal.ZERO;
      BigDecimal totalForeignDebit = BigDecimal.ZERO;
      BigDecimal totalForeignCredit = BigDecimal.ZERO;
      List<JournalLine> postedLines = new ArrayList<>();
      for (JournalEntryRequest.JournalLineRequest lineRequest : lines) {
        if (lineRequest.accountId() == null) {
          throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT, "Account is required for every journal line");
        }
        Account account = lockedAccounts.get(lineRequest.accountId());
        if (account == null) {
          throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_REFERENCE, "Account not found");
        }
        JournalLine line = new JournalLine();
        line.setJournalEntry(entry);
        line.setAccount(account);
        line.setDescription(lineRequest.description());

        BigDecimal debitInput = lineRequest.debit() == null ? BigDecimal.ZERO : lineRequest.debit();
        BigDecimal creditInput =
            lineRequest.credit() == null ? BigDecimal.ZERO : lineRequest.credit();
        if (debitInput.compareTo(BigDecimal.ZERO) < 0
            || creditInput.compareTo(BigDecimal.ZERO) < 0) {
          throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT, "Debit/Credit cannot be negative");
        }
        if (debitInput.compareTo(BigDecimal.ZERO) > 0
            && creditInput.compareTo(BigDecimal.ZERO) > 0) {
          throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT,
              "Debit and credit cannot both be non-zero on the same line");
        }

        if (foreignCurrency) {
          totalForeignDebit = totalForeignDebit.add(debitInput);
          totalForeignCredit = totalForeignCredit.add(creditInput);
        }

        BigDecimal baseDebit = toBaseCurrency(debitInput, fxRate);
        BigDecimal baseCredit = toBaseCurrency(creditInput, fxRate);
        line.setDebit(baseDebit);
        line.setCredit(baseCredit);
        entry.addLine(line);
        postedLines.add(line);
        accountDeltas.merge(account, baseDebit.subtract(baseCredit), BigDecimal::add);
        totalBaseDebit = totalBaseDebit.add(baseDebit);
        totalBaseCredit = totalBaseCredit.add(baseCredit);

        if (dealerReceivableAccount != null
            && Objects.equals(account.getId(), dealerReceivableAccount.getId())) {
          dealerArLines++;
        }
        if (supplierPayableAccount != null
            && Objects.equals(account.getId(), supplierPayableAccount.getId())) {
          supplierApLines++;
        }
      }
      BigDecimal roundingDelta = totalBaseDebit.subtract(totalBaseCredit);
      if (roundingDelta.compareTo(BigDecimal.ZERO) != 0) {
        if (roundingDelta.abs().compareTo(FX_ROUNDING_TOLERANCE) > 0) {
          throw new ApplicationException(
                  ErrorCode.VALIDATION_INVALID_INPUT, "Journal entry must balance")
              .withDetail("delta", roundingDelta)
              .withDetail("currency", currency)
              .withDetail("fxRate", fxRate);
        }
        // Adjust a single line to absorb minor FX/base rounding variance.
        if (roundingDelta.signum() > 0) {
          JournalLine target =
              postedLines.stream()
                  .filter(l -> l.getCredit().compareTo(BigDecimal.ZERO) > 0)
                  .max(Comparator.comparing(JournalLine::getCredit))
                  .orElse(null);
          if (target != null) {
            target.setCredit(target.getCredit().add(roundingDelta));
            accountDeltas.merge(target.getAccount(), roundingDelta.negate(), BigDecimal::add);
            totalBaseCredit = totalBaseCredit.add(roundingDelta);
          }
        } else {
          BigDecimal adjust = roundingDelta.abs();
          JournalLine target =
              postedLines.stream()
                  .filter(l -> l.getDebit().compareTo(BigDecimal.ZERO) > 0)
                  .max(Comparator.comparing(JournalLine::getDebit))
                  .orElse(null);
          if (target != null) {
            target.setDebit(target.getDebit().add(adjust));
            accountDeltas.merge(target.getAccount(), adjust, BigDecimal::add);
            totalBaseDebit = totalBaseDebit.add(adjust);
          }
        }
      }
      if (totalBaseDebit.subtract(totalBaseCredit).abs().compareTo(JOURNAL_BALANCE_TOLERANCE) > 0) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT, "Journal entry must balance");
      }

      if (foreignCurrency && totalForeignDebit.compareTo(BigDecimal.ZERO) > 0) {
        entry.setForeignAmountTotal(totalForeignDebit.setScale(2, RoundingMode.HALF_UP));
      }

      if (duplicate.isPresent()) {
        JournalEntry existingEntry = duplicate.get();
        if (existingEntry.getId() != null) {
          auditMetadata.put("journalEntryId", existingEntry.getId().toString());
        }
        ensureDuplicateMatchesExisting(existingEntry, entry, postedLines);
        log.info("Idempotent return: journal entry already exists, returning existing entry");
        auditMetadata.put("idempotent", "true");
        accountingAuditService.logAuditSuccessAfterCommit(
            AuditEvent.JOURNAL_ENTRY_POSTED, auditMetadata);
        return toDto(existingEntry);
      }

      // Only enforce AR/AP validation when AR/AP lines are present (allow partner context with zero
      // AR/AP lines for COGS/inventory entries)
      if (dealer != null
          && dealerReceivableAccount != null
          && dealerArLines > 1
          && !overrideAuthorized) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT,
            "Dealer journal entry has multiple receivable lines; admin override required");
      }
      if (supplier != null
          && supplierPayableAccount != null
          && supplierApLines > 1
          && !overrideAuthorized) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT,
            "Supplier journal entry has multiple payable lines; admin override required");
      }
      Instant now = CompanyTime.now(company);
      String username = accountingAuditService.resolveCurrentUsername();
      entry.setCreatedAt(now);
      entry.setUpdatedAt(now);
      entry.setPostedAt(now);
      entry.setCreatedBy(username);
      entry.setLastModifiedBy(username);
      entry.setPostedBy(username);
      JournalEntry saved;
      try {
        saved = journalEntryRepository.save(entry);
      } catch (DataIntegrityViolationException ex) {
        Optional<JournalEntry> existing =
            journalEntryRepository.findByCompanyAndReferenceNumber(
                company, entry.getReferenceNumber());
        if (existing.isPresent()) {
          JournalEntry existingEntry = existing.get();
          if (existingEntry.getId() != null) {
            auditMetadata.put("journalEntryId", existingEntry.getId().toString());
          }
          ensureDuplicateMatchesExisting(existingEntry, entry, postedLines);
          log.info(
              "Idempotent return after concurrent save race: journal entry already exists,"
                  + " returning existing entry");
          auditMetadata.put("idempotent", "true");
          accountingAuditService.logAuditSuccessAfterCommit(
              AuditEvent.JOURNAL_ENTRY_POSTED, auditMetadata);
          return toDto(existingEntry);
        }
        log.info("Concurrent journal save conflict detected; retrying in fresh transaction");
        throw ex;
      }
      boolean postedEventTrailRecorded = true;
      if (!accountDeltas.isEmpty()) {
        // Sort accounts by ID to prevent deadlocks - consistent lock ordering
        List<Map.Entry<Account, BigDecimal>> sortedDeltas =
            accountDeltas.entrySet().stream()
                .sorted(java.util.Comparator.comparing(e -> e.getKey().getId()))
                .toList();
        Map<Long, BigDecimal> balancesBefore = new HashMap<>();
        for (Map.Entry<Account, BigDecimal> delta : sortedDeltas) {
          Account account = delta.getKey();
          BigDecimal current =
              account.getBalance() == null ? BigDecimal.ZERO : account.getBalance();
          if (account.getId() != null) {
            balancesBefore.putIfAbsent(account.getId(), current);
          }
          BigDecimal updated = current.add(delta.getValue());
          account.validateBalanceUpdate(updated);
          int rows =
              accountRepository.updateBalanceAtomic(company, account.getId(), delta.getValue());
          if (rows != 1) {
            throw new ApplicationException(
                ErrorCode.INTERNAL_CONCURRENCY_FAILURE,
                "Account balance update failed for " + account.getCode());
          }
        }
        // Detach accounts from persistence context so they'll be re-fetched fresh with updated
        // balances
        for (Account account : accountDeltas.keySet()) {
          entityManager.detach(account);
        }
        accountingAuditService.publishAccountCacheInvalidated(company.getId());
        postedEventTrailRecorded =
            accountingAuditService.recordJournalEntryPostedEventSafe(saved, balancesBefore);
      }
      if (saved.getDealer() != null && dealerReceivableAccount != null) {
        for (JournalLine l : saved.getLines()) {
          if (l.getAccount() != null
              && Objects.equals(l.getAccount().getId(), dealerReceivableAccount.getId())) {
            dealerLedgerDebitTotal = dealerLedgerDebitTotal.add(l.getDebit());
            dealerLedgerCreditTotal = dealerLedgerCreditTotal.add(l.getCredit());
          }
        }
        if (dealerLedgerDebitTotal.compareTo(BigDecimal.ZERO) != 0
            || dealerLedgerCreditTotal.compareTo(BigDecimal.ZERO) != 0) {
          dealerLedgerService.recordLedgerEntry(
              saved.getDealer(),
              new AbstractPartnerLedgerService.LedgerContext(
                  saved.getEntryDate(),
                  saved.getReferenceNumber(),
                  saved.getMemo(),
                  dealerLedgerDebitTotal,
                  dealerLedgerCreditTotal,
                  saved));
        }
      }
      if (saved.getSupplier() != null && supplierPayableAccount != null) {
        for (JournalLine l : saved.getLines()) {
          if (l.getAccount() != null
              && Objects.equals(l.getAccount().getId(), supplierPayableAccount.getId())) {
            supplierLedgerDebitTotal = supplierLedgerDebitTotal.add(l.getDebit());
            supplierLedgerCreditTotal = supplierLedgerCreditTotal.add(l.getCredit());
          }
        }
        if (supplierLedgerDebitTotal.compareTo(BigDecimal.ZERO) != 0
            || supplierLedgerCreditTotal.compareTo(BigDecimal.ZERO) != 0) {
          supplierLedgerService.recordLedgerEntry(
              saved.getSupplier(),
              new AbstractPartnerLedgerService.LedgerContext(
                  saved.getEntryDate(),
                  saved.getReferenceNumber(),
                  saved.getMemo(),
                  supplierLedgerDebitTotal,
                  supplierLedgerCreditTotal,
                  saved));
        }
      }
      if (saved.getId() != null) {
        auditMetadata.put("journalEntryId", saved.getId().toString());
      }
      auditMetadata.put("status", saved.getStatus() != null ? saved.getStatus().name() : null);
      if (postedEventTrailRecorded) {
        accountingAuditService.logAuditSuccessAfterCommit(
            AuditEvent.JOURNAL_ENTRY_POSTED, auditMetadata);
      }
      if (accountingComplianceAuditService != null) {
        accountingComplianceAuditService.recordJournalCreation(company, saved);
      }
      if (closedPeriodPostingExceptionService != null
          && saved.getAccountingPeriod() != null
          && saved.getAccountingPeriod().getStatus() != AccountingPeriodStatus.OPEN) {
        closedPeriodPostingExceptionService.linkJournalEntry(
            company,
            resolvePostingDocumentType(saved),
            resolvePostingDocumentReference(saved),
            saved);
      }
      return toDto(saved);
    } catch (Exception e) {
      if (e.getMessage() != null) {
        auditMetadata.put("error", e.getMessage());
      }
      auditService.logFailure(AuditEvent.JOURNAL_ENTRY_POSTED, auditMetadata);
      throw e;
    }
  }

  private AccountDto toDto(Account account) {
    return new AccountDto(
        account.getId(),
        account.getPublicId(),
        account.getCode(),
        account.getName(),
        account.getType(),
        account.getBalance());
  }

  protected JournalEntryDto toDto(JournalEntry entry) {
    return toDto(entry, entry.getReferenceNumber());
  }

  protected JournalEntryDto toDto(JournalEntry entry, String displayReferenceNumber) {
    List<JournalLineDto> lines =
        entry.getLines().stream()
            .map(
                line ->
                    new JournalLineDto(
                        line.getAccount().getId(),
                        line.getAccount().getCode(),
                        line.getDescription(),
                        line.getDebit(),
                        line.getCredit()))
            .toList();
    Dealer dealer = entry.getDealer();
    Supplier supplier = entry.getSupplier();
    String dealerName = dealer != null ? dealer.getName() : null;
    String supplierName = supplier != null ? supplier.getName() : null;
    AccountingPeriod period = entry.getAccountingPeriod();
    Long periodId = period != null ? period.getId() : null;
    String periodLabel = period != null ? period.getLabel() : null;
    String periodStatus = period != null ? period.getStatus().name() : null;
    JournalEntry reversalOf = entry.getReversalOf();
    JournalEntry reversalEntry = entry.getReversalEntry();
    String correctionType =
        entry.getCorrectionType() != null ? entry.getCorrectionType().name() : null;
    return new JournalEntryDto(
        entry.getId(),
        entry.getPublicId(),
        displayReferenceNumber,
        entry.getEntryDate(),
        entry.getMemo(),
        entry.getStatus() != null ? entry.getStatus().name() : null,
        dealer != null ? dealer.getId() : null,
        dealerName,
        supplier != null ? supplier.getId() : null,
        supplierName,
        periodId,
        periodLabel,
        periodStatus,
        reversalOf != null ? reversalOf.getId() : null,
        reversalEntry != null ? reversalEntry.getId() : null,
        correctionType,
        entry.getCorrectionReason(),
        entry.getVoidReason(),
        lines,
        entry.getCreatedAt(),
        entry.getUpdatedAt(),
        entry.getPostedAt(),
        entry.getCreatedBy(),
        entry.getPostedBy(),
        entry.getLastModifiedBy());
  }

  private String resolveDisplayReferenceNumber(Company company, JournalEntry entry) {
    if (entry == null || !StringUtils.hasText(entry.getReferenceNumber())) {
      return entry != null ? entry.getReferenceNumber() : null;
    }
    String canonicalReference = entry.getReferenceNumber().trim();
    List<JournalReferenceMapping> mappings =
        journalReferenceMappingRepository.findAllByCompanyAndCanonicalReferenceIgnoreCase(
            company, canonicalReference);
    if (mappings.isEmpty()) {
      return canonicalReference;
    }
    return mappings.stream()
        .map(JournalReferenceMapping::getLegacyReference)
        .filter(StringUtils::hasText)
        .map(String::trim)
        .filter(legacyReference -> !legacyReference.equalsIgnoreCase(canonicalReference))
        .sorted(
            Comparator.comparing(
                    (String reference) ->
                        reference.toUpperCase(Locale.ROOT).contains("-INV-") ? 0 : 1)
                .thenComparingInt(String::length))
        .findFirst()
        .orElse(canonicalReference);
  }

  protected Dealer requireDealer(Company company, Long dealerId) {
    return salesLookupService.requireDealer(company, dealerId);
  }

  protected Supplier requireSupplier(Company company, Long supplierId) {
    return purchasingLookupService.requireSupplier(company, supplierId);
  }

  protected Account requireAccount(Company company, Long accountId) {
    return accountingLookupService.requireAccount(company, accountId);
  }

  protected Account requireCashAccountForSettlement(
      Company company, Long accountId, String operation) {
    return requireCashAccountForSettlement(company, accountId, operation, true);
  }

  protected Account requireCashAccountForSettlement(
      Company company, Long accountId, String operation, boolean requireActive) {
    Account account = requireAccount(company, accountId);
    if (requireActive && !account.isActive()) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT,
              "Cash/bank account for " + operation + " must be active")
          .withDetail("operation", operation)
          .withDetail("accountId", account.getId())
          .withDetail("accountCode", account.getCode())
          .withDetail("active", false);
    }
    if (account.getType() != null && account.getType() != AccountType.ASSET) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT,
              "Cash/bank account for " + operation + " must be an ASSET account")
          .withDetail("operation", operation)
          .withDetail("accountId", account.getId())
          .withDetail("accountCode", account.getCode())
          .withDetail("accountType", account.getType().name());
    }
    if (isReceivableAccount(account) || isPayableAccount(account)) {
      throw new ApplicationException(
              ErrorCode.VALIDATION_INVALID_INPUT,
              "Cash/bank account for " + operation + " cannot be AR/AP control account")
          .withDetail("operation", operation)
          .withDetail("accountId", account.getId())
          .withDetail("accountCode", account.getCode())
          .withDetail("accountName", account.getName());
    }
    return account;
  }

  protected Long resolveAutoSettlementCashAccountId(
      Company company, Long requestedCashAccountId, String operation) {
    if (requestedCashAccountId != null) {
      return requestedCashAccountId;
    }
    return accountRepository.findByCompanyOrderByCodeAsc(company).stream()
        .filter(Account::isActive)
        .filter(account -> account.getType() == null || account.getType() == AccountType.ASSET)
        .filter(account -> !isReceivableAccount(account) && !isPayableAccount(account))
        .filter(
            account -> {
              String code =
                  account.getCode() == null ? "" : account.getCode().toUpperCase(Locale.ROOT);
              String name =
                  account.getName() == null ? "" : account.getName().toUpperCase(Locale.ROOT);
              return code.contains("CASH")
                  || code.contains("BANK")
                  || name.contains("CASH")
                  || name.contains("BANK");
            })
        .map(Account::getId)
        .filter(Objects::nonNull)
        .findFirst()
        .orElseThrow(
            () ->
                new ApplicationException(
                    ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
                    "cashAccountId is required when no active default cash/bank account is"
                        + " configured for "
                        + operation));
  }

  protected void validateEntryDate(
      Company company, LocalDate entryDate, boolean overrideRequested, boolean overrideAuthorized) {
    if (entryDate == null) {
      throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT, "Entry date is required");
    }
    // Benchmark bypass must never apply when prod profile is active.
    if (skipDateValidation && !isProductionProfileActive()) {
      return;
    }
    LocalDate today = currentDate(company);
    LocalDate oldestAllowed = today.minusDays(30);
    boolean future = entryDate.isAfter(today);
    boolean tooOld = entryDate.isBefore(oldestAllowed);
    if ((!overrideAuthorized) && (future || tooOld)) {
      if (overrideRequested && !overrideAuthorized) {
        String reason = future ? "the future" : "entries older than 30 days";
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT,
            "Administrator approval with a mandatory reason is required to post into " + reason);
      }
      if (future) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT, "Entry date cannot be in the future");
      }
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "Entry date cannot be more than 30 days old without an explicit admin exception");
    }
  }

  JournalEntryDto createJournalEntryForReversal(
      JournalEntryRequest payload, boolean allowClosedPeriodOverride) {
    if (!allowClosedPeriodOverride) {
      return createJournalEntry(payload);
    }
    return runWithSystemEntryDateOverride(() -> createJournalEntry(payload));
  }

  <T> T runWithSystemEntryDateOverride(java.util.function.Supplier<T> action) {
    Boolean previous = SYSTEM_ENTRY_DATE_OVERRIDE.get();
    SYSTEM_ENTRY_DATE_OVERRIDE.set(Boolean.TRUE);
    try {
      return action.get();
    } finally {
      if (Boolean.TRUE.equals(previous)) {
        SYSTEM_ENTRY_DATE_OVERRIDE.set(Boolean.TRUE);
      } else {
        SYSTEM_ENTRY_DATE_OVERRIDE.remove();
      }
    }
  }

  protected boolean hasEntryDateOverrideAuthority() {
    if (Boolean.TRUE.equals(SYSTEM_ENTRY_DATE_OVERRIDE.get())) {
      return true;
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getAuthorities() == null) {
      return false;
    }
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      if ("ROLE_ADMIN".equals(authority.getAuthority())
          || "ROLE_SUPER_ADMIN".equals(authority.getAuthority())) {
        return true;
      }
    }
    return false;
  }

  protected String joinAttachmentReferences(List<String> attachmentReferences) {
    if (attachmentReferences == null || attachmentReferences.isEmpty()) {
      return null;
    }
    List<String> normalized =
        attachmentReferences.stream()
            .filter(StringUtils::hasText)
            .map(String::trim)
            .distinct()
            .toList();
    return normalized.isEmpty() ? null : String.join("\n", normalized);
  }

  protected String resolvePostingDocumentType(JournalEntry entry) {
    if (entry == null || !StringUtils.hasText(entry.getSourceModule())) {
      return "JOURNAL_ENTRY";
    }
    return entry.getSourceModule().trim().toUpperCase(Locale.ROOT);
  }

  protected String resolvePostingDocumentReference(JournalEntry entry) {
    if (entry == null) {
      return null;
    }
    if (StringUtils.hasText(entry.getSourceReference())) {
      return entry.getSourceReference().trim();
    }
    if (StringUtils.hasText(entry.getReferenceNumber())) {
      return entry.getReferenceNumber().trim();
    }
    return null;
  }

  protected JournalEntryType resolveJournalEntryType(String journalType) {
    if (!StringUtils.hasText(journalType)) {
      return JournalEntryType.AUTOMATED;
    }
    try {
      return JournalEntryType.valueOf(journalType.trim().toUpperCase(Locale.ROOT));
    } catch (IllegalArgumentException ex) {
      throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT, "Unsupported journal type")
          .withDetail("journalType", journalType);
    }
  }

  private JournalEntryType parseJournalTypeFilter(String journalType) {
    if (!StringUtils.hasText(journalType)) {
      return null;
    }
    return resolveJournalEntryType(journalType);
  }

  protected String normalizeSourceModule(String sourceModule) {
    if (!StringUtils.hasText(sourceModule)) {
      return null;
    }
    return sourceModule.trim().toUpperCase(Locale.ROOT);
  }

  protected String normalizeSourceReference(String sourceReference) {
    if (!StringUtils.hasText(sourceReference)) {
      return null;
    }
    return sourceReference.trim();
  }

  private JournalListItemDto toJournalListItemDto(JournalEntry entry) {
    BigDecimal totalDebit = BigDecimal.ZERO;
    BigDecimal totalCredit = BigDecimal.ZERO;
    if (entry.getLines() != null) {
      for (JournalLine line : entry.getLines()) {
        totalDebit = totalDebit.add(line.getDebit() == null ? BigDecimal.ZERO : line.getDebit());
        totalCredit =
            totalCredit.add(line.getCredit() == null ? BigDecimal.ZERO : line.getCredit());
      }
    }
    return new JournalListItemDto(
        entry.getId(),
        entry.getReferenceNumber(),
        entry.getEntryDate(),
        entry.getMemo(),
        entry.getStatus() != null ? entry.getStatus().name() : null,
        entry.getJournalType() != null
            ? entry.getJournalType().name()
            : JournalEntryType.AUTOMATED.name(),
        entry.getSourceModule(),
        entry.getSourceReference(),
        totalDebit,
        totalCredit);
  }

  protected String resolveJournalReference(Company company, String provided) {
    if (provided != null) {
      String normalizedProvided = provided.trim();
      if (!normalizedProvided.isEmpty()) {
        return normalizedProvided;
      }
    }
    return referenceNumberService.nextJournalReference(company);
  }

  protected String resolveCurrency(String requested, Company company) {
    String base =
        company != null && StringUtils.hasText(company.getBaseCurrency())
            ? company.getBaseCurrency().trim().toUpperCase()
            : "INR";
    if (!StringUtils.hasText(requested)) {
      return base;
    }
    return requested.trim().toUpperCase();
  }

  protected BigDecimal resolveFxRate(String currency, Company company, BigDecimal requestedRate) {
    String base =
        company != null && StringUtils.hasText(company.getBaseCurrency())
            ? company.getBaseCurrency().trim().toUpperCase()
            : "INR";
    if (!StringUtils.hasText(currency) || currency.equalsIgnoreCase(base)) {
      return BigDecimal.ONE;
    }
    if (requestedRate == null) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "FX rate is required for currency " + currency);
    }
    BigDecimal rate = requestedRate;
    if (rate.compareTo(BigDecimal.ZERO) <= 0) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT, "FX rate must be positive");
    }
    if (rate.compareTo(FX_RATE_MIN) < 0 || rate.compareTo(FX_RATE_MAX) > 0) {
      throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT, "FX rate out of bounds")
          .withDetail("min", FX_RATE_MIN)
          .withDetail("max", FX_RATE_MAX)
          .withDetail("requested", rate);
    }
    return rate.setScale(6, RoundingMode.HALF_UP);
  }

  protected BigDecimal toBaseCurrency(BigDecimal amount, BigDecimal fxRate) {
    if (amount == null) {
      return BigDecimal.ZERO;
    }
    BigDecimal rate = fxRate == null ? BigDecimal.ONE : fxRate;
    return amount.multiply(rate).setScale(2, RoundingMode.HALF_UP);
  }

  private boolean isProductionProfileActive() {
    return environment != null && environment.acceptsProfiles(Profiles.of("prod"));
  }

  private String partnerFieldLabel(PartnerType partnerType) {
    if (partnerType == PartnerType.DEALER) {
      return "dealerId";
    }
    if (partnerType == PartnerType.SUPPLIER) {
      return "supplierId";
    }
    return "partnerId";
  }

  private boolean isTokenMatch(String value, String token) {
    if (!StringUtils.hasText(value)) {
      return false;
    }
    return value.equals(token)
        || value.startsWith(token + "-")
        || value.endsWith("-" + token)
        || value.contains("-" + token + "-");
  }

  protected LocalDate currentDate(Company company) {
    return companyClock.today(company);
  }

  protected void ensureDuplicateMatchesExisting(
      JournalEntry existing, JournalEntry candidate, List<JournalLine> candidateLines) {
    List<String> mismatches = new ArrayList<>();
    List<String> partnerMismatchTypes = new ArrayList<>();
    if (!Objects.equals(existing.getEntryDate(), candidate.getEntryDate())) {
      mismatches.add("entryDate");
    }
    if (!Objects.equals(
        existing.getDealer() != null ? existing.getDealer().getId() : null,
        candidate.getDealer() != null ? candidate.getDealer().getId() : null)) {
      mismatches.add(partnerFieldLabel(PartnerType.DEALER));
      partnerMismatchTypes.add(PartnerType.DEALER.name());
    }
    if (!Objects.equals(
        existing.getSupplier() != null ? existing.getSupplier().getId() : null,
        candidate.getSupplier() != null ? candidate.getSupplier().getId() : null)) {
      mismatches.add(partnerFieldLabel(PartnerType.SUPPLIER));
      partnerMismatchTypes.add(PartnerType.SUPPLIER.name());
    }
    if (!sameCurrency(existing.getCurrency(), candidate.getCurrency())) {
      mismatches.add("currency");
    }
    if (!sameFxRate(existing.getFxRate(), candidate.getFxRate())) {
      mismatches.add("fxRate");
    }
    if (StringUtils.hasText(candidate.getMemo())
        && !Objects.equals(existing.getMemo(), candidate.getMemo())) {
      mismatches.add("memo");
    }
    if (!lineSignatureCounts(existing.getLines()).equals(lineSignatureCounts(candidateLines))) {
      mismatches.add("lines");
    }
    if (!mismatches.isEmpty()) {
      ApplicationException exception =
          new ApplicationException(
                  ErrorCode.BUSINESS_DUPLICATE_ENTRY,
                  "Journal entry reference already exists with different details")
              .withDetail("reference", existing.getReferenceNumber())
              .withDetail("mismatches", mismatches);
      if (!partnerMismatchTypes.isEmpty()) {
        exception.withDetail("partnerMismatchTypes", partnerMismatchTypes);
      }
      throw exception;
    }
  }

  private Map<JournalLineSignature, Integer> lineSignatureCounts(List<JournalLine> lines) {
    Map<JournalLineSignature, Integer> counts = new HashMap<>();
    if (lines == null) {
      return counts;
    }
    for (JournalLine line : lines) {
      if (line.getAccount() == null || line.getAccount().getId() == null) {
        continue;
      }
      JournalLineSignature signature =
          new JournalLineSignature(
              line.getAccount().getId(),
              roundedAmount(line.getDebit()),
              roundedAmount(line.getCredit()));
      counts.merge(signature, 1, Integer::sum);
    }
    return counts;
  }

  private BigDecimal roundedAmount(BigDecimal amount) {
    if (amount == null) {
      return BigDecimal.ZERO.setScale(2, RoundingMode.HALF_UP);
    }
    return amount.setScale(2, RoundingMode.HALF_UP);
  }

  private boolean sameCurrency(String left, String right) {
    if (left == null && right == null) {
      return true;
    }
    if (left == null || right == null) {
      return false;
    }
    return left.equalsIgnoreCase(right);
  }

  private boolean sameFxRate(BigDecimal left, BigDecimal right) {
    BigDecimal normalizedLeft = left == null ? BigDecimal.ONE : left;
    BigDecimal normalizedRight = right == null ? BigDecimal.ONE : right;
    return normalizedLeft.compareTo(normalizedRight) == 0;
  }

  private record JournalLineSignature(Long accountId, BigDecimal debit, BigDecimal credit) {}

  private record DealerPaymentSignature(Long accountId, BigDecimal amount) {}

  private record ExistingDealerPaymentLine(
      DealerPaymentSignature signature, String normalizedDescription) {}

  private record SettlementAdjustmentSignature(String normalizedDescription, BigDecimal amount) {}

  protected Account requireDealerReceivable(Dealer dealer) {
    if (dealer == null || dealer.getReceivableAccount() == null) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_REFERENCE,
          "Dealer "
              + (dealer != null ? dealer.getName() : "unknown")
              + " is missing a receivable account");
    }
    return dealer.getReceivableAccount();
  }

  protected Account requireSupplierPayable(Supplier supplier) {
    if (supplier == null || supplier.getPayableAccount() == null) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_REFERENCE,
          "Supplier "
              + (supplier != null ? supplier.getName() : "unknown")
              + " is missing a payable account");
    }
    return supplier.getPayableAccount();
  }

  protected boolean settlementOverrideRequested(SettlementSupportService.SettlementTotals totals) {
    if (totals == null) {
      return false;
    }
    return totals.totalDiscount().compareTo(BigDecimal.ZERO) > 0
        || totals.totalWriteOff().compareTo(BigDecimal.ZERO) > 0
        || totals.totalFxGain().compareTo(BigDecimal.ZERO) > 0
        || totals.totalFxLoss().compareTo(BigDecimal.ZERO) > 0;
  }

  protected String requireAdminExceptionReason(
      String operation, Boolean adminOverride, String reason) {
    if (!Boolean.TRUE.equals(adminOverride)) {
      throw new ApplicationException(
          ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS,
          operation + " requires an explicit admin override for this document");
    }
    if (!hasEntryDateOverrideAuthority()) {
      throw new ApplicationException(
          ErrorCode.AUTH_INSUFFICIENT_PERMISSIONS, operation + " is admin-only");
    }
    if (StringUtils.hasText(reason)) {
      return reason.trim();
    }
    throw new ApplicationException(
            ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, operation + " reason is required")
        .withDetail("field", "memo");
  }

  protected boolean isReceivableAccount(Account account) {
    if (account == null || account.getType() != AccountType.ASSET) {
      return false;
    }
    String code = IdempotencyUtils.normalizeUpperToken(account.getCode());
    String name = IdempotencyUtils.normalizeUpperToken(account.getName());
    return isTokenMatch(code, "AR") || name.contains("ACCOUNTS RECEIVABLE");
  }

  protected boolean isPayableAccount(Account account) {
    if (account == null || account.getType() != AccountType.LIABILITY) {
      return false;
    }
    String code = IdempotencyUtils.normalizeUpperToken(account.getCode());
    String name = IdempotencyUtils.normalizeUpperToken(account.getName());
    return isTokenMatch(code, "AP") || name.contains("ACCOUNTS PAYABLE");
  }

  private void requireGenericSubledgerOverride(String label, JournalEntryRequest request) {
    if (!Boolean.TRUE.equals(request.adminOverride())) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "Posting to " + label + " requires dealer/supplier context or admin override");
    }
    String memo = request.memo();
    String normalized = IdempotencyUtils.normalizeUpperToken(memo);
    String token = "GENERIC " + label + ":";
    if (!normalized.contains(token)) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "Generic " + label + " postings require memo to include '" + token + " <reason>'");
    }
  }
}
