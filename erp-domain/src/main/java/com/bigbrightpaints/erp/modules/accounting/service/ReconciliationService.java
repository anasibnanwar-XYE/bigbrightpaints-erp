package com.bigbrightpaints.erp.modules.accounting.service;

import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.accounting.domain.DealerLedgerRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.SupplierLedgerRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalLine;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalLineRepository;
import com.bigbrightpaints.erp.modules.accounting.dto.DealerBalanceView;
import com.bigbrightpaints.erp.modules.accounting.dto.SupplierBalanceView;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryReservation;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryReservationRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlip;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlipRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.Supplier;
import com.bigbrightpaints.erp.modules.purchasing.domain.SupplierRepository;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Service for reconciling GL accounts with sub-ledgers.
 * Used to detect discrepancies between AR/AP accounts and dealer/supplier ledgers.
 */
@Service
public class ReconciliationService {

    private static final Logger log = LoggerFactory.getLogger(ReconciliationService.class);
    private static final BigDecimal TOLERANCE = new BigDecimal("0.01");

    private final CompanyContextService companyContextService;
    private final AccountRepository accountRepository;
    private final DealerRepository dealerRepository;
    private final DealerLedgerRepository dealerLedgerRepository;
    private final SupplierRepository supplierRepository;
    private final SupplierLedgerRepository supplierLedgerRepository;
    private final InventoryReservationRepository inventoryReservationRepository;
    private final PackagingSlipRepository packagingSlipRepository;
    private final SalesOrderRepository salesOrderRepository;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalLineRepository journalLineRepository;

    public ReconciliationService(CompanyContextService companyContextService,
                                  AccountRepository accountRepository,
                                  DealerRepository dealerRepository,
                                  DealerLedgerRepository dealerLedgerRepository,
                                  SupplierRepository supplierRepository,
                                  SupplierLedgerRepository supplierLedgerRepository,
                                  InventoryReservationRepository inventoryReservationRepository,
                                  PackagingSlipRepository packagingSlipRepository,
                                  SalesOrderRepository salesOrderRepository,
                                  JournalEntryRepository journalEntryRepository,
                                  JournalLineRepository journalLineRepository) {
        this.companyContextService = companyContextService;
        this.accountRepository = accountRepository;
        this.dealerRepository = dealerRepository;
        this.dealerLedgerRepository = dealerLedgerRepository;
        this.supplierRepository = supplierRepository;
        this.supplierLedgerRepository = supplierLedgerRepository;
        this.inventoryReservationRepository = inventoryReservationRepository;
        this.packagingSlipRepository = packagingSlipRepository;
        this.salesOrderRepository = salesOrderRepository;
        this.journalEntryRepository = journalEntryRepository;
        this.journalLineRepository = journalLineRepository;
    }

    /**
     * Reconcile AR GL account balance with sum of dealer ledger balances.
     * Returns discrepancies if any.
     */
    @Transactional(readOnly = true)
    public ReconciliationResult reconcileArWithDealerLedger() {
        Company company = companyContextService.requireCurrentCompany();
        List<Account> allAccounts = accountRepository.findByCompanyOrderByCodeAsc(company);
        List<Dealer> dealers = dealerRepository.findByCompanyOrderByNameAsc(company);
        List<Account> arAccounts = resolveReceivableAccounts(allAccounts, dealers);

        BigDecimal totalArBalance = arAccounts.stream()
                .map(Account::getBalance)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        // Get all dealers with their ledger balances
        List<Long> dealerIds = dealers.stream().map(Dealer::getId).toList();
        
        Map<Long, BigDecimal> dealerBalances = dealerLedgerRepository
                .aggregateBalances(company, dealerIds)
                .stream()
                .collect(Collectors.toMap(DealerBalanceView::dealerId, DealerBalanceView::balance));

        BigDecimal totalDealerLedgerBalance = dealerBalances.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variance = totalArBalance.subtract(totalDealerLedgerBalance);
        boolean isReconciled = variance.abs().compareTo(TOLERANCE) <= 0;

        List<DealerDiscrepancy> discrepancies = new ArrayList<>();
        
        // Check individual dealers
        for (Dealer dealer : dealers) {
            BigDecimal ledgerBalance = dealerBalances.getOrDefault(dealer.getId(), BigDecimal.ZERO);
            BigDecimal outstandingBalance = dealer.getOutstandingBalance() != null 
                    ? dealer.getOutstandingBalance() 
                    : BigDecimal.ZERO;
            
            BigDecimal dealerVariance = outstandingBalance.subtract(ledgerBalance);
            if (dealerVariance.abs().compareTo(TOLERANCE) > 0) {
                discrepancies.add(new DealerDiscrepancy(
                        dealer.getId(),
                        dealer.getCode(),
                        dealer.getName(),
                        outstandingBalance,
                        ledgerBalance,
                        dealerVariance
                ));
            }
        }

        log.info("AR Reconciliation: GL={}, DealerLedger={}, Variance={}, Reconciled={}",
                totalArBalance, totalDealerLedgerBalance, variance, isReconciled);

        return new ReconciliationResult(
                totalArBalance,
                totalDealerLedgerBalance,
                variance,
                isReconciled,
                discrepancies,
                arAccounts.size(),
                dealers.size()
        );
    }

