package com.bigbrightpaints.erp.modules.inventory.service;

import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.modules.accounting.domain.CostingMethod;
import com.bigbrightpaints.erp.modules.accounting.service.CostingMethodService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryMovement;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryMovementRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryReference;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryReservation;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryReservationRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlip;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlipLine;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlipRepository;
import com.bigbrightpaints.erp.modules.inventory.dto.PackagingSlipDto;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderItem;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderRepository;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@Tag("critical")
class FinishedGoodsReservationEngineDirectTest {

    @Mock private CompanyContextService companyContextService;
    @Mock private FinishedGoodRepository finishedGoodRepository;
    @Mock private FinishedGoodBatchRepository finishedGoodBatchRepository;
    @Mock private PackagingSlipRepository packagingSlipRepository;
    @Mock private InventoryMovementRepository inventoryMovementRepository;
    @Mock private InventoryReservationRepository inventoryReservationRepository;
    @Mock private SalesOrderRepository salesOrderRepository;
    @Mock private BatchNumberService batchNumberService;
    @Mock private CostingMethodService costingMethodService;
    @Mock private CompanyClock companyClock;
    @Mock private InventoryMovementRecorder movementRecorder;
    @Mock private InventoryValuationService inventoryValuationService;

    private FinishedGoodsReservationEngine engine;

    @BeforeEach
    void setUp() {
        engine = new FinishedGoodsReservationEngine(
                companyContextService,
                finishedGoodRepository,
                finishedGoodBatchRepository,
                packagingSlipRepository,
                inventoryMovementRepository,
                inventoryReservationRepository,
                salesOrderRepository,
                batchNumberService,
                costingMethodService,
                companyClock,
                movementRecorder,
                inventoryValuationService);
        lenient().when(inventoryValuationService.safeQuantity(any())).thenAnswer(invocation -> {
            BigDecimal value = invocation.getArgument(0);
            return value != null ? value : BigDecimal.ZERO;
        });
        lenient().when(costingMethodService.resolveActiveMethod(any(), any())).thenReturn(CostingMethod.FIFO);
        lenient().when(companyClock.today(any())).thenReturn(LocalDate.of(2026, 3, 12));
        lenient().when(finishedGoodBatchRepository.findAllocatableBatchesFIFO(any())).thenReturn(List.of());
    }

    @Test
    void selectReservationSlip_prefersLatestBackorderWhenPrimarySlipIsMissing() {
        Company company = company("SYNC-DIRECT");
        SalesOrder order = salesOrder(7L);
        PackagingSlip oldBackorder = slip(company, order, 12L, "BACKORDER");
        oldBackorder.setBackorder(true);
        PackagingSlip latestBackorder = slip(company, order, 13L, "BACKORDER");
        latestBackorder.setBackorder(true);

        when(packagingSlipRepository.findAllByCompanyAndSalesOrderIdForUpdate(company, 7L))
                .thenReturn(List.of(oldBackorder, latestBackorder));

        PackagingSlip selected = selectReservationSlip(company, order);

        assertThat(selected).isSameAs(latestBackorder);
    }

    @Test
    void selectReservationSlip_prefersExistingPrimarySlip_overBackorderFallback() {
        Company company = company("SYNC-PRIMARY");
        SalesOrder order = salesOrder(71L);
        PackagingSlip backorder = slip(company, order, 72L, "BACKORDER");
        backorder.setBackorder(true);
        PackagingSlip primary = slip(company, order, 73L, "PENDING");

        when(packagingSlipRepository.findAllByCompanyAndSalesOrderIdForUpdate(company, 71L))
                .thenReturn(List.of(backorder, primary));

        PackagingSlip selected = selectReservationSlip(company, order);

        assertThat(selected).isSameAs(primary);
    }

