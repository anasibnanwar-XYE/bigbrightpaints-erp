package com.bigbrightpaints.erp.codered;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryMovement;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryMovementRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryReference;
import com.bigbrightpaints.erp.modules.inventory.domain.OpeningStockImport;
import com.bigbrightpaints.erp.modules.inventory.domain.OpeningStockImportRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovement;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovementRepository;
import com.bigbrightpaints.erp.modules.inventory.dto.OpeningStockImportResponse;
import com.bigbrightpaints.erp.modules.inventory.service.OpeningStockImportService;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

class CR_OpeningStockImportIdempotencyIT extends AbstractIntegrationTest {

    private static final String COMPANY_CODE = "CR-OPEN-IDEMP";
    private static final String IDEMPOTENCY_KEY = "OPEN-STOCK-IDEMP-001";

    @Autowired
    private CompanyRepository companyRepository;
    @Autowired
    private AccountRepository accountRepository;
    @Autowired
    private OpeningStockImportService openingStockImportService;
    @Autowired
    private OpeningStockImportRepository openingStockImportRepository;
    @Autowired
    private RawMaterialMovementRepository rawMaterialMovementRepository;
    @Autowired
    private InventoryMovementRepository inventoryMovementRepository;

    private Company company;

    @BeforeEach
    void setUp() {
        company = companyRepository.findByCodeIgnoreCase(COMPANY_CODE)
                .orElseGet(() -> {
                    Company created = new Company();
                    created.setCode(COMPANY_CODE);
                    created.setName("CR Opening Stock Idempotency");
                    created.setTimezone("UTC");
                    return companyRepository.save(created);
                });
        Account inventory = ensureAccount(company, "INV-OPEN", "Inventory", AccountType.ASSET);
        ensureAccount(company, "COGS-OPEN", "COGS", AccountType.COGS);
        ensureAccount(company, "REV-OPEN", "Revenue", AccountType.REVENUE);
        ensureAccount(company, "GST-OPEN", "GST Output", AccountType.LIABILITY);

        company.setDefaultInventoryAccountId(inventory.getId());
        company.setDefaultCogsAccountId(accountRepository.findByCompanyAndCodeIgnoreCase(company, "COGS-OPEN").orElseThrow().getId());
        company.setDefaultRevenueAccountId(accountRepository.findByCompanyAndCodeIgnoreCase(company, "REV-OPEN").orElseThrow().getId());
        company.setDefaultTaxAccountId(accountRepository.findByCompanyAndCodeIgnoreCase(company, "GST-OPEN").orElseThrow().getId());
        companyRepository.save(company);
        CompanyContextHolder.setCompanyId(COMPANY_CODE);
    }

    @AfterEach
    void tearDown() {
        CompanyContextHolder.clear();
    }

    @Test
    void openingStockImport_isIdempotentForSameKeyAndFile() {
        MockMultipartFile file = csvFile();

        OpeningStockImportResponse first = openingStockImportService.importOpeningStock(file, IDEMPOTENCY_KEY);
        OpeningStockImportResponse second = openingStockImportService.importOpeningStock(file, IDEMPOTENCY_KEY);

        assertThat(second).isEqualTo(first);

        OpeningStockImport record = openingStockImportRepository
                .findByCompanyAndIdempotencyKey(company, IDEMPOTENCY_KEY)
                .orElseThrow();
        assertThat(record.getJournalEntryId()).isNotNull();

        List<RawMaterialMovement> rmMovements = rawMaterialMovementRepository
                .findByRawMaterialCompanyAndReferenceTypeAndReferenceId(
                        company, InventoryReference.OPENING_STOCK, "RM-OPEN-B1");
        assertThat(rmMovements).hasSize(1);

        List<InventoryMovement> fgMovements = inventoryMovementRepository
                .findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                        company, InventoryReference.OPENING_STOCK, "FG-OPEN-B1");
        assertThat(fgMovements).hasSize(1);
    }

    private MockMultipartFile csvFile() {
        String csv = String.join("\n",
                "type,sku,name,unit,unit_type,batch_code,quantity,unit_cost,material_type,manufactured_at",
                "RAW_MATERIAL,RM-OPEN-1,Resin,KG,KG,RM-OPEN-B1,10,5.00,PRODUCTION,",
                "FINISHED_GOOD,FG-OPEN-1,Paint 1L,L,L,FG-OPEN-B1,5,12.50,,2026-01-10"
        );
        return new MockMultipartFile(
                "file",
                "opening.csv",
                "text/csv",
                csv.getBytes(StandardCharsets.UTF_8));
    }

    private Account ensureAccount(Company company, String code, String name, AccountType type) {
        return accountRepository.findByCompanyAndCodeIgnoreCase(company, code)
                .orElseGet(() -> {
                    Account account = new Account();
                    account.setCompany(company);
                    account.setCode(code);
                    account.setName(name);
                    account.setType(type);
                    return accountRepository.save(account);
                });
    }
}
