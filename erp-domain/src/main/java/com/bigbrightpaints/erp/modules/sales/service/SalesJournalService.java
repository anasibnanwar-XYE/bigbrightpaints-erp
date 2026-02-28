package com.bigbrightpaints.erp.modules.sales.service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.MoneyUtils;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingFacade;
import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService;
import com.bigbrightpaints.erp.modules.inventory.service.FinishedGoodsService.FinishedGoodAccountingProfile;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrder;
import com.bigbrightpaints.erp.modules.sales.domain.SalesOrderItem;
import com.bigbrightpaints.erp.modules.sales.util.SalesOrderReference;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProduct;
import com.bigbrightpaints.erp.modules.production.domain.ProductionProductRepository;
import com.bigbrightpaints.erp.modules.accounting.service.CompanyDefaultAccountsService;
import com.bigbrightpaints.erp.modules.accounting.service.CompanyAccountingSettingsService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

/**
 * Service for posting sales journal entries.
 * Delegates to AccountingFacade for actual journal creation.
 */
@Service
public class SalesJournalService {

    private static final Logger log = LoggerFactory.getLogger(SalesJournalService.class);
    private static final BigDecimal ROUNDING_TOLERANCE = new BigDecimal("0.05");

    private final FinishedGoodsService finishedGoodsService;
    private final AccountingFacade accountingFacade;
    private final ProductionProductRepository productionProductRepository;
    private final CompanyDefaultAccountsService companyDefaultAccountsService;
    private final CompanyAccountingSettingsService companyAccountingSettingsService;

    public SalesJournalService(FinishedGoodsService finishedGoodsService,
                               AccountingFacade accountingFacade,
                               ProductionProductRepository productionProductRepository,
                               CompanyDefaultAccountsService companyDefaultAccountsService,
                               CompanyAccountingSettingsService companyAccountingSettingsService) {
        this.finishedGoodsService = finishedGoodsService;
        this.accountingFacade = accountingFacade;
        this.productionProductRepository = productionProductRepository;
        this.companyDefaultAccountsService = companyDefaultAccountsService;
        this.companyAccountingSettingsService = companyAccountingSettingsService;
    }

    /**
     * Post sales journal entry for a sales order.
     * Delegates to AccountingFacade which handles idempotency, validation, and posting.
     *
     * @param order Sales order to post journal for
     * @param amountOverride Optional amount override (null = use order total)
     * @param referenceNumber Optional custom reference number
     * @param entryDate Optional entry date (null = current date)
     * @param memo Optional memo text
     * @return Journal entry ID or null if skipped
     */
    public Long postSalesJournal(SalesOrder order,
                                 BigDecimal amountOverride,
                                 String referenceNumber,
                                 LocalDate entryDate,
                                 String memo) {
        Objects.requireNonNull(order, "Sales order is required for journal posting");
        throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                "Order-truth sales journal posting is disabled (CODE-RED). Use dispatch confirmation.");
    }

    private ProductAccounts resolveAccounts(Company company,
                                            String productCode,
                                            FinishedGoodAccountingProfile profile) {
        Long revenueAccountId = profile != null ? profile.revenueAccountId() : null;
        Long taxAccountId = profile != null ? profile.taxAccountId() : null;
        Long discountAccountId = profile != null ? profile.discountAccountId() : null;
        ProductionProduct product = null;
        if (revenueAccountId == null || taxAccountId == null || discountAccountId == null) {
            product = productionProductRepository.findByCompanyAndSkuCode(company, productCode)
                    .orElseThrow(() -> com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState("Product " + productCode + " missing finished good and metadata account mapping"));
        }
        if (revenueAccountId == null && product != null) {
            revenueAccountId = metadataLong(product, "fgRevenueAccountId");
        }
        if (taxAccountId == null && product != null) {
            taxAccountId = metadataLong(product, "fgTaxAccountId");
        }
        if (discountAccountId == null && product != null) {
            discountAccountId = metadataLong(product, "fgDiscountAccountId");
        }
        if (revenueAccountId == null || taxAccountId == null || discountAccountId == null) {
            var defaults = companyDefaultAccountsService.getDefaults();
            if (revenueAccountId == null) {
                revenueAccountId = defaults.revenueAccountId();
            }
            if (taxAccountId == null) {
                taxAccountId = defaults.taxAccountId();
            }
            if (discountAccountId == null) {
                discountAccountId = defaults.discountAccountId();
            }
        }
        if (revenueAccountId == null || taxAccountId == null) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState("Company default revenue/tax accounts are not configured for product " + productCode);
        }
        return new ProductAccounts(revenueAccountId, taxAccountId, discountAccountId);
    }

    private Long metadataLong(ProductionProduct product, String key) {
        if (product.getMetadata() == null) {
            return null;
        }
        Object value = product.getMetadata().get(key);
        if (value instanceof Number number) {
            return number.longValue();
        }
        if (value instanceof String text && !text.isBlank()) {
            try {
                return Long.parseLong(text.trim());
            } catch (NumberFormatException ex) {
                log.warn("Unable to parse metadata {}={} for product {}", key, text, product.getSkuCode());
            }
        }
        return null;
    }

    private BigDecimal normalizeDiscountNet(BigDecimal discount, BigDecimal taxRate, boolean gstInclusive) {
        BigDecimal normalized = discount != null ? discount : BigDecimal.ZERO;
        if (!gstInclusive || normalized.compareTo(BigDecimal.ZERO) <= 0) {
            return currency(normalized);
        }
        BigDecimal rate = taxRate != null ? taxRate : BigDecimal.ZERO;
        if (rate.compareTo(BigDecimal.ZERO) <= 0) {
            return currency(normalized);
        }
        BigDecimal divisor = BigDecimal.ONE.add(rate.divide(new BigDecimal("100"), 6, RoundingMode.HALF_UP));
        if (divisor.compareTo(BigDecimal.ZERO) == 0) {
            return currency(normalized);
        }
        return currency(normalized.divide(divisor, 6, RoundingMode.HALF_UP));
    }

    private BigDecimal currency(BigDecimal value) {
        return MoneyUtils.roundCurrency(value);
    }

    private record ProductAccounts(Long revenueAccountId, Long taxAccountId, Long discountAccountId) {}

}
