package com.bigbrightpaints.erp.regression;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.factory.dto.BulkPackRequest;
import com.bigbrightpaints.erp.modules.factory.service.BulkPackingService;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.MaterialType;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;

@DisplayName("Regression: Bulk pack rejects fractional quantities")
class BulkPackingFractionalQuantityRegressionIT extends AbstractIntegrationTest {

  private static final String COMPANY_CODE = "LF-018";

  @Autowired private AccountRepository accountRepository;
  @Autowired private FinishedGoodRepository finishedGoodRepository;
  @Autowired private RawMaterialRepository rawMaterialRepository;
  @Autowired private RawMaterialBatchRepository rawMaterialBatchRepository;
  @Autowired private BulkPackingService bulkPackingService;

  private Company company;
  private Account bulkInventory;
  private Account fgInventory;

  @BeforeEach
  void init() {
    company = dataSeeder.ensureCompany(COMPANY_CODE, COMPANY_CODE + " Ltd");
    CompanyContextHolder.setCompanyCode(COMPANY_CODE);
    bulkInventory = ensureAccount("INV-BULK-LF018", AccountType.ASSET);
    fgInventory = ensureAccount("INV-FG-LF018", AccountType.ASSET);
  }

  @AfterEach
  void cleanupContext() {
    CompanyContextHolder.clear();
  }

  @Test
  void fractionalPackQuantitiesAreRejected() {
    FinishedGood child = createFinishedGood("FG-1L-LF018", "Paint 1L", "UNIT", fgInventory);
    RawMaterial bulkMaterial =
        createSemiFinishedMaterial(
            "FG-BULK-LF018-BULK", "Bulk Paint (Semi-Finished)", "L", bulkInventory.getId());
    RawMaterialBatch bulkBatch =
        createBulkBatch(bulkMaterial, new BigDecimal("10"), new BigDecimal("5"));

    BulkPackRequest request =
        new BulkPackRequest(
            bulkBatch.getId(),
            List.of(new BulkPackRequest.PackLine(child.getId(), new BigDecimal("1.5"), "1L", "L")),
            LocalDate.now(),
            "packer",
            null,
            null);

    assertThatThrownBy(() -> bulkPackingService.pack(request))
        .isInstanceOf(ApplicationException.class)
        .satisfies(
            ex -> {
              ApplicationException appEx = (ApplicationException) ex;
              assertThat(appEx.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_INVALID_INPUT);
              assertThat(appEx.getMessage()).contains("whole number");
            });
  }

  private Account ensureAccount(String code, AccountType type) {
    return accountRepository
        .findByCompanyAndCodeIgnoreCase(company, code)
        .orElseGet(
            () -> {
              Account account = new Account();
              account.setCompany(company);
              account.setCode(code);
              account.setName(code);
              account.setType(type);
              return accountRepository.save(account);
            });
  }

  private FinishedGood createFinishedGood(
      String code, String name, String unit, Account valuationAccount) {
    FinishedGood fg = new FinishedGood();
    fg.setCompany(company);
    fg.setProductCode(code);
    fg.setName(name);
    fg.setUnit(unit);
    fg.setCostingMethod("FIFO");
    fg.setValuationAccountId(valuationAccount.getId());
    fg.setCurrentStock(BigDecimal.ZERO);
    fg.setReservedStock(BigDecimal.ZERO);
    return finishedGoodRepository.save(fg);
  }

  private RawMaterial createSemiFinishedMaterial(
      String sku, String name, String unitType, Long inventoryAccountId) {
    RawMaterial material = new RawMaterial();
    material.setCompany(company);
    material.setSku(sku);
    material.setName(name);
    material.setUnitType(unitType);
    material.setMaterialType(MaterialType.PRODUCTION);
    material.setInventoryAccountId(inventoryAccountId);
    material.setCostingMethod("FIFO");
    material.setCurrentStock(BigDecimal.ZERO);
    return rawMaterialRepository.save(material);
  }

  private RawMaterialBatch createBulkBatch(
      RawMaterial bulkMaterial, BigDecimal quantity, BigDecimal unitCost) {
    RawMaterialBatch batch = new RawMaterialBatch();
    batch.setRawMaterial(bulkMaterial);
    batch.setBatchCode("BULK-" + System.currentTimeMillis());
    batch.setQuantity(quantity);
    batch.setUnit(Optional.ofNullable(bulkMaterial.getUnitType()).orElse("L"));
    batch.setCostPerUnit(unitCost);
    batch.setManufacturedAt(Instant.now());
    RawMaterialBatch saved = rawMaterialBatchRepository.save(batch);
    BigDecimal current = Optional.ofNullable(bulkMaterial.getCurrentStock()).orElse(BigDecimal.ZERO);
    bulkMaterial.setCurrentStock(current.add(quantity));
    rawMaterialRepository.save(bulkMaterial);
    return saved;
  }
}
