package com.bigbrightpaints.erp.modules.inventory.service;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryReference;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryReservation;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryReservationRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlip;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlipLine;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlipRepository;
import com.bigbrightpaints.erp.modules.inventory.dto.DispatchConfirmationRequest;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderItem;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

@Transactional
class FinishedGoodsReservationEngineTest extends AbstractIntegrationTest {

    @Autowired
    private FinishedGoodsService finishedGoodsService;

    @Autowired
    private FinishedGoodRepository finishedGoodRepository;

    @Autowired
    private FinishedGoodBatchRepository finishedGoodBatchRepository;

    @Autowired
    private PackagingSlipRepository packagingSlipRepository;

    @Autowired
    private InventoryReservationRepository inventoryReservationRepository;

    @Autowired
    private SalesOrderRepository salesOrderRepository;

    @AfterEach
    void clearContext() {
        CompanyContextHolder.clear();
    }

    @Test
    void reserveForOrderReplayAfterDispatch_keepsFulfilledReservationsTerminal() {
        Company company = seedCompany("RES-REPLAY-FULL");
        FinishedGood finishedGood = createFinishedGood(company, "FG-RES-REPLAY-FULL", new BigDecimal("5"), BigDecimal.ZERO);
        FinishedGoodBatch batch = createBatch(finishedGood, "BATCH-RES-REPLAY-FULL", new BigDecimal("5"), new BigDecimal("5"), new BigDecimal("8"));
        SalesOrder order = createOrder(company, "SO-RES-REPLAY-FULL-" + UUID.randomUUID(), finishedGood.getProductCode(), new BigDecimal("5"));

        finishedGoodsService.reserveForOrder(order);
        PackagingSlip primarySlip = findPrimarySlip(company, order.getId());
        finishedGoodsService.markSlipDispatched(order.getId(), primarySlip);

        FinishedGoodsService.InventoryReservationResult replay = finishedGoodsService.reserveForOrder(order);

        List<InventoryReservation> reservations = reservationsFor(company, order.getId());
        assertThat(replay.shortages()).isEmpty();
        assertThat(reservations).hasSize(1);
        assertThat(reservations.getFirst().getStatus()).isEqualTo("FULFILLED");
        assertThat(reservations.getFirst().getFulfilledQuantity()).isEqualByComparingTo(new BigDecimal("5"));
        assertThat(zeroIfNull(reservations.getFirst().getReservedQuantity())).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(finishedGoodRepository.findById(finishedGood.getId()).orElseThrow().getReservedStock())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(finishedGoodBatchRepository.findById(batch.getId()).orElseThrow().getQuantityAvailable())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(packagingSlipRepository.findByIdAndCompany(primarySlip.getId(), company).orElseThrow().getStatus())
                .isEqualTo("DISPATCHED");
    }

