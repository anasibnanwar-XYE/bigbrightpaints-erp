package com.bigbrightpaints.erp.modules.inventory.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryRequest;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingService;
import com.bigbrightpaints.erp.modules.accounting.service.ReferenceNumberService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryMovement;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryMovementRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovement;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovementRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialRepository;
import com.bigbrightpaints.erp.modules.inventory.dto.OpeningStockImportResponse;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.time.LocalDate;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;

@ExtendWith(MockitoExtension.class)
class OpeningStockImportServiceTest {

    @Mock private CompanyContextService companyContextService;
    @Mock private RawMaterialRepository rawMaterialRepository;
    @Mock private RawMaterialBatchRepository rawMaterialBatchRepository;
    @Mock private RawMaterialMovementRepository rawMaterialMovementRepository;
    @Mock private FinishedGoodRepository finishedGoodRepository;
    @Mock private FinishedGoodBatchRepository finishedGoodBatchRepository;
    @Mock private InventoryMovementRepository inventoryMovementRepository;
    @Mock private BatchNumberService batchNumberService;
    @Mock private RawMaterialService rawMaterialService;
    @Mock private FinishedGoodsService finishedGoodsService;
    @Mock private AccountingService accountingService;
    @Mock private AccountRepository accountRepository;
    @Mock private ReferenceNumberService referenceNumberService;
    @Mock private CompanyClock companyClock;

    private OpeningStockImportService service;
    private Company company;

    @BeforeEach
    void setup() {
        service = new OpeningStockImportService(
                companyContextService,
                rawMaterialRepository,
                rawMaterialBatchRepository,
                rawMaterialMovementRepository,
                finishedGoodRepository,
                finishedGoodBatchRepository,
                inventoryMovementRepository,
                batchNumberService,
                rawMaterialService,
                finishedGoodsService,
                accountingService,
                accountRepository,
                referenceNumberService,
                companyClock
        );

        company = new Company();
        org.springframework.test.util.ReflectionTestUtils.setField(company, "id", 1L);
        company.setCode("TEST");
        company.setTimezone("UTC");
        company.setDefaultInventoryAccountId(100L);
        when(companyContextService.requireCurrentCompany()).thenReturn(company);

        when(companyClock.today(company)).thenReturn(LocalDate.now());
    }

    @Test
    void importOpeningStock_CallsFindBy_RepeatedlyForDuplicates() {
        // Setup data
        String csv = """
                type,sku,name,unit,unit_type,batch_code,quantity,unit_cost
                RAW_MATERIAL,RM1,Raw Mat 1,KG,KG,B1,10,5
                RAW_MATERIAL,RM1,Raw Mat 1,KG,KG,B2,20,5
                FINISHED_GOOD,FG1,Fin Good 1,PCS,,F1,5,10
                FINISHED_GOOD,FG1,Fin Good 1,PCS,,F2,5,10
                """;

        MockMultipartFile file = new MockMultipartFile("file", "test.csv", "text/csv", csv.getBytes(StandardCharsets.UTF_8));

        // Mock behaviors
        RawMaterial rm1 = new RawMaterial();
        rm1.setSku("RM1");
        rm1.setUnitType("KG");
        rm1.setInventoryAccountId(101L);

        FinishedGood fg1 = new FinishedGood();
        fg1.setProductCode("FG1");
        fg1.setUnit("PCS");
        fg1.setValuationAccountId(102L);

        // Current behavior: called for every row
        when(rawMaterialRepository.findByCompanyAndSku(company, "RM1")).thenReturn(Optional.of(rm1));
        when(finishedGoodRepository.findByCompanyAndProductCode(company, "FG1")).thenReturn(Optional.of(fg1));

        when(rawMaterialBatchRepository.save(any(RawMaterialBatch.class))).thenAnswer(i -> i.getArgument(0));
        when(finishedGoodBatchRepository.save(any(FinishedGoodBatch.class))).thenAnswer(i -> i.getArgument(0));
        when(rawMaterialMovementRepository.save(any(RawMaterialMovement.class))).thenAnswer(i -> i.getArgument(0));
        when(inventoryMovementRepository.save(any(InventoryMovement.class))).thenAnswer(i -> i.getArgument(0));

        // Mock account for opening balance
        Account equity = new Account();
        equity.setType(AccountType.EQUITY);
        when(accountRepository.findByCompanyAndCodeIgnoreCase(company, "OPEN-BAL")).thenReturn(Optional.of(equity));

        // Mock Journal Entry creation
        JournalEntryDto jeDto = new JournalEntryDto(
                999L, null, "REF", LocalDate.now(), "Desc", null, null, null, null, null,
                null, null, null, null, null, null, null, null, null, null, null, null, null, null, null
        );
        when(accountingService.createJournalEntry(any(JournalEntryRequest.class))).thenReturn(jeDto);

        // Execute
        OpeningStockImportResponse response = service.importOpeningStock(file);

        // Verify results
        assertEquals(0, response.errors().size());
        assertEquals(4, response.rowsProcessed());
        assertEquals(2, response.rawMaterialBatchesCreated());
        assertEquals(2, response.finishedGoodBatchesCreated());

        // Verify optimized calls (After Caching)
        // Expected: 1 call for RM1, 1 call for FG1 (subsequent lookups hit the cache)
        verify(rawMaterialRepository, times(1)).findByCompanyAndSku(company, "RM1");
        verify(finishedGoodRepository, times(1)).findByCompanyAndProductCode(company, "FG1");
    }
}
