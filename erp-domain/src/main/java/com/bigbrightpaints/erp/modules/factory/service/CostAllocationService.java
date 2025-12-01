package com.bigbrightpaints.erp.modules.factory.service;

import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingFacade;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.factory.domain.ProductionLog;
import com.bigbrightpaints.erp.modules.factory.domain.ProductionLogRepository;
import com.bigbrightpaints.erp.modules.factory.dto.CostAllocationRequest;
import com.bigbrightpaints.erp.modules.factory.dto.CostAllocationResponse;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatchRepository;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.Instant;
import java.time.LocalDate;
import java.time.YearMonth;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.List;

@Service
public class CostAllocationService {

    private final CompanyContextService companyContextService;
    private final ProductionLogRepository productionLogRepository;
    private final FinishedGoodBatchRepository finishedGoodBatchRepository;
    private final AccountingFacade accountingFacade;
    private final CompanyEntityLookup companyEntityLookup;

    public CostAllocationService(CompanyContextService companyContextService,
                                 ProductionLogRepository productionLogRepository,
                                 FinishedGoodBatchRepository finishedGoodBatchRepository,
                                 AccountingFacade accountingFacade,
                                 CompanyEntityLookup companyEntityLookup) {
        this.companyContextService = companyContextService;
        this.productionLogRepository = productionLogRepository;
        this.finishedGoodBatchRepository = finishedGoodBatchRepository;
        this.accountingFacade = accountingFacade;
        this.companyEntityLookup = companyEntityLookup;
    }

    @Transactional
    public CostAllocationResponse allocateCosts(CostAllocationRequest request) {
        Company company = companyContextService.requireCurrentCompany();

        // Calculate month start and end dates
        YearMonth yearMonth = YearMonth.of(request.year(), request.month());
        LocalDate startDate = yearMonth.atDay(1);
        LocalDate endDate = yearMonth.atEndOfMonth().plusDays(1);

        Instant startInstant = startDate.atStartOfDay(ZoneId.systemDefault()).toInstant();
        Instant endInstant = endDate.atStartOfDay(ZoneId.systemDefault()).toInstant();

        // Find all fully packed batches in this month
        List<ProductionLog> batches = productionLogRepository.findFullyPackedBatchesByMonth(
                company, startInstant, endInstant);

        BigDecimal laborCost = request.laborCost() == null ? BigDecimal.ZERO : request.laborCost();
        BigDecimal overheadCost = request.overheadCost() == null ? BigDecimal.ZERO : request.overheadCost();
        if (laborCost.compareTo(BigDecimal.ZERO) <= 0 && overheadCost.compareTo(BigDecimal.ZERO) <= 0) {
            return new CostAllocationResponse(
                    request.year(),
                    request.month(),
                    batches.size(),
                    BigDecimal.ZERO,
                    laborCost,
                    overheadCost,
                    BigDecimal.ZERO,
                    List.of(),
                    "Labor and overhead costs must be greater than zero to allocate"
            );
        }

        if (batches.isEmpty()) {
            return new CostAllocationResponse(
                    request.year(),
                    request.month(),
                    0,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    BigDecimal.ZERO,
                    List.of(),
                    "No fully packed batches found for this period"
            );
        }

        // Calculate total liters produced
        BigDecimal totalLitersProduced = batches.stream()
                .map(ProductionLog::getMixedQuantity)
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (totalLitersProduced.compareTo(BigDecimal.ZERO) <= 0) {
            return new CostAllocationResponse(
                    request.year(),
                    request.month(),
                    batches.size(),
                    BigDecimal.ZERO,
                    laborCost,
                    overheadCost,
                    BigDecimal.ZERO,
                    List.of(),
                    "Total liters produced is zero or negative"
            );
        }

        // Calculate cost per liter
        BigDecimal totalCosts = laborCost.add(overheadCost);
        if (totalCosts.compareTo(BigDecimal.ZERO) <= 0) {
            return new CostAllocationResponse(
                    request.year(),
                    request.month(),
                    batches.size(),
                    totalLitersProduced,
                    laborCost,
                    overheadCost,
                    BigDecimal.ZERO,
                    List.of(),
                    "Total allocation costs must be greater than zero"
            );
        }
        BigDecimal costPerLiter = totalCosts.divide(totalLitersProduced, 4, RoundingMode.HALF_UP);

        // Find accounting accounts
        Account finishedGoodsAccount = requireAccount(company, request.finishedGoodsAccountId(), AccountType.ASSET);
        Account payrollExpenseAccount = requireAccount(company, request.laborExpenseAccountId(), AccountType.EXPENSE);
        Account overheadExpenseAccount = requireAccount(company, request.overheadExpenseAccountId(), AccountType.EXPENSE);

        List<Long> journalEntryIds = new ArrayList<>();

        // Allocate costs to each batch
        for (ProductionLog batch : batches) {
            BigDecimal batchLiters = batch.getMixedQuantity();
            BigDecimal batchLaborCost = batchLiters
                    .multiply(laborCost)
                    .divide(totalLitersProduced, 4, RoundingMode.HALF_UP);
            BigDecimal batchOverheadCost = batchLiters
                    .multiply(overheadCost)
                    .divide(totalLitersProduced, 4, RoundingMode.HALF_UP);

            // Update production log
            batch.setLaborCostTotal(batchLaborCost);
            batch.setOverheadCostTotal(batchOverheadCost);

            // Recalculate unit cost (material + labor + overhead) / packed quantity
            BigDecimal totalBatchCost = batch.getMaterialCostTotal()
                    .add(batchLaborCost)
                    .add(batchOverheadCost);

            BigDecimal packedQty = batch.getTotalPackedQuantity();
            if (packedQty.compareTo(BigDecimal.ZERO) > 0) {
                BigDecimal newUnitCost = totalBatchCost.divide(packedQty, 4, RoundingMode.HALF_UP);
                batch.setUnitCost(newUnitCost);

                // Note: Direct link between ProductionLog and FinishedGoodBatch not implemented
                // Unit cost updates would need to be done through batch tracking system
            }

            productionLogRepository.save(batch);

            // Post journal entry to move costs from expense accounts to inventory
            if (batchLaborCost.add(batchOverheadCost).compareTo(BigDecimal.ZERO) > 0) {
                JournalEntryDto journalEntry = accountingFacade.postCostAllocation(
                        batch.getProductionCode(),
                        finishedGoodsAccount.getId(),
                        payrollExpenseAccount.getId(),
                        overheadExpenseAccount.getId(),
                        batchLaborCost,
                        batchOverheadCost,
                        request.notes() != null ? request.notes() : "Cost Allocation"
                );

                if (journalEntry != null) {
                    journalEntryIds.add(journalEntry.id());
                }
            }
        }

        String summary = String.format(
                "Allocated %s in labor and %s in overhead across %d batches (%.2f liters)",
                request.laborCost(),
                request.overheadCost(),
                batches.size(),
                totalLitersProduced
        );

        return new CostAllocationResponse(
                request.year(),
                request.month(),
                batches.size(),
                totalLitersProduced,
                request.laborCost(),
                request.overheadCost(),
                costPerLiter,
                journalEntryIds,
                summary
        );
    }

    private Account requireAccount(Company company, Long accountId, AccountType expectedType) {
        Account account = companyEntityLookup.requireAccount(company, accountId);
        if (account.getType() != expectedType) {
            throw new IllegalStateException("Account " + account.getCode() + " is not of type " + expectedType);
        }
        return account;
    }
}
