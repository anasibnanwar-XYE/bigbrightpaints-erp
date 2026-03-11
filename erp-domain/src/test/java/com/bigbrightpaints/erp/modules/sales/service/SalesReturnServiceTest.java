package com.bigbrightpaints.erp.modules.sales.service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalCorrectionType;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.dto.SalesReturnRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.SalesReturnPreviewDto;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingFacade;
import com.bigbrightpaints.erp.modules.accounting.service.CompanyAccountingSettingsService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGood;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.FinishedGoodRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryMovement;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryMovementRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryReference;
import com.bigbrightpaints.erp.modules.inventory.service.BatchNumberService;
import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService;
import com.bigbrightpaints.erp.modules.invoice.domain.Invoice;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceLine;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceRepository;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.ArgumentMatchers.contains;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class SalesReturnServiceTest {

    @Mock
    private CompanyContextService companyContextService;
    @Mock
    private FinishedGoodRepository finishedGoodRepository;
    @Mock
    private FinishedGoodBatchRepository finishedGoodBatchRepository;
    @Mock
    private InventoryMovementRepository inventoryMovementRepository;
    @Mock
    private BatchNumberService batchNumberService;
    @Mock
    private AccountingFacade accountingFacade;
    @Mock
    private JournalEntryRepository journalEntryRepository;
    @Mock
    private CompanyAccountingSettingsService companyAccountingSettingsService;
    @Mock
    private FinishedGoodsService finishedGoodsService;
    @Mock
    private InvoiceRepository invoiceRepository;

    private SalesReturnService salesReturnService;
    private Company company;

    @BeforeEach
    void setup() {
        salesReturnService = new SalesReturnService(
                companyContextService,
                finishedGoodRepository,
                finishedGoodBatchRepository,
                inventoryMovementRepository,
                batchNumberService,
                accountingFacade,
                journalEntryRepository,
                invoiceRepository,
                companyAccountingSettingsService,
                finishedGoodsService
        );
        company = new Company();
        company.setTimezone("UTC");
        when(companyContextService.requireCurrentCompany()).thenReturn(company);
        lenient().when(companyAccountingSettingsService.requireTaxAccounts())
                .thenReturn(new CompanyAccountingSettingsService.TaxAccountConfiguration(900L, 800L, null));
        lenient().when(journalEntryRepository.findByCompanyAndId(any(), anyLong())).thenReturn(Optional.empty());
    }

    @Test
    void processReturn_postsSalesAndCogsJournals() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        dealer.setName("Retail Partner");
        Account receivable = new Account();
        setField(receivable, "id", 70L);
        dealer.setReceivableAccount(receivable);
        setField(dealer, "id", 7L);

        SalesOrder salesOrder = new SalesOrder();
        setField(salesOrder, "id", 99L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setSalesOrder(salesOrder);
        invoice.setInvoiceNumber("INV-1");
        attachPostedJournal(invoice, 901L);
        setField(invoice, "id", 10L);

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProductCode("FG-1");
        line.setQuantity(new BigDecimal("2"));
        line.setUnitPrice(new BigDecimal("100"));
        line.setDiscountAmount(new BigDecimal("20"));
        line.setTaxableAmount(new BigDecimal("180"));
        line.setTaxAmount(new BigDecimal("18"));
        line.setLineTotal(new BigDecimal("198")); // net + tax
        setField(line, "id", 55L);
        invoice.getLines().add(line);

        FinishedGood fg = new FinishedGood();
        fg.setCompany(company);
        fg.setProductCode("FG-1");
        fg.setValuationAccountId(500L);
        fg.setCogsAccountId(600L);
        fg.setRevenueAccountId(710L);
        fg.setDiscountAccountId(700L);
        fg.setTaxAccountId(800L);
        setField(fg, "id", 21L);

        when(invoiceRepository.lockByCompanyAndId(company, 10L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-1")).thenReturn(Optional.of(fg));
        when(finishedGoodRepository.lockByCompanyAndId(company, 21L)).thenReturn(Optional.of(fg));
        when(finishedGoodRepository.findByCompanyAndId(company, 21L)).thenReturn(Optional.of(fg));
        when(finishedGoodRepository.save(any(FinishedGood.class))).thenAnswer(inv -> inv.getArgument(0));
        when(finishedGoodBatchRepository.save(any(FinishedGoodBatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(batchNumberService.nextFinishedGoodBatchCode(any(), any())).thenReturn("RET-BATCH");
        when(inventoryMovementRepository.save(any(InventoryMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryMovement dispatchMovement = new InventoryMovement();
        dispatchMovement.setFinishedGood(fg);
        dispatchMovement.setReferenceType(InventoryReference.SALES_ORDER);
        dispatchMovement.setReferenceId("99");
        dispatchMovement.setMovementType("DISPATCH");
        dispatchMovement.setQuantity(new BigDecimal("1"));
        dispatchMovement.setUnitCost(new BigDecimal("50"));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq(InventoryReference.SALES_ORDER),
                eq("99"))
        ).thenReturn(List.of(dispatchMovement));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-1")
        )).thenReturn(List.of());
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdStartingWithOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-1:")
        )).thenReturn(List.of());

        JournalEntryDto salesReturnEntry = stubEntry(100L);
        when(accountingFacade.postSalesReturn(
                anyLong(),
                anyString(),
                anyMap(),
                any(BigDecimal.class),
                anyString())
        ).thenReturn(salesReturnEntry);
        when(accountingFacade.postInventoryAdjustment(
                anyString(),
                anyString(),
                anyLong(),
                anyMap(),
                anyBoolean(),
                anyBoolean(),
                anyString())
        ).thenReturn(stubEntry(101L));

        SalesReturnRequest request = new SalesReturnRequest(
                10L,
                "Damaged goods",
                List.of(new SalesReturnRequest.ReturnLine(55L, new BigDecimal("1")))
        );

        JournalEntryDto result = salesReturnService.processReturn(request);

        assertThat(result.id()).isEqualTo(100L);

        ArgumentCaptor<Map<Long, BigDecimal>> returnLinesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(accountingFacade).postSalesReturn(
                eq(dealer.getId()),
                eq("INV-1"),
                returnLinesCaptor.capture(),
                argThat(total -> total.compareTo(new BigDecimal("99")) == 0),
                eq("Damaged goods")
        );
        Map<Long, BigDecimal> capturedReturnLines = returnLinesCaptor.getValue();
        assertThat(capturedReturnLines.get(710L)).isEqualByComparingTo("100");
        assertThat(capturedReturnLines.get(700L)).isEqualByComparingTo("-10");
        assertThat(capturedReturnLines.get(800L)).isEqualByComparingTo("9");

        verify(accountingFacade).postInventoryAdjustment(
                eq("SALES_RETURN_COGS"),
                eq("CRN-INV-1-COGS-0"),
                eq(600L),
                argThat(lines -> lines.containsKey(500L)
                        && new BigDecimal("50").compareTo(lines.get(500L)) == 0),
                eq(true),
                eq(false),
                contains("COGS reversal")
        );
        verify(inventoryMovementRepository).save(argThat(movement -> movement.getJournalEntryId() != null
                && movement.getJournalEntryId().equals(100L)));
    }

    @Test
    void previewReturn_reportsLinkedImpactBeforePosting() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        Account receivable = new Account();
        setField(receivable, "id", 70L);
        dealer.setReceivableAccount(receivable);
        setField(dealer, "id", 7L);

        SalesOrder salesOrder = new SalesOrder();
        setField(salesOrder, "id", 199L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setSalesOrder(salesOrder);
        invoice.setInvoiceNumber("INV-PREVIEW-1");
        attachPostedJournal(invoice, 908L);
        setField(invoice, "id", 110L);

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProductCode("FG-PREVIEW");
        line.setQuantity(new BigDecimal("2"));
        line.setUnitPrice(new BigDecimal("100"));
        line.setTaxableAmount(new BigDecimal("200"));
        line.setTaxAmount(new BigDecimal("20"));
        line.setLineTotal(new BigDecimal("220"));
        setField(line, "id", 111L);
        invoice.getLines().add(line);

        FinishedGood fg = new FinishedGood();
        fg.setCompany(company);
        fg.setProductCode("FG-PREVIEW");
        setField(fg, "id", 211L);

        InventoryMovement dispatchMovement = new InventoryMovement();
        dispatchMovement.setFinishedGood(fg);
        dispatchMovement.setReferenceType(InventoryReference.SALES_ORDER);
        dispatchMovement.setReferenceId("199");
        dispatchMovement.setMovementType("DISPATCH");
        dispatchMovement.setQuantity(new BigDecimal("2"));
        dispatchMovement.setUnitCost(new BigDecimal("55"));

        when(invoiceRepository.lockByCompanyAndId(company, 110L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-PREVIEW")).thenReturn(Optional.of(fg));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq(InventoryReference.SALES_ORDER),
                eq("199")
        )).thenReturn(List.of(dispatchMovement));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-PREVIEW-1")
        )).thenReturn(List.of());
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdStartingWithOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-PREVIEW-1:")
        )).thenReturn(List.of());

        SalesReturnPreviewDto preview = salesReturnService.previewReturn(new SalesReturnRequest(
                110L,
                "Preview only",
                List.of(new SalesReturnRequest.ReturnLine(111L, BigDecimal.ONE))
        ));

        assertThat(preview.invoiceId()).isEqualTo(110L);
        assertThat(preview.totalReturnAmount()).isEqualByComparingTo("110.00");
        assertThat(preview.totalInventoryValue()).isEqualByComparingTo("55.00");
        assertThat(preview.lines()).singleElement().satisfies(linePreview -> {
            assertThat(linePreview.remainingQuantityAfterReturn()).isEqualByComparingTo("1.00");
            assertThat(linePreview.inventoryUnitCost()).isEqualByComparingTo("55.0000");
        });
    }

    @Test
    void processReturn_rejectsDraftInvoiceMutationPath() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        Account receivable = new Account();
        setField(receivable, "id", 70L);
        dealer.setReceivableAccount(receivable);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setInvoiceNumber("INV-DRAFT-1");
        invoice.setStatus("DRAFT");
        setField(invoice, "id", 120L);

        when(invoiceRepository.lockByCompanyAndId(company, 120L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> salesReturnService.processReturn(new SalesReturnRequest(
                120L,
                "Draft correction",
                List.of(new SalesReturnRequest.ReturnLine(1L, BigDecimal.ONE))
        )))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("Only posted invoices can be corrected through sales return");
    }

    @Test
    void previewReturn_rejectsInvoiceWithoutPostedJournal() {
        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setInvoiceNumber("INV-NO-JOURNAL");
        invoice.setStatus("POSTED");
        setField(invoice, "id", 121L);

        when(invoiceRepository.lockByCompanyAndId(company, 121L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> salesReturnService.previewReturn(new SalesReturnRequest(
                121L,
                "Preview guard",
                List.of(new SalesReturnRequest.ReturnLine(1L, BigDecimal.ONE))
        )))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("Only posted invoices can be corrected through sales return");
    }

    @Test
    void previewReturn_rejectsVoidedInvoiceMutationPath() {
        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setInvoiceNumber("INV-VOID-1");
        attachPostedJournal(invoice, 910L);
        invoice.setStatus("VOID");
        setField(invoice, "id", 122L);

        when(invoiceRepository.lockByCompanyAndId(company, 122L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> salesReturnService.previewReturn(new SalesReturnRequest(
                122L,
                "Preview void",
                List.of(new SalesReturnRequest.ReturnLine(1L, BigDecimal.ONE))
        )))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("Only posted invoices can be corrected through sales return");
    }

    @Test
    void processReturn_rejectsWhenPriorReturnsExceedInvoiceQuantity() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        dealer.setName("Retail Partner");
        Account receivable = new Account();
        setField(receivable, "id", 70L);
        dealer.setReceivableAccount(receivable);
        setField(dealer, "id", 7L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setInvoiceNumber("INV-1");
        attachPostedJournal(invoice, 902L);
        setField(invoice, "id", 10L);

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProductCode("FG-1");
        line.setQuantity(new BigDecimal("2"));
        line.setUnitPrice(new BigDecimal("100"));
        line.setLineTotal(new BigDecimal("200"));
        setField(line, "id", 55L);
        invoice.getLines().add(line);

        FinishedGood fg = new FinishedGood();
        fg.setCompany(company);
        fg.setProductCode("FG-1");
        fg.setValuationAccountId(500L);
        fg.setCogsAccountId(600L);
        fg.setRevenueAccountId(710L);
        fg.setDiscountAccountId(700L);
        fg.setTaxAccountId(800L);
        setField(fg, "id", 21L);

        InventoryMovement priorReturn = new InventoryMovement();
        priorReturn.setFinishedGood(fg);
        priorReturn.setQuantity(new BigDecimal("1.5"));
        priorReturn.setReferenceType("SALES_RETURN");
        priorReturn.setReferenceId("INV-1");

        when(invoiceRepository.lockByCompanyAndId(company, 10L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-1")).thenReturn(Optional.of(fg));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-1")
        )).thenReturn(List.of(priorReturn));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdStartingWithOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-1:")
        )).thenReturn(List.of());

        SalesReturnRequest request = new SalesReturnRequest(
                10L,
                "Damaged goods",
                List.of(new SalesReturnRequest.ReturnLine(55L, new BigDecimal("1")))
        );

        assertThatThrownBy(() -> salesReturnService.processReturn(request))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("remaining invoiced amount");
    }

    @Test
    void processReturn_rejectsDuplicateProductLineOverReturn() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        dealer.setName("Retail Partner");
        Account receivable = new Account();
        setField(receivable, "id", 70L);
        dealer.setReceivableAccount(receivable);
        setField(dealer, "id", 7L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setInvoiceNumber("INV-1");
        attachPostedJournal(invoice, 903L);
        setField(invoice, "id", 10L);

        InvoiceLine firstLine = new InvoiceLine();
        firstLine.setInvoice(invoice);
        firstLine.setProductCode("FG-1");
        firstLine.setQuantity(new BigDecimal("1"));
        setField(firstLine, "id", 55L);
        invoice.getLines().add(firstLine);

        InvoiceLine secondLine = new InvoiceLine();
        secondLine.setInvoice(invoice);
        secondLine.setProductCode("FG-1");
        secondLine.setQuantity(new BigDecimal("1"));
        setField(secondLine, "id", 56L);
        invoice.getLines().add(secondLine);

        FinishedGood fg = new FinishedGood();
        fg.setCompany(company);
        fg.setProductCode("FG-1");
        fg.setRevenueAccountId(710L);
        setField(fg, "id", 21L);

        InventoryMovement priorReturn = new InventoryMovement();
        priorReturn.setFinishedGood(fg);
        priorReturn.setQuantity(new BigDecimal("1"));
        priorReturn.setReferenceType("SALES_RETURN");
        priorReturn.setReferenceId("INV-1:55");

        when(invoiceRepository.lockByCompanyAndId(company, 10L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-1")).thenReturn(Optional.of(fg));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-1")
        )).thenReturn(List.of());
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdStartingWithOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-1:")
        )).thenReturn(List.of(priorReturn));

        SalesReturnRequest request = new SalesReturnRequest(
                10L,
                "Duplicate line return",
                List.of(new SalesReturnRequest.ReturnLine(55L, new BigDecimal("1")))
        );

        assertThatThrownBy(() -> salesReturnService.processReturn(request))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("remaining invoiced amount");
    }

    @Test
    void processReturn_rejectsLegacyReturnOverLine() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        dealer.setName("Retail Partner");
        Account receivable = new Account();
        setField(receivable, "id", 70L);
        dealer.setReceivableAccount(receivable);
        setField(dealer, "id", 7L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setInvoiceNumber("INV-1");
        attachPostedJournal(invoice, 904L);
        setField(invoice, "id", 10L);

        InvoiceLine firstLine = new InvoiceLine();
        firstLine.setInvoice(invoice);
        firstLine.setProductCode("FG-1");
        firstLine.setQuantity(new BigDecimal("1"));
        setField(firstLine, "id", 55L);
        invoice.getLines().add(firstLine);

        InvoiceLine secondLine = new InvoiceLine();
        secondLine.setInvoice(invoice);
        secondLine.setProductCode("FG-1");
        secondLine.setQuantity(new BigDecimal("1"));
        setField(secondLine, "id", 56L);
        invoice.getLines().add(secondLine);

        FinishedGood fg = new FinishedGood();
        fg.setCompany(company);
        fg.setProductCode("FG-1");
        fg.setRevenueAccountId(710L);
        setField(fg, "id", 21L);

        InventoryMovement priorReturn = new InventoryMovement();
        priorReturn.setFinishedGood(fg);
        priorReturn.setQuantity(new BigDecimal("1"));
        priorReturn.setReferenceType("SALES_RETURN");
        priorReturn.setReferenceId("INV-1");

        when(invoiceRepository.lockByCompanyAndId(company, 10L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-1")).thenReturn(Optional.of(fg));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-1")
        )).thenReturn(List.of(priorReturn));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdStartingWithOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-1:")
        )).thenReturn(List.of());

        SalesReturnRequest request = new SalesReturnRequest(
                10L,
                "Legacy return",
                List.of(new SalesReturnRequest.ReturnLine(55L, new BigDecimal("1")))
        );

        assertThatThrownBy(() -> salesReturnService.processReturn(request))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("remaining invoiced amount");
    }

    @Test
    void previewReturn_allocatesLegacyReturnToEarliestLineBeforeRemainingValidation() {
        SalesOrder salesOrder = new SalesOrder();
        setField(salesOrder, "id", 501L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setSalesOrder(salesOrder);
        invoice.setInvoiceNumber("INV-LEGACY-ALLOC");
        attachPostedJournal(invoice, 911L);
        setField(invoice, "id", 123L);

        InvoiceLine firstLine = new InvoiceLine();
        firstLine.setInvoice(invoice);
        firstLine.setProductCode("FG-ALLOC");
        firstLine.setQuantity(BigDecimal.ONE);
        firstLine.setUnitPrice(new BigDecimal("100"));
        firstLine.setTaxableAmount(new BigDecimal("100"));
        firstLine.setTaxAmount(BigDecimal.ZERO);
        firstLine.setLineTotal(new BigDecimal("100"));
        setField(firstLine, "id", 201L);
        invoice.getLines().add(firstLine);

        InvoiceLine secondLine = new InvoiceLine();
        secondLine.setInvoice(invoice);
        secondLine.setProductCode("FG-ALLOC");
        secondLine.setQuantity(BigDecimal.ONE);
        secondLine.setUnitPrice(new BigDecimal("100"));
        secondLine.setTaxableAmount(new BigDecimal("100"));
        secondLine.setTaxAmount(BigDecimal.ZERO);
        secondLine.setLineTotal(new BigDecimal("100"));
        setField(secondLine, "id", 202L);
        invoice.getLines().add(secondLine);

        FinishedGood fg = new FinishedGood();
        fg.setCompany(company);
        fg.setProductCode("FG-ALLOC");
        setField(fg, "id", 301L);

        InventoryMovement dispatchMovement = new InventoryMovement();
        dispatchMovement.setFinishedGood(fg);
        dispatchMovement.setReferenceType(InventoryReference.SALES_ORDER);
        dispatchMovement.setReferenceId("501");
        dispatchMovement.setMovementType("DISPATCH");
        dispatchMovement.setQuantity(new BigDecimal("2"));
        dispatchMovement.setUnitCost(new BigDecimal("50"));

        InventoryMovement legacyReturn = new InventoryMovement();
        legacyReturn.setFinishedGood(fg);
        legacyReturn.setReferenceType("SALES_RETURN");
        legacyReturn.setReferenceId("INV-LEGACY-ALLOC");
        legacyReturn.setQuantity(BigDecimal.ONE);

        when(invoiceRepository.lockByCompanyAndId(company, 123L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-ALLOC")).thenReturn(Optional.of(fg));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq(InventoryReference.SALES_ORDER),
                eq("501")
        )).thenReturn(List.of(dispatchMovement));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-LEGACY-ALLOC")
        )).thenReturn(List.of(legacyReturn));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdStartingWithOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-LEGACY-ALLOC:")
        )).thenReturn(List.of());

        SalesReturnPreviewDto preview = salesReturnService.previewReturn(new SalesReturnRequest(
                123L,
                "Legacy allocation",
                List.of(new SalesReturnRequest.ReturnLine(202L, BigDecimal.ONE))
        ));

        assertThat(preview.totalReturnAmount()).isEqualByComparingTo("100.00");
        assertThat(preview.totalInventoryValue()).isEqualByComparingTo("50.00");
        assertThat(preview.lines()).singleElement().satisfies(linePreview -> {
            assertThat(linePreview.invoiceLineId()).isEqualTo(202L);
            assertThat(linePreview.remainingQuantityAfterReturn()).isEqualByComparingTo("0.00");
            assertThat(linePreview.inventoryUnitCost()).isEqualByComparingTo("50.0000");
        });
    }

    @Test
    void previewReturn_allocatesLegacyBalanceAfterSkippingFullyReturnedAndUnrequestedLines() {
        SalesOrder salesOrder = new SalesOrder();
        setField(salesOrder, "id", 502L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setSalesOrder(salesOrder);
        invoice.setInvoiceNumber("INV-LEGACY-MIX");
        attachPostedJournal(invoice, 912L);
        setField(invoice, "id", 124L);

        InvoiceLine firstLine = new InvoiceLine();
        firstLine.setInvoice(invoice);
        firstLine.setProductCode("FG-ALLOC");
        firstLine.setQuantity(BigDecimal.ONE);
        firstLine.setUnitPrice(new BigDecimal("100"));
        firstLine.setTaxableAmount(new BigDecimal("100"));
        firstLine.setTaxAmount(BigDecimal.ZERO);
        firstLine.setLineTotal(new BigDecimal("100"));
        setField(firstLine, "id", 211L);
        invoice.getLines().add(firstLine);

        InvoiceLine secondLine = new InvoiceLine();
        secondLine.setInvoice(invoice);
        secondLine.setProductCode("FG-ALLOC");
        secondLine.setQuantity(new BigDecimal("3"));
        secondLine.setUnitPrice(new BigDecimal("100"));
        secondLine.setTaxableAmount(new BigDecimal("300"));
        secondLine.setTaxAmount(BigDecimal.ZERO);
        secondLine.setLineTotal(new BigDecimal("300"));
        setField(secondLine, "id", 212L);
        invoice.getLines().add(secondLine);

        InvoiceLine unrelatedLine = new InvoiceLine();
        unrelatedLine.setInvoice(invoice);
        unrelatedLine.setProductCode("FG-OTHER");
        unrelatedLine.setQuantity(BigDecimal.ONE);
        unrelatedLine.setUnitPrice(new BigDecimal("25"));
        unrelatedLine.setTaxableAmount(new BigDecimal("25"));
        unrelatedLine.setTaxAmount(BigDecimal.ZERO);
        unrelatedLine.setLineTotal(new BigDecimal("25"));
        setField(unrelatedLine, "id", 213L);
        invoice.getLines().add(unrelatedLine);

        FinishedGood allocFg = new FinishedGood();
        allocFg.setCompany(company);
        allocFg.setProductCode("FG-ALLOC");
        setField(allocFg, "id", 311L);

        FinishedGood otherFg = new FinishedGood();
        otherFg.setCompany(company);
        otherFg.setProductCode("FG-OTHER");
        setField(otherFg, "id", 312L);

        InventoryMovement allocDispatch = new InventoryMovement();
        allocDispatch.setFinishedGood(allocFg);
        allocDispatch.setReferenceType(InventoryReference.SALES_ORDER);
        allocDispatch.setReferenceId("502");
        allocDispatch.setMovementType("DISPATCH");
        allocDispatch.setQuantity(new BigDecimal("4"));
        allocDispatch.setUnitCost(new BigDecimal("50"));

        InventoryMovement legacyAlloc = new InventoryMovement();
        legacyAlloc.setFinishedGood(allocFg);
        legacyAlloc.setReferenceType("SALES_RETURN");
        legacyAlloc.setReferenceId("INV-LEGACY-MIX");
        legacyAlloc.setQuantity(new BigDecimal("2"));

        InventoryMovement lineReturned = new InventoryMovement();
        lineReturned.setFinishedGood(allocFg);
        lineReturned.setReferenceType("SALES_RETURN");
        lineReturned.setReferenceId("INV-LEGACY-MIX:211");
        lineReturned.setQuantity(BigDecimal.ONE);

        InventoryMovement unrelatedProductLineReturned = new InventoryMovement();
        unrelatedProductLineReturned.setFinishedGood(otherFg);
        unrelatedProductLineReturned.setReferenceType("SALES_RETURN");
        unrelatedProductLineReturned.setReferenceId("INV-LEGACY-MIX:213");
        unrelatedProductLineReturned.setQuantity(BigDecimal.ONE);

        InventoryMovement unknownLineReturned = new InventoryMovement();
        unknownLineReturned.setFinishedGood(otherFg);
        unknownLineReturned.setReferenceType("SALES_RETURN");
        unknownLineReturned.setReferenceId("INV-LEGACY-MIX:999");
        unknownLineReturned.setQuantity(BigDecimal.ONE);

        when(invoiceRepository.lockByCompanyAndId(company, 124L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-ALLOC")).thenReturn(Optional.of(allocFg));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq(InventoryReference.SALES_ORDER),
                eq("502")
        )).thenReturn(List.of(allocDispatch));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-LEGACY-MIX")
        )).thenReturn(List.of(legacyAlloc));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdStartingWithOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-LEGACY-MIX:")
        )).thenReturn(List.of(lineReturned, unrelatedProductLineReturned, unknownLineReturned));

        SalesReturnPreviewDto preview = salesReturnService.previewReturn(new SalesReturnRequest(
                124L,
                "Legacy allocation mix",
                List.of(new SalesReturnRequest.ReturnLine(212L, BigDecimal.ONE))
        ));

        assertThat(preview.totalReturnAmount()).isEqualByComparingTo("100.00");
        assertThat(preview.totalInventoryValue()).isEqualByComparingTo("50.00");
        assertThat(preview.lines()).singleElement().satisfies(linePreview -> {
            assertThat(linePreview.invoiceLineId()).isEqualTo(212L);
            assertThat(linePreview.remainingQuantityAfterReturn()).isEqualByComparingTo("2.00");
            assertThat(linePreview.inventoryUnitCost()).isEqualByComparingTo("50.0000");
        });
    }

    @Test
    void processReturn_ignoresUnrelatedInvoicePrefixMovements() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        dealer.setName("Retail Partner");
        Account receivable = new Account();
        setField(receivable, "id", 70L);
        dealer.setReceivableAccount(receivable);
        setField(dealer, "id", 7L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setInvoiceNumber("INV-1");
        attachPostedJournal(invoice, 905L);
        setField(invoice, "id", 10L);

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProductCode("FG-1");
        line.setQuantity(new BigDecimal("1"));
        line.setUnitPrice(new BigDecimal("100"));
        line.setTaxableAmount(new BigDecimal("100"));
        line.setLineTotal(new BigDecimal("100"));
        setField(line, "id", 55L);
        invoice.getLines().add(line);

        FinishedGood fg = new FinishedGood();
        fg.setCompany(company);
        fg.setProductCode("FG-1");
        fg.setRevenueAccountId(710L);
        setField(fg, "id", 21L);

        InventoryMovement unrelatedReturn = new InventoryMovement();
        unrelatedReturn.setFinishedGood(fg);
        unrelatedReturn.setQuantity(new BigDecimal("1"));
        unrelatedReturn.setReferenceType("SALES_RETURN");
        unrelatedReturn.setReferenceId("INV-10:999");

        when(invoiceRepository.lockByCompanyAndId(company, 10L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-1")).thenReturn(Optional.of(fg));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-1")
        )).thenReturn(List.of());
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdStartingWithOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-1:")
        )).thenReturn(List.of(unrelatedReturn));

        SalesReturnRequest request = new SalesReturnRequest(
                10L,
                "Prefix safety",
                List.of(new SalesReturnRequest.ReturnLine(55L, new BigDecimal("1")))
        );

        assertThatThrownBy(() -> salesReturnService.processReturn(request))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("dispatch cost layers");
    }

    @Test
    void processReturn_replaySkipsRestockAndReusesAccountingReplay() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        dealer.setName("Replay Partner");
        Account receivable = new Account();
        setField(receivable, "id", 72L);
        dealer.setReceivableAccount(receivable);
        setField(dealer, "id", 9L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setInvoiceNumber("INV-REPLAY-1");
        attachPostedJournal(invoice, 906L);
        setField(invoice, "id", 30L);

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProductCode("FG-REPLAY");
        line.setQuantity(new BigDecimal("1"));
        line.setUnitPrice(new BigDecimal("100"));
        line.setTaxableAmount(new BigDecimal("100"));
        line.setTaxAmount(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal("100"));
        setField(line, "id", 77L);
        invoice.getLines().add(line);

        FinishedGood fg = new FinishedGood();
        fg.setCompany(company);
        fg.setProductCode("FG-REPLAY");
        fg.setRevenueAccountId(712L);
        setField(fg, "id", 23L);

        when(invoiceRepository.lockByCompanyAndId(company, 30L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-REPLAY")).thenReturn(Optional.of(fg));
        when(inventoryMovementRepository.existsByFinishedGood_CompanyAndReferenceTypeAndReferenceIdContainingIgnoreCase(
                eq(company),
                eq("SALES_RETURN"),
                anyString()
        )).thenReturn(true);
        when(accountingFacade.postSalesReturn(
                eq(dealer.getId()),
                eq("INV-REPLAY-1"),
                anyMap(),
                argThat(total -> total.compareTo(new BigDecimal("100")) == 0),
                eq("Replay return")
        )).thenReturn(stubEntry(120L));

        SalesReturnRequest request = new SalesReturnRequest(
                30L,
                "Replay return",
                List.of(new SalesReturnRequest.ReturnLine(77L, new BigDecimal("1")))
        );

        JournalEntryDto result = salesReturnService.processReturn(request);

        assertThat(result.id()).isEqualTo(120L);
        verify(accountingFacade).postSalesReturn(
                eq(dealer.getId()),
                eq("INV-REPLAY-1"),
                anyMap(),
                argThat(total -> total.compareTo(new BigDecimal("100")) == 0),
                eq("Replay return")
        );
        verify(finishedGoodRepository, never()).save(any(FinishedGood.class));
        verify(finishedGoodBatchRepository, never()).save(any(FinishedGoodBatch.class));
        verify(inventoryMovementRepository, never()).save(any(InventoryMovement.class));
        verify(accountingFacade, never()).postInventoryAdjustment(anyString(), anyString(), anyLong(), anyMap(), anyBoolean(), anyBoolean(), anyString());
    }

    @Test
    void processReturn_replayWithNullAccountingReplayIdSkipsMetadataAndRelink() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        dealer.setName("Replay Partner");
        Account receivable = new Account();
        setField(receivable, "id", 84L);
        dealer.setReceivableAccount(receivable);
        setField(dealer, "id", 14L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setInvoiceNumber("INV-REPLAY-NULL");
        JournalEntry sourceJournal = new JournalEntry();
        setField(sourceJournal, "id", 9063L);
        sourceJournal.setStatus("POSTED");
        invoice.setJournalEntry(sourceJournal);
        invoice.setStatus("POSTED");
        setField(invoice, "id", 132L);

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProductCode("FG-REPLAY-NULL");
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(new BigDecimal("100"));
        line.setTaxableAmount(new BigDecimal("100"));
        line.setTaxAmount(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal("100"));
        setField(line, "id", 179L);
        invoice.getLines().add(line);

        FinishedGood fg = new FinishedGood();
        fg.setCompany(company);
        fg.setProductCode("FG-REPLAY-NULL");
        fg.setRevenueAccountId(715L);
        setField(fg, "id", 225L);

        when(invoiceRepository.lockByCompanyAndId(company, 132L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-REPLAY-NULL")).thenReturn(Optional.of(fg));
        when(inventoryMovementRepository.existsByFinishedGood_CompanyAndReferenceTypeAndReferenceIdContainingIgnoreCase(
                eq(company),
                eq("SALES_RETURN"),
                anyString()
        )).thenReturn(true);
        when(accountingFacade.postSalesReturn(
                eq(dealer.getId()),
                eq("INV-REPLAY-NULL"),
                anyMap(),
                argThat(total -> total.compareTo(new BigDecimal("100")) == 0),
                eq("Replay null")
        )).thenReturn(journalEntryDto(null, null, null, null));

        JournalEntryDto result = salesReturnService.processReturn(new SalesReturnRequest(
                132L,
                "Replay null",
                List.of(new SalesReturnRequest.ReturnLine(179L, BigDecimal.ONE))
        ));

        assertThat(result.id()).isNull();
        verify(journalEntryRepository, never()).findByCompanyAndId(any(), anyLong());
        verify(inventoryMovementRepository, never()).findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdStartingWithOrderByCreatedAtAsc(
                any(),
                anyString(),
                anyString()
        );
    }

    @Test
    void processReturn_replayWithAlignedMetadataSkipsPersistence() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        dealer.setName("Replay Partner");
        Account receivable = new Account();
        setField(receivable, "id", 83L);
        dealer.setReceivableAccount(receivable);
        setField(dealer, "id", 13L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setInvoiceNumber("INV-REPLAY-CLEAN");
        JournalEntry sourceJournal = new JournalEntry();
        setField(sourceJournal, "id", 9062L);
        sourceJournal.setStatus("POSTED");
        invoice.setJournalEntry(sourceJournal);
        invoice.setStatus("POSTED");
        setField(invoice, "id", 131L);

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProductCode("FG-REPLAY-CLEAN");
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(new BigDecimal("100"));
        line.setTaxableAmount(new BigDecimal("100"));
        line.setTaxAmount(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal("100"));
        setField(line, "id", 178L);
        invoice.getLines().add(line);

        FinishedGood fg = new FinishedGood();
        fg.setCompany(company);
        fg.setProductCode("FG-REPLAY-CLEAN");
        fg.setRevenueAccountId(714L);
        setField(fg, "id", 224L);

        JournalEntry replayJournal = new JournalEntry();
        setField(replayJournal, "id", 1202L);
        replayJournal.setStatus("POSTED");
        replayJournal.setCorrectionType(JournalCorrectionType.REVERSAL);
        replayJournal.setCorrectionReason("SALES_RETURN");
        replayJournal.setSourceModule("SALES_RETURN");
        replayJournal.setSourceReference("INV-REPLAY-CLEAN");

        SalesReturnRequest request = new SalesReturnRequest(
                131L,
                "Replay clean",
                List.of(new SalesReturnRequest.ReturnLine(178L, BigDecimal.ONE))
        );
        String returnKey = invokeBuildReturnIdempotencyKey(invoice, request);

        InventoryMovement matchingMovement = new InventoryMovement();
        matchingMovement.setReferenceType("SALES_RETURN");
        matchingMovement.setReferenceId("INV-REPLAY-CLEAN:178:RET-" + returnKey);
        matchingMovement.setJournalEntryId(1202L);

        when(invoiceRepository.lockByCompanyAndId(company, 131L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-REPLAY-CLEAN")).thenReturn(Optional.of(fg));
        when(inventoryMovementRepository.existsByFinishedGood_CompanyAndReferenceTypeAndReferenceIdContainingIgnoreCase(
                eq(company),
                eq("SALES_RETURN"),
                anyString()
        )).thenReturn(true);
        when(accountingFacade.postSalesReturn(
                eq(dealer.getId()),
                eq("INV-REPLAY-CLEAN"),
                anyMap(),
                argThat(total -> total.compareTo(new BigDecimal("100")) == 0),
                eq("Replay clean")
        )).thenReturn(stubEntry(1202L));
        when(journalEntryRepository.findByCompanyAndId(company, 1202L)).thenReturn(Optional.of(replayJournal));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdStartingWithOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-REPLAY-CLEAN:")
        )).thenReturn(List.of(matchingMovement));

        JournalEntryDto result = salesReturnService.processReturn(request);

        assertThat(result.id()).isEqualTo(1202L);
        verify(journalEntryRepository, never()).save(replayJournal);
        verify(inventoryMovementRepository, never()).saveAll(any());
        verify(finishedGoodRepository, never()).save(any(FinishedGood.class));
        verify(finishedGoodBatchRepository, never()).save(any(FinishedGoodBatch.class));
        verify(accountingFacade, never()).postInventoryAdjustment(anyString(), anyString(), anyLong(), anyMap(), anyBoolean(), anyBoolean(), anyString());
    }

    @Test
    void processReturn_replayRelinksMatchingReturnMovementsAndCorrectionMetadata() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        dealer.setName("Replay Partner");
        Account receivable = new Account();
        setField(receivable, "id", 82L);
        dealer.setReceivableAccount(receivable);
        setField(dealer, "id", 12L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setInvoiceNumber("INV-REPLAY-LINK");
        JournalEntry sourceJournal = new JournalEntry();
        setField(sourceJournal, "id", 9061L);
        sourceJournal.setStatus("POSTED");
        invoice.setJournalEntry(sourceJournal);
        invoice.setStatus("POSTED");
        setField(invoice, "id", 130L);

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProductCode("FG-RELINK");
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(new BigDecimal("100"));
        line.setTaxableAmount(new BigDecimal("100"));
        line.setTaxAmount(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal("100"));
        setField(line, "id", 177L);
        invoice.getLines().add(line);

        FinishedGood fg = new FinishedGood();
        fg.setCompany(company);
        fg.setProductCode("FG-RELINK");
        fg.setRevenueAccountId(713L);
        setField(fg, "id", 223L);

        JournalEntry replayJournal = new JournalEntry();
        setField(replayJournal, "id", 1201L);
        replayJournal.setStatus("POSTED");

        SalesReturnRequest request = new SalesReturnRequest(
                130L,
                "Replay relink",
                List.of(new SalesReturnRequest.ReturnLine(177L, BigDecimal.ONE))
        );
        String returnKey = invokeBuildReturnIdempotencyKey(invoice, request);

        InventoryMovement matchingMovement = new InventoryMovement();
        matchingMovement.setReferenceType("SALES_RETURN");
        matchingMovement.setReferenceId("INV-REPLAY-LINK:177:RET-" + returnKey);
        matchingMovement.setJournalEntryId(null);

        InventoryMovement unrelatedMovement = new InventoryMovement();
        unrelatedMovement.setReferenceType("SALES_RETURN");
        unrelatedMovement.setReferenceId("INV-REPLAY-LINK:999:RET-" + returnKey);
        unrelatedMovement.setJournalEntryId(44L);

        when(invoiceRepository.lockByCompanyAndId(company, 130L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-RELINK")).thenReturn(Optional.of(fg));
        when(inventoryMovementRepository.existsByFinishedGood_CompanyAndReferenceTypeAndReferenceIdContainingIgnoreCase(
                eq(company),
                eq("SALES_RETURN"),
                anyString()
        )).thenReturn(true);
        when(accountingFacade.postSalesReturn(
                eq(dealer.getId()),
                eq("INV-REPLAY-LINK"),
                anyMap(),
                argThat(total -> total.compareTo(new BigDecimal("100")) == 0),
                eq("Replay relink")
        )).thenReturn(stubEntry(1201L));
        when(journalEntryRepository.findByCompanyAndId(company, 1201L)).thenReturn(Optional.of(replayJournal));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdStartingWithOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-REPLAY-LINK:")
        )).thenReturn(List.of(matchingMovement, unrelatedMovement));

        JournalEntryDto result = salesReturnService.processReturn(request);

        assertThat(result.id()).isEqualTo(1201L);
        assertThat(replayJournal.getCorrectionType()).isEqualTo(JournalCorrectionType.REVERSAL);
        assertThat(replayJournal.getCorrectionReason()).isEqualTo("SALES_RETURN");
        assertThat(replayJournal.getSourceModule()).isEqualTo("SALES_RETURN");
        assertThat(replayJournal.getSourceReference()).isEqualTo("INV-REPLAY-LINK");
        verify(journalEntryRepository).save(replayJournal);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<InventoryMovement>> movementCaptor = ArgumentCaptor.forClass(List.class);
        verify(inventoryMovementRepository).saveAll(movementCaptor.capture());
        List<InventoryMovement> savedMovements = movementCaptor.getValue();
        assertThat(savedMovements).containsExactly(matchingMovement, unrelatedMovement);
        assertThat(matchingMovement.getJournalEntryId()).isEqualTo(1201L);
        assertThat(unrelatedMovement.getJournalEntryId()).isEqualTo(44L);
        verify(finishedGoodRepository, never()).save(any(FinishedGood.class));
    }

    void previewReturn_rejectsMissingLines() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        Account receivable = new Account();
        setField(receivable, "id", 84L);
        dealer.setReceivableAccount(receivable);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setInvoiceNumber("INV-NO-LINES");
        attachPostedJournal(invoice, 910L);
        setField(invoice, "id", 141L);

        when(invoiceRepository.lockByCompanyAndId(company, 141L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> salesReturnService.previewReturn(new SalesReturnRequest(
                141L,
                "No lines",
                List.of()
        )))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("Return lines are required");
    }

    @Test
    void previewReturn_rejectsUnknownInvoiceLine() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        Account receivable = new Account();
        setField(receivable, "id", 85L);
        dealer.setReceivableAccount(receivable);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setInvoiceNumber("INV-MISSING-LINE");
        attachPostedJournal(invoice, 911L);
        setField(invoice, "id", 142L);

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProductCode("FG-MISSING");
        line.setQuantity(BigDecimal.ONE);
        setField(line, "id", 212L);
        invoice.getLines().add(line);

        when(invoiceRepository.lockByCompanyAndId(company, 142L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> salesReturnService.previewReturn(new SalesReturnRequest(
                142L,
                "Unknown line",
                List.of(new SalesReturnRequest.ReturnLine(999L, BigDecimal.ONE))
        )))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("Invoice line not found: 999");
    }

    @Test
    void previewReturn_capsRemainingQuantityAtZeroWhenRequestConsumesBalance() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        Account receivable = new Account();
        setField(receivable, "id", 84L);
        dealer.setReceivableAccount(receivable);

        SalesOrder salesOrder = new SalesOrder();
        setField(salesOrder, "id", 299L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setSalesOrder(salesOrder);
        invoice.setInvoiceNumber("INV-PREVIEW-2");
        attachPostedJournal(invoice, 909L);
        setField(invoice, "id", 150L);

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProductCode("FG-PREVIEW-2");
        line.setQuantity(new BigDecimal("2"));
        line.setUnitPrice(new BigDecimal("50"));
        line.setTaxableAmount(new BigDecimal("100"));
        line.setTaxAmount(new BigDecimal("10"));
        line.setLineTotal(new BigDecimal("110"));
        setField(line, "id", 211L);
        invoice.getLines().add(line);

        FinishedGood fg = new FinishedGood();
        fg.setCompany(company);
        fg.setProductCode("FG-PREVIEW-2");
        setField(fg, "id", 311L);

        InventoryMovement dispatchMovement = new InventoryMovement();
        dispatchMovement.setFinishedGood(fg);
        dispatchMovement.setReferenceType(InventoryReference.SALES_ORDER);
        dispatchMovement.setReferenceId("299");
        dispatchMovement.setMovementType("DISPATCH");
        dispatchMovement.setQuantity(new BigDecimal("2"));
        dispatchMovement.setUnitCost(new BigDecimal("20"));

        InventoryMovement priorReturn = new InventoryMovement();
        priorReturn.setFinishedGood(fg);
        priorReturn.setReferenceType("SALES_RETURN");
        priorReturn.setReferenceId("INV-PREVIEW-2:211");
        priorReturn.setQuantity(new BigDecimal("1.5"));

        when(invoiceRepository.lockByCompanyAndId(company, 150L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-PREVIEW-2")).thenReturn(Optional.of(fg));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq(InventoryReference.SALES_ORDER),
                eq("299")
        )).thenReturn(List.of(dispatchMovement));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-PREVIEW-2")
        )).thenReturn(List.of());
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdStartingWithOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-PREVIEW-2:")
        )).thenReturn(List.of(priorReturn));

        SalesReturnPreviewDto preview = salesReturnService.previewReturn(new SalesReturnRequest(
                150L,
                "Preview exact remainder",
                List.of(new SalesReturnRequest.ReturnLine(211L, new BigDecimal("0.5")))
        ));

        assertThat(preview.totalReturnAmount()).isEqualByComparingTo("27.50");
        assertThat(preview.totalInventoryValue()).isEqualByComparingTo("10.00");
        assertThat(preview.lines()).singleElement().satisfies(linePreview -> {
            assertThat(linePreview.alreadyReturnedQuantity()).isEqualByComparingTo("1.50");
            assertThat(linePreview.remainingQuantityAfterReturn()).isEqualByComparingTo("0.00");
            assertThat(linePreview.lineAmount()).isEqualByComparingTo("27.50");
            assertThat(linePreview.taxAmount()).isEqualByComparingTo("2.50");
            assertThat(linePreview.inventoryUnitCost()).isEqualByComparingTo("20.0000");
            assertThat(linePreview.inventoryValue()).isEqualByComparingTo("10.00");
        });
    }

    @Test
    void processReturn_rejectsInvoiceWithoutDealerReceivableContext() {
        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setInvoiceNumber("INV-NO-DEALER");
        attachPostedJournal(invoice, 912L);
        setField(invoice, "id", 160L);

        when(invoiceRepository.lockByCompanyAndId(company, 160L)).thenReturn(Optional.of(invoice));

        assertThatThrownBy(() -> salesReturnService.processReturn(new SalesReturnRequest(
                160L,
                "Dealer context missing",
                List.of(new SalesReturnRequest.ReturnLine(1L, BigDecimal.ONE))
        )))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("missing dealer receivable context");
    }

    @Test
    void processReturn_rejectsZeroValuedReturnAmount() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        Account receivable = new Account();
        setField(receivable, "id", 86L);
        dealer.setReceivableAccount(receivable);
        setField(dealer, "id", 10L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setInvoiceNumber("INV-ZERO");
        attachPostedJournal(invoice, 913L);
        setField(invoice, "id", 161L);

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProductCode("FG-ZERO");
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(BigDecimal.ZERO);
        line.setTaxableAmount(BigDecimal.ZERO);
        line.setTaxAmount(BigDecimal.ZERO);
        line.setLineTotal(BigDecimal.ZERO);
        setField(line, "id", 213L);
        invoice.getLines().add(line);

        FinishedGood fg = new FinishedGood();
        fg.setCompany(company);
        fg.setProductCode("FG-ZERO");
        fg.setRevenueAccountId(714L);
        setField(fg, "id", 214L);

        when(invoiceRepository.lockByCompanyAndId(company, 161L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-ZERO")).thenReturn(Optional.of(fg));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-ZERO")
        )).thenReturn(List.of());
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdStartingWithOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-ZERO:")
        )).thenReturn(List.of());

        assertThatThrownBy(() -> salesReturnService.processReturn(new SalesReturnRequest(
                161L,
                "Zero value",
                List.of(new SalesReturnRequest.ReturnLine(213L, BigDecimal.ONE))
        )))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("Return amount must be greater than zero");
    }

    @Test
    void processReturn_rejectsMissingDiscountAccountWhenReturnIncludesDiscount() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        Account receivable = new Account();
        setField(receivable, "id", 87L);
        dealer.setReceivableAccount(receivable);
        setField(dealer, "id", 11L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setInvoiceNumber("INV-DISCOUNT");
        attachPostedJournal(invoice, 914L);
        setField(invoice, "id", 162L);

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProductCode("FG-DISCOUNT");
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(new BigDecimal("100"));
        line.setDiscountAmount(new BigDecimal("10"));
        line.setTaxableAmount(new BigDecimal("90"));
        line.setTaxAmount(BigDecimal.ZERO);
        line.setLineTotal(new BigDecimal("90"));
        setField(line, "id", 214L);
        invoice.getLines().add(line);

        FinishedGood fg = new FinishedGood();
        fg.setCompany(company);
        fg.setProductCode("FG-DISCOUNT");
        fg.setRevenueAccountId(715L);
        fg.setDiscountAccountId(null);
        setField(fg, "id", 215L);

        when(invoiceRepository.lockByCompanyAndId(company, 162L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-DISCOUNT")).thenReturn(Optional.of(fg));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-DISCOUNT")
        )).thenReturn(List.of());
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdStartingWithOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-DISCOUNT:")
        )).thenReturn(List.of());

        assertThatThrownBy(() -> salesReturnService.processReturn(new SalesReturnRequest(
                162L,
                "Discount correction",
                List.of(new SalesReturnRequest.ReturnLine(214L, BigDecimal.ONE))
        )))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("Discount account is required");
    }

    @Test
    void processReturn_rejectsTaxAccountMismatchAgainstConfiguredOutputTax() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        Account receivable = new Account();
        setField(receivable, "id", 88L);
        dealer.setReceivableAccount(receivable);
        setField(dealer, "id", 12L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setInvoiceNumber("INV-TAX-MISMATCH");
        attachPostedJournal(invoice, 915L);
        setField(invoice, "id", 163L);

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProductCode("FG-TAX");
        line.setQuantity(BigDecimal.ONE);
        line.setUnitPrice(new BigDecimal("100"));
        line.setTaxableAmount(new BigDecimal("100"));
        line.setTaxAmount(new BigDecimal("18"));
        line.setLineTotal(new BigDecimal("118"));
        setField(line, "id", 215L);
        invoice.getLines().add(line);

        FinishedGood fg = new FinishedGood();
        fg.setCompany(company);
        fg.setProductCode("FG-TAX");
        fg.setRevenueAccountId(716L);
        fg.setTaxAccountId(999L);
        setField(fg, "id", 216L);

        when(invoiceRepository.lockByCompanyAndId(company, 163L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-TAX")).thenReturn(Optional.of(fg));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-TAX-MISMATCH")
        )).thenReturn(List.of());
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdStartingWithOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-TAX-MISMATCH:")
        )).thenReturn(List.of());

        assertThatThrownBy(() -> salesReturnService.processReturn(new SalesReturnRequest(
                163L,
                "Tax mismatch",
                List.of(new SalesReturnRequest.ReturnLine(215L, BigDecimal.ONE))
        )))
                .isInstanceOf(ApplicationException.class)
                .hasMessageContaining("tax account must match GST output account");
    }

    @Test
    void processReturn_normalizesGstInclusiveDiscounts() {
        Dealer dealer = new Dealer();
        dealer.setCompany(company);
        dealer.setName("Retail Partner");
        Account receivable = new Account();
        setField(receivable, "id", 71L);
        dealer.setReceivableAccount(receivable);
        setField(dealer, "id", 8L);

        SalesOrder salesOrder = new SalesOrder();
        salesOrder.setGstInclusive(true);
        setField(salesOrder, "id", 101L);

        Invoice invoice = new Invoice();
        invoice.setCompany(company);
        invoice.setDealer(dealer);
        invoice.setSalesOrder(salesOrder);
        invoice.setInvoiceNumber("INV-2");
        attachPostedJournal(invoice, 907L);
        setField(invoice, "id", 20L);

        InvoiceLine line = new InvoiceLine();
        line.setInvoice(invoice);
        line.setProductCode("FG-2");
        line.setQuantity(new BigDecimal("1"));
        line.setUnitPrice(new BigDecimal("110"));
        line.setTaxRate(new BigDecimal("10"));
        line.setDiscountAmount(new BigDecimal("11"));
        line.setTaxableAmount(new BigDecimal("90"));
        line.setTaxAmount(new BigDecimal("9"));
        line.setLineTotal(new BigDecimal("99"));
        setField(line, "id", 66L);
        invoice.getLines().add(line);

        FinishedGood fg = new FinishedGood();
        fg.setCompany(company);
        fg.setProductCode("FG-2");
        fg.setValuationAccountId(501L);
        fg.setCogsAccountId(601L);
        fg.setRevenueAccountId(711L);
        fg.setDiscountAccountId(701L);
        fg.setTaxAccountId(800L);
        setField(fg, "id", 22L);

        when(invoiceRepository.lockByCompanyAndId(company, 20L)).thenReturn(Optional.of(invoice));
        when(finishedGoodRepository.lockByCompanyAndProductCode(company, "FG-2")).thenReturn(Optional.of(fg));
        when(finishedGoodRepository.lockByCompanyAndId(company, 22L)).thenReturn(Optional.of(fg));
        when(finishedGoodRepository.findByCompanyAndId(company, 22L)).thenReturn(Optional.of(fg));
        when(finishedGoodRepository.save(any(FinishedGood.class))).thenAnswer(inv -> inv.getArgument(0));
        when(finishedGoodBatchRepository.save(any(FinishedGoodBatch.class))).thenAnswer(inv -> inv.getArgument(0));
        when(batchNumberService.nextFinishedGoodBatchCode(any(), any())).thenReturn("RET-BATCH-2");
        when(inventoryMovementRepository.save(any(InventoryMovement.class))).thenAnswer(inv -> inv.getArgument(0));

        InventoryMovement dispatchMovement = new InventoryMovement();
        dispatchMovement.setFinishedGood(fg);
        dispatchMovement.setReferenceType(InventoryReference.SALES_ORDER);
        dispatchMovement.setReferenceId("101");
        dispatchMovement.setMovementType("DISPATCH");
        dispatchMovement.setQuantity(new BigDecimal("1"));
        dispatchMovement.setUnitCost(new BigDecimal("50"));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq(InventoryReference.SALES_ORDER),
                eq("101"))
        ).thenReturn(List.of(dispatchMovement));
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-2")
        )).thenReturn(List.of());
        when(inventoryMovementRepository.findByFinishedGood_CompanyAndReferenceTypeAndReferenceIdStartingWithOrderByCreatedAtAsc(
                eq(company),
                eq("SALES_RETURN"),
                eq("INV-2:")
        )).thenReturn(List.of());

        when(accountingFacade.postSalesReturn(
                anyLong(),
                anyString(),
                anyMap(),
                any(BigDecimal.class),
                anyString())
        ).thenReturn(stubEntry(110L));
        SalesReturnRequest request = new SalesReturnRequest(
                20L,
                "Inclusive return",
                List.of(new SalesReturnRequest.ReturnLine(66L, new BigDecimal("1")))
        );

        JournalEntryDto result = salesReturnService.processReturn(request);
        assertThat(result.id()).isEqualTo(110L);

        ArgumentCaptor<Map<Long, BigDecimal>> returnLinesCaptor = ArgumentCaptor.forClass(Map.class);
        verify(accountingFacade).postSalesReturn(
                eq(dealer.getId()),
                eq("INV-2"),
                returnLinesCaptor.capture(),
                argThat(total -> total.compareTo(new BigDecimal("99")) == 0),
                eq("Inclusive return")
        );
        Map<Long, BigDecimal> capturedReturnLines = returnLinesCaptor.getValue();
        assertThat(capturedReturnLines.get(711L)).isEqualByComparingTo("100");
        assertThat(capturedReturnLines.get(701L)).isEqualByComparingTo("-10");
        assertThat(capturedReturnLines.get(800L)).isEqualByComparingTo("9");
    }

    private JournalEntryDto stubEntry(long id) {
        return journalEntryDto(id, null, LocalDate.now(), null);
    }

    private JournalEntryDto journalEntryDto(Long id, String reference, LocalDate entryDate, String memo) {
        return new JournalEntryDto(
                id,
                null,
                reference,
                entryDate,
                memo,
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

    private void setField(Object target, String name, Object value) {
        try {
            Field field = target.getClass().getDeclaredField(name);
            field.setAccessible(true);
            field.set(target, value);
        } catch (NoSuchFieldException | IllegalAccessException ex) {
            throw new RuntimeException(ex);
        }
    }

    private String invokeBuildReturnIdempotencyKey(Invoice invoice, SalesReturnRequest request) {
        try {
            var method = SalesReturnService.class.getDeclaredMethod(
                    "buildReturnIdempotencyKey",
                    Invoice.class,
                    SalesReturnRequest.class
            );
            method.setAccessible(true);
            return (String) method.invoke(salesReturnService, invoice, request);
        } catch (ReflectiveOperationException ex) {
            throw new RuntimeException(ex);
        }
    }

    private void attachPostedJournal(Invoice invoice, long journalId) {
        JournalEntry journalEntry = new JournalEntry();
        setField(journalEntry, "id", journalId);
        journalEntry.setStatus("POSTED");
        invoice.setJournalEntry(journalEntry);
        invoice.setStatus("POSTED");
    }
}
