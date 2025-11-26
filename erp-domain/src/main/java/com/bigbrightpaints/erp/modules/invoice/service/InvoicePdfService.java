package com.bigbrightpaints.erp.modules.invoice.service;

import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.invoice.domain.Invoice;
import com.bigbrightpaints.erp.modules.invoice.domain.InvoiceLine;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import com.openhtmltopdf.pdfboxout.PdfRendererBuilder;

import java.io.ByteArrayOutputStream;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;

@Service
public class InvoicePdfService {

    private final CompanyContextService companyContextService;
    private final CompanyEntityLookup companyEntityLookup;
    private final TemplateEngine templateEngine;

    public InvoicePdfService(CompanyContextService companyContextService,
                             CompanyEntityLookup companyEntityLookup,
                             TemplateEngine templateEngine) {
        this.companyContextService = companyContextService;
        this.companyEntityLookup = companyEntityLookup;
        this.templateEngine = templateEngine;
    }

    public PdfDocument renderInvoicePdf(Long invoiceId) {
        Company company = companyContextService.requireCurrentCompany();
        Invoice invoice = companyEntityLookup.requireInvoice(company, invoiceId);
        Dealer dealer = invoice.getDealer();
        SalesOrder order = invoice.getSalesOrder();

        List<InvoiceLineView> lines = invoice.getLines().stream()
                .map(this::toLineView)
                .toList();

        BigDecimal subtotal = nonNull(invoice.getSubtotal());
        BigDecimal tax = nonNull(invoice.getTaxTotal());
        BigDecimal total = nonNull(invoice.getTotalAmount());
        BigDecimal discount = subtotal.add(tax).subtract(total);
        if (discount.compareTo(BigDecimal.ZERO) < 0) {
            discount = BigDecimal.ZERO;
        }

        InvoiceView view = new InvoiceView(
                company.getName(),
                company.getCode(),
                "N/A",
                "N/A",
                "",
                invoice.getInvoiceNumber(),
                order != null ? order.getOrderNumber() : "-",
                invoice.getIssueDate(),
                invoice.getDueDate(),
                dealer != null ? dealer.getName() : "N/A",
                dealer != null && dealer.getAddress() != null ? dealer.getAddress() : "N/A",
                dealer != null && dealer.getPhone() != null ? dealer.getPhone() : "",
                lines,
                subtotal,
                discount,
                tax,
                total,
                resolveCurrencySymbol(invoice.getCurrency())
        );

        Context context = new Context();
        context.setVariable("invoice", view);

        String html = templateEngine.process("invoice-template", context);
        byte[] pdf = renderPdf(html);

        String fileName = "invoice-" + (invoice.getInvoiceNumber() != null ? invoice.getInvoiceNumber() : invoice.getId()) + ".pdf";
        return new PdfDocument(fileName, pdf);
    }

    private InvoiceLineView toLineView(InvoiceLine line) {
        String description = line.getDescription();
        if (description == null || description.isBlank()) {
            description = line.getProductCode() != null ? line.getProductCode() : "Item";
        }
        return new InvoiceLineView(
                description,
                nonNull(line.getQuantity()),
                nonNull(line.getUnitPrice()),
                nonNull(line.getLineTotal()),
                nonNull(line.getTaxRate())
        );
    }

    private BigDecimal nonNull(BigDecimal value) {
        return value != null ? value : BigDecimal.ZERO;
    }

    private byte[] renderPdf(String html) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfRendererBuilder builder = new PdfRendererBuilder();
            builder.useFastMode();
            builder.withHtmlContent(html, "");
            builder.toStream(out);
            builder.run();
            return out.toByteArray();
        } catch (Exception e) {
            throw new IllegalStateException("Failed to render invoice PDF", e);
        }
    }

    private String resolveCurrencySymbol(String currency) {
        String code = currency != null ? currency.trim().toUpperCase() : "INR";
        return switch (code) {
            case "INR" -> "₹";
            case "USD" -> "$";
            case "EUR" -> "€";
            case "GBP" -> "£";
            default -> code + " ";
        };
    }

    public record InvoiceLineView(String description,
                                  BigDecimal quantity,
                                  BigDecimal unitPrice,
                                  BigDecimal lineTotal,
                                  BigDecimal taxRate) {}

    public record InvoiceView(String companyName,
                              String companyCode,
                              String companyAddress,
                              String companyGstin,
                              String companyPhone,
                              String invoiceNumber,
                              String orderNumber,
                              LocalDate issueDate,
                              LocalDate dueDate,
                              String billToName,
                              String billToAddress,
                              String billToPhone,
                              List<InvoiceLineView> lines,
                              BigDecimal subtotal,
                              BigDecimal discount,
                              BigDecimal tax,
                              BigDecimal total,
                              String currencySymbol) {}

    public record PdfDocument(String fileName, byte[] content) {
        public PdfDocument {
            Objects.requireNonNull(content, "PDF content is required");
        }
    }
}
