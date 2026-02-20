package com.bigbrightpaints.erp.truthsuite.o2c;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bigbrightpaints.erp.core.audit.AuditEvent;
import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingFacade;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingService;
import com.bigbrightpaints.erp.modules.accounting.service.CompanyAccountingSettingsService;
import com.bigbrightpaints.erp.modules.accounting.service.CompanyDefaultAccountsService;
import com.bigbrightpaints.erp.modules.accounting.service.DealerLedgerService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.factory.domain.FactoryTaskRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlip;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlipLine;
import com.bigbrightpaints.erp.modules.inventory.domain.PackagingSlipRepository;
import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService;
import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService.InventoryReservationResult;
import com.bigbrightpaints.erp.modules.invoice.domain.Invoice;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceRepository;
import com.bigbrightpaints.erp.modules.invoice.service.InvoiceNumberService;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProductRepository;
import com.bigbrightpaints.erp.modules.sales.domain.CreditRequestRepository;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import com.bigbrightpaints.erp.modules.sales.domain.PromotionRepository;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderItem;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderRepository;
import com.bigbrightpaints.erp.modules.sales.domain.SalesTargetRepository;
import com.bigbrightpaints.erp.modules.sales.dto.DispatchConfirmRequest;
import com.bigbrightpaints.erp.modules.sales.dto.DispatchConfirmResponse;
import com.bigbrightpaints.erp.modules.sales.service.CreditLimitOverrideService;
import com.bigbrightpaints.erp.modules.sales.service.OrderNumberService;
import com.bigbrightpaints.erp.modules.sales.service.SalesService;
import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.support.SimpleTransactionStatus;

@Tag("critical")
@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
class TS_truthsuite_o2c_Approval_RuntimeTest {

    @Mock
    private CompanyContextService companyContextService;
    @Mock
    private DealerRepository dealerRepository;
    @Mock
    private SalesOrderRepository salesOrderRepository;
    @Mock
    private PromotionRepository promotionRepository;
    @Mock
    private SalesTargetRepository salesTargetRepository;
    @Mock
    private CreditRequestRepository creditRequestRepository;
    @Mock
    private OrderNumberService orderNumberService;
    @Mock
    private ApplicationEventPublisher eventPublisher;
    @Mock
    private ProductionProductRepository productionProductRepository;
    @Mock
    private DealerLedgerService dealerLedgerService;
    @Mock
    private FinishedGoodRepository finishedGoodRepository;
    @Mock
    private AccountRepository accountRepository;
    @Mock
    private CompanyEntityLookup companyEntityLookup;
    @Mock
    private PackagingSlipRepository packagingSlipRepository;
    @Mock
    private FinishedGoodsService finishedGoodsService;
    @Mock
    private AccountingService accountingService;
    @Mock
    private AccountingFacade accountingFacade;
    @Mock
    private com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository journalEntryRepository;
    @Mock
    private InvoiceNumberService invoiceNumberService;
    @Mock
    private InvoiceRepository invoiceRepository;
    @Mock
    private FactoryTaskRepository factoryTaskRepository;
    @Mock
    private CompanyDefaultAccountsService companyDefaultAccountsService;
    @Mock
    private CompanyAccountingSettingsService companyAccountingSettingsService;
    @Mock
    private CreditLimitOverrideService creditLimitOverrideService;
    @Mock
    private AuditService auditService;
    @Mock
    private CompanyClock companyClock;

    private final PlatformTransactionManager transactionManager = new NoopTransactionManager();

    private SalesService salesService;
    private Company company;

    @BeforeEach
    void setUp() {
        salesService = new SalesService(
                companyContextService,
                dealerRepository,
                salesOrderRepository,
                promotionRepository,
                salesTargetRepository,
                creditRequestRepository,
                orderNumberService,
                eventPublisher,
                productionProductRepository,
                dealerLedgerService,
                finishedGoodRepository,
                accountRepository,
                companyEntityLookup,
                packagingSlipRepository,
                finishedGoodsService,
                accountingService,
                accountingFacade,
                journalEntryRepository,
                invoiceNumberService,
                invoiceRepository,
                factoryTaskRepository,
                companyDefaultAccountsService,
                companyAccountingSettingsService,
                creditLimitOverrideService,
                auditService,
                companyClock,
                transactionManager);

        when(finishedGoodsService.reserveForOrder(any()))
                .thenReturn(new InventoryReservationResult(null, List.of()));
        when(companyDefaultAccountsService.requireDefaults())
                .thenReturn(new CompanyDefaultAccountsService.DefaultAccounts(1L, 2L, 3L, 4L, 5L));
        when(companyDefaultAccountsService.getDefaults())
                .thenReturn(new CompanyDefaultAccountsService.DefaultAccounts(1L, 2L, 3L, 4L, 5L));

        company = new Company();
        company.setCode("COMP");
        company.setTimezone("UTC");
        company.setDefaultInventoryAccountId(1L);
        company.setDefaultCogsAccountId(2L);
        company.setDefaultRevenueAccountId(3L);
        company.setDefaultDiscountAccountId(4L);
        company.setDefaultTaxAccountId(5L);

        when(companyContextService.requireCurrentCompany()).thenReturn(company);
        when(companyClock.today(any())).thenReturn(LocalDate.of(2026, 1, 27));
        when(invoiceRepository.findAllByCompanyAndSalesOrderId(eq(company), any(Long.class))).thenReturn(List.of());
    }

