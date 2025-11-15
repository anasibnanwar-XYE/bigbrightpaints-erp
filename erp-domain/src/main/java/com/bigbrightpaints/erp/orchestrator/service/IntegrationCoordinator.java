package com.bigbrightpaints.erp.orchestrator.service;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.dto.AccountDto;
import com.bigbrightpaints.erp.modules.accounting.dto.AccountRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingService;
import com.bigbrightpaints.erp.modules.factory.dto.FactoryDashboardDto;
import com.bigbrightpaints.erp.modules.factory.dto.FactoryTaskDto;
import com.bigbrightpaints.erp.modules.factory.dto.FactoryTaskRequest;
import com.bigbrightpaints.erp.modules.factory.dto.ProductionBatchDto;
import com.bigbrightpaints.erp.modules.factory.dto.ProductionBatchRequest;
import com.bigbrightpaints.erp.modules.factory.dto.ProductionPlanDto;
import com.bigbrightpaints.erp.modules.factory.dto.ProductionPlanRequest;
import com.bigbrightpaints.erp.modules.factory.service.FactoryService;
import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService;
import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService.InventoryReservationResult;
import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService.InventoryShortage;
import com.bigbrightpaints.erp.modules.invoice.service.InvoiceService;
import com.bigbrightpaints.erp.modules.hr.dto.EmployeeDto;
import com.bigbrightpaints.erp.modules.hr.dto.PayrollRunRequest;
import com.bigbrightpaints.erp.modules.hr.service.HrService;
import com.bigbrightpaints.erp.modules.reports.dto.AgedDebtorDto;
import com.bigbrightpaints.erp.modules.reports.dto.CashFlowDto;
import com.bigbrightpaints.erp.modules.reports.dto.InventoryValuationDto;
import com.bigbrightpaints.erp.modules.reports.service.ReportService;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderItem;
import com.bigbrightpaints.erp.modules.sales.dto.DealerDto;
import com.bigbrightpaints.erp.modules.sales.dto.SalesOrderDto;
import com.bigbrightpaints.erp.modules.sales.service.SalesService;
import com.bigbrightpaints.erp.orchestrator.repository.OrderAutoApprovalState;
import com.bigbrightpaints.erp.orchestrator.repository.OrderAutoApprovalStateRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.Supplier;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class IntegrationCoordinator {

    private static final Logger log = LoggerFactory.getLogger(IntegrationCoordinator.class);

    private final SalesService salesService;
    private final FactoryService factoryService;
    private final FinishedGoodsService finishedGoodsService;
    private final InvoiceService invoiceService;
    private final AccountingService accountingService;
    private final HrService hrService;
    private final ReportService reportService;
    private final OrderAutoApprovalStateRepository orderAutoApprovalStateRepository;

    public IntegrationCoordinator(SalesService salesService,
                                  FactoryService factoryService,
                                  FinishedGoodsService finishedGoodsService,
                                  InvoiceService invoiceService,
                                  AccountingService accountingService,
                                  HrService hrService,
                                  ReportService reportService,
                                  OrderAutoApprovalStateRepository orderAutoApprovalStateRepository) {
        this.salesService = salesService;
        this.factoryService = factoryService;
        this.finishedGoodsService = finishedGoodsService;
        this.invoiceService = invoiceService;
        this.accountingService = accountingService;
        this.hrService = hrService;
        this.reportService = reportService;
        this.orderAutoApprovalStateRepository = orderAutoApprovalStateRepository;
    }

    public InventoryReservationResult reserveInventory(String orderId, String companyId) {
        return withCompanyContext(companyId, () -> {
            Long id = parseNumericId(orderId);
            if (id == null) {
                return null;
            }
            SalesOrder order = salesService.getOrderWithItems(id);
            InventoryReservationResult reservation = finishedGoodsService.reserveForOrder(order);
            if (!reservation.shortages().isEmpty()) {
                scheduleUrgentProduction(order, reservation.shortages());
                log.warn("Order {} has {} pending shortage line(s); queued urgent production", id, reservation.shortages().size());
            } else {
                log.info("Reserved inventory for order {}", id);
            }
            salesService.updateStatus(id, "RESERVED");
            return reservation;
        });
    }

    public void queueProduction(String orderId, String companyId) {
        runWithCompanyContext(companyId, () -> {
            ProductionPlanRequest request = new ProductionPlanRequest(
                    "PLAN-" + orderId,
                    "Order " + orderId,
                    1.0,
                    LocalDate.now().plusDays(1),
                    "Auto-generated from orchestrator");
            factoryService.createPlan(request);
            log.info("Queued production plan for order {}", orderId);
        });
    }

    public void createAccountingEntry(String orderId, BigDecimal amount, String companyId) {
        runWithCompanyContext(companyId, () -> postSalesJournal(orderId, amount));
    }

    private void postSalesJournal(String orderId, BigDecimal amountOverride) {
        Long id = parseNumericId(orderId);
        if (id == null) {
            return;
        }
        SalesOrder order = salesService.getOrderWithItems(id);
        Dealer dealer = order.getDealer();
        if (dealer == null) {
            throw new IllegalStateException("Dealer is required to post a sales journal");
        }
        Account receivableAccount = dealer.getReceivableAccount();
        if (receivableAccount == null) {
            throw new IllegalStateException("Dealer " + dealer.getName() + " is missing a receivable account");
        }
        BigDecimal journalAmount = amountOverride != null ? amountOverride : order.getTotalAmount();
        if (journalAmount == null || journalAmount.compareTo(BigDecimal.ZERO) == 0) {
            log.info("Skipping sales journal for order {} because amount is zero", orderId);
            return;
        }
        List<SalesOrderItem> items = order.getItems();
        if (items == null || items.isEmpty()) {
            log.warn("Order {} has no items; skipping sales journal", orderId);
            return;
        }
        List<String> productCodes = items.stream()
                .map(SalesOrderItem::getProductCode)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        Map<String, FinishedGoodsService.FinishedGoodAccountingProfile> profiles =
                finishedGoodsService.accountingProfiles(productCodes);
        Long fallbackRevenueAccount = resolveRevenueAccountId()
                .orElse(null);
        Map<Long, BigDecimal> revenueLines = new LinkedHashMap<>();
        Map<Long, BigDecimal> taxLines = new LinkedHashMap<>();
        for (SalesOrderItem item : items) {
            String productCode = item.getProductCode();
            FinishedGoodsService.FinishedGoodAccountingProfile profile = profiles.get(productCode);
            Long revenueAccountId = profile != null ? profile.revenueAccountId() : null;
            if (revenueAccountId == null) {
                if (fallbackRevenueAccount == null) {
                    throw new IllegalStateException("Finished good " + productCode + " missing revenue account");
                }
                log.warn("Finished good {} missing revenue account; falling back to {}", productCode, fallbackRevenueAccount);
                revenueAccountId = fallbackRevenueAccount;
            }
            BigDecimal lineSubtotal = item.getLineSubtotal() != null
                    ? item.getLineSubtotal()
                    : item.getQuantity().multiply(item.getUnitPrice());
            if (lineSubtotal != null && lineSubtotal.compareTo(BigDecimal.ZERO) > 0) {
                revenueLines.merge(revenueAccountId, lineSubtotal, BigDecimal::add);
            }
            BigDecimal lineTax = item.getGstAmount() != null ? item.getGstAmount() : BigDecimal.ZERO;
            if (lineTax.compareTo(BigDecimal.ZERO) > 0) {
                Long taxAccountId = profile != null ? profile.taxAccountId() : null;
                if (taxAccountId == null) {
                    throw new IllegalStateException("Finished good " + productCode + " missing tax account for GST posting");
                }
                taxLines.merge(taxAccountId, lineTax, BigDecimal::add);
            }
        }
        if (revenueLines.isEmpty()) {
            throw new IllegalStateException("No revenue lines derived for order " + order.getOrderNumber());
        }
        BigDecimal totalCredits = revenueLines.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add)
                .add(taxLines.values().stream().reduce(BigDecimal.ZERO, BigDecimal::add));
        BigDecimal delta = journalAmount.abs().subtract(totalCredits);
        if (delta.compareTo(BigDecimal.ZERO) != 0) {
            Long firstAccount = revenueLines.keySet().iterator().next();
            revenueLines.merge(firstAccount, delta, BigDecimal::add);
        }
        String memo = "Sales order " + order.getOrderNumber();
        List<JournalEntryRequest.JournalLineRequest> lines = new ArrayList<>();
        lines.add(new JournalEntryRequest.JournalLineRequest(
                receivableAccount.getId(), memo, journalAmount.abs(), BigDecimal.ZERO));
        revenueLines.forEach((accountId, amount) -> {
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(new JournalEntryRequest.JournalLineRequest(accountId, memo, BigDecimal.ZERO, amount.abs()));
            }
        });
        taxLines.forEach((accountId, amount) -> {
            if (amount.compareTo(BigDecimal.ZERO) > 0) {
                lines.add(new JournalEntryRequest.JournalLineRequest(accountId, memo, BigDecimal.ZERO, amount.abs()));
            }
        });
        accountingService.createJournalEntry(new JournalEntryRequest(
                "SALE-" + orderId,
                LocalDate.now(),
                memo,
                dealer.getId(),
                lines
        ));
    }

    private void postCogsEntry(String orderId, List<FinishedGoodsService.DispatchPosting> postings) {
        if (postings == null || postings.isEmpty()) {
            return;
        }
        List<JournalEntryRequest.JournalLineRequest> lines = new ArrayList<>();
        for (FinishedGoodsService.DispatchPosting posting : postings) {
            if (posting.cogsAccountId() == null || posting.inventoryAccountId() == null) {
                continue;
            }
            BigDecimal cost = posting.cost();
            if (cost == null || cost.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            String memo = "COGS for order " + orderId;
            lines.add(new JournalEntryRequest.JournalLineRequest(posting.cogsAccountId(), memo, cost, BigDecimal.ZERO));
            lines.add(new JournalEntryRequest.JournalLineRequest(posting.inventoryAccountId(), memo, BigDecimal.ZERO, cost));
        }
        if (lines.isEmpty()) {
            return;
        }
        JournalEntryRequest request = new JournalEntryRequest(
                "COGS-" + orderId + "-" + UUID.randomUUID().toString().substring(0, 6).toUpperCase(),
                LocalDate.now(),
                "COGS posting for order " + orderId,
                null,
                lines
        );
        accountingService.createJournalEntry(request);
    }

    @Transactional
    public AutoApprovalResult autoApproveOrder(String orderId, BigDecimal amount, String companyId) {
        String normalizedCompanyId = normalizeCompanyId(companyId);
        if (normalizedCompanyId == null) {
            log.warn("Cannot auto-approve order {} without a company context", orderId);
            return new AutoApprovalResult("PENDING_PRODUCTION", true);
        }
        Long numericId = parseNumericId(orderId);
        if (numericId == null) {
            return new AutoApprovalResult("PENDING_PRODUCTION", true);
        }
        AtomicReference<String> status = new AtomicReference<>("PENDING_PRODUCTION");
        AtomicBoolean awaitingProduction = new AtomicBoolean(false);
        runWithCompanyContext(normalizedCompanyId, () -> {
            OrderAutoApprovalState state = lockAutoApprovalState(normalizedCompanyId, numericId);
            if (state.isCompleted()) {
                log.info("Auto-approval already completed for order {} (company {})", orderId, normalizedCompanyId);
                status.set("APPROVED");
                return;
            }
            state.startAttempt();
            try {
                InventoryReservationResult reservation = null;
                if (!state.isInventoryReserved()) {
                    reservation = reserveInventory(orderId, normalizedCompanyId);
                    awaitingProduction.set(reservation != null && !reservation.shortages().isEmpty());
                    state.markInventoryReserved();
                } else if (reservation == null) {
                    awaitingProduction.set(false);
                }
                if (!state.isOrderStatusUpdated()) {
                    salesService.updateStatus(numericId, awaitingProduction.get() ? "PENDING_PRODUCTION" : "READY_TO_SHIP");
                    state.markOrderStatusUpdated();
                }
                log.info("Auto-approved order {} for company {}; awaitingProduction={}", orderId, normalizedCompanyId, awaitingProduction.get());
                status.set(awaitingProduction.get() ? "PENDING_PRODUCTION" : "READY_TO_SHIP");
            } catch (RuntimeException ex) {
                state.markFailed(ex.getMessage());
                status.set("FAILED");
                log.error("Auto-approval failed for order {} (company {})", orderId, normalizedCompanyId, ex);
                throw ex;
            }
        });
        return new AutoApprovalResult(status.get(), awaitingProduction.get());
    }

    public void updateProductionStatus(String planId, String companyId) {
        runWithCompanyContext(companyId, () -> {
            Long id = parseNumericId(planId);
            if (id != null) {
                ProductionPlanDto plan = factoryService.updatePlanStatus(id, "COMPLETED");
                log.info("Marked production plan {} as completed", planId);
                extractOrderIdFromPlan(plan)
                        .ifPresent(orderId -> {
                            AutoApprovalResult result = autoApproveOrder(String.valueOf(orderId), null, companyId);
                            log.info("Resumed auto-approval for order {} after plan completion; status={}, awaitingProduction={}",
                                    orderId, result.orderStatus(), result.awaitingProduction());
                        });
            }
        });
    }

    public AutoApprovalResult updateFulfillment(String orderId, String requestedStatus, String companyId) {
        return withCompanyContext(companyId, () -> {
            Long id = parseNumericId(orderId);
            if (id == null) {
                return new AutoApprovalResult("INVALID", false);
            }
            String status = requestedStatus == null ? "" : requestedStatus.trim().toUpperCase();
            switch (status) {
                case "PROCESSING":
                    salesService.updateStatus(id, "PROCESSING");
                    return new AutoApprovalResult("PROCESSING", false);
                case "CANCELLED":
                    OrderAutoApprovalState state = lockAutoApprovalState(companyId, id);
                    state.markFailed("Cancelled");
                    salesService.updateStatus(id, "CANCELLED");
                    return new AutoApprovalResult("CANCELLED", false);
                case "READY_TO_SHIP":
                    return autoApproveOrder(orderId, null, companyId);
                case "SHIPPED":
                    return finalizeShipment(orderId, companyId);
                default:
                    throw new IllegalArgumentException("Unsupported fulfillment status: " + requestedStatus);
            }
        });
    }

    public void releaseInventory(String batchId, String companyId) {
        runWithCompanyContext(companyId, () -> {
            ProductionBatchRequest request = new ProductionBatchRequest(
                    batchId + "-DISPATCH",
                    0.0,
                    "system",
                    "Auto release for dispatch " + batchId);
            factoryService.logBatch(null, request);
            log.info("Logged release batch {}", batchId);
        });
    }

    public void postDispatchJournal(String batchId, String companyId) {
        runWithCompanyContext(companyId, () ->
                postJournal("DISPATCH-" + batchId, BigDecimal.ZERO, "Dispatch journal for batch " + batchId));
    }

    public void syncEmployees(String companyId) {
        runWithCompanyContext(companyId, () -> {
            hrService.listEmployees();
            log.info("Synced employees view for company {}", companyId);
        });
    }

    public void generatePayroll(LocalDate payrollDate, String companyId) {
        runWithCompanyContext(companyId, () -> {
            hrService.createPayrollRun(new PayrollRunRequest(payrollDate, "Auto payroll run"));
            log.info("Triggered payroll run for {}", payrollDate);
        });
    }

    public void postPayrollVouchers(LocalDate payrollDate, String companyId) {
        runWithCompanyContext(companyId, () ->
                postJournal("PAYROLL-" + payrollDate, BigDecimal.ZERO, "Payroll vouchers for " + payrollDate));
    }

    public Map<String, Object> health() {
        Map<String, Object> health = new HashMap<>();
        health.put("orders", salesService.listOrders(null).size());
        health.put("plans", factoryService.listPlans().size());
        health.put("accounts", accountingService.listAccounts().size());
        health.put("employees", hrService.listEmployees().size());
        return health;
    }

    public Map<String, Object> fetchAdminDashboard(String companyId) {
        return withCompanyContext(companyId, () -> {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("orders", fetchOrdersSnapshot());
            snapshot.put("dealers", fetchDealerSnapshot());
            snapshot.put("accounting", fetchAccountingSnapshot());
            snapshot.put("hr", fetchHrSnapshot());
            return snapshot;
        });
    }

    public Map<String, Object> fetchFactoryDashboard(String companyId) {
        return withCompanyContext(companyId, () -> {
            Map<String, Object> snapshot = new HashMap<>();
            FactoryDashboardDto dashboard = factoryService.dashboard();
            snapshot.put("production", Map.of(
                    "efficiency", dashboard.productionEfficiency(),
                    "completed", dashboard.completedPlans(),
                    "batchesLogged", dashboard.batchesLogged()));
            snapshot.put("tasks", factoryService.listTasks().size());
            snapshot.put("inventory", fetchInventorySnapshot());
            return snapshot;
        });
    }

    public Map<String, Object> fetchFinanceDashboard(String companyId) {
        return withCompanyContext(companyId, () -> {
            Map<String, Object> snapshot = new HashMap<>();
            snapshot.put("cashflow", fetchCashflowSnapshot());
            snapshot.put("agedDebtors", reportService.agedDebtors());
            snapshot.put("ledger", fetchAccountingSnapshot());
            return snapshot;
        });
    }

    private Map<String, Object> fetchOrdersSnapshot() {
        List<SalesOrderDto> orders = salesService.listOrders(null);
        long pending = orders.stream().filter(o -> "PENDING".equalsIgnoreCase(o.status())).count();
        long approved = orders.stream().filter(o -> "CONFIRMED".equalsIgnoreCase(o.status())
                || "APPROVED".equalsIgnoreCase(o.status())).count();
        return Map.of("pending", pending, "approved", approved, "total", orders.size());
    }

    private Map<String, Object> fetchDealerSnapshot() {
        List<DealerDto> dealers = salesService.listDealers();
        long active = dealers.stream().filter(d -> "ACTIVE".equalsIgnoreCase(d.status())).count();
        BigDecimal outstanding = dealers.stream()
                .map(DealerDto::outstandingBalance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("active", active, "total", dealers.size(), "creditUtilization", outstanding);
    }

    private Map<String, Object> fetchAccountingSnapshot() {
        List<AccountDto> accounts = accountingService.listAccounts();
        BigDecimal balance = accounts.stream()
                .map(AccountDto::balance)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return Map.of("accounts", accounts.size(), "ledgerBalance", balance);
    }

    private Map<String, Object> fetchHrSnapshot() {
        List<EmployeeDto> employees = hrService.listEmployees();
        long active = employees.stream().filter(e -> "ACTIVE".equalsIgnoreCase(e.status())).count();
        long onLeave = hrService.listLeaveRequests().stream()
                .filter(l -> "APPROVED".equalsIgnoreCase(l.status()))
                .count();
        return Map.of("activeEmployees", active, "onLeave", onLeave);
    }

    private Map<String, Object> fetchInventorySnapshot() {
        InventoryValuationDto valuation = reportService.inventoryValuation();
        return Map.of("value", valuation.totalValue(), "lowStock", valuation.lowStockItems());
    }

    private Map<String, Object> fetchCashflowSnapshot() {
        CashFlowDto cashflow = reportService.cashFlow();
        return Map.of("operating", cashflow.operating(),
                "investing", cashflow.investing(),
                "financing", cashflow.financing(),
                "net", cashflow.netChange());
    }

    private void scheduleUrgentProduction(SalesOrder order, List<InventoryShortage> shortages) {
        if (shortages == null || shortages.isEmpty()) {
            return;
        }
        LocalDate today = LocalDate.now();
        for (InventoryShortage shortage : shortages) {
            ProductionPlanRequest planRequest = new ProductionPlanRequest(
                    "URG-" + order.getId() + "-" + shortage.productCode(),
                    shortage.productName() + " (" + shortage.productCode() + ")",
                    shortage.shortageQuantity().doubleValue(),
                    today,
                    "Urgent replenishment for order " + order.getOrderNumber());
            factoryService.createPlan(planRequest);

            FactoryTaskRequest taskRequest = new FactoryTaskRequest(
                    "Urgent build " + shortage.productCode(),
                    "Short by " + shortage.shortageQuantity() + " units for order " + order.getOrderNumber(),
                    "production",
                    "URGENT",
                    today.plusDays(1));
            factoryService.createTask(taskRequest);
        }
    }

    private OrderAutoApprovalState lockAutoApprovalState(String companyId, Long orderId) {
        return orderAutoApprovalStateRepository.findByCompanyCodeAndOrderId(companyId, orderId)
                .orElseGet(() -> {
                    orderAutoApprovalStateRepository.save(new OrderAutoApprovalState(companyId, orderId));
                    return orderAutoApprovalStateRepository.findByCompanyCodeAndOrderId(companyId, orderId)
                            .orElseThrow(() -> new IllegalStateException("Unable to initialize auto-approval state"));
                });
    }

    private void runWithCompanyContext(String companyId, Runnable action) {
        withCompanyContext(companyId, () -> {
            action.run();
            return null;
        });
    }

    private <T> T withCompanyContext(String companyId, Supplier<T> callback) {
        String normalizedCompanyId = normalizeCompanyId(companyId);
        String previousCompany = CompanyContextHolder.getCompanyId();
        boolean changed = normalizedCompanyId != null && !Objects.equals(previousCompany, normalizedCompanyId);
        if (changed) {
            CompanyContextHolder.setCompanyId(normalizedCompanyId);
        }
        try {
            return callback.get();
        } finally {
            if (changed) {
                if (previousCompany != null) {
                    CompanyContextHolder.setCompanyId(previousCompany);
                } else {
                    CompanyContextHolder.clear();
                }
            }
        }
    }

    private String normalizeCompanyId(String companyId) {
        if (companyId == null) {
            return null;
        }
        String trimmed = companyId.trim();
        return trimmed.isEmpty() ? null : trimmed;
    }

    private Optional<Long> extractOrderIdFromPlan(ProductionPlanDto plan) {
        if (plan == null) {
            return Optional.empty();
        }
        if (plan.planNumber() != null) {
            String[] parts = plan.planNumber().split("-");
            if (parts.length >= 2) {
                Optional<Long> parsed = parseLong(parts[1]);
                if (parsed.isPresent()) {
                    return parsed;
                }
            }
        }
        if (plan.notes() != null) {
            String[] tokens = plan.notes().split("\\D+");
            for (String token : tokens) {
                Optional<Long> parsed = parseLong(token);
                if (parsed.isPresent()) {
                    return parsed;
                }
            }
        }
        return Optional.empty();
    }

    private Optional<Long> parseLong(String value) {
        if (value == null || value.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Long.parseLong(value));
        } catch (NumberFormatException ex) {
            return Optional.empty();
        }
    }

    private Optional<Long> resolveRevenueAccountId() {
        ensureDefaultAccounts();
        return accountingService.listAccounts().stream()
                .filter(acc -> "REVENUE".equalsIgnoreCase(acc.type()))
                .map(AccountDto::id)
                .findFirst();
    }

    private void postJournal(String reference, BigDecimal amount, String memo) {
        ensureDefaultAccounts();
        List<AccountDto> accounts = accountingService.listAccounts();
        if (accounts.size() < 2) {
            log.warn("Unable to post journal entry; not enough accounts");
            return;
        }
        AccountDto debit = accounts.get(0);
        AccountDto credit = accounts.get(1);
        JournalEntryRequest request = new JournalEntryRequest(
                reference,
                LocalDate.now(),
                memo,
                null,
                List.of(
                        new JournalEntryRequest.JournalLineRequest(debit.id(), memo, amount.abs(), BigDecimal.ZERO),
                        new JournalEntryRequest.JournalLineRequest(credit.id(), memo, BigDecimal.ZERO, amount.abs())
                ));
        accountingService.createJournalEntry(request);
    }

    private void ensureDefaultAccounts() {
        List<AccountDto> accounts = accountingService.listAccounts();
        if (accounts.size() >= 2) {
            return;
        }
        String suffix = UUID.randomUUID().toString().substring(0, 4).toUpperCase();
        accountingService.createAccount(new AccountRequest("1000-" + suffix, "Auto Cash", "ASSET"));
        accountingService.createAccount(new AccountRequest("4000-" + suffix, "Auto Revenue", "REVENUE"));
    }

    private AutoApprovalResult finalizeShipment(String orderId, String companyId) {
        return withCompanyContext(companyId, () -> {
            Long numericId = parseNumericId(orderId);
            if (numericId == null) {
                return new AutoApprovalResult("INVALID", false);
            }
            SalesOrder order = salesService.getOrderWithItems(numericId);
            OrderAutoApprovalState state = lockAutoApprovalState(companyId, numericId);
            if (!state.isSalesJournalPosted()) {
                createAccountingEntry(orderId, order.getTotalAmount(), companyId);
                state.markSalesJournalPosted();
            }
            if (!state.isDispatchFinalized()) {
                List<FinishedGoodsService.DispatchPosting> postings = finishedGoodsService.markSlipDispatched(numericId);
                postCogsEntry(orderId, postings);
                state.markDispatchFinalized();
            }
            if (!state.isInvoiceIssued()) {
                invoiceService.issueInvoiceForOrder(numericId);
                state.markInvoiceIssued();
            }
            salesService.updateStatus(numericId, "SHIPPED");
            state.markOrderStatusUpdated();
            state.markCompleted();
            log.info("Finalized shipment for order {}", orderId);
            return new AutoApprovalResult("SHIPPED", false);
        });
    }

    private Long parseNumericId(String id) {
        try {
            return Long.parseLong(id);
        } catch (NumberFormatException ex) {
            log.warn("Value {} is not a numeric identifier", id);
            return null;
        }
    }

    public record AutoApprovalResult(String orderStatus, boolean awaitingProduction) {}
}
