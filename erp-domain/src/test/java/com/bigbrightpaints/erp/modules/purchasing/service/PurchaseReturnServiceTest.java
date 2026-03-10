package com.bigbrightpaints.erp.modules.purchasing.service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalCorrectionType;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingFacade;
import com.bigbrightpaints.erp.modules.accounting.service.GstService;
import com.bigbrightpaints.erp.modules.accounting.service.ReferenceNumberService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryReference;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovement;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovementRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchase;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseLine;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.Supplier;
import com.bigbrightpaints.erp.modules.purchasing.domain.SupplierStatus;
import com.bigbrightpaints.erp.modules.purchasing.dto.PurchaseReturnPreviewDto;
import com.bigbrightpaints.erp.modules.purchasing.dto.PurchaseReturnRequest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("critical")
class PurchaseReturnServiceTest {

    @Mock private CompanyContextService companyContextService;
    @Mock private RawMaterialPurchaseRepository purchaseRepository;
    @Mock private RawMaterialRepository rawMaterialRepository;
    @Mock private RawMaterialBatchRepository rawMaterialBatchRepository;
    @Mock private RawMaterialMovementRepository movementRepository;
    @Mock private AccountingFacade accountingFacade;
    @Mock private JournalEntryRepository journalEntryRepository;
    @Mock private CompanyEntityLookup companyEntityLookup;
    @Mock private ReferenceNumberService referenceNumberService;
    @Mock private CompanyClock companyClock;
    @Mock private GstService gstService;
    @Mock private PurchaseReturnAllocationService allocationService;

    private PurchaseReturnService purchaseReturnService;
    private Company company;
    private Supplier supplier;
    private RawMaterial material;
    private RawMaterialPurchase purchase;
    private Account payableAccount;

    @BeforeEach
    void setUp() {
        purchaseReturnService = new PurchaseReturnService(
                companyContextService,
                purchaseRepository,
                rawMaterialRepository,
                rawMaterialBatchRepository,
                movementRepository,
                accountingFacade,
                journalEntryRepository,
                companyEntityLookup,
                referenceNumberService,
                companyClock,
                gstService,
                allocationService
        );

        company = new Company();
        ReflectionTestUtils.setField(company, "id", 1L);
        company.setStateCode("KA");

        supplier = new Supplier();
        ReflectionTestUtils.setField(supplier, "id", 10L);
        supplier.setCompany(company);
        supplier.setCode("SUP-10");
        supplier.setName("Supplier 10");
        supplier.setStatus(SupplierStatus.ACTIVE);
        payableAccount = new Account();
        ReflectionTestUtils.setField(payableAccount, "id", 40L);
        supplier.setPayableAccount(payableAccount);

        material = new RawMaterial();
        ReflectionTestUtils.setField(material, "id", 20L);
        material.setCompany(company);
        material.setName("Resin");
        material.setInventoryAccountId(200L);

        purchase = new RawMaterialPurchase();
        ReflectionTestUtils.setField(purchase, "id", 30L);
        purchase.setCompany(company);
        purchase.setSupplier(supplier);
        purchase.setInvoiceNumber("PINV-30");
        purchase.setStatus("POSTED");
        purchase.setTaxAmount(BigDecimal.ZERO);
        JournalEntry purchaseJournal = new JournalEntry();
        ReflectionTestUtils.setField(purchaseJournal, "id", 50L);
        purchase.setJournalEntry(purchaseJournal);
        RawMaterialPurchaseLine line = new RawMaterialPurchaseLine();
        line.setPurchase(purchase);
        line.setRawMaterial(material);
        line.setQuantity(new BigDecimal("4.0000"));
        line.setLineTotal(new BigDecimal("20.00"));
        purchase.getLines().add(line);

        when(companyContextService.requireCurrentCompany()).thenReturn(company);
        when(companyEntityLookup.requireSupplier(company, 10L)).thenReturn(supplier);
        lenient().when(purchaseRepository.lockByCompanyAndId(company, 30L)).thenReturn(Optional.of(purchase));
        lenient().when(rawMaterialRepository.lockByCompanyAndId(company, 20L)).thenReturn(Optional.of(material));
        lenient().when(movementRepository.findByRawMaterialCompanyAndReferenceTypeAndReferenceId(eq(company), eq(com.bigbrightpaints.erp.modules.inventory.domain.InventoryReference.PURCHASE_RETURN), eq("PR-30")))
                .thenReturn(List.of());
    }

