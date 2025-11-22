package com.bigbrightpaints.tally.service.csv;

import com.bigbrightpaints.tally.domain.IngestionFile;
import com.bigbrightpaints.tally.domain.IngestionRun;
import com.bigbrightpaints.tally.domain.staging.StagingAccount;
import com.bigbrightpaints.tally.domain.staging.StagingDealer;
import com.bigbrightpaints.tally.domain.staging.StagingProduct;
import com.bigbrightpaints.tally.repository.IngestionFileRepository;
import com.bigbrightpaints.tally.repository.staging.StagingAccountRepository;
import com.bigbrightpaints.tally.repository.staging.StagingDealerRepository;
import com.bigbrightpaints.tally.repository.staging.StagingProductRepository;
import com.bigbrightpaints.tally.util.HashUtil;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.math.BigDecimal;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service for processing CSV files and loading data into staging tables
 */
@Service
@RequiredArgsConstructor
@Slf4j
public class CsvFileProcessor {

    private final CsvParser csvParser;
    private final StagingProductRepository stagingProductRepository;
    private final StagingDealerRepository stagingDealerRepository;
    private final StagingAccountRepository stagingAccountRepository;
    private final IngestionFileRepository ingestionFileRepository;
    private final ObjectMapper objectMapper;

    /**
     * Process uploaded CSV file based on its type
     */
    @Transactional
    public ProcessingResult processFile(
            IngestionRun run,
            IngestionFile file,
            MultipartFile uploadedFile) throws IOException {

        log.info("Processing file: {} of type: {} for run: {}",
                file.getFileName(), file.getFileType(), run.getRunId());

        file.markProcessing();
        ingestionFileRepository.save(file);

        try (InputStream inputStream = uploadedFile.getInputStream()) {
            ProcessingResult result = switch (file.getFileType()) {
                case PRODUCTS -> processProductsFile(run, file, inputStream);
                case DEALERS -> processDealersFile(run, file, inputStream);
                case SUPPLIERS -> processSuppliersFile(run, file, inputStream);
                case ACCOUNTS -> processAccountsFile(run, file, inputStream);
                case INVENTORY -> processInventoryFile(run, file, inputStream);
                case PRICING -> processPricingFile(run, file, inputStream);
                default -> throw new IllegalArgumentException(
                        "Unsupported file type: " + file.getFileType());
            };

            file.setTotalRows(result.totalRows);
            file.setProcessedRows(result.processedRows);
            file.setFailedRows(result.failedRows);
            file.markCompleted();
            ingestionFileRepository.save(file);

            return result;

        } catch (Exception e) {
            log.error("Error processing file: " + file.getFileName(), e);
            file.markFailed(e.getMessage());
            ingestionFileRepository.save(file);
            throw new RuntimeException("Failed to process file", e);
        }
    }

    /**
     * Process products CSV file
     */
    private ProcessingResult processProductsFile(
            IngestionRun run,
            IngestionFile file,
            InputStream inputStream) throws IOException {

        AtomicInteger rowNumber = new AtomicInteger(0);
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        csvParser.parseCsvInBatches(inputStream, batch -> {
            List<StagingProduct> stagingProducts = batch.stream()
                    .map(row -> {
                        try {
                            return mapToStagingProduct(run, row, rowNumber.incrementAndGet());
                        } catch (Exception e) {
                            log.error("Error mapping row to staging product: " + row, e);
                            failed.incrementAndGet();
                            return null;
                        }
                    })
                    .filter(product -> product != null)
                    .toList();

            if (!stagingProducts.isEmpty()) {
                stagingProductRepository.saveAll(stagingProducts);
                processed.addAndGet(stagingProducts.size());
            }
        });

        return new ProcessingResult(
                rowNumber.get(),
                processed.get(),
                failed.get());
    }

    /**
     * Process dealers CSV file
     */
    private ProcessingResult processDealersFile(
            IngestionRun run,
            IngestionFile file,
            InputStream inputStream) throws IOException {

        AtomicInteger rowNumber = new AtomicInteger(0);
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        csvParser.parseCsvInBatches(inputStream, batch -> {
            List<StagingDealer> stagingDealers = batch.stream()
                    .map(row -> {
                        try {
                            return mapToStagingDealer(run, row, rowNumber.incrementAndGet());
                        } catch (Exception e) {
                            log.error("Error mapping row to staging dealer: " + row, e);
                            failed.incrementAndGet();
                            return null;
                        }
                    })
                    .filter(dealer -> dealer != null)
                    .toList();

            if (!stagingDealers.isEmpty()) {
                stagingDealerRepository.saveAll(stagingDealers);
                processed.addAndGet(stagingDealers.size());
            }
        });

        return new ProcessingResult(
                rowNumber.get(),
                processed.get(),
                failed.get());
    }

    /**
     * Process suppliers CSV file (similar to dealers)
     */
    private ProcessingResult processSuppliersFile(
            IngestionRun run,
            IngestionFile file,
            InputStream inputStream) throws IOException {

        // Similar implementation to processDealersFile but for suppliers
        // For now, treating suppliers like dealers since they're similar
        return processDealersFile(run, file, inputStream);
    }

