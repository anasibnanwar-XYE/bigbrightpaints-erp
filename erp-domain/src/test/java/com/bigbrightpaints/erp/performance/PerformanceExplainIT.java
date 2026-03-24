package com.bigbrightpaints.erp.performance;

import static org.junit.jupiter.api.Assertions.assertFalse;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;

import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.invoice.domain.Invoice;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceLine;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceRepository;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.DealerRepository;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderItem;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderRepository;
import com.bigbrightpaints.erp.test.AbstractIntegrationTest;

class PerformanceExplainIT extends AbstractIntegrationTest {

  private static final Logger log = LoggerFactory.getLogger(PerformanceExplainIT.class);

  @Autowired private JdbcTemplate jdbcTemplate;

  @Autowired private AccountRepository accountRepository;

  @Autowired private DealerRepository dealerRepository;

  @Autowired private InvoiceRepository invoiceRepository;

  @Autowired private SalesOrderRepository salesOrderRepository;

  @Test
  void explainPlansForHotQueries() {
    Company company = dataSeeder.ensureCompany("PERF", "Perf Co");
    Dealer dealer =
        dealerRepository
            .findByCompanyAndCodeIgnoreCase(company, "FIX-DEALER")
            .orElseThrow(() -> new IllegalStateException("Fixture dealer missing for PERF"));
    Account receivable =
        accountRepository
            .findByCompanyAndCodeIgnoreCase(company, "AR")
            .orElseThrow(() -> new IllegalStateException("Fixture account AR missing for PERF"));

    seedSalesOrder(company, dealer);
    seedInvoice(company, dealer);

    logPlan(
        "sales-orders",
        explain(
            "EXPLAIN SELECT * FROM sales_orders WHERE company_id = ? ORDER BY created_at DESC LIMIT"
                + " 25",
            company.getId()));
    logPlan(
        "sales-orders-status",
        explain(
            "EXPLAIN SELECT * FROM sales_orders WHERE company_id = ? AND status = ? ORDER BY"
                + " created_at DESC LIMIT 25",
            company.getId(),
            "BOOKED"));
    logPlan(
        "invoices-company",
        explain(
            "EXPLAIN SELECT * FROM invoices WHERE company_id = ? ORDER BY issue_date DESC LIMIT 25",
            company.getId()));
    logPlan(
        "invoices-dealer",
        explain(
            "EXPLAIN SELECT * FROM invoices WHERE company_id = ? AND dealer_id = ? ORDER BY"
                + " issue_date DESC LIMIT 25",
            company.getId(),
            dealer.getId()));
    logPlan(
        "journal-lines",
        explain(
            "EXPLAIN SELECT jl.* FROM journal_lines jl "
                + "JOIN journal_entries je ON jl.journal_entry_id = je.id "
                + "WHERE je.company_id = ? AND jl.account_id = ? "
                + "AND je.entry_date BETWEEN ? AND ? "
                + "ORDER BY je.entry_date ASC, je.reference_number ASC, jl.id ASC",
            company.getId(),
            receivable.getId(),
            LocalDate.now().minusDays(30),
            LocalDate.now()));
    logPlan(
        "outbox-pending",
        explain(
            "EXPLAIN SELECT * FROM orchestrator_outbox "
                + "WHERE status = 'PENDING' AND dead_letter = false "
                + "AND next_attempt_at <= now() "
                + "ORDER BY created_at ASC LIMIT 10"));
    logPlan(
        "inventory-movements",
        explain(
            "EXPLAIN SELECT * FROM inventory_movements "
                + "WHERE reference_type = ? AND reference_id = ? "
                + "ORDER BY created_at ASC",
            "DISPATCH",
            "PERF-REF"));
    logPlan(
        "dealer-ledger",
        explain(
            "EXPLAIN SELECT * FROM dealer_ledger_entries "
                + "WHERE company_id = ? AND dealer_id = ? "
                + "ORDER BY entry_date ASC LIMIT 50",
            company.getId(),
            dealer.getId()));
  }

  private List<String> explain(String sql, Object... params) {
    List<String> plan = jdbcTemplate.queryForList(sql, String.class, params);
    assertFalse(plan.isEmpty(), "EXPLAIN plan should not be empty");
    return plan;
  }

  private void logPlan(String label, List<String> plan) {
    log.info("EXPLAIN {}:\n{}", label, String.join("\n", plan));
  }

  private void seedSalesOrder(Company company, Dealer dealer) {
    SalesOrder order = new SalesOrder();
    order.setCompany(company);
    order.setDealer(dealer);
    order.setOrderNumber("SO-PERF-" + System.nanoTime());
    order.setStatus("BOOKED");
    order.setTotalAmount(new BigDecimal("100.00"));
    order.setSubtotalAmount(new BigDecimal("100.00"));
    order.setGstTotal(BigDecimal.ZERO);
    order.setCurrency("INR");
    order.setNotes("Perf seed order");

    SalesOrderItem item = new SalesOrderItem();
    item.setSalesOrder(order);
    item.setProductCode("FG-FIXTURE");
    item.setDescription("Fixture item");
    item.setQuantity(BigDecimal.ONE);
    item.setUnitPrice(new BigDecimal("100.00"));
    item.setLineSubtotal(new BigDecimal("100.00"));
    item.setLineTotal(new BigDecimal("100.00"));
    item.setGstRate(BigDecimal.ZERO);
    item.setGstAmount(BigDecimal.ZERO);
    order.getItems().add(item);

    salesOrderRepository.save(order);
  }

  private void seedInvoice(Company company, Dealer dealer) {
    Invoice invoice = new Invoice();
    invoice.setCompany(company);
    invoice.setDealer(dealer);
    invoice.setInvoiceNumber("INV-PERF-" + System.nanoTime());
    invoice.setStatus("ISSUED");
    invoice.setSubtotal(new BigDecimal("100.00"));
    invoice.setTaxTotal(new BigDecimal("18.00"));
    invoice.setTotalAmount(new BigDecimal("118.00"));
    invoice.setOutstandingAmount(new BigDecimal("118.00"));
    invoice.setCurrency("INR");
    invoice.setIssueDate(LocalDate.now());
    invoice.setDueDate(LocalDate.now().plusDays(15));
    invoice.setNotes("Perf seed invoice");

    InvoiceLine line = new InvoiceLine();
    line.setInvoice(invoice);
    line.setProductCode("FG-FIXTURE");
    line.setDescription("Fixture item");
    line.setQuantity(BigDecimal.ONE);
    line.setUnitPrice(new BigDecimal("100.00"));
    line.setTaxRate(new BigDecimal("18.00"));
    line.setLineTotal(new BigDecimal("118.00"));
    invoice.getLines().add(line);

    invoiceRepository.save(invoice);
  }
}