    @Test
    void confirmDispatchFailsWhenExceptionPresentWithoutOverrideRequestId() {
        Dealer dealer = dealerWithCreditLimit(42L, BigDecimal.valueOf(1000));
        Account receivable = new Account();
        receivable.setName("AR");
        setField(receivable, "id", 900L);
        dealer.setReceivableAccount(receivable);

        SalesOrder order = newOrderWithSingleItem(dealer, new BigDecimal("100"));
        PackagingSlip slip = pendingSlip(order);
        slip.getLines().add(defaultSlipLine(slip));

        when(packagingSlipRepository.findAndLockByIdAndCompany(55L, company)).thenReturn(Optional.of(slip));
        when(packagingSlipRepository.findAllByCompanyAndSalesOrderId(company, 10L)).thenReturn(List.of(slip));
        when(companyEntityLookup.requireSalesOrder(company, 10L)).thenReturn(order);
        when(dealerRepository.lockByCompanyAndId(company, dealer.getId())).thenReturn(Optional.of(dealer));

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> salesService.confirmDispatch(new DispatchConfirmRequest(
                        55L,
                        null,
                        List.of(new DispatchConfirmRequest.DispatchLine(99L, null, BigDecimal.ONE, null, new BigDecimal("10"), null, null, null)),
                        null,
                        "admin",
                        Boolean.FALSE,
                        "Discount exception requires approval",
                        null)));