    /**
     * Process accounts CSV file
     */
    private ProcessingResult processAccountsFile(
            IngestionRun run,
            IngestionFile file,
            InputStream inputStream) throws IOException {

        AtomicInteger rowNumber = new AtomicInteger(0);
        AtomicInteger processed = new AtomicInteger(0);
        AtomicInteger failed = new AtomicInteger(0);

        csvParser.parseCsvInBatches(inputStream, batch -> {
            List<StagingAccount> stagingAccounts = batch.stream()
                    .map(row -> {
                        try {
                            return mapToStagingAccount(run, row, rowNumber.incrementAndGet());
                        } catch (Exception e) {
                            log.error("Error mapping row to staging account: " + row, e);
                            failed.incrementAndGet();
                            return null;
                        }
                    })
                    .filter(account -> account != null)
                    .toList();

            if (!stagingAccounts.isEmpty()) {
                stagingAccountRepository.saveAll(stagingAccounts);
                processed.addAndGet(stagingAccounts.size());
            }
        });

        return new ProcessingResult(
                rowNumber.get(),
                processed.get(),
                failed.get());
    }

    /**
     * Process inventory CSV file
     */
    private ProcessingResult processInventoryFile(
            IngestionRun run,
            IngestionFile file,
            InputStream inputStream) throws IOException {

        // TODO: Implement inventory processing
        log.warn("Inventory processing not yet implemented");
        return new ProcessingResult(0, 0, 0);
    }

    /**
     * Process pricing CSV file
     */
    private ProcessingResult processPricingFile(
            IngestionRun run,
            IngestionFile file,
            InputStream inputStream) throws IOException {

        // TODO: Implement pricing processing
        log.warn("Pricing processing not yet implemented");
        return new ProcessingResult(0, 0, 0);
    }

    /**
     * Map CSV row to StagingProduct entity
     */
    private StagingProduct mapToStagingProduct(
            IngestionRun run,
            Map<String, String> row,
            int rowNumber) {

        StagingProduct product = new StagingProduct();
        product.setRun(run);
        product.setRowNumber(rowNumber);

        // Map Tally fields
        product.setStockItemName(getStringValue(row, "stock_item_name", "item_name", "product_name"));
        product.setStockGroup(getStringValue(row, "stock_group", "group"));
        product.setStockCategory(getStringValue(row, "stock_category", "category"));
        product.setBaseUnit(getStringValue(row, "base_unit", "unit", "uom"));
        product.setAlternateUnit(getStringValue(row, "alternate_unit", "alt_unit"));
        product.setGstRate(getBigDecimalValue(row, "gst_rate", "tax_rate"));
        product.setHsnCode(getStringValue(row, "hsn_code", "hsn"));
        product.setItemCode(getStringValue(row, "item_code", "code"));
        product.setBarcode(getStringValue(row, "barcode", "ean"));
        product.setBrand(extractBrand(product.getStockItemName()));

        // Parse variant attributes from name
        parseProductVariants(product);

        // Set processing fields
        product.setRawData(new HashMap<>(row));
        product.setSourceHash(HashUtil.generateHash(row.toString()));
        product.setValidationStatus(StagingProduct.ValidationStatus.PENDING);

        return product;
    }

    /**
     * Map CSV row to StagingDealer entity
     */
    private StagingDealer mapToStagingDealer(
            IngestionRun run,
            Map<String, String> row,
            int rowNumber) {

        StagingDealer dealer = new StagingDealer();
        dealer.setRun(run);
        dealer.setRowNumber(rowNumber);

        // Map Tally fields
        dealer.setPartyName(getStringValue(row, "party_name", "dealer_name", "name"));
        dealer.setLedgerName(getStringValue(row, "ledger_name", "ledger"));
        dealer.setAddress(getStringValue(row, "address", "street_address"));
        dealer.setCity(getStringValue(row, "city"));
        dealer.setState(getStringValue(row, "state"));
        dealer.setPincode(getStringValue(row, "pincode", "zip", "postal_code"));
        dealer.setCountry(getStringValue(row, "country"));
        dealer.setEmail(getStringValue(row, "email", "email_address"));
        dealer.setPhone(getStringValue(row, "phone", "telephone"));
        dealer.setMobile(getStringValue(row, "mobile", "cell"));
        dealer.setPan(getStringValue(row, "pan", "pan_number"));
        dealer.setGstin(getStringValue(row, "gstin", "gst_number"));
        dealer.setCreditPeriodDays(getIntegerValue(row, "credit_period", "payment_terms"));
        dealer.setCreditLimit(getBigDecimalValue(row, "credit_limit"));
        dealer.setOpeningBalance(getBigDecimalValue(row, "opening_balance", "opening_bal"));
        dealer.setDrCr(getStringValue(row, "dr_cr", "debit_credit"));

        // Set processing fields
        dealer.setRawData(new HashMap<>(row));
        dealer.setSourceHash(HashUtil.generateHash(row.toString()));
        dealer.setValidationStatus(StagingDealer.ValidationStatus.PENDING);

        return dealer;
    }