    @Test
    void reserveForOrderReplayAfterPartialDispatch_preservesRemainingBalanceAndPartialState() {
        Company company = seedCompany("RES-REPLAY-PARTIAL");
        FinishedGood finishedGood = createFinishedGood(company, "FG-RES-REPLAY-PARTIAL", new BigDecimal("10"), BigDecimal.ZERO);
        FinishedGoodBatch batch = createBatch(finishedGood, "BATCH-RES-REPLAY-PARTIAL", new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("9"));
        SalesOrder order = createOrder(company, "SO-RES-REPLAY-PARTIAL-" + UUID.randomUUID(), finishedGood.getProductCode(), new BigDecimal("10"));

        finishedGoodsService.reserveForOrder(order);
        PackagingSlip primarySlip = findPrimarySlip(company, order.getId());
        PackagingSlipLine line = primarySlip.getLines().getFirst();
        finishedGoodsService.confirmDispatch(
                new DispatchConfirmationRequest(
                        primarySlip.getId(),
                        List.of(new DispatchConfirmationRequest.LineConfirmation(line.getId(), new BigDecimal("6"), null)),
                        "partial shipment",
                        "tester",
                        null),
                "tester");

        FinishedGoodsService.InventoryReservationResult replay = finishedGoodsService.reserveForOrder(order);

        List<InventoryReservation> reservations = reservationsFor(company, order.getId());
        assertThat(replay.shortages()).isEmpty();
        assertThat(reservations).hasSize(1);
        assertThat(reservations.getFirst().getStatus()).isEqualTo("PARTIAL");
        assertThat(reservations.getFirst().getQuantity()).isEqualByComparingTo(new BigDecimal("10"));
        assertThat(reservations.getFirst().getFulfilledQuantity()).isEqualByComparingTo(new BigDecimal("6"));
        assertThat(reservations.getFirst().getReservedQuantity()).isEqualByComparingTo(new BigDecimal("4"));
        assertThat(finishedGoodRepository.findById(finishedGood.getId()).orElseThrow().getReservedStock())
                .isEqualByComparingTo(new BigDecimal("4"));
        assertThat(finishedGoodBatchRepository.findById(batch.getId()).orElseThrow().getQuantityAvailable())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(packagingSlipRepository.findByIdAndCompany(primarySlip.getId(), company).orElseThrow().getStatus())
                .isEqualTo("DISPATCHED");
        assertThat(packagingSlipRepository.findAllByCompanyAndSalesOrderId(company, order.getId()))
                .anyMatch(existing -> existing.isBackorder() && "BACKORDER".equalsIgnoreCase(existing.getStatus()));
    }

    @Test
    void reserveForOrderReplay_preservesBackorderReservationState() {
        Company company = seedCompany("RES-REPLAY-BACKORDER");
        FinishedGood finishedGood = createFinishedGood(company, "FG-RES-REPLAY-BACKORDER", new BigDecimal("5"), new BigDecimal("5"));
        FinishedGoodBatch batch = createBatch(finishedGood, "BATCH-RES-REPLAY-BACKORDER", new BigDecimal("5"), BigDecimal.ZERO, new BigDecimal("7"));
        SalesOrder order = createOrder(company, "SO-RES-REPLAY-BACKORDER-" + UUID.randomUUID(), finishedGood.getProductCode(), new BigDecimal("5"));
        createSlip(company, order, "BACKORDER", batch, new BigDecimal("5"));
        InventoryReservation reservation = createReservation(order, finishedGood, batch, new BigDecimal("5"));
        reservation.setStatus("BACKORDER");
        inventoryReservationRepository.saveAndFlush(reservation);

        FinishedGoodsService.InventoryReservationResult replay = finishedGoodsService.reserveForOrder(order);

        List<InventoryReservation> reservations = reservationsFor(company, order.getId());
        assertThat(replay.shortages()).isEmpty();
        assertThat(reservations).hasSize(1);
        assertThat(reservations.getFirst().getStatus()).isEqualTo("BACKORDER");
        assertThat(reservations.getFirst().getReservedQuantity()).isEqualByComparingTo(new BigDecimal("5"));
        assertThat(zeroIfNull(reservations.getFirst().getFulfilledQuantity())).isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(finishedGoodRepository.findById(finishedGood.getId()).orElseThrow().getReservedStock())
                .isEqualByComparingTo(new BigDecimal("5"));
        assertThat(finishedGoodBatchRepository.findById(batch.getId()).orElseThrow().getQuantityAvailable())
                .isEqualByComparingTo(BigDecimal.ZERO);
    }

    private Company seedCompany(String code) {
        Company company = dataSeeder.ensureCompany(code, code + " Ltd");
        CompanyContextHolder.setCompanyId(company.getCode());
        return company;
    }

    private SalesOrder createOrder(Company company, String orderNumber, String productCode, BigDecimal quantity) {
        SalesOrder order = new SalesOrder();
        order.setCompany(company);
        order.setOrderNumber(orderNumber);
        order.setStatus("PENDING");
        order.setTotalAmount(BigDecimal.ZERO);
        order.setCurrency("INR");

        SalesOrderItem item = new SalesOrderItem();
        item.setSalesOrder(order);
        item.setProductCode(productCode);
        item.setQuantity(quantity);
        item.setUnitPrice(BigDecimal.ONE);
        item.setLineSubtotal(BigDecimal.ZERO);
        item.setLineTotal(BigDecimal.ZERO);
        order.getItems().add(item);
        return salesOrderRepository.saveAndFlush(order);
    }