    /**
     * Reconcile AP GL account balance with sum of supplier ledger balances.
     */
    @Transactional(readOnly = true)
    public SupplierReconciliationResult reconcileApWithSupplierLedger() {
        Company company = companyContextService.requireCurrentCompany();
        List<Account> allAccounts = accountRepository.findByCompanyOrderByCodeAsc(company);
        List<Supplier> suppliers = supplierRepository.findByCompanyOrderByNameAsc(company);
        List<Account> apAccounts = resolvePayableAccounts(allAccounts, suppliers);

        BigDecimal totalApBalance = apAccounts.stream()
                .map(Account::getBalance)
                .filter(b -> b != null)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        // Account.balance uses a debit-positive convention (debit - credit). For AP we reconcile against the
        // supplier sub-ledger which is aggregated as (credit - debit), so normalize the GL balance here.
        BigDecimal glApBalance = totalApBalance.negate();

        List<Long> supplierIds = suppliers.stream().map(Supplier::getId).toList();

        Map<Long, BigDecimal> supplierBalances = supplierLedgerRepository
                .aggregateBalances(company, supplierIds)
                .stream()
                .collect(Collectors.toMap(SupplierBalanceView::supplierId, SupplierBalanceView::balance));

        BigDecimal totalSupplierLedgerBalance = supplierBalances.values().stream()
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal variance = glApBalance.subtract(totalSupplierLedgerBalance);
        boolean isReconciled = variance.abs().compareTo(TOLERANCE) <= 0;

        List<SupplierDiscrepancy> discrepancies = new ArrayList<>();

        for (Supplier supplier : suppliers) {
            BigDecimal ledgerBalance = supplierBalances.getOrDefault(supplier.getId(), BigDecimal.ZERO);
            BigDecimal outstandingBalance = supplier.getOutstandingBalance() != null
                    ? supplier.getOutstandingBalance()
                    : BigDecimal.ZERO;

            BigDecimal supplierVariance = outstandingBalance.subtract(ledgerBalance);
            if (supplierVariance.abs().compareTo(TOLERANCE) > 0) {
                discrepancies.add(new SupplierDiscrepancy(
                        supplier.getId(),
                        supplier.getCode(),
                        supplier.getName(),
                        outstandingBalance,
                        ledgerBalance,
                        supplierVariance
                ));
            }
        }

        log.info("AP Reconciliation: GL={}, SupplierLedger={}, Variance={}, Reconciled={}",
                glApBalance, totalSupplierLedgerBalance, variance, isReconciled);

        return new SupplierReconciliationResult(
                glApBalance,
                totalSupplierLedgerBalance,
                variance,
                isReconciled,
                discrepancies,
                apAccounts.size(),
                suppliers.size()
        );
    }