        assertEquals(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("overrideRequestId"));
    }

    @Test
    void confirmDispatchFailsWhenOverrideRequestIsNotApproved() {
        Dealer dealer = dealerWithCreditLimit(42L, BigDecimal.valueOf(1000));
        Account receivable = new Account();
        receivable.setName("AR");
        setField(receivable, "id", 900L);
        dealer.setReceivableAccount(receivable);

        SalesOrder order = newOrderWithSingleItem(dealer, new BigDecimal("100"));
        PackagingSlip slip = pendingSlip(order);
        slip.getLines().add(defaultSlipLine(slip));

        when(packagingSlipRepository.findAndLockByIdAndCompany(55L, company)).thenReturn(Optional.of(slip));
        when(packagingSlipRepository.findAllByCompanyAndSalesOrderId(company, 10L)).thenReturn(List.of(slip));
        when(companyEntityLookup.requireSalesOrder(company, 10L)).thenReturn(order);
        when(dealerRepository.lockByCompanyAndId(company, dealer.getId())).thenReturn(Optional.of(dealer));
        when(creditLimitOverrideService.isOverrideApproved(
                eq(900L),
                eq(company),
                eq(dealer),
                eq(slip),
                eq(order),
                any())).thenReturn(false);

        ApplicationException ex = assertThrows(
                ApplicationException.class,
                () -> salesService.confirmDispatch(new DispatchConfirmRequest(
                        55L,
                        null,
                        List.of(new DispatchConfirmRequest.DispatchLine(99L, null, BigDecimal.ONE, null, new BigDecimal("10"), null, null, null)),
                        null,
                        "admin",
                        Boolean.FALSE,
                        "Discount exception requires approval",
                        900L)));

        assertEquals(ErrorCode.BUSINESS_INVALID_STATE, ex.getErrorCode());
        assertTrue(ex.getMessage().contains("approved maker-checker override request"));
    }

    @Test
    void confirmDispatchReplayFastPathEnrichesAuditMetadataWithOverrideFields() {
        SalesOrder order = new SalesOrder();
        setField(order, "id", 10L);
        order.setCompany(company);
        order.setStatus("SHIPPED");

        PackagingSlip slip = new PackagingSlip();
        setField(slip, "id", 55L);
        slip.setCompany(company);
        slip.setSalesOrder(order);
        slip.setSlipNumber("PS-55");
        slip.setStatus("DISPATCHED");
        slip.setInvoiceId(777L);
        slip.setJournalEntryId(222L);
        slip.setCogsJournalEntryId(333L);

        Invoice existingInvoice = new Invoice();
        setField(existingInvoice, "id", 777L);
        existingInvoice.setTotalAmount(new BigDecimal("90.00"));

        when(packagingSlipRepository.findAndLockByIdAndCompany(55L, company)).thenReturn(Optional.of(slip));
        when(packagingSlipRepository.findAllByCompanyAndSalesOrderId(company, 10L)).thenReturn(List.of(slip));
        when(companyEntityLookup.requireSalesOrder(company, 10L)).thenReturn(order);
        when(invoiceRepository.findByCompanyAndId(company, 777L)).thenReturn(Optional.of(existingInvoice));
        when(creditLimitOverrideService.isOverrideApproved(
                eq(805L),
                eq(company),
                isNull(),
                eq(slip),
                eq(order),
                isNull())).thenReturn(true);

        DispatchConfirmResponse response = salesService.confirmDispatch(new DispatchConfirmRequest(
                55L,
                null,
                List.of(new DispatchConfirmRequest.DispatchLine(99L, null, BigDecimal.ONE, null, new BigDecimal("10"), null, null, null)),
                null,
                "admin",
                Boolean.TRUE,
                "Replay override in fast path",
                805L));

        assertEquals(777L, response.finalInvoiceId());
        assertEquals(222L, response.arJournalEntryId());

        ArgumentCaptor<Map<String, String>> metadataCaptor = ArgumentCaptor.forClass(Map.class);
        verify(auditService).logSuccess(eq(AuditEvent.DISPATCH_CONFIRMED), metadataCaptor.capture());
        Map<String, String> metadata = metadataCaptor.getValue();
        assertEquals("Replay override in fast path", metadata.get("dispatchOverrideReason"));
        assertEquals("DISCOUNT_OVERRIDE", metadata.get("dispatchOverrideReasonCode"));
        assertEquals("805", metadata.get("overrideRequestId"));
    }

    private SalesOrder newOrderWithSingleItem(Dealer dealer, BigDecimal unitPrice) {
        SalesOrder order = new SalesOrder();
        setField(order, "id", 10L);
        order.setCompany(company);
        order.setDealer(dealer);
        order.setOrderNumber("SO-10");
        order.setStatus("READY_TO_SHIP");

        SalesOrderItem item = new SalesOrderItem();
        setField(item, "id", 1L);
        item.setSalesOrder(order);
        item.setProductCode("SKU-D");
        item.setDescription("Desc");
        item.setQuantity(BigDecimal.ONE);
        item.setUnitPrice(unitPrice);
        item.setGstRate(BigDecimal.ZERO);
        order.getItems().add(item);
        return order;
    }

    private PackagingSlip pendingSlip(SalesOrder order) {
        PackagingSlip slip = new PackagingSlip();
        setField(slip, "id", 55L);
        slip.setCompany(company);
        slip.setSalesOrder(order);
        slip.setSlipNumber("PS-55");
        slip.setStatus("PENDING");
        return slip;
    }

    private PackagingSlipLine defaultSlipLine(PackagingSlip slip) {
        FinishedGood finishedGood = new FinishedGood();
        finishedGood.setCompany(company);
        finishedGood.setProductCode("SKU-D");
        finishedGood.setName("Name");
        finishedGood.setCurrentStock(BigDecimal.ONE);
        finishedGood.setReservedStock(BigDecimal.ZERO);
        finishedGood.setRevenueAccountId(3L);
        finishedGood.setDiscountAccountId(4L);
        finishedGood.setValuationAccountId(11L);
        finishedGood.setCogsAccountId(12L);

        FinishedGoodBatch batch = new FinishedGoodBatch();
        batch.setFinishedGood(finishedGood);
        batch.setBatchCode("B-1");
        batch.setQuantityTotal(BigDecimal.ONE);
        batch.setQuantityAvailable(BigDecimal.ONE);
        batch.setUnitCost(BigDecimal.ZERO);

        PackagingSlipLine slipLine = new PackagingSlipLine();
        setField(slipLine, "id", 99L);
        slipLine.setPackagingSlip(slip);
        slipLine.setFinishedGoodBatch(batch);
        slipLine.setOrderedQuantity(BigDecimal.ONE);
        slipLine.setQuantity(BigDecimal.ONE);
        slipLine.setUnitCost(BigDecimal.ZERO);
        return slipLine;
    }

    private Dealer dealerWithCreditLimit(long id, BigDecimal limit) {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        dealer.setName("Dealer");
        dealer.setCreditLimit(limit);
        setField(dealer, "id", id);
        return dealer;
    }

    private void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    private static class NoopTransactionManager implements PlatformTransactionManager {
        @Override
        public TransactionStatus getTransaction(TransactionDefinition definition) {
            return new SimpleTransactionStatus();
        }

        @Override
        public void commit(TransactionStatus status) {
        }

        @Override
        public void rollback(TransactionStatus status) {
        }
    }
}