    private FinishedGood createFinishedGood(Company company,
                                            String productCode,
                                            BigDecimal currentStock,
                                            BigDecimal reservedStock) {
        FinishedGood finishedGood = new FinishedGood();
        finishedGood.setCompany(company);
        finishedGood.setProductCode(productCode);
        finishedGood.setName(productCode);
        finishedGood.setUnit("UNIT");
        finishedGood.setCostingMethod("FIFO");
        finishedGood.setCurrentStock(currentStock);
        finishedGood.setReservedStock(reservedStock);
        finishedGood.setValuationAccountId(100L);
        finishedGood.setCogsAccountId(200L);
        finishedGood.setRevenueAccountId(300L);
        finishedGood.setTaxAccountId(400L);
        return finishedGoodRepository.saveAndFlush(finishedGood);
    }

    private FinishedGoodBatch createBatch(FinishedGood finishedGood,
                                          String batchCode,
                                          BigDecimal quantityTotal,
                                          BigDecimal quantityAvailable,
                                          BigDecimal unitCost) {
        FinishedGoodBatch batch = new FinishedGoodBatch();
        batch.setFinishedGood(finishedGood);
        batch.setBatchCode(batchCode);
        batch.setQuantityTotal(quantityTotal);
        batch.setQuantityAvailable(quantityAvailable);
        batch.setUnitCost(unitCost);
        batch.setManufacturedAt(Instant.now());
        return finishedGoodBatchRepository.saveAndFlush(batch);
    }

    private PackagingSlip createSlip(Company company,
                                     SalesOrder order,
                                     String status,
                                     FinishedGoodBatch batch,
                                     BigDecimal quantity) {
        PackagingSlip slip = new PackagingSlip();
        slip.setCompany(company);
        slip.setSalesOrder(order);
        slip.setSlipNumber(order.getOrderNumber() + "-PS");
        slip.setStatus(status);
        slip.setBackorder("BACKORDER".equalsIgnoreCase(status));

        PackagingSlipLine line = new PackagingSlipLine();
        line.setPackagingSlip(slip);
        line.setFinishedGoodBatch(batch);
        line.setOrderedQuantity(quantity);
        line.setQuantity(quantity);
        line.setUnitCost(batch.getUnitCost());
        slip.getLines().add(line);
        return packagingSlipRepository.saveAndFlush(slip);
    }

    private InventoryReservation createReservation(SalesOrder order,
                                                   FinishedGood finishedGood,
                                                   FinishedGoodBatch batch,
                                                   BigDecimal quantity) {
        InventoryReservation reservation = new InventoryReservation();
        reservation.setFinishedGood(finishedGood);
        reservation.setFinishedGoodBatch(batch);
        reservation.setReferenceType(InventoryReference.SALES_ORDER);
        reservation.setReferenceId(order.getId().toString());
        reservation.setQuantity(quantity);
        reservation.setReservedQuantity(quantity);
        reservation.setStatus("RESERVED");
        return inventoryReservationRepository.saveAndFlush(reservation);
    }

    private PackagingSlip findPrimarySlip(Company company, Long orderId) {
        return packagingSlipRepository.findAllByCompanyAndSalesOrderId(company, orderId).stream()
                .filter(existing -> !existing.isBackorder())
                .findFirst()
                .orElseThrow();
    }

    private List<InventoryReservation> reservationsFor(Company company, Long orderId) {
        return inventoryReservationRepository.findByFinishedGoodCompanyAndReferenceTypeAndReferenceId(
                company,
                InventoryReference.SALES_ORDER,
                orderId.toString());
    }

    private BigDecimal zeroIfNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }
}