    @Test
    void selectReservationSlip_createsPrimarySlipWhenNoEligibleSlipExists() {
        Company company = company("SYNC-NEW");
        SalesOrder order = salesOrder(70L);
        order.setCompany(company);
        PackagingSlip cancelledBackorder = slip(company, order, 21L, "CANCELLED");
        cancelledBackorder.setBackorder(true);

        when(packagingSlipRepository.findAllByCompanyAndSalesOrderIdForUpdate(company, 70L))
                .thenReturn(List.of(cancelledBackorder));
        when(companyContextService.requireCurrentCompany()).thenReturn(company);
        when(batchNumberService.nextPackagingSlipNumber(company)).thenReturn("PS-NEW");
        when(packagingSlipRepository.saveAndFlush(any(PackagingSlip.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PackagingSlip selected = selectReservationSlip(company, order);

        assertThat(selected.getSlipNumber()).isEqualTo("PS-NEW");
        assertThat(selected.getStatus()).isEqualTo("PENDING");
        assertThat(selected.isBackorder()).isFalse();
        assertThat(selected.getSalesOrder()).isSameAs(order);
    }

    @Test
    void continueBackorderReservation_keepsBackorderStatus_whenSlipDoesNotRepresentOrderedProduct() {
        Company company = company("SYNC-LOCK");
        SalesOrder order = salesOrder(8L);
        order.setCompany(company);
        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrder(order);
        item.setProductCode("FG-LOCK");
        item.setQuantity(BigDecimal.ONE);
        order.getItems().add(item);

        PackagingSlip slip = slip(company, order, 14L, "BACKORDER");
        slip.setBackorder(true);
        slip.getLines().add(line(batchWithId(51L, finishedGoodWithId(52L, "FG-OTHER"), "BATCH-LOCK"), BigDecimal.ONE));

        FinishedGood finishedGood = finishedGoodWithId(41L, "FG-LOCK");
        when(inventoryReservationRepository.findByFinishedGoodCompanyAndReferenceTypeAndReferenceId(company, InventoryReference.SALES_ORDER, "8"))
                .thenReturn(List.of());
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-LOCK")).thenReturn(Optional.of(finishedGood));
        when(finishedGoodBatchRepository.findAllocatableBatchesFIFO(finishedGood)).thenReturn(List.of());
        when(packagingSlipRepository.save(slip)).thenReturn(slip);

        FinishedGoodsService.InventoryReservationResult result = continueBackorderReservation(order, slip);

        assertThat(result.shortages()).hasSize(1);
        assertThat(slip.getStatus()).isEqualTo("BACKORDER");
        assertThat(slip.isBackorder()).isTrue();
    }

    @Test
    void continueBackorderReservation_marksSlipReserved_whenExistingLinesFullyCoverOrder() {
        Company company = company("SYNC-FULL");
        SalesOrder order = salesOrder(9L);
        order.setCompany(company);
        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrder(order);
        item.setProductCode("FG-FULL");
        item.setQuantity(BigDecimal.ONE);
        order.getItems().add(item);

        FinishedGood finishedGood = finishedGoodWithId(61L, "FG-FULL");
        FinishedGoodBatch batch = batchWithId(62L, finishedGood, "BATCH-FULL");
        PackagingSlip slip = slip(company, order, 15L, "BACKORDER");
        slip.setBackorder(true);
        PackagingSlipLine slipLine = line(batch, BigDecimal.ONE);
        slipLine.setOrderedQuantity(null);
        slip.getLines().add(slipLine);

        InventoryReservation existing = reservation(batch, finishedGood, null, "PARTIAL");
        existing.setReservedQuantity(null);
        when(inventoryReservationRepository.findByFinishedGoodCompanyAndReferenceTypeAndReferenceId(
                company, InventoryReference.SALES_ORDER, "9")).thenReturn(List.of(existing));
        when(packagingSlipRepository.save(slip)).thenReturn(slip);

        FinishedGoodsService.InventoryReservationResult result = continueBackorderReservation(order, slip);

        assertThat(result.shortages()).isEmpty();
        assertThat(slip.getStatus()).isEqualTo("RESERVED");
        assertThat(slip.isBackorder()).isFalse();
        assertThat(existing.getQuantity()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(existing.getReservedQuantity()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(existing.getStatus()).isEqualTo("PARTIAL");
        verify(inventoryReservationRepository).save(existing);
        verify(packagingSlipRepository).save(slip);
    }

    @Test
    void normalizeBackorderReservations_repairsZeroReservationQuantities_and_skipsInvalidTerminalInputs() {
        Company company = company("SYNC-REPAIR");
        SalesOrder order = salesOrder(901L);
        order.setCompany(company);
        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrder(order);
        item.setProductCode("FG-REPAIR");
        item.setQuantity(BigDecimal.ONE);
        order.getItems().add(item);

        FinishedGood finishedGood = finishedGoodWithId(902L, "FG-REPAIR");
        FinishedGoodBatch validBatch = batchWithId(903L, finishedGood, "BATCH-REPAIR");
        PackagingSlip slip = slip(company, order, 904L, "BACKORDER");
        slip.setBackorder(true);
        PackagingSlipLine missingBatch = new PackagingSlipLine();
        missingBatch.setQuantity(BigDecimal.ONE);
        PackagingSlipLine missingFinishedGood = line(new FinishedGoodBatch(), BigDecimal.ONE);
        PackagingSlipLine missingBatchId = line(batchWithId(null, finishedGood, "BATCH-NO-ID"), BigDecimal.ONE);
        PackagingSlipLine validLine = line(validBatch, BigDecimal.ONE);
        validLine.setQuantity(null);
        validLine.setOrderedQuantity(BigDecimal.ONE);
        slip.getLines().addAll(List.of(missingBatch, missingFinishedGood, missingBatchId, validLine));

        InventoryReservation noBatchReservation = reservation(null, finishedGood, BigDecimal.ONE, "RESERVED");
        InventoryReservation fulfilledReservation = reservation(validBatch, finishedGood, BigDecimal.ONE, "FULFILLED");
        InventoryReservation existing = reservation(validBatch, finishedGood, BigDecimal.ZERO, "RESERVED");
        existing.setReservedQuantity(BigDecimal.ZERO);

        when(inventoryReservationRepository.findByFinishedGoodCompanyAndReferenceTypeAndReferenceId(
                company, InventoryReference.SALES_ORDER, "901")).thenReturn(List.of(noBatchReservation, fulfilledReservation, existing));
        normalizeBackorderReservations(order, slip);

        assertThat(existing.getQuantity()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(existing.getReservedQuantity()).isEqualByComparingTo(BigDecimal.ONE);
        assertThat(existing.getStatus()).isEqualTo("RESERVED");
    }

    @Test
    void continueBackorderReservation_preservesPositiveReservationValues_whenAlreadyHealthy() {
        Company company = company("SYNC-POSITIVE");
        SalesOrder order = salesOrder(905L);
        order.setCompany(company);
        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrder(order);
        item.setProductCode("FG-POSITIVE");
        item.setQuantity(BigDecimal.ONE);
        order.getItems().add(item);

        FinishedGood finishedGood = finishedGoodWithId(906L, "FG-POSITIVE");
        FinishedGoodBatch validBatch = batchWithId(907L, finishedGood, "BATCH-POSITIVE");
        PackagingSlip slip = slip(company, order, 908L, "BACKORDER");
        slip.setBackorder(true);
        slip.getLines().add(line(validBatch, BigDecimal.ONE));

        InventoryReservation existing = reservation(validBatch, finishedGood, new BigDecimal("5"), "RESERVED");
        existing.setReservedQuantity(new BigDecimal("4"));
        when(inventoryReservationRepository.findByFinishedGoodCompanyAndReferenceTypeAndReferenceId(
                company, InventoryReference.SALES_ORDER, "905")).thenReturn(List.of(existing));
        when(packagingSlipRepository.save(slip)).thenReturn(slip);

        continueBackorderReservation(order, slip);

        assertThat(existing.getQuantity()).isEqualByComparingTo(new BigDecimal("5"));
        assertThat(existing.getReservedQuantity()).isEqualByComparingTo(new BigDecimal("4"));
        assertThat(existing.getStatus()).isEqualTo("RESERVED");
    }

    @Test
    void reserveForOrder_returnsExistingDto_whenSlipAlreadyMatchesOrder() {
        Company company = company("RESERVE-MATCH");
        SalesOrder order = salesOrder(101L);
        order.setCompany(company);
        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrder(order);
        item.setProductCode("FG-MATCH");
        item.setQuantity(BigDecimal.ONE);
        order.getItems().add(item);

        PackagingSlip slip = slip(company, order, 102L, "PENDING");
        FinishedGoodBatch batch = batchWithId(103L, finishedGoodWithId(104L, "FG-MATCH"), "BATCH-MATCH");
        slip.getLines().add(line(batch, BigDecimal.ONE));

        when(companyContextService.requireCurrentCompany()).thenReturn(company);
        when(salesOrderRepository.findWithItemsByCompanyAndIdForUpdate(company, 101L)).thenReturn(Optional.of(order));
        when(packagingSlipRepository.findAllByCompanyAndSalesOrderIdForUpdate(company, 101L)).thenReturn(List.of(slip));
        when(packagingSlipRepository.save(slip)).thenReturn(slip);

        FinishedGoodsService.InventoryReservationResult result = engine.reserveForOrder(order);

        assertThat(result.shortages()).isEmpty();
        assertThat(result.packagingSlip().id()).isEqualTo(102L);
        assertThat(slip.getStatus()).isEqualTo("RESERVED");
        verify(inventoryReservationRepository, never()).save(any(InventoryReservation.class));
    }

    @Test
    void reserveForOrder_routesBackorderSlip_throughContinuationPath() {
        Company company = company("RESERVE-BACKORDER");
        SalesOrder order = salesOrder(105L);
        order.setCompany(company);
        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrder(order);
        item.setProductCode("FG-BACK");
        item.setQuantity(BigDecimal.ONE);
        order.getItems().add(item);

        PackagingSlip slip = slip(company, order, 106L, "BACKORDER");
        slip.setBackorder(true);
        slip.getLines().add(line(batchWithId(107L, finishedGoodWithId(108L, "FG-OTHER"), "BATCH-BACK"), BigDecimal.ONE));

        FinishedGood finishedGood = finishedGoodWithId(109L, "FG-BACK");
        when(companyContextService.requireCurrentCompany()).thenReturn(company);
        when(salesOrderRepository.findWithItemsByCompanyAndIdForUpdate(company, 105L)).thenReturn(Optional.of(order));
        when(packagingSlipRepository.findAllByCompanyAndSalesOrderIdForUpdate(company, 105L)).thenReturn(List.of(slip));
        when(inventoryReservationRepository.findByFinishedGoodCompanyAndReferenceTypeAndReferenceId(
                company, InventoryReference.SALES_ORDER, "105")).thenReturn(List.of());
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-BACK")).thenReturn(Optional.of(finishedGood));
        when(finishedGoodBatchRepository.findAllocatableBatchesFIFO(finishedGood)).thenReturn(List.of());
        when(packagingSlipRepository.save(slip)).thenReturn(slip);

        FinishedGoodsService.InventoryReservationResult result = engine.reserveForOrder(order);

        assertThat(result.shortages()).hasSize(1);
        assertThat(slip.getStatus()).isEqualTo("BACKORDER");
        assertThat(slip.isBackorder()).isTrue();
    }

    @Test
    void reserveForOrder_leavesSlipPendingProduction_whenAllocationShortageRemains() {
        Company company = company("RESERVE-SHORT");
        SalesOrder order = salesOrder(107L);
        order.setCompany(company);
        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrder(order);
        item.setProductCode("FG-SHORT");
        item.setQuantity(new BigDecimal("3"));
        order.getItems().add(item);

        PackagingSlip slip = slip(company, order, 108L, "PENDING");
        FinishedGood finishedGood = finishedGoodWithId(109L, "FG-SHORT");
        finishedGood.setCompany(company);
        FinishedGoodBatch partialBatch = batchWithId(110L, finishedGood, "BATCH-SHORT");
        partialBatch.setQuantityAvailable(BigDecimal.ONE);

        when(companyContextService.requireCurrentCompany()).thenReturn(company);
        when(salesOrderRepository.findWithItemsByCompanyAndIdForUpdate(company, 107L)).thenReturn(Optional.of(order));
        when(packagingSlipRepository.findAllByCompanyAndSalesOrderIdForUpdate(company, 107L)).thenReturn(List.of(slip));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-SHORT")).thenReturn(Optional.of(finishedGood));
        when(finishedGoodBatchRepository.findAllocatableBatchesFIFO(finishedGood)).thenReturn(List.of(partialBatch));
        when(finishedGoodBatchRepository.save(any(FinishedGoodBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(finishedGoodRepository.save(any(FinishedGood.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryReservationRepository.save(any(InventoryReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(packagingSlipRepository.save(slip)).thenReturn(slip);

        FinishedGoodsService.InventoryReservationResult result = engine.reserveForOrder(order);

        assertThat(result.shortages()).hasSize(1);
        assertThat(slip.getStatus()).isEqualTo("PENDING_PRODUCTION");
    }

    @Test
    void reserveForOrder_resetsCancelledSlip_beforeReturningReservationResult() {
        Company company = company("RESERVE-CANCELLED");
        SalesOrder order = salesOrder(110L);
        order.setCompany(company);
        PackagingSlip slip = slip(company, order, 111L, "CANCELLED");
        slip.getLines().add(line(batchWithId(112L, finishedGoodWithId(113L, "FG-CANCEL"), "BATCH-CANCEL"), BigDecimal.ONE));

        when(companyContextService.requireCurrentCompany()).thenReturn(company);
        when(salesOrderRepository.findWithItemsByCompanyAndIdForUpdate(company, 110L)).thenReturn(Optional.of(order));
        when(packagingSlipRepository.findAllByCompanyAndSalesOrderIdForUpdate(company, 110L)).thenReturn(List.of(slip));
        when(inventoryReservationRepository.findByFinishedGoodCompanyAndReferenceTypeAndReferenceId(
                company, InventoryReference.SALES_ORDER, "110")).thenReturn(List.of());
        when(packagingSlipRepository.save(slip)).thenReturn(slip);

        FinishedGoodsService.InventoryReservationResult result = engine.reserveForOrder(order);

        assertThat(result.shortages()).isEmpty();
        assertThat(slip.getLines()).isEmpty();
        assertThat(slip.getStatus()).isEqualTo("RESERVED");
        assertThat(slip.isBackorder()).isFalse();
    }

    @Test
    void reserveForOrder_releasesMismatchedSlip_and_allocatesFreshReservation() {
        Company company = company("RESERVE-MISMATCH");
        SalesOrder order = salesOrder(114L);
        order.setCompany(company);
        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrder(order);
        item.setProductCode("FG-NEW");
        item.setQuantity(BigDecimal.ONE);
        order.getItems().add(item);

        PackagingSlip slip = slip(company, order, 115L, "PENDING");
        slip.getLines().add(line(batchWithId(116L, finishedGoodWithId(117L, "FG-OLD"), "BATCH-OLD"), BigDecimal.ONE));

        FinishedGood finishedGood = finishedGoodWithId(118L, "FG-NEW");
        finishedGood.setCompany(company);
        finishedGood.setReservedStock(BigDecimal.ZERO);
        FinishedGoodBatch batch = batchWithId(119L, finishedGood, "BATCH-NEW");
        batch.setUnitCost(new BigDecimal("6"));

        when(companyContextService.requireCurrentCompany()).thenReturn(company);
        when(salesOrderRepository.findWithItemsByCompanyAndIdForUpdate(company, 114L)).thenReturn(Optional.of(order));
        when(packagingSlipRepository.findAllByCompanyAndSalesOrderIdForUpdate(company, 114L)).thenReturn(List.of(slip));
        when(inventoryReservationRepository.findByFinishedGoodCompanyAndReferenceTypeAndReferenceId(
                company, InventoryReference.SALES_ORDER, "114")).thenReturn(List.of());
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-NEW")).thenReturn(Optional.of(finishedGood));
        when(finishedGoodBatchRepository.findAllocatableBatchesFIFO(finishedGood)).thenReturn(List.of(batch));
        when(finishedGoodBatchRepository.save(any(FinishedGoodBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(finishedGoodRepository.save(any(FinishedGood.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryReservationRepository.save(any(InventoryReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(packagingSlipRepository.save(slip)).thenReturn(slip);

        FinishedGoodsService.InventoryReservationResult result = engine.reserveForOrder(order);

        assertThat(result.shortages()).isEmpty();
        assertThat(slip.getStatus()).isEqualTo("RESERVED");
        assertThat(slip.getLines()).hasSize(1);
        assertThat(slip.getLines().getFirst().getFinishedGoodBatch().getId()).isEqualTo(119L);
        verify(inventoryReservationRepository).save(any(InventoryReservation.class));
    }

    @Test
    void directHelpers_cover_remaining_failClosed_paths() {
        InventoryReservation nullStatus = reservation(null, null, BigDecimal.ONE, null);
        nullStatus.setReservedQuantity(new BigDecimal("2"));
        assertThat(resolveReservedQuantity(nullStatus)).isEqualByComparingTo(new BigDecimal("2"));

        PackagingSlip nullStatusSlip = slip(company("HELPER-DIRECT"), salesOrder(10L), 16L, null);
        updateSlipStatusBasedOnAvailability(nullStatusSlip, null);
        assertThat(nullStatusSlip.getStatus()).isEqualTo("RESERVED");

        PackagingSlip nullLinesSlip = new PackagingSlip();
        ReflectionTestUtils.setField(nullLinesSlip, "lines", null);
        assertThat(slipLinesMatchOrder(nullLinesSlip, salesOrder(11L))).isFalse();

        PackagingSlip missingFinishedGoodSlip = new PackagingSlip();
        missingFinishedGoodSlip.getLines().add(line(new FinishedGoodBatch(), BigDecimal.ONE));
        assertThat(slipLinesMatchOrder(missingFinishedGoodSlip, salesOrder(11L))).isFalse();
    }

    @Test
    void releaseReservationsForOrder_cancelsActiveReservations_and_nonTerminalSlips() {
        Company company = company("RELEASE-ACTIVE");
        when(companyContextService.requireCurrentCompany()).thenReturn(company);

        FinishedGood finishedGood = finishedGoodWithId(71L, "FG-REL");
        finishedGood.setCompany(company);
        finishedGood.setReservedStock(new BigDecimal("5"));
        FinishedGoodBatch batch = batchWithId(61L, finishedGood, "BATCH-REL");
        batch.setQuantityAvailable(BigDecimal.ONE);
        batch.setUnitCost(new BigDecimal("4"));
        InventoryReservation active = reservation(batch, finishedGood, new BigDecimal("2"), "RESERVED");
        InventoryReservation cancelled = reservation(batch, finishedGood, BigDecimal.ONE, "CANCELLED");

        when(inventoryReservationRepository.findByFinishedGoodCompanyAndReferenceTypeAndReferenceId(
                company, InventoryReference.SALES_ORDER, "42")).thenReturn(List.of(active, cancelled));
        when(finishedGoodRepository.lockByCompanyAndId(company, 71L)).thenReturn(Optional.of(finishedGood));

        PackagingSlip pendingSlip = slip(company, salesOrder(42L), 72L, "PENDING");
        pendingSlip.getLines().add(line(batch, BigDecimal.ONE));
        PackagingSlip dispatchedSlip = slip(company, salesOrder(42L), 73L, "DISPATCHED");
        dispatchedSlip.getLines().add(line(batch, BigDecimal.ONE));
        when(packagingSlipRepository.findAllByCompanyAndSalesOrderId(company, 42L))
                .thenReturn(List.of(pendingSlip, dispatchedSlip));

        engine.releaseReservationsForOrder(42L);

        assertThat(active.getStatus()).isEqualTo("CANCELLED");
        assertThat(active.getReservedQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(finishedGood.getReservedStock()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(batch.getQuantityAvailable()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(pendingSlip.getStatus()).isEqualTo("CANCELLED");
        assertThat(pendingSlip.getDispatchNotes()).isEqualTo("Order cancelled");
        assertThat(pendingSlip.getLines()).isEmpty();
        assertThat(dispatchedSlip.getStatus()).isEqualTo("DISPATCHED");
        verify(inventoryReservationRepository).saveAll(List.of(active, cancelled));
        verify(finishedGoodBatchRepository).saveAll(any());
        verify(movementRecorder).recordFinishedGoodMovement(
                finishedGood,
                batch,
                InventoryReference.SALES_ORDER,
                "42",
                "RELEASE",
                new BigDecimal("2"),
                new BigDecimal("4"),
                null);
    }

    @Test
    void releaseReservationsForOrder_throwsWhenFinishedGoodCannotBeLocked() {
        Company company = company("RELEASE-DIRECT");
        when(companyContextService.requireCurrentCompany()).thenReturn(company);
        InventoryReservation reservation = reservation(batchWithId(61L, finishedGoodWithId(71L, "FG-REL"), "BATCH-REL"), finishedGoodWithId(71L, "FG-REL"), BigDecimal.ONE, "RESERVED");
        when(inventoryReservationRepository.findByFinishedGoodCompanyAndReferenceTypeAndReferenceId(company, InventoryReference.SALES_ORDER, "42"))
                .thenReturn(List.of(reservation));
        when(finishedGoodRepository.lockByCompanyAndId(company, 71L)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> engine.releaseReservationsForOrder(42L))
                .hasMessageContaining("Finished good not found: 71");
    }

    @Test
    void releaseReservationsForOrder_returnsImmediately_whenNoReservationsExist() {
        Company company = company("RELEASE-EMPTY");
        when(companyContextService.requireCurrentCompany()).thenReturn(company);
        when(inventoryReservationRepository.findByFinishedGoodCompanyAndReferenceTypeAndReferenceId(
                company, InventoryReference.SALES_ORDER, "400")).thenReturn(List.of());

        engine.releaseReservationsForOrder(400L);

        verify(packagingSlipRepository, never()).findAllByCompanyAndSalesOrderId(company, 400L);
        verify(finishedGoodRepository, never()).saveAll(any());
    }

    @Test
    void releaseReservationsForOrder_cancelsZeroReservedReservation_withoutTouchingBatchStock() {
        Company company = company("RELEASE-ZERO");
        when(companyContextService.requireCurrentCompany()).thenReturn(company);

        FinishedGood finishedGood = finishedGoodWithId(72L, "FG-ZERO");
        finishedGood.setCompany(company);
        finishedGood.setReservedStock(new BigDecimal("4"));
        FinishedGoodBatch batch = batchWithId(73L, finishedGood, "BATCH-ZERO");
        batch.setQuantityAvailable(BigDecimal.ONE);
        InventoryReservation reservation = reservation(batch, finishedGood, BigDecimal.ZERO, "RESERVED");
        reservation.setReservedQuantity(BigDecimal.ZERO);

        when(inventoryReservationRepository.findByFinishedGoodCompanyAndReferenceTypeAndReferenceId(
                company, InventoryReference.SALES_ORDER, "401")).thenReturn(List.of(reservation));
        when(finishedGoodRepository.lockByCompanyAndId(company, 72L)).thenReturn(Optional.of(finishedGood));
        when(packagingSlipRepository.findAllByCompanyAndSalesOrderId(company, 401L)).thenReturn(List.of());

        engine.releaseReservationsForOrder(401L);

        assertThat(reservation.getStatus()).isEqualTo("CANCELLED");
        assertThat(reservation.getReservedQuantity()).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(batch.getQuantityAvailable()).isEqualByComparingTo(BigDecimal.ONE);
        verify(movementRecorder, never()).recordFinishedGoodMovement(
                any(), any(), any(), any(), any(), any(), any(), any());
        verify(finishedGoodBatchRepository, never()).saveAll(any());
    }

    @Test
    void releaseReservationsForOrder_recordsReleaseWithoutBatch_usingZeroUnitCost() {
        Company company = company("RELEASE-NO-BATCH");
        when(companyContextService.requireCurrentCompany()).thenReturn(company);

        FinishedGood finishedGood = finishedGoodWithId(74L, "FG-NOBATCH");
        finishedGood.setCompany(company);
        finishedGood.setReservedStock(new BigDecimal("2"));
        InventoryReservation reservation = reservation(null, finishedGood, BigDecimal.ONE, "RESERVED");

        when(inventoryReservationRepository.findByFinishedGoodCompanyAndReferenceTypeAndReferenceId(
                company, InventoryReference.SALES_ORDER, "402")).thenReturn(List.of(reservation));
        when(finishedGoodRepository.lockByCompanyAndId(company, 74L)).thenReturn(Optional.of(finishedGood));
        when(packagingSlipRepository.findAllByCompanyAndSalesOrderId(company, 402L)).thenReturn(List.of());

        engine.releaseReservationsForOrder(402L);

        assertThat(finishedGood.getReservedStock()).isEqualByComparingTo(BigDecimal.ONE);
        verify(movementRecorder).recordFinishedGoodMovement(
                finishedGood,
                null,
                InventoryReference.SALES_ORDER,
                "402",
                "RELEASE",
                BigDecimal.ONE,
                BigDecimal.ZERO,
                null);
    }

    @Test
    void rebuildReservationsFromSlip_throwsWhenSlipHasNoLines() {
        PackagingSlip slip = slip(company("REBUILD-EMPTY"), salesOrder(89L), 90L, "RESERVED");

        assertThatThrownBy(() -> engine.rebuildReservationsFromSlip(slip, 89L))
                .hasMessageContaining("No packaging slip lines available");
    }

    @Test
    void rebuildReservationsFromSlip_throwsWhenSlipLinesCollectionIsNull() {
        PackagingSlip slip = slip(company("REBUILD-NULL"), salesOrder(891L), 901L, "RESERVED");
        ReflectionTestUtils.setField(slip, "lines", null);

        assertThatThrownBy(() -> engine.rebuildReservationsFromSlip(slip, 891L))
                .hasMessageContaining("No packaging slip lines available");
    }

    @Test
    void rebuildReservationsFromSlip_throwsWhenFinishedGoodBatchIsMissing() {
        PackagingSlip slip = slip(company("REBUILD-FAIL"), salesOrder(90L), 91L, "RESERVED");
        PackagingSlipLine line = new PackagingSlipLine();
        line.setQuantity(BigDecimal.ONE);
        slip.getLines().add(line);

        assertThatThrownBy(() -> engine.rebuildReservationsFromSlip(slip, 90L))
                .hasMessageContaining("Cannot rebuild reservation without a finished good batch");
    }

    @Test
    void rebuildReservationsFromSlip_throwsWhenFinishedGoodOnBatchIsMissing() {
        PackagingSlip slip = slip(company("REBUILD-NO-FG"), salesOrder(902L), 903L, "RESERVED");
        PackagingSlipLine line = new PackagingSlipLine();
        FinishedGoodBatch batch = new FinishedGoodBatch();
        line.setFinishedGoodBatch(batch);
        line.setQuantity(BigDecimal.ONE);
        slip.getLines().add(line);

        assertThatThrownBy(() -> engine.rebuildReservationsFromSlip(slip, 902L))
                .hasMessageContaining("Cannot rebuild reservation without a finished good batch");
    }

    @Test
    void rebuildReservationsFromSlip_skipsTouchMaps_whenIdsAreMissing() {
        Company company = company("REBUILD-DIRECT");
        PackagingSlip slip = slip(company, salesOrder(12L), 17L, "RESERVED");
        FinishedGood finishedGood = finishedGoodWithId(null, "FG-REBUILD");
        FinishedGoodBatch batch = batchWithId(null, finishedGood, "BATCH-REBUILD");
        slip.getLines().add(line(batch, new BigDecimal("2")));
        when(inventoryReservationRepository.saveAll(any())).thenAnswer(invocation -> invocation.getArgument(0));

        List<InventoryReservation> rebuilt = engine.rebuildReservationsFromSlip(slip, 12L);

        assertThat(rebuilt).hasSize(1);
        verify(finishedGoodRepository, never()).saveAll(any());
        verify(finishedGoodBatchRepository, never()).saveAll(any());
    }

    @Test
    void rebuildReservationsFromSlip_recomputesTouchedGoodsAndBatches_whenIdsExist() {
        Company company = company("REBUILD-TOUCHED");
        PackagingSlip slip = slip(company, salesOrder(22L), 23L, "RESERVED");
        FinishedGood finishedGood = finishedGoodWithId(24L, "FG-TOUCHED");
        finishedGood.setCompany(company);
        finishedGood.setReservedStock(BigDecimal.ZERO);
        FinishedGoodBatch batch = batchWithId(25L, finishedGood, "BATCH-TOUCHED");
        batch.setQuantityTotal(new BigDecimal("10"));
        batch.setQuantityAvailable(new BigDecimal("10"));
        slip.getLines().add(line(batch, new BigDecimal("3")));

        when(finishedGoodRepository.lockByCompanyAndId(company, 24L)).thenReturn(Optional.of(finishedGood));
        when(finishedGoodBatchRepository.lockById(25L)).thenReturn(Optional.of(batch));
        AtomicReference<List<InventoryReservation>> savedReservations = new AtomicReference<>(List.of());
        when(inventoryReservationRepository.saveAll(any())).thenAnswer(invocation -> {
            List<InventoryReservation> saved = invocation.getArgument(0);
            savedReservations.set(saved);
            return saved;
        });
        when(inventoryReservationRepository.findByFinishedGood(finishedGood))
                .thenAnswer(invocation -> savedReservations.get());
        when(inventoryReservationRepository.findByFinishedGoodBatch(batch))
                .thenAnswer(invocation -> savedReservations.get());

        List<InventoryReservation> rebuilt = engine.rebuildReservationsFromSlip(slip, 22L);

        assertThat(rebuilt).hasSize(1);
        assertThat(finishedGood.getReservedStock()).isEqualByComparingTo(new BigDecimal("3"));
        assertThat(batch.getQuantityAvailable()).isEqualByComparingTo(new BigDecimal("7"));
        verify(finishedGoodRepository).saveAll(any());
        verify(finishedGoodBatchRepository).saveAll(any());
        verify(inventoryValuationService).invalidateWeightedAverageCost(24L);
    }

    @Test
    void allocateItem_breaksAfterSatisfiedAllocation_and_skipsZeroAvailableBatch() {
        Company company = company("ALLOC-DIRECT");
        SalesOrder order = salesOrder(13L);
        order.setCompany(company);
        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrder(order);
        item.setProductCode("FG-ALLOC");
        item.setQuantity(BigDecimal.ONE);
        order.getItems().add(item);

        FinishedGood finishedGood = finishedGoodWithId(81L, "FG-ALLOC");
        finishedGood.setCompany(company);
        finishedGood.setReservedStock(BigDecimal.ZERO);

        FinishedGoodBatch emptyBatch = batchWithId(91L, finishedGood, "BATCH-EMPTY");
        emptyBatch.setQuantityAvailable(BigDecimal.ZERO);
        emptyBatch.setUnitCost(new BigDecimal("7"));
        FinishedGoodBatch usableBatch = batchWithId(92L, finishedGood, "BATCH-USE");
        usableBatch.setQuantityAvailable(BigDecimal.ONE);
        usableBatch.setUnitCost(new BigDecimal("8"));
        FinishedGoodBatch extraBatch = batchWithId(93L, finishedGood, "BATCH-EXTRA");
        extraBatch.setQuantityAvailable(new BigDecimal("5"));
        extraBatch.setUnitCost(new BigDecimal("9"));

        when(finishedGoodBatchRepository.findAllocatableBatchesFIFO(finishedGood)).thenReturn(List.of(emptyBatch, usableBatch, extraBatch));
        when(finishedGoodBatchRepository.save(any(FinishedGoodBatch.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(finishedGoodRepository.save(any(FinishedGood.class))).thenAnswer(invocation -> invocation.getArgument(0));
        when(inventoryReservationRepository.save(any(InventoryReservation.class))).thenAnswer(invocation -> invocation.getArgument(0));

        PackagingSlip slip = slip(company, order, 18L, "PENDING");
        allocateItem(order, slip, finishedGood, item, new ArrayList<>());

        assertThat(slip.getLines()).hasSize(1);
        assertThat(slip.getLines().getFirst().getFinishedGoodBatch().getId()).isEqualTo(92L);
        verify(inventoryReservationRepository).save(any(InventoryReservation.class));
    }

    @Test
    void selectBatchesByCostingMethod_routesToWacAndLifoRepositories() {
        FinishedGood finishedGood = finishedGoodWithId(130L, "FG-COST");
        finishedGood.setCompany(company("COST"));
        LocalDate referenceDate = LocalDate.of(2026, 3, 12);

        when(costingMethodService.resolveActiveMethod(finishedGood.getCompany(), referenceDate)).thenReturn(CostingMethod.WEIGHTED_AVERAGE);
        assertThat(selectBatchesByCostingMethod(finishedGood, referenceDate)).isEmpty();
        verify(finishedGoodBatchRepository).findAllocatableBatches(finishedGood);

        when(costingMethodService.resolveActiveMethod(finishedGood.getCompany(), referenceDate)).thenReturn(CostingMethod.LIFO);
        assertThat(selectBatchesByCostingMethod(finishedGood, referenceDate)).isEmpty();
        verify(finishedGoodBatchRepository).findAllocatableBatchesLIFO(finishedGood);
    }

    @Test
    void resolveReservedQuantity_returnsZeroForTerminalStates_and_fallsBackToQuantity() {
        InventoryReservation cancelled = reservation(null, null, new BigDecimal("4"), "CANCELLED");
        cancelled.setReservedQuantity(new BigDecimal("2"));
        InventoryReservation fallback = reservation(null, null, new BigDecimal("5"), "RESERVED");
        fallback.setReservedQuantity(BigDecimal.ZERO);
        InventoryReservation fulfilled = reservation(null, null, new BigDecimal("6"), "FULFILLED");
        fulfilled.setReservedQuantity(new BigDecimal("3"));
        InventoryReservation nullReserved = reservation(null, null, new BigDecimal("7"), "RESERVED");
        nullReserved.setReservedQuantity(null);

        assertThat(resolveReservedQuantity(cancelled)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resolveReservedQuantity(fulfilled)).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(resolveReservedQuantity(fallback)).isEqualByComparingTo(new BigDecimal("5"));
        assertThat(resolveReservedQuantity(nullReserved)).isEqualByComparingTo(new BigDecimal("7"));
    }

    @Test
    void slipLinesMatchOrder_usesQuantityFallback_and_rejectsExtraProducts() {
        SalesOrder order = salesOrder(131L);
        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrder(order);
        item.setProductCode("FG-MATCH");
        item.setQuantity(new BigDecimal("2"));
        order.getItems().add(item);

        PackagingSlip exact = slip(company("MATCH"), order, 132L, "RESERVED");
        PackagingSlipLine exactLine = line(batchWithId(133L, finishedGoodWithId(134L, "FG-MATCH"), "BATCH-MATCH"), new BigDecimal("2"));
        exactLine.setOrderedQuantity(null);
        exact.getLines().add(exactLine);
        assertThat(slipLinesMatchOrder(exact, order)).isTrue();

        PackagingSlip extra = slip(company("MATCH"), order, 135L, "RESERVED");
        extra.getLines().add(line(batchWithId(136L, finishedGoodWithId(137L, "FG-MATCH"), "BATCH-MATCH"), new BigDecimal("2")));
        extra.getLines().add(line(batchWithId(138L, finishedGoodWithId(139L, "FG-EXTRA"), "BATCH-EXTRA"), BigDecimal.ONE));
        assertThat(slipLinesMatchOrder(extra, order)).isFalse();

        PackagingSlip missingBatch = slip(company("MATCH"), order, 140L, "RESERVED");
        PackagingSlipLine missingBatchLine = new PackagingSlipLine();
        missingBatchLine.setQuantity(BigDecimal.ONE);
        missingBatch.getLines().add(missingBatchLine);
        assertThat(slipLinesMatchOrder(missingBatch, order)).isFalse();

        PackagingSlip missingRequiredProduct = slip(company("MATCH"), order, 141L, "RESERVED");
        missingRequiredProduct.getLines().add(line(batchWithId(142L, finishedGoodWithId(143L, "FG-OTHER"), "BATCH-OTHER"), BigDecimal.ONE));
        assertThat(slipLinesMatchOrder(missingRequiredProduct, order)).isFalse();

        PackagingSlip wrongQuantity = slip(company("MATCH"), order, 144L, "RESERVED");
        wrongQuantity.getLines().add(line(batchWithId(145L, finishedGoodWithId(146L, "FG-MATCH"), "BATCH-WRONG"), BigDecimal.ONE));
        assertThat(slipLinesMatchOrder(wrongQuantity, order)).isFalse();
    }

    @Test
    void updateSlipStatusBasedOnAvailability_ignoresTerminalStates_and_setsPendingProductionForShortages() {
        PackagingSlip dispatched = slip(company("STATUS"), salesOrder(30L), 31L, "DISPATCHED");
        PackagingSlip active = slip(company("STATUS"), salesOrder(30L), 32L, "PENDING");
        PackagingSlip cancelled = slip(company("STATUS"), salesOrder(30L), 33L, "CANCELLED");
        PackagingSlip backorder = slip(company("STATUS"), salesOrder(30L), 34L, "BACKORDER");

        updateSlipStatusBasedOnAvailability(null, List.of());
        updateSlipStatusBasedOnAvailability(dispatched, List.of(new FinishedGoodsService.InventoryShortage("FG", BigDecimal.ONE, "Primer")));
        updateSlipStatusBasedOnAvailability(active, List.of(new FinishedGoodsService.InventoryShortage("FG", BigDecimal.ONE, "Primer")));
        updateSlipStatusBasedOnAvailability(cancelled, List.of(new FinishedGoodsService.InventoryShortage("FG", BigDecimal.ONE, "Primer")));
        updateSlipStatusBasedOnAvailability(backorder, List.of(new FinishedGoodsService.InventoryShortage("FG", BigDecimal.ONE, "Primer")));

        assertThat(dispatched.getStatus()).isEqualTo("DISPATCHED");
        assertThat(active.getStatus()).isEqualTo("PENDING_PRODUCTION");
        assertThat(cancelled.getStatus()).isEqualTo("CANCELLED");
        assertThat(backorder.getStatus()).isEqualTo("BACKORDER");
        verify(packagingSlipRepository, times(1)).save(active);
    }

    @Test
    void toSlipDto_usesNullDealerName_whenDealerIsMissing() {
        SalesOrder order = salesOrder(14L);
        PackagingSlip slip = slip(company("DTO-DIRECT"), order, 19L, "RESERVED");
        FinishedGood finishedGood = finishedGoodWithId(101L, "FG-DTO");
        FinishedGoodBatch batch = batchWithId(111L, finishedGood, "BATCH-DTO");
        ReflectionTestUtils.setField(batch, "publicId", UUID.randomUUID());
        PackagingSlipLine line = line(batch, BigDecimal.ONE);
        ReflectionTestUtils.setField(line, "id", 121L);
        slip.getLines().add(line);

        PackagingSlipDto dto = toSlipDto(slip);

        assertThat(dto.dealerName()).isNull();
    }

    private PackagingSlip selectReservationSlip(Company company, SalesOrder order) {
        return (PackagingSlip) ReflectionTestUtils.invokeMethod(engine, "selectReservationSlip", company, order);
    }

    private FinishedGoodsService.InventoryReservationResult continueBackorderReservation(SalesOrder order, PackagingSlip slip) {
        return (FinishedGoodsService.InventoryReservationResult) ReflectionTestUtils.invokeMethod(
                engine,
                "continueBackorderReservation",
                order,
                slip);
    }

    private void normalizeBackorderReservations(SalesOrder order, PackagingSlip slip) {
        ReflectionTestUtils.invokeMethod(engine, "normalizeBackorderReservations", order, slip);
    }

    private BigDecimal resolveReservedQuantity(InventoryReservation reservation) {
        return (BigDecimal) ReflectionTestUtils.invokeMethod(engine, "resolveReservedQuantity", reservation);
    }

    private boolean slipLinesMatchOrder(PackagingSlip slip, SalesOrder order) {
        return Boolean.TRUE.equals(ReflectionTestUtils.invokeMethod(engine, "slipLinesMatchOrder", slip, order));
    }

    private void updateSlipStatusBasedOnAvailability(PackagingSlip slip, List<FinishedGoodsService.InventoryShortage> shortages) {
        ReflectionTestUtils.invokeMethod(engine, "updateSlipStatusBasedOnAvailability", slip, shortages);
    }

    private void allocateItem(SalesOrder order,
                              PackagingSlip slip,
                              FinishedGood finishedGood,
                              SalesOrderItem item,
                              List<FinishedGoodsService.InventoryShortage> shortages) {
        ReflectionTestUtils.invokeMethod(engine, "allocateItem", order, slip, finishedGood, item, shortages);
    }

    private List<FinishedGoodBatch> selectBatchesByCostingMethod(FinishedGood finishedGood, LocalDate referenceDate) {
        @SuppressWarnings("unchecked")
        List<FinishedGoodBatch> batches = (List<FinishedGoodBatch>) ReflectionTestUtils.invokeMethod(
                engine, "selectBatchesByCostingMethod", finishedGood, referenceDate);
        return batches;
    }

    private PackagingSlipDto toSlipDto(PackagingSlip slip) {
        return (PackagingSlipDto) ReflectionTestUtils.invokeMethod(engine, "toSlipDto", slip);
    }

    private static Company company(String code) {
        Company company = new Company();
        company.setCode(code);
        return company;
    }

    private static SalesOrder salesOrder(Long id) {
        SalesOrder order = new SalesOrder();
        ReflectionTestUtils.setField(order, "id", id);
        order.setOrderNumber("SO-" + id);
        return order;
    }

    private static FinishedGood finishedGoodWithId(Long id, String productCode) {
        FinishedGood finishedGood = new FinishedGood();
        ReflectionTestUtils.setField(finishedGood, "id", id);
        finishedGood.setProductCode(productCode);
        finishedGood.setName(productCode);
        return finishedGood;
    }

    private static FinishedGoodBatch batchWithId(Long id, FinishedGood finishedGood, String code) {
        FinishedGoodBatch batch = new FinishedGoodBatch();
        ReflectionTestUtils.setField(batch, "id", id);
        batch.setFinishedGood(finishedGood);
        batch.setBatchCode(code);
        batch.setQuantityTotal(new BigDecimal("10"));
        batch.setQuantityAvailable(new BigDecimal("10"));
        return batch;
    }

    private static PackagingSlip slip(Company company, SalesOrder order, Long id, String status) {
        PackagingSlip slip = new PackagingSlip();
        ReflectionTestUtils.setField(slip, "id", id);
        slip.setCompany(company);
        slip.setSalesOrder(order);
        slip.setSlipNumber("PS-" + id);
        slip.setStatus(status);
        return slip;
    }

    private static PackagingSlipLine line(FinishedGoodBatch batch, BigDecimal quantity) {
        PackagingSlipLine line = new PackagingSlipLine();
        line.setFinishedGoodBatch(batch);
        line.setQuantity(quantity);
        line.setOrderedQuantity(quantity);
        return line;
    }

    private static InventoryReservation reservation(FinishedGoodBatch batch,
                                                    FinishedGood finishedGood,
                                                    BigDecimal quantity,
                                                    String status) {
        InventoryReservation reservation = new InventoryReservation();
        reservation.setFinishedGoodBatch(batch);
        reservation.setFinishedGood(finishedGood);
        reservation.setQuantity(quantity);
        reservation.setReservedQuantity(quantity);
        reservation.setStatus(status);
        return reservation;
    }
}
