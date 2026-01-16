package com.bigbrightpaints.erp.regression;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryReference;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialRepository;
import com.bigbrightpaints.erp.modules.inventory.dto.RawMaterialBatchRequest;
import com.bigbrightpaints.erp.modules.inventory.service.RawMaterialService;
import com.bigbrightpaints.erp.modules.purchasing.domain.Supplier;
import com.bigbrightpaints.erp.modules.purchasing.domain.SupplierRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

public class RawMaterialBatchCodeUniquenessRegressionIT extends AbstractIntegrationTest {

    private static final String COMPANY_CODE = "RM-BATCH-UNI";

    @Autowired
    private RawMaterialService rawMaterialService;

    @Autowired
    private RawMaterialRepository rawMaterialRepository;

    @Autowired
    private RawMaterialBatchRepository rawMaterialBatchRepository;

    @Autowired
    private SupplierRepository supplierRepository;

    @Autowired
    private AccountRepository accountRepository;

    @AfterEach
    void clearContext() {
        CompanyContextHolder.clear();
    }

    @Test
    void rejectsDuplicateBatchCodesPerMaterial() {
        Company company = dataSeeder.ensureCompany(COMPANY_CODE, "Raw Material Batch Co");
        CompanyContextHolder.setCompanyId(company.getCode());

        Account inventoryAccount = createAccount(company, "INV-RM-001", "Raw Material Inventory", AccountType.ASSET);
        Account payableAccount = createAccount(company, "AP-RM-001", "Raw Material Payable", AccountType.LIABILITY);

        RawMaterial material = new RawMaterial();
        material.setCompany(company);
        material.setName("Pigment A");
        material.setSku("RM-PIG-001");
        material.setUnitType("KG");
        material.setInventoryAccountId(inventoryAccount.getId());
        RawMaterial savedMaterial = rawMaterialRepository.save(material);

        Supplier supplier = new Supplier();
        supplier.setCompany(company);
        supplier.setCode("SUP-RM-001");
        supplier.setName("Raw Material Supplier");
        supplier.setPayableAccount(payableAccount);
        supplier = supplierRepository.save(supplier);

        RawMaterialBatchRequest request = new RawMaterialBatchRequest(
                "RM-BATCH-001",
                new BigDecimal("10.00"),
                "KG",
                new BigDecimal("50.00"),
                supplier.getId(),
                "Initial receipt"
        );
        RawMaterialService.ReceiptContext context = new RawMaterialService.ReceiptContext(
                InventoryReference.RAW_MATERIAL_PURCHASE,
                "RM-BATCH-001",
                "Test receipt",
                false
        );

        rawMaterialService.recordReceipt(savedMaterial.getId(), request, context);

        assertThatThrownBy(() -> rawMaterialService.recordReceipt(savedMaterial.getId(), request, context))
                .isInstanceOf(ApplicationException.class)
                .extracting(error -> ((ApplicationException) error).getErrorCode())
                .isEqualTo(ErrorCode.VALIDATION_INVALID_INPUT);

        assertThat(rawMaterialBatchRepository.findByRawMaterial(savedMaterial)).hasSize(1);
    }

    private Account createAccount(Company company, String code, String name, AccountType type) {
        Account account = new Account();
        account.setCompany(company);
        account.setCode(code);
        account.setName(name);
        account.setType(type);
        return accountRepository.save(account);
    }
}