    @Test
    void previewPurchaseReturn_rejectsReferenceOnlySupplierBeforeMutations() {
        supplier.setStatus(SupplierStatus.SUSPENDED);

        PurchaseReturnRequest request = new PurchaseReturnRequest(
                10L,
                30L,
                20L,
                new BigDecimal("1.0000"),
                new BigDecimal("5.00"),
                "PR-30",
                LocalDate.of(2026, 3, 9),
                "Damaged"
        );

        assertThatThrownBy(() -> purchaseReturnService.previewPurchaseReturn(request))
                .isInstanceOfSatisfying(ApplicationException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.BUSINESS_INVALID_STATE);
                    assertThat(ex).hasMessageContaining("reference only")
                            .hasMessageContaining("preview purchase returns");
                });

        verifyNoInteractions(accountingFacade, allocationService);
        verify(rawMaterialRepository, never()).deductStockIfSufficient(any(), any());
    }

    @Test
    void recordPurchaseReturn_rejectsUnpostedPurchaseBeforeAllocationMutations() {
        purchase.setJournalEntry(null);

        PurchaseReturnRequest request = new PurchaseReturnRequest(
                10L,
                30L,
                20L,
                new BigDecimal("1.0000"),
                new BigDecimal("5.00"),
                "PR-30",
                LocalDate.of(2026, 3, 9),
                "Damaged"
        );

        assertThatThrownBy(() -> purchaseReturnService.recordPurchaseReturn(request))
                .isInstanceOfSatisfying(ApplicationException.class, ex -> {
                    assertThat(ex.getErrorCode()).isEqualTo(ErrorCode.VALIDATION_INVALID_STATE);
                    assertThat(ex).hasMessageContaining("Only posted purchases can be corrected through purchase return");
                });

        verifyNoInteractions(accountingFacade, allocationService);
        verify(rawMaterialRepository, never()).deductStockIfSufficient(any(), any());
    }

    @Test
    void previewPurchaseReturn_returnsDerivedTotalsAndGeneratedReference() {
        purchase.setTaxAmount(new BigDecimal("8.00"));
        purchase.getLines().getFirst().setTaxAmount(new BigDecimal("8.00"));
        when(allocationService.remainingReturnableQuantity(purchase, material)).thenReturn(new BigDecimal("3.0000"));
        when(referenceNumberService.purchaseReturnReference(company, supplier)).thenReturn("PR-AUTO-30");
        when(companyClock.today(company)).thenReturn(LocalDate.of(2026, 3, 10));

        PurchaseReturnPreviewDto preview = purchaseReturnService.previewPurchaseReturn(new PurchaseReturnRequest(
                10L,
                30L,
                20L,
                new BigDecimal("1.5000"),
                new BigDecimal("5.00"),
                "   ",
                null,
                "Damaged"
        ));

        assertThat(preview.purchaseId()).isEqualTo(30L);
        assertThat(preview.purchaseInvoiceNumber()).isEqualTo("PINV-30");
        assertThat(preview.remainingReturnableQuantity()).isEqualByComparingTo("1.5000");
        assertThat(preview.lineAmount()).isEqualByComparingTo("7.50");
        assertThat(preview.taxAmount()).isEqualByComparingTo("3.00");
        assertThat(preview.totalAmount()).isEqualByComparingTo("10.50");
        assertThat(preview.returnDate()).isEqualTo(LocalDate.of(2026, 3, 10));
        assertThat(preview.referenceNumber()).isEqualTo("PR-AUTO-30");
    }

    @Test
    void recordPurchaseReturn_postsInventoryAndLinksCorrectionJournal() {
        purchase.setTaxAmount(new BigDecimal("4.00"));
        purchase.getLines().getFirst().setTaxAmount(new BigDecimal("4.00"));
        when(allocationService.remainingReturnableQuantity(purchase, material)).thenReturn(new BigDecimal("4.0000"));
        when(rawMaterialRepository.deductStockIfSufficient(20L, new BigDecimal("1.0000"))).thenReturn(1);
        RawMaterialBatch batch = new RawMaterialBatch();
        ReflectionTestUtils.setField(batch, "id", 61L);
        batch.setBatchCode("RM-BATCH-1");
        batch.setQuantity(new BigDecimal("5.0000"));
        when(rawMaterialBatchRepository.findAvailableBatchesFIFO(material)).thenReturn(List.of(batch));
        when(rawMaterialBatchRepository.deductQuantityIfSufficient(61L, new BigDecimal("1.0000"))).thenReturn(1);
        when(gstService.splitTaxAmount(new BigDecimal("5.00"), new BigDecimal("1.00"), "KA", null))
                .thenReturn(new GstService.GstBreakdown(new BigDecimal("5.00"), new BigDecimal("0.50"), new BigDecimal("0.50"), BigDecimal.ZERO, GstService.TaxType.INTRA_STATE));
        when(accountingFacade.postPurchaseReturn(eq(10L), eq("PR-30"), eq(LocalDate.of(2026, 3, 9)), any(), any(), any(), any(), eq(new BigDecimal("6.00"))))
                .thenReturn(stubEntry(901L));
        JournalEntry persistedReturn = new JournalEntry();
        ReflectionTestUtils.setField(persistedReturn, "id", 901L);
        when(journalEntryRepository.findByCompanyAndId(company, 901L)).thenReturn(Optional.of(persistedReturn));

        JournalEntryDto result = purchaseReturnService.recordPurchaseReturn(new PurchaseReturnRequest(
                10L,
                30L,
                20L,
                new BigDecimal("1.0000"),
                new BigDecimal("5.00"),
                "PR-30",
                LocalDate.of(2026, 3, 9),
                "Damaged"
        ));

        assertThat(result.id()).isEqualTo(901L);
        verify(journalEntryRepository).save(argThat(entry -> Objects.equals(entry.getId(), 901L)
                && entry.getCorrectionType() == JournalCorrectionType.REVERSAL
                && Objects.equals(entry.getCorrectionReason(), "PURCHASE_RETURN")
                && Objects.equals(entry.getSourceModule(), "PURCHASING_RETURN")
                && Objects.equals(entry.getSourceReference(), "PINV-30")
                && entry.getReversalOf() != null
                && Objects.equals(entry.getReversalOf().getId(), 50L)));
        verify(movementRepository).saveAll(argThat(movements -> {
            List<RawMaterialMovement> saved = (List<RawMaterialMovement>) movements;
            return saved.size() == 1
                    && saved.getFirst().getReferenceType() == InventoryReference.PURCHASE_RETURN
                    && Objects.equals(saved.getFirst().getReferenceId(), "PR-30")
                    && Objects.equals(saved.getFirst().getJournalEntryId(), 901L)
                    && saved.getFirst().getQuantity().compareTo(new BigDecimal("1.0000")) == 0;
        }));
        verify(allocationService).applyPurchaseReturnQuantity(purchase, material, new BigDecimal("1.0000"));
        verify(allocationService).applyPurchaseReturnToOutstanding(purchase, new BigDecimal("6.00"));
    }

    @Test
    void recordPurchaseReturn_replayRelinksExistingMovementsAndCorrectionJournal() {
        RawMaterialMovement existingMovement = new RawMaterialMovement();
        existingMovement.setRawMaterial(material);
        existingMovement.setReferenceType(InventoryReference.PURCHASE_RETURN);
        existingMovement.setReferenceId("PR-30");
        existingMovement.setQuantity(new BigDecimal("1.0000"));
        existingMovement.setUnitCost(new BigDecimal("5.00"));
        when(movementRepository.findByRawMaterialCompanyAndReferenceTypeAndReferenceId(company, InventoryReference.PURCHASE_RETURN, "PR-30"))
                .thenReturn(List.of(existingMovement));
        when(accountingFacade.postPurchaseReturn(eq(10L), eq("PR-30"), eq(LocalDate.of(2026, 3, 9)), any(), any(), any(), any(), eq(new BigDecimal("5.00"))))
                .thenReturn(stubEntry(902L));
        JournalEntry persistedReturn = new JournalEntry();
        ReflectionTestUtils.setField(persistedReturn, "id", 902L);
        when(journalEntryRepository.findByCompanyAndId(company, 902L)).thenReturn(Optional.of(persistedReturn));

        JournalEntryDto result = purchaseReturnService.recordPurchaseReturn(new PurchaseReturnRequest(
                10L,
                30L,
                20L,
                new BigDecimal("1.0000"),
                new BigDecimal("5.00"),
                "PR-30",
                LocalDate.of(2026, 3, 9),
                "Damaged"
        ));

        assertThat(result.id()).isEqualTo(902L);
        verify(movementRepository).saveAll(argThat(movements -> {
            List<RawMaterialMovement> saved = (List<RawMaterialMovement>) movements;
            return saved.size() == 1 && Objects.equals(saved.getFirst().getJournalEntryId(), 902L);
        }));
        verify(rawMaterialRepository, never()).deductStockIfSufficient(any(), any());
        verify(allocationService, never()).applyPurchaseReturnQuantity(any(), any(), any());
        verify(allocationService, never()).applyPurchaseReturnToOutstanding(any(), any());
    }

    private JournalEntryDto stubEntry(long id) {
        return new JournalEntryDto(
                id,
                null,
                null,
                LocalDate.now(),
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                null,
                List.of(),
                null,
                null,
                null,
                null,
                null,
                null
        );
    }
}
