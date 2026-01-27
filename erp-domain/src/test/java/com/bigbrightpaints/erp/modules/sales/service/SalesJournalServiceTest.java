package com.bigbrightpaints.erp.modules.sales.service;

import com.bigbrightpaints.erp.modules.accounting.service.AccountingFacade;
import com.bigbrightpaints.erp.modules.accounting.service.CompanyAccountingSettingsService;
import com.bigbrightpaints.erp.modules.accounting.service.CompanyDefaultAccountsService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProductRepository;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderItem;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Map;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class SalesJournalServiceTest {

    @Mock
    private FinishedGoodsService finishedGoodsService;
    @Mock
    private AccountingFacade accountingFacade;
    @Mock
    private ProductionProductRepository productionProductRepository;
    @Mock
    private CompanyDefaultAccountsService companyDefaultAccountsService;
    @Mock
    private CompanyAccountingSettingsService companyAccountingSettingsService;

    private SalesJournalService salesJournalService;

    @BeforeEach
    void setup() {
        salesJournalService = new SalesJournalService(
                finishedGoodsService,
                accountingFacade,
                productionProductRepository,
                companyDefaultAccountsService,
                companyAccountingSettingsService
        );
    }

    @Test
    void postSalesJournal_includesDiscountLinesForLineDiscounts() {
        Company company = new Company();
        company.setTimezone("UTC");
        Dealer dealer = new Dealer();
        ReflectionTestUtils.setField(dealer, "id", 42L);

        SalesOrder order = new SalesOrder();
        order.setCompany(company);
        order.setDealer(dealer);
        order.setOrderNumber("SO-1");
        order.setGstInclusive(false);

        SalesOrderItem item = new SalesOrderItem();
        item.setProductCode("SKU-1");
        item.setQuantity(new BigDecimal("2"));
        item.setUnitPrice(new BigDecimal("100.00"));
        item.setLineSubtotal(new BigDecimal("180.00"));
        item.setGstAmount(new BigDecimal("18.00"));
        order.getItems().add(item);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.bigbrightpaints.erp.core.exception.ApplicationException.class,
                () -> salesJournalService.postSalesJournal(
                        order,
                        new BigDecimal("198.00"),
                        "INV-REF",
                        LocalDate.of(2024, 4, 8),
                        "Invoice INV-1"));
        verify(accountingFacade, never()).postSalesJournal(
                eq(42L),
                eq("SO-1"),
                eq(LocalDate.of(2024, 4, 8)),
                eq("Invoice INV-1"),
                eq(Map.of()),
                eq(Map.of()),
                eq(Map.of()),
                eq(new BigDecimal("198.00")),
                eq("INV-REF"));
    }

    @Test
    void postSalesJournal_normalizesGstInclusiveDiscounts() {
        Company company = new Company();
        company.setTimezone("UTC");
        Dealer dealer = new Dealer();
        ReflectionTestUtils.setField(dealer, "id", 77L);

        SalesOrder order = new SalesOrder();
        order.setCompany(company);
        order.setDealer(dealer);
        order.setOrderNumber("SO-2");
        order.setGstInclusive(true);

        SalesOrderItem item = new SalesOrderItem();
        item.setProductCode("SKU-2");
        item.setQuantity(new BigDecimal("1"));
        item.setUnitPrice(new BigDecimal("110.00"));
        item.setLineSubtotal(new BigDecimal("90.00"));
        item.setGstAmount(new BigDecimal("9.00"));
        item.setGstRate(new BigDecimal("10.00"));
        order.getItems().add(item);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.bigbrightpaints.erp.core.exception.ApplicationException.class,
                () -> salesJournalService.postSalesJournal(
                        order,
                        new BigDecimal("99.00"),
                        "INV-REF-2",
                        LocalDate.of(2024, 4, 9),
                        "Invoice INV-2"));
        verify(accountingFacade, never()).postSalesJournal(
                eq(77L),
                eq("SO-2"),
                eq(LocalDate.of(2024, 4, 9)),
                eq("Invoice INV-2"),
                eq(Map.of()),
                eq(Map.of()),
                eq(Map.of()),
                eq(new BigDecimal("99.00")),
                eq("INV-REF-2"));
    }

    @Test
    void postSalesJournal_ignoresInclusiveRoundingDelta() {
        Company company = new Company();
        company.setTimezone("UTC");
        Dealer dealer = new Dealer();
        ReflectionTestUtils.setField(dealer, "id", 88L);

        SalesOrder order = new SalesOrder();
        order.setCompany(company);
        order.setDealer(dealer);
        order.setOrderNumber("SO-3");
        order.setGstInclusive(true);

        SalesOrderItem item = new SalesOrderItem();
        item.setProductCode("SKU-3");
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(new BigDecimal("100.01"));
        item.setLineSubtotal(new BigDecimal("84.75"));
        item.setGstAmount(new BigDecimal("15.25"));
        item.setGstRate(new BigDecimal("18.00"));
        order.getItems().add(item);

        org.junit.jupiter.api.Assertions.assertThrows(
                com.bigbrightpaints.erp.core.exception.ApplicationException.class,
                () -> salesJournalService.postSalesJournal(
                        order,
                        new BigDecimal("100.00"),
                        "INV-REF-3",
                        LocalDate.of(2024, 4, 10),
                        "Invoice INV-3"));
        verify(accountingFacade, never()).postSalesJournal(
                eq(88L),
                eq("SO-3"),
                eq(LocalDate.of(2024, 4, 10)),
                eq("Invoice INV-3"),
                eq(Map.of()),
                eq(Map.of()),
                eq(Map.of()),
                eq(new BigDecimal("100.00")),
                eq("INV-REF-3"));
    }
}
