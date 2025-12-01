package com.bigbrightpaints.erp.modules.accounting.event;

import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.inventory.event.InventoryMovementEvent;
import com.bigbrightpaints.erp.modules.inventory.event.InventoryValuationChangedEvent;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

/**
 * Listens to inventory domain events and creates corresponding GL entries.
 * Ensures tight integration between Inventory and Accounting modules.
 * 
 * This eliminates the need for manual/hardcoded GL postings in inventory operations.
 */
@Component
public class InventoryAccountingEventListener {

    private static final Logger log = LoggerFactory.getLogger(InventoryAccountingEventListener.class);

    private final AccountingService accountingService;
    private final AccountRepository accountRepository;
    private final CompanyRepository companyRepository;

    public InventoryAccountingEventListener(AccountingService accountingService,
                                            AccountRepository accountRepository,
                                            CompanyRepository companyRepository) {
        this.accountingService = accountingService;
        this.accountRepository = accountRepository;
        this.companyRepository = companyRepository;
    }

    /**
     * Handle inventory valuation changes (revaluations, cost method changes, etc.)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void onInventoryValuationChanged(InventoryValuationChangedEvent event) {
        log.info("Processing inventory valuation change: {} {} - {} from {} to {}",
                event.inventoryType(), event.itemCode(), event.reason(),
                event.oldValue(), event.newValue());

        BigDecimal valueChange = event.getValueChange();
        if (valueChange.compareTo(BigDecimal.ZERO) == 0) {
            log.debug("No value change, skipping journal entry");
            return;
        }

        try {
            Company company = companyRepository.findById(event.companyId())
                    .orElseThrow(() -> new IllegalStateException("Company not found: " + event.companyId()));

            // Set company context for the accounting service
            CompanyContextHolder.setCompanyId(company.getCode());

            // Get revaluation account (expense for decreases, income for increases)
            Account revaluationAccount = getRevaluationAccount(company, event.reason());
            Account inventoryAccount = accountRepository.findById(event.inventoryAccountId())
                    .orElseThrow(() -> new IllegalStateException("Inventory account not found"));

            String refNumber = "REVAL-" + event.itemCode() + "-" + System.currentTimeMillis();
            String memo = buildRevaluationMemo(event);

            List<JournalEntryRequest.JournalLineRequest> lines;
            if (event.isIncrease()) {
                // Increase: Debit Inventory, Credit Revaluation Gain
                lines = List.of(
                        new JournalEntryRequest.JournalLineRequest(
                                inventoryAccount.getId(),
                                "Inventory revaluation - " + event.itemName(),
                                valueChange.abs(),
                                BigDecimal.ZERO
                        ),
                        new JournalEntryRequest.JournalLineRequest(
                                revaluationAccount.getId(),
                                "Revaluation gain - " + event.reason(),
                                BigDecimal.ZERO,
                                valueChange.abs()
                        )
                );
            } else {
                // Decrease: Debit Revaluation Loss, Credit Inventory
                lines = List.of(
                        new JournalEntryRequest.JournalLineRequest(
                                revaluationAccount.getId(),
                                "Revaluation loss - " + event.reason(),
                                valueChange.abs(),
                                BigDecimal.ZERO
                        ),
                        new JournalEntryRequest.JournalLineRequest(
                                inventoryAccount.getId(),
                                "Inventory revaluation - " + event.itemName(),
                                BigDecimal.ZERO,
                                valueChange.abs()
                        )
                );
            }

            JournalEntryRequest request = new JournalEntryRequest(
                    refNumber,
                    LocalDate.now(),
                    memo,
                    null, null, false,
                    lines
            );

            accountingService.createJournalEntry(request);
            log.info("Created revaluation journal entry: {}", refNumber);

        } catch (Exception e) {
            log.error("Failed to create revaluation journal entry for {}: {}",
                    event.itemCode(), e.getMessage(), e);
            // Don't rethrow - we don't want to fail the inventory transaction
            // Consider publishing to a dead-letter queue for retry
        }
    }

    /**
     * Handle inventory movements (receipts, issues, transfers)
     */
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.REQUIRES_NEW)
    public void onInventoryMovement(InventoryMovementEvent event) {
        log.info("Processing inventory movement: {} {} - {} units @ {}",
                event.movementType(), event.itemCode(), event.quantity(), event.unitCost());

        if (event.totalCost().compareTo(BigDecimal.ZERO) == 0) {
            log.debug("Zero cost movement, skipping journal entry");
            return;
        }

        // Skip if accounts not specified (let caller handle GL posting)
        if (event.sourceAccountId() == null || event.destinationAccountId() == null) {
            log.debug("Source/destination accounts not specified, skipping auto-posting");
            return;
        }

        try {
            Company company = companyRepository.findById(event.companyId())
                    .orElseThrow(() -> new IllegalStateException("Company not found"));

            CompanyContextHolder.setCompanyId(company.getCode());

            String refNumber = event.referenceNumber() != null 
                    ? event.referenceNumber() 
                    : event.movementType().name() + "-" + event.itemCode() + "-" + System.currentTimeMillis();

            String memo = String.format("%s: %s x %s @ %s",
                    event.movementType(), event.itemCode(), event.quantity(), event.unitCost());

            // Debit destination, Credit source
            List<JournalEntryRequest.JournalLineRequest> lines = List.of(
                    new JournalEntryRequest.JournalLineRequest(
                            event.destinationAccountId(),
                            memo,
                            event.totalCost(),
                            BigDecimal.ZERO
                    ),
                    new JournalEntryRequest.JournalLineRequest(
                            event.sourceAccountId(),
                            memo,
                            BigDecimal.ZERO,
                            event.totalCost()
                    )
            );

            JournalEntryRequest request = new JournalEntryRequest(
                    refNumber,
                    event.movementDate(),
                    event.memo() != null ? event.memo() : memo,
                    null, null, false,
                    lines
            );

            accountingService.createJournalEntry(request);
            log.info("Created movement journal entry: {}", refNumber);

        } catch (Exception e) {
            log.error("Failed to create movement journal entry for {}: {}",
                    event.itemCode(), e.getMessage(), e);
        }
    }

    private Account getRevaluationAccount(Company company, InventoryValuationChangedEvent.ValuationChangeReason reason) {
        // Try to find a specific revaluation account, fall back to generic expense
        String accountCode = switch (reason) {
            case SCRAP_WRITEOFF -> "INV-WRITEOFF";
            case MARKET_REVALUATION -> "INV-REVAL";
            case PHYSICAL_COUNT_ADJUSTMENT -> "INV-ADJUSTMENT";
            default -> "INV-REVAL";
        };

        return accountRepository.findByCompanyAndCodeIgnoreCase(company, accountCode)
                .or(() -> accountRepository.findByCompanyAndCodeIgnoreCase(company, "EXPENSE"))
                .orElseThrow(() -> new IllegalStateException(
                        "No revaluation or expense account found for company " + company.getCode()));
    }

    private String buildRevaluationMemo(InventoryValuationChangedEvent event) {
        return String.format("Inventory revaluation [%s]: %s - %s to %s (Qty: %s, Unit cost: %s → %s)",
                event.reason(),
                event.itemCode(),
                event.oldValue(),
                event.newValue(),
                event.quantity(),
                event.oldUnitCost(),
                event.newUnitCost()
        );
    }
}