    @Transactional(readOnly = true)
    public PeriodReconciliationResult reconcileSubledgersForPeriod(java.time.LocalDate start, java.time.LocalDate end) {
        Company company = companyContextService.requireCurrentCompany();
        if (start == null || end == null) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("start and end dates are required");
        }
        if (start.isAfter(end)) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("start date must be on or before end date");
        }
        List<Account> allAccounts = accountRepository.findByCompanyOrderByCodeAsc(company);
        List<Dealer> dealers = dealerRepository.findByCompanyOrderByNameAsc(company);
        List<Account> arAccounts = resolveReceivableAccounts(allAccounts, dealers);
        List<Long> dealerIds = dealers.stream().map(Dealer::getId).toList();
        BigDecimal dealerLedgerNet = dealerIds.isEmpty()
                ? BigDecimal.ZERO
                : dealerLedgerRepository.aggregateBalancesBetween(company, dealerIds, start, end)
                        .stream()
                        .map(DealerBalanceView::balance)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        List<Supplier> suppliers = supplierRepository.findByCompanyOrderByNameAsc(company);
        List<Account> apAccounts = resolvePayableAccounts(allAccounts, suppliers);
        List<Long> supplierIds = suppliers.stream().map(Supplier::getId).toList();
        BigDecimal supplierLedgerNet = supplierIds.isEmpty()
                ? BigDecimal.ZERO
                : supplierLedgerRepository.aggregateBalancesBetween(company, supplierIds, start, end)
                        .stream()
                        .map(SupplierBalanceView::balance)
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

        BigDecimal glArNet = sumAccountNet(company, arAccounts, start, end, true);
        BigDecimal glApNet = sumAccountNet(company, apAccounts, start, end, false);

        BigDecimal arVariance = glArNet.subtract(dealerLedgerNet);
        BigDecimal apVariance = glApNet.subtract(supplierLedgerNet);
        boolean arReconciled = arVariance.abs().compareTo(TOLERANCE) <= 0;
        boolean apReconciled = apVariance.abs().compareTo(TOLERANCE) <= 0;

        return new PeriodReconciliationResult(
                start,
                end,
                glArNet,
                dealerLedgerNet,
                arVariance,
                arReconciled,
                glApNet,
                supplierLedgerNet,
                apVariance,
                apReconciled
        );
    }

    private BigDecimal sumAccountNet(Company company,
                                     List<Account> accounts,
                                     java.time.LocalDate start,
                                     java.time.LocalDate end,
                                     boolean receivable) {
        if (accounts == null || accounts.isEmpty()) {
            return BigDecimal.ZERO;
        }
        BigDecimal total = BigDecimal.ZERO;
        for (Account account : accounts) {
            if (account.getId() == null) {
                continue;
            }
            List<JournalLine> lines = journalLineRepository.findLinesForAccountBetween(company, account.getId(), start, end);
            for (JournalLine line : lines) {
                BigDecimal debit = line.getDebit() != null ? line.getDebit() : BigDecimal.ZERO;
                BigDecimal credit = line.getCredit() != null ? line.getCredit() : BigDecimal.ZERO;
                BigDecimal delta = receivable ? debit.subtract(credit) : credit.subtract(debit);
                total = total.add(delta);
            }
        }
        return total;
    }

    private List<Account> resolveReceivableAccounts(List<Account> accounts, List<Dealer> dealers) {
        if (accounts == null || accounts.isEmpty()) {
            return List.of();
        }
        Set<Long> receivableIds = dealers == null
                ? Set.of()
                : dealers.stream()
                        .map(Dealer::getReceivableAccount)
                        .filter(Objects::nonNull)
                        .map(Account::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (!receivableIds.isEmpty()) {
            return accounts.stream()
                    .filter(account -> account.getId() != null && receivableIds.contains(account.getId()))
                    .toList();
        }
        return accounts.stream()
                .filter(account -> account.getType() == AccountType.ASSET)
                .filter(this::isReceivableCandidate)
                .toList();
    }

    private List<Account> resolvePayableAccounts(List<Account> accounts, List<Supplier> suppliers) {
        if (accounts == null || accounts.isEmpty()) {
            return List.of();
        }
        Set<Long> payableIds = suppliers == null
                ? Set.of()
                : suppliers.stream()
                        .map(Supplier::getPayableAccount)
                        .filter(Objects::nonNull)
                        .map(Account::getId)
                        .filter(Objects::nonNull)
                        .collect(Collectors.toSet());
        if (!payableIds.isEmpty()) {
            return accounts.stream()
                    .filter(account -> account.getId() != null && payableIds.contains(account.getId()))
                    .toList();
        }
        return accounts.stream()
                .filter(account -> account.getType() == AccountType.LIABILITY)
                .filter(this::isPayableCandidate)
                .toList();
    }

    private boolean isReceivableCandidate(Account account) {
        if (account == null || account.getCode() == null) {
            return false;
        }
        String code = account.getCode().toUpperCase();
        String name = account.getName() == null ? "" : account.getName().toUpperCase();
        return code.contains("AR") || name.contains("ACCOUNTS RECEIVABLE");
    }

    private boolean isPayableCandidate(Account account) {
        if (account == null || account.getCode() == null) {
            return false;
        }
        String code = account.getCode().toUpperCase();
        String name = account.getName() == null ? "" : account.getName().toUpperCase();
        return code.contains("AP") || name.contains("ACCOUNTS PAYABLE");
    }

    /**
     * Check for orphan reservations (RESERVED status without corresponding packaging slips)
     * These could block dispatch and should be cleaned up or regenerated.
     */
    @Transactional(readOnly = true)
    public OrphanReservationReport findOrphanReservations() {
        Company company = companyContextService.requireCurrentCompany();
        
        List<InventoryReservation> reservedItems = inventoryReservationRepository
                .findByFinishedGoodCompanyAndStatus(company, "RESERVED");
        
        List<OrphanReservation> orphans = new ArrayList<>();
        
        for (InventoryReservation reservation : reservedItems) {
            String refType = reservation.getReferenceType();
            String refId = reservation.getReferenceId();
            
            // Only check SALES_ORDER type reservations
            if (!"SALES_ORDER".equals(refType)) {
                continue;
            }
            
            // Check if there's a corresponding packaging slip
            Long orderId = Long.parseLong(refId);
            List<PackagingSlip> slips = packagingSlipRepository
                    .findAllByCompanyAndSalesOrderId(company, orderId);
            
            if (slips.isEmpty()) {
                // No slip = orphan reservation
                Optional<SalesOrder> order = salesOrderRepository.findByCompanyAndId(company, orderId);
                orphans.add(new OrphanReservation(
                        reservation.getId(),
                        orderId,
                        order.map(SalesOrder::getOrderNumber).orElse("UNKNOWN-" + refId),
                        reservation.getReservedQuantity() != null 
                                ? reservation.getReservedQuantity() 
                                : reservation.getQuantity()
                ));
            }
        }

        log.info("Orphan reservation check for company {}: found {} orphans",
                company.getCode(), orphans.size());
        
        return new OrphanReservationReport(orphans.size(), orphans);
    }

    /**
     * Clean up orphan reservations by setting their status to CANCELLED.
     * Returns count of cleaned reservations.
     */
    @Transactional
    public int cleanOrphanReservations() {
        Company company = companyContextService.requireCurrentCompany();
        OrphanReservationReport report = findOrphanReservations();
        int cleaned = 0;
        
        for (OrphanReservation orphan : report.orphans()) {
            Optional<InventoryReservation> opt = inventoryReservationRepository
                    .findByFinishedGoodCompanyAndId(company, orphan.reservationId());
            if (opt.isPresent()) {
                InventoryReservation reservation = opt.get();
                reservation.setStatus("CANCELLED");
                reservation.setReservedQuantity(BigDecimal.ZERO);
                inventoryReservationRepository.save(reservation);
                cleaned++;
                log.info("Cleaned orphan reservation {} for order {}", orphan.reservationId(), orphan.orderNumber());
            }
        }
        
        return cleaned;
    }

    /**
     * Check for potential reference collisions with new idempotency pattern.
     * Detects SALE-{id} and *-COGS-* references that could conflict with new flow.
     */
    @Transactional(readOnly = true)
    public ReferenceCollisionReport checkReferenceCollisions() {
        Company company = companyContextService.requireCurrentCompany();
        
        List<JournalEntry> allEntries = journalEntryRepository.findByCompanyOrderByEntryDateDesc(company);
        
        int saleCount = 0;
        int cogsCount = 0;
        List<String> potentialCollisions = new ArrayList<>();
        
        for (JournalEntry entry : allEntries) {
            String ref = entry.getReferenceNumber();
            if (ref == null) continue;
            
            // Check for SALE-{number} pattern
            if (ref.matches("^SALE-\\d+$")) {
                saleCount++;
                potentialCollisions.add(ref);
            }
            
            // Check for *-COGS-* pattern
            if (ref.contains("-COGS-")) {
                cogsCount++;
                potentialCollisions.add(ref);
            }
        }

        log.info("Reference collision check for company {}: {} SALE-*, {} *-COGS-*",
                company.getCode(), saleCount, cogsCount);
        
        return new ReferenceCollisionReport(saleCount, cogsCount, potentialCollisions);
    }

    // Result DTOs
    public record ReconciliationResult(
            BigDecimal glArBalance,
            BigDecimal dealerLedgerTotal,
            BigDecimal variance,
            boolean isReconciled,
            List<DealerDiscrepancy> discrepancies,
            int arAccountCount,
            int dealerCount
    ) {}

    public record DealerDiscrepancy(
            Long dealerId,
            String dealerCode,
            String dealerName,
            BigDecimal outstandingBalance,
            BigDecimal ledgerBalance,
            BigDecimal variance
    ) {}

    public record OrphanReservationReport(
            int orphanCount,
            List<OrphanReservation> orphans
    ) {}

    public record OrphanReservation(
            Long reservationId,
            Long orderId,
            String orderNumber,
            BigDecimal quantity
    ) {}

    public record ReferenceCollisionReport(
            int salePatternCount,
            int cogsPatternCount,
            List<String> potentialCollisions
    ) {}

    public record SupplierReconciliationResult(
            BigDecimal glApBalance,
            BigDecimal supplierLedgerTotal,
            BigDecimal variance,
            boolean isReconciled,
            List<SupplierDiscrepancy> discrepancies,
            int apAccountCount,
            int supplierCount
    ) {}

    public record SupplierDiscrepancy(
            Long supplierId,
            String supplierCode,
            String supplierName,
            BigDecimal outstandingBalance,
            BigDecimal ledgerBalance,
            BigDecimal variance
    ) {}

    public record PeriodReconciliationResult(
            java.time.LocalDate startDate,
            java.time.LocalDate endDate,
            BigDecimal glArNet,
            BigDecimal dealerLedgerNet,
            BigDecimal arVariance,
            boolean arReconciled,
            BigDecimal glApNet,
            BigDecimal supplierLedgerNet,
            BigDecimal apVariance,
            boolean apReconciled
    ) {}
}
