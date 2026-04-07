package com.bigbrightpaints.erp.modules.sales.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.invoice.domain.Invoice;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceRepository;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderRepository;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderStatusHistory;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderStatusHistoryRepository;

@ExtendWith(MockitoExtension.class)
@Tag("critical")
class SalesOrderAutoCloseServiceTest {

  @Mock private SalesOrderRepository salesOrderRepository;
  @Mock private SalesOrderStatusHistoryRepository salesOrderStatusHistoryRepository;
  @Mock private InvoiceRepository invoiceRepository;
  @Mock private CompanyClock companyClock;

  private SalesOrderAutoCloseService service;
  private Company company;

  @BeforeEach
  void setUp() {
    service =
        new SalesOrderAutoCloseService(
            salesOrderRepository, salesOrderStatusHistoryRepository, invoiceRepository, companyClock);
    company = new Company();
    ReflectionTestUtils.setField(company, "id", 7L);
  }

  @Test
  void autoCloseFullyPaidOrders_closesEligibleOrderWhenAllInvoicesPaid() {
    SalesOrder order = new SalesOrder();
    ReflectionTestUtils.setField(order, "id", 44L);
    order.setCompany(company);
    order.setStatus("INVOICED");

    Invoice touched = new Invoice();
    touched.setCompany(company);
    touched.setSalesOrder(order);
    touched.setStatus("PAID");
    touched.setOutstandingAmount(BigDecimal.ZERO);

    Invoice sibling = new Invoice();
    sibling.setCompany(company);
    sibling.setSalesOrder(order);
    sibling.setStatus("PAID");
    sibling.setOutstandingAmount(BigDecimal.ZERO);

    when(salesOrderRepository.findWithItemsByCompanyAndIdForUpdate(company, 44L))
        .thenReturn(Optional.of(order));
    when(invoiceRepository.findAllByCompanyAndSalesOrderId(company, 44L))
        .thenReturn(List.of(touched, sibling));
    when(companyClock.now(company)).thenReturn(Instant.parse("2026-04-07T10:15:30Z"));

    service.autoCloseFullyPaidOrders(company, List.of(touched));

    assertThat(order.getStatus()).isEqualTo("CLOSED");
    verify(salesOrderRepository).save(order);
    ArgumentCaptor<SalesOrderStatusHistory> historyCaptor =
        ArgumentCaptor.forClass(SalesOrderStatusHistory.class);
    verify(salesOrderStatusHistoryRepository).save(historyCaptor.capture());
    assertThat(historyCaptor.getValue().getReasonCode()).isEqualTo("ORDER_CLOSED_AUTO");
    assertThat(historyCaptor.getValue().getFromStatus()).isEqualTo("INVOICED");
    assertThat(historyCaptor.getValue().getToStatus()).isEqualTo("CLOSED");
  }

  @Test
  void autoCloseFullyPaidOrders_keepsOrderOpenWhenAnyActionableInvoiceOutstanding() {
    SalesOrder order = new SalesOrder();
    ReflectionTestUtils.setField(order, "id", 45L);
    order.setCompany(company);
    order.setStatus("INVOICED");

    Invoice touched = new Invoice();
    touched.setCompany(company);
    touched.setSalesOrder(order);
    touched.setStatus("PARTIAL");
    touched.setOutstandingAmount(new BigDecimal("50.00"));

    when(salesOrderRepository.findWithItemsByCompanyAndIdForUpdate(company, 45L))
        .thenReturn(Optional.of(order));
    when(invoiceRepository.findAllByCompanyAndSalesOrderId(company, 45L)).thenReturn(List.of(touched));

    service.autoCloseFullyPaidOrders(company, List.of(touched));

    assertThat(order.getStatus()).isEqualTo("INVOICED");
    verify(salesOrderRepository, never()).save(order);
    verify(salesOrderStatusHistoryRepository, never()).save(any());
  }

  @Test
  void autoCloseFullyPaidOrders_ignoresOrdersOutsideEligibleStatuses() {
    SalesOrder order = new SalesOrder();
    ReflectionTestUtils.setField(order, "id", 46L);
    order.setCompany(company);
    order.setStatus("CONFIRMED");

    Invoice touched = new Invoice();
    touched.setCompany(company);
    touched.setSalesOrder(order);
    touched.setStatus("PAID");
    touched.setOutstandingAmount(BigDecimal.ZERO);

    when(salesOrderRepository.findWithItemsByCompanyAndIdForUpdate(company, 46L))
        .thenReturn(Optional.of(order));

    service.autoCloseFullyPaidOrders(company, List.of(touched));

    verify(invoiceRepository, never()).findAllByCompanyAndSalesOrderId(company, 46L);
    verify(salesOrderRepository, never()).save(any(SalesOrder.class));
    verify(salesOrderStatusHistoryRepository, never()).save(any(SalesOrderStatusHistory.class));
  }
}
