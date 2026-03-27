package com.bigbrightpaints.erp.modules.factory.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingFacade;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.factory.dto.BulkPackRequest;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatchRepository;

@ExtendWith(MockitoExtension.class)
@Tag("critical")
class BulkPackingServiceIdempotencyTest {

  @Mock private CompanyContextService companyContextService;
  @Mock private RawMaterialBatchRepository rawMaterialBatchRepository;
  @Mock private AccountingFacade accountingFacade;
  @Mock private CompanyClock companyClock;
  @Mock private BulkPackingOrchestrator bulkPackingOrchestrator;
  @Mock private BulkPackingCostService bulkPackingCostService;
  @Mock private BulkPackingInventoryService bulkPackingInventoryService;
  @Mock private BulkPackingReadService bulkPackingReadService;
  @Mock private PackingJournalLinkHelper packingJournalLinkHelper;

  private BulkPackingService bulkPackingService;
  private Company company;

  @BeforeEach
  void setUp() {
    bulkPackingService =
        new BulkPackingService(
            companyContextService,
            rawMaterialBatchRepository,
            accountingFacade,
            companyClock,
            bulkPackingOrchestrator,
            bulkPackingCostService,
            bulkPackingInventoryService,
            bulkPackingReadService,
            packingJournalLinkHelper);

    company = new Company();
    ReflectionTestUtils.setField(company, "id", 1L);
    when(companyContextService.requireCurrentCompany()).thenReturn(company);
  }

  @Test
  void pack_idempotentReplayWithoutJournalFailsClosed() {
    RawMaterial bulkMaterial = new RawMaterial();
    ReflectionTestUtils.setField(bulkMaterial, "id", 100L);
    bulkMaterial.setCompany(company);
    bulkMaterial.setSku("SKU-BULK");

    RawMaterialBatch bulkBatch = new RawMaterialBatch();
    ReflectionTestUtils.setField(bulkBatch, "id", 10L);
    bulkBatch.setRawMaterial(bulkMaterial);
    bulkBatch.setBatchCode("BULK-10");
    bulkBatch.setQuantity(new BigDecimal("100"));
    bulkBatch.setCostPerUnit(new BigDecimal("10.00"));

    when(rawMaterialBatchRepository.lockByRawMaterialCompanyAndId(company, 10L))
        .thenReturn(Optional.of(bulkBatch));
    when(bulkPackingReadService.resolveIdempotentPack(eq(company), eq(bulkBatch), anyString()))
        .thenThrow(
            new ApplicationException(
                ErrorCode.INTERNAL_CONCURRENCY_FAILURE,
                "Partial bulk pack detected for reference PACK-BULK-REF (inventory movements exist"
                    + " without journal)"));

    BulkPackRequest request =
        new BulkPackRequest(
            10L,
            List.of(new BulkPackRequest.PackLine(200L, new BigDecimal("5"), "1L", "L")),
            LocalDate.of(2026, 2, 12),
            "factory-user",
            "idempotent replay",
            "bulk-idem-1");

    assertThatThrownBy(() -> bulkPackingService.pack(request))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Partial bulk pack detected")
        .hasMessageContaining("without journal");

    verify(accountingFacade, never()).postPackingJournal(anyString(), any(), anyString(), any());
  }

  @Test
  void pack_rejectsDuplicateChildSkuLinesBeforeMutation() {
    doThrow(
            new ApplicationException(
                ErrorCode.VALIDATION_INVALID_INPUT, "Duplicate child SKU line is not allowed: 200"))
        .when(bulkPackingOrchestrator)
        .validatePackLines(any());

    BulkPackRequest request =
        new BulkPackRequest(
            10L,
            List.of(
                new BulkPackRequest.PackLine(200L, new BigDecimal("2"), "1L", "L"),
                new BulkPackRequest.PackLine(200L, new BigDecimal("3"), "1L", "L")),
            LocalDate.of(2026, 2, 12),
            "factory-user",
            "duplicate child line",
            "bulk-idem-dup");

    assertThatThrownBy(() -> bulkPackingService.pack(request))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Duplicate child SKU line is not allowed");

    verify(rawMaterialBatchRepository, never()).lockByRawMaterialCompanyAndId(any(), any());
  }

  @Test
  void pack_rejectsZeroPackQuantityBeforeMutation() {
    doThrow(
            new ApplicationException(
                ErrorCode.VALIDATION_INVALID_INPUT,
                "Pack quantity must be greater than zero for child SKU 200"))
        .when(bulkPackingOrchestrator)
        .validatePackLines(any());

    BulkPackRequest request =
        new BulkPackRequest(
            10L,
            List.of(new BulkPackRequest.PackLine(200L, BigDecimal.ZERO, "1L", "L")),
            LocalDate.of(2026, 2, 12),
            "factory-user",
            "zero quantity",
            "bulk-idem-zero");

    assertThatThrownBy(() -> bulkPackingService.pack(request))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("Pack quantity must be greater than zero");

    verify(rawMaterialBatchRepository, never()).lockByRawMaterialCompanyAndId(any(), any());
  }
}
