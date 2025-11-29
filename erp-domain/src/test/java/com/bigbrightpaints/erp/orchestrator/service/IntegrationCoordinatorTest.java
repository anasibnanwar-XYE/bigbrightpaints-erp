package com.bigbrightpaints.erp.orchestrator.service;

import com.bigbrightpaints.erp.core.security.CompanyContextHolder;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingFacade;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingService;
import com.bigbrightpaints.erp.modules.factory.service.FactoryService;
import com.bigbrightpaints.erp.modules.hr.service.HrService;
import com.bigbrightpaints.erp.modules.invoice.service.InvoiceService;
import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService;
import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService.InventoryReservationResult;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import com.bigbrightpaints.erp.modules.sales.service.SalesJournalService;
import com.bigbrightpaints.erp.modules.sales.service.SalesService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.reports.service.ReportService;
import com.bigbrightpaints.erp.orchestrator.repository.OrderAutoApprovalState;
import com.bigbrightpaints.erp.orchestrator.repository.OrderAutoApprovalStateRepository;
import java.math.BigDecimal;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.AbstractPlatformTransactionManager;
import org.springframework.transaction.support.DefaultTransactionStatus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class IntegrationCoordinatorTest {

    private static final String COMPANY_ID = "COMP";
    private static final Long ORDER_ID = 42L;

    @Mock
    private SalesService salesService;
    @Mock
    private FactoryService factoryService;
    @Mock
    private FinishedGoodsService finishedGoodsService;
    @Mock
    private InvoiceService invoiceService;
    @Mock
    private AccountingService accountingService;
    @Mock
    private SalesJournalService salesJournalService;
    @Mock
    private HrService hrService;
    @Mock
    private ReportService reportService;
    @Mock
    private OrderAutoApprovalStateRepository orderAutoApprovalStateRepository;
    @Mock
    private AccountingFacade accountingFacade;

    private IntegrationCoordinator integrationCoordinator;
    private SalesOrder order;
    private Company company;
    private OrderAutoApprovalState state;

    @BeforeEach
    void setUp() {
        integrationCoordinator = new IntegrationCoordinator(
                salesService,
                factoryService,
                finishedGoodsService,
                invoiceService,
                accountingService,
                salesJournalService,
                hrService,
                reportService,
                orderAutoApprovalStateRepository,
                accountingFacade,
                new NoOpTransactionManager(),
                10L,
                20L);

        company = new Company();
        company.setCode(COMPANY_ID);
        company.setTimezone("UTC");
        order = new SalesOrder();
        order.setCompany(company);
        order.setDealer(new Dealer());

        state = new OrderAutoApprovalState(COMPANY_ID, ORDER_ID);
        when(orderAutoApprovalStateRepository.findByCompanyCodeAndOrderId(COMPANY_ID, ORDER_ID))
                .thenReturn(Optional.of(state));
    }

    @AfterEach
    void tearDown() {
        CompanyContextHolder.clear();
    }

    @Test
    void autoApproveOrderFinalizesShipmentWhenInventoryAvailable() {
        InventoryReservationResult reservation = new InventoryReservationResult(null, List.of());
        when(salesService.getOrderWithItems(ORDER_ID)).thenReturn(order);
        when(finishedGoodsService.reserveForOrder(order)).thenReturn(reservation);
        when(finishedGoodsService.markSlipDispatched(ORDER_ID)).thenReturn(List.of());
        when(salesJournalService.postSalesJournal(any(), any(), any(), any(), any())).thenReturn(100L);
        when(invoiceService.issueInvoiceForOrder(ORDER_ID)).thenReturn(null);

        IntegrationCoordinator.AutoApprovalResult result =
                integrationCoordinator.autoApproveOrder(String.valueOf(ORDER_ID), new BigDecimal("1500"), COMPANY_ID);

        assertThat(result.orderStatus()).isEqualTo("SHIPPED");
        assertThat(result.awaitingProduction()).isFalse();
        assertThat(state.isInventoryReserved()).isTrue();
        assertThat(state.isDispatchFinalized()).isTrue();
        assertThat(state.isInvoiceIssued()).isTrue();
        assertThat(state.isOrderStatusUpdated()).isTrue();
        assertThat(state.isCompleted()).isTrue();

        verify(salesService).updateStatus(ORDER_ID, "RESERVED");
        verify(salesService).updateStatus(ORDER_ID, "READY_TO_SHIP");
        verify(salesService).updateStatus(ORDER_ID, "SHIPPED");
        verify(finishedGoodsService).markSlipDispatched(ORDER_ID);
        verify(invoiceService).issueInvoiceForOrder(ORDER_ID);
    }

    @Test
    void updateFulfillmentCancelledMarksStateFailed() {
        IntegrationCoordinator.AutoApprovalResult result =
                integrationCoordinator.updateFulfillment(String.valueOf(ORDER_ID), "cancelled", COMPANY_ID);

        assertThat(result.orderStatus()).isEqualTo("CANCELLED");
        assertThat(result.awaitingProduction()).isFalse();
        assertThat(state.getStatus()).isEqualToIgnoringCase("FAILED");
        assertThat(state.getLastError()).isEqualTo("Cancelled");
        verify(salesService).updateStatus(ORDER_ID, "CANCELLED");
    }

    @Test
    void autoApproveOrderRetriesWithoutReplayingReservation() {
        state.markInventoryReserved();
        state.markOrderStatusUpdated();

        when(salesService.getOrderWithItems(ORDER_ID)).thenReturn(order);
        when(finishedGoodsService.markSlipDispatched(ORDER_ID)).thenReturn(List.of());
        when(salesJournalService.postSalesJournal(any(), any(), any(), any(), any())).thenReturn(200L);
        when(invoiceService.issueInvoiceForOrder(ORDER_ID)).thenReturn(null);

        IntegrationCoordinator.AutoApprovalResult result =
                integrationCoordinator.autoApproveOrder(String.valueOf(ORDER_ID), null, COMPANY_ID);

        assertThat(result.orderStatus()).isEqualTo("SHIPPED");
        assertThat(result.awaitingProduction()).isFalse();
        assertThat(state.isInventoryReserved()).isTrue();
        assertThat(state.isDispatchFinalized()).isTrue();
        assertThat(state.isSalesJournalPosted()).isTrue();
        assertThat(state.isInvoiceIssued()).isTrue();
        assertThat(state.isCompleted()).isTrue();

        verify(finishedGoodsService, never()).reserveForOrder(any());
        verify(salesService, never()).updateStatus(eq(ORDER_ID), eq("READY_TO_SHIP"));
        verify(salesService, never()).updateStatus(eq(ORDER_ID), eq("RESERVED"));
        verify(salesService, times(1)).updateStatus(ORDER_ID, "SHIPPED");
    }

    private static class NoOpTransactionManager extends AbstractPlatformTransactionManager {
        @Override
        protected Object doGetTransaction() {
            return new Object();
        }

        @Override
        protected void doBegin(Object transaction, TransactionDefinition definition) {
            // no-op
        }

        @Override
        protected void doCommit(DefaultTransactionStatus status) {
            // no-op
        }

        @Override
        protected void doRollback(DefaultTransactionStatus status) {
            // no-op
        }
    }
}