    /**
     * Map CSV row to StagingAccount entity
     */
    private StagingAccount mapToStagingAccount(
            IngestionRun run,
            Map<String, String> row,
            int rowNumber) {

        StagingAccount account = new StagingAccount();
        account.setRun(run);
        account.setRowNumber(rowNumber);

        // Map Tally fields
        account.setLedgerName(getStringValue(row, "ledger_name", "account_name"));
        account.setLedgerGroup(getStringValue(row, "ledger_group", "account_group", "group"));
        account.setOpeningBalance(getBigDecimalValue(row, "opening_balance", "opening_bal"));
        account.setDrCr(getStringValue(row, "dr_cr", "debit_credit"));
        account.setAddress(getStringValue(row, "address"));
        account.setCity(getStringValue(row, "city"));
        account.setState(getStringValue(row, "state"));
        account.setPincode(getStringValue(row, "pincode", "zip"));
        account.setPan(getStringValue(row, "pan", "pan_number"));
        account.setGstin(getStringValue(row, "gstin", "gst_number"));
        account.setBankName(getStringValue(row, "bank_name", "bank"));
        account.setAccountNumber(getStringValue(row, "account_number", "account_no"));
        account.setIfscCode(getStringValue(row, "ifsc_code", "ifsc"));

        // Set processing fields
        account.setRawData(new HashMap<>(row));
        account.setSourceHash(HashUtil.generateHash(row.toString()));
        account.setValidationStatus(StagingAccount.ValidationStatus.PENDING);

        return account;
    }

    /**
     * Extract brand from product name
     */
    private String extractBrand(String productName) {
        if (productName == null || productName.isEmpty()) {
            return null;
        }

        // Common paint brands in India
        String[] brands = {"SAPPHIRE", "NEROLAC", "ASIAN", "BERGER", "DULUX", "NIPPON"};

        String upperName = productName.toUpperCase();
        for (String brand : brands) {
            if (upperName.startsWith(brand)) {
                return brand;
            }
        }

        // If no known brand, take first word
        String[] parts = productName.split("\\s+");
        return parts.length > 0 ? parts[0].toUpperCase() : null;
    }

    /**
     * Parse product variant attributes from name
     */
    private void parseProductVariants(StagingProduct product) {
        String name = product.getStockItemName();
        if (name == null || name.isEmpty()) {
            return;
        }

        // Remove brand if present
        String brand = product.getBrand();
        if (brand != null && name.toUpperCase().startsWith(brand)) {
            name = name.substring(brand.length()).trim();
        }

        // Parse color
        String[] colors = {"WHITE", "BLACK", "RED", "BLUE", "GREEN", "YELLOW", "ORANGE", "PURPLE"};
        for (String color : colors) {
            if (name.toUpperCase().contains(color)) {
                product.setColor(color);
                break;
            }
        }

        // Parse size (e.g., 1L, 5L, 10KG, 500ML)
        String sizePattern = "\\b(\\d+(?:\\.\\d+)?)(L|LT|LTR|ML|KG|G|GM)\\b";
        java.util.regex.Pattern pattern = java.util.regex.Pattern.compile(sizePattern, java.util.regex.Pattern.CASE_INSENSITIVE);
        java.util.regex.Matcher matcher = pattern.matcher(name);
        if (matcher.find()) {
            product.setSize(matcher.group(0).toUpperCase());
        }

        // Extract base product name (remove color and size)
        String baseName = name;
        if (product.getColor() != null) {
            baseName = baseName.replaceAll("(?i)\\b" + product.getColor() + "\\b", "").trim();
        }
        if (product.getSize() != null) {
            baseName = baseName.replaceAll("(?i)\\b" + product.getSize() + "\\b", "").trim();
        }
        product.setBaseProductName(baseName.trim());
    }

    // Helper methods for getting values from row map
    private String getStringValue(Map<String, String> row, String... keys) {
        for (String key : keys) {
            String value = row.get(key);
            if (value != null && !value.isEmpty()) {
                return value.trim();
            }
        }
        return null;
    }

    private BigDecimal getBigDecimalValue(Map<String, String> row, String... keys) {
        String value = getStringValue(row, keys);
        if (value != null) {
            try {
                return new BigDecimal(value.replaceAll("[^0-9.-]", ""));
            } catch (NumberFormatException e) {
                log.debug("Unable to parse BigDecimal from: {}", value);
            }
        }
        return null;
    }

    private Integer getIntegerValue(Map<String, String> row, String... keys) {
        String value = getStringValue(row, keys);
        if (value != null) {
            try {
                return Integer.parseInt(value.replaceAll("[^0-9-]", ""));
            } catch (NumberFormatException e) {
                log.debug("Unable to parse Integer from: {}", value);
            }
        }
        return null;
    }

    /**
     * Result of file processing
     */
    public record ProcessingResult(
            int totalRows,
            int processedRows,
            int failedRows) {

        public double getSuccessRate() {
            if (totalRows == 0) {
                return 0.0;
            }
            return (double) processedRows / totalRows * 100;
        }
    }
}