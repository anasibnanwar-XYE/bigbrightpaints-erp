package com.bigbrightpaints.erp.modules.purchasing.service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.core.util.CompanyEntityLookup;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.core.util.MoneyUtils;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalCorrectionType;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalReferenceMapping;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalReferenceMappingRepository;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalCreationRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.JournalEntryDto;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingFacade;
import com.bigbrightpaints.erp.modules.accounting.service.GstService;
import com.bigbrightpaints.erp.modules.accounting.service.ReferenceNumberService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.inventory.domain.InventoryReference;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatch;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialBatchRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovement;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialMovementRepository;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterialRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchase;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseLine;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseRepository;
import com.bigbrightpaints.erp.modules.purchasing.domain.Supplier;
import com.bigbrightpaints.erp.modules.purchasing.dto.PurchaseReturnPreviewDto;
import com.bigbrightpaints.erp.modules.purchasing.dto.PurchaseReturnRequest;
import jakarta.transaction.Transactional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

@Service
public class PurchaseReturnService {

    private static final String PURCHASE_RETURN_PREVIEW_PREFIX = "PRN-PREVIEW-";
    private static final String JOURNAL_ENTRY_MAPPING_TYPE = "JOURNAL_ENTRY";

    private final CompanyContextService companyContextService;
    private final RawMaterialPurchaseRepository purchaseRepository;
    private final RawMaterialRepository rawMaterialRepository;
    private final RawMaterialBatchRepository rawMaterialBatchRepository;
    private final RawMaterialMovementRepository movementRepository;
    private final AccountingFacade accountingFacade;
    private final JournalEntryRepository journalEntryRepository;
    private final JournalReferenceMappingRepository journalReferenceMappingRepository;
    private final CompanyEntityLookup companyEntityLookup;
    private final ReferenceNumberService referenceNumberService;
    private final CompanyClock companyClock;
    private final GstService gstService;
    private final PurchaseReturnAllocationService allocationService;

    public PurchaseReturnService(CompanyContextService companyContextService,
                                 RawMaterialPurchaseRepository purchaseRepository,
                                 RawMaterialRepository rawMaterialRepository,
                                 RawMaterialBatchRepository rawMaterialBatchRepository,
                                 RawMaterialMovementRepository movementRepository,
                                 AccountingFacade accountingFacade,
                                 JournalEntryRepository journalEntryRepository,
                                 JournalReferenceMappingRepository journalReferenceMappingRepository,
                                 CompanyEntityLookup companyEntityLookup,
                                 ReferenceNumberService referenceNumberService,
                                 CompanyClock companyClock,
                                 GstService gstService,
                                 PurchaseReturnAllocationService allocationService) {
        this.companyContextService = companyContextService;
        this.purchaseRepository = purchaseRepository;
        this.rawMaterialRepository = rawMaterialRepository;
        this.rawMaterialBatchRepository = rawMaterialBatchRepository;
        this.movementRepository = movementRepository;
        this.accountingFacade = accountingFacade;
        this.journalEntryRepository = journalEntryRepository;
        this.journalReferenceMappingRepository = journalReferenceMappingRepository;
        this.companyEntityLookup = companyEntityLookup;
        this.referenceNumberService = referenceNumberService;
        this.companyClock = companyClock;
        this.gstService = gstService;
        this.allocationService = allocationService;
    }

    @Transactional
    public PurchaseReturnPreviewDto previewPurchaseReturn(PurchaseReturnRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        Supplier supplier = companyEntityLookup.requireSupplier(company, request.supplierId());
        supplier.requireTransactionalUsage("preview purchase returns");
        RawMaterialPurchase purchase = purchaseRepository.lockByCompanyAndId(company, request.purchaseId())
                .orElseThrow(() -> com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Raw material purchase not found"));
        ensurePostedPurchase(purchase);
        if (purchase.getSupplier() == null || !purchase.getSupplier().getId().equals(supplier.getId())) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Purchase does not belong to the supplier");
        }
        RawMaterial material = rawMaterialRepository.lockByCompanyAndId(company, request.rawMaterialId())
                .orElseThrow(() -> com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Raw material not found"));
        boolean materialInPurchase = purchase.getLines().stream()
                .anyMatch(line -> line.getRawMaterial() != null
                        && line.getRawMaterial().getId().equals(material.getId()));
        if (!materialInPurchase) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                    "Purchase does not include raw material " + material.getName());
        }
        BigDecimal quantity = positive(request.quantity(), "quantity");
        BigDecimal unitCost = positive(request.unitCost(), "unitCost");
        BigDecimal remainingReturnableQty = allocationService.remainingReturnableQuantity(purchase, material);
        if (quantity.compareTo(remainingReturnableQty) > 0) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "Purchase return quantity exceeds remaining returnable quantity")
                    .withDetail("purchaseId", purchase.getId())
                    .withDetail("rawMaterialId", material.getId())
                    .withDetail("remainingReturnableQuantity", remainingReturnableQty)
                    .withDetail("requestedQuantity", quantity);
        }
        BigDecimal lineNet = currency(MoneyUtils.safeMultiply(quantity, unitCost));
        BigDecimal taxAmount = computeReturnTax(purchase, material, quantity);
        String reference = StringUtils.hasText(request.referenceNumber())
                ? request.referenceNumber().trim()
                : referenceNumberService.purchaseReturnPreviewReference(company, supplier);
        LocalDate returnDate = request.returnDate() != null ? request.returnDate() : companyClock.today(company);
        return new PurchaseReturnPreviewDto(
                purchase.getId(),
                purchase.getInvoiceNumber(),
                material.getId(),
                material.getName(),
                quantity,
                remainingReturnableQty.subtract(quantity).max(BigDecimal.ZERO),
                lineNet,
                taxAmount,
                currency(lineNet.add(taxAmount)),
                returnDate,
                reference
        );
    }

    @Transactional
    public JournalEntryDto recordPurchaseReturn(PurchaseReturnRequest request) {
        Company company = companyContextService.requireCurrentCompany();
        Supplier supplier = companyEntityLookup.requireSupplier(company, request.supplierId());
        RawMaterialPurchase purchase = purchaseRepository.lockByCompanyAndId(company, request.purchaseId())
                .orElseThrow(() -> com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Raw material purchase not found"));
        if (purchase.getSupplier() == null || !purchase.getSupplier().getId().equals(supplier.getId())) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Purchase does not belong to the supplier");
        }
        RawMaterial material = rawMaterialRepository.lockByCompanyAndId(company, request.rawMaterialId())
                .orElseThrow(() -> com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput("Raw material not found"));
        boolean materialInPurchase = purchase.getLines().stream()
                .anyMatch(line -> line.getRawMaterial() != null
                        && line.getRawMaterial().getId().equals(material.getId()));
        if (!materialInPurchase) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                    "Purchase does not include raw material " + material.getName());
        }
        if (material.getInventoryAccountId() == null) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(
                    "Raw material " + material.getName() + " is missing an inventory account mapping");
        }
        BigDecimal quantity = positive(request.quantity(), "quantity");
        BigDecimal unitCost = positive(request.unitCost(), "unitCost");
        BigDecimal lineNet = currency(MoneyUtils.safeMultiply(quantity, unitCost));
        BigDecimal taxAmount = computeReturnTax(purchase, material, quantity);
        BigDecimal totalAmount = currency(lineNet.add(taxAmount));
        String memo = returnMemo(material, supplier, request.reason());
        String providedReference = StringUtils.hasText(request.referenceNumber())
                ? request.referenceNumber().trim()
                : null;
        String reference = resolvePostingReference(company, supplier, providedReference);
        LocalDate returnDate = request.returnDate() != null ? request.returnDate() : companyClock.today(company);

        List<RawMaterialMovement> existingMovements = movementRepository
                .findByRawMaterialCompanyAndReferenceTypeAndReferenceId(company,
                        InventoryReference.PURCHASE_RETURN,
                        reference);
        if (!existingMovements.isEmpty()) {
            JournalEntryDto replay = returnExistingPurchaseReturn(purchase, material, supplier, quantity, unitCost, reference,
                    returnDate, memo, existingMovements);
            finalizePreviewReferenceMapping(company, providedReference, reference, replay != null ? replay.id() : null);
            return replay;
        }

        supplier.requireTransactionalUsage("record purchase returns");
        if (supplier.getPayableAccount() == null || supplier.getPayableAccount().getId() == null) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(
                    "Supplier " + supplier.getName() + " is missing payable account mapping");
        }
        ensurePostedPurchase(purchase);

        BigDecimal remainingReturnableQty = allocationService.remainingReturnableQuantity(purchase, material);
        if (remainingReturnableQty.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "All purchased quantity has already been returned for this material")
                    .withDetail("purchaseId", purchase.getId())
                    .withDetail("rawMaterialId", material.getId());
        }
        if (quantity.compareTo(remainingReturnableQty) > 0) {
            throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT,
                    "Purchase return quantity exceeds remaining returnable quantity")
                    .withDetail("purchaseId", purchase.getId())
                    .withDetail("rawMaterialId", material.getId())
                    .withDetail("remainingReturnableQuantity", remainingReturnableQty)
                    .withDetail("requestedQuantity", quantity);
        }

        Map<Long, BigDecimal> taxCredits = null;
        JournalCreationRequest.GstBreakdown gstBreakdown = null;
        if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
            taxCredits = new HashMap<>();
            taxCredits.put(null, taxAmount);
            GstService.GstBreakdown split = gstService.splitTaxAmount(
                    lineNet,
                    taxAmount,
                    company.getStateCode(),
                    supplier.getStateCode());
            gstBreakdown = new JournalCreationRequest.GstBreakdown(lineNet, split.cgst(), split.sgst(), split.igst());
        }
        JournalEntryDto entry = accountingFacade.postPurchaseReturn(
                supplier.getId(),
                reference,
                returnDate,
                memo,
                Map.of(material.getInventoryAccountId(), lineNet),
                taxCredits,
                gstBreakdown,
                totalAmount
        );
        ensureLinkedCorrectionJournal(companyContextService.requireCurrentCompany(), entry, purchase.getJournalEntry(), purchase.getInvoiceNumber());
        finalizePreviewReferenceMapping(company, providedReference, reference, entry != null ? entry.id() : null);

        int updated = rawMaterialRepository.deductStockIfSufficient(material.getId(), quantity);
        if (updated == 0) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                    "Cannot return more than on-hand inventory for " + material.getName());
        }

        List<RawMaterialMovement> movements = issueReturnFromBatches(material, quantity, unitCost, reference, entry.id());
        movementRepository.saveAll(movements);
        allocationService.applyPurchaseReturnQuantity(purchase, material, quantity);
        allocationService.applyPurchaseReturnToOutstanding(purchase, totalAmount);
        return entry;
    }

    private JournalEntryDto returnExistingPurchaseReturn(RawMaterialPurchase purchase,
                                                         RawMaterial material,
                                                         Supplier supplier,
                                                         BigDecimal quantity,
                                                         BigDecimal unitCost,
                                                         String reference,
                                                         LocalDate returnDate,
                                                         String memo,
                                                         List<RawMaterialMovement> existingMovements) {
        validateReturnReplay(material, quantity, unitCost, reference, existingMovements);
        BigDecimal lineNet = currency(MoneyUtils.safeMultiply(quantity, unitCost));
        BigDecimal taxAmount = computeReturnTax(purchase, material, quantity);
        BigDecimal totalAmount = currency(lineNet.add(taxAmount));
        Map<Long, BigDecimal> taxCredits = null;
        JournalCreationRequest.GstBreakdown gstBreakdown = null;
        if (taxAmount.compareTo(BigDecimal.ZERO) > 0) {
            taxCredits = new HashMap<>();
            taxCredits.put(null, taxAmount);
            GstService.GstBreakdown split = gstService.splitTaxAmount(
                    lineNet,
                    taxAmount,
                    companyContextService.requireCurrentCompany().getStateCode(),
                    supplier.getStateCode());
            gstBreakdown = new JournalCreationRequest.GstBreakdown(lineNet, split.cgst(), split.sgst(), split.igst());
        }
        JournalEntryDto entry = accountingFacade.postPurchaseReturn(
                supplier.getId(),
                reference,
                returnDate,
                memo,
                Map.of(material.getInventoryAccountId(), lineNet),
                taxCredits,
                gstBreakdown,
                totalAmount
        );
        JournalEntry replayJournal = validateReplayJournalBinding(
                companyContextService.requireCurrentCompany(),
                purchase,
                reference,
                existingMovements,
                entry);
        ensureLinkedCorrectionJournal(replayJournal, purchase.getJournalEntry(), purchase.getInvoiceNumber());
        if (replayJournal != null) {
            Long replayJournalId = replayJournal.getId();
            boolean needsLink = existingMovements.stream()
                    .anyMatch(movement -> movement.getJournalEntryId() == null);
            if (needsLink) {
                existingMovements.forEach(movement -> movement.setJournalEntryId(replayJournalId));
                movementRepository.saveAll(existingMovements);
            }
        }
        return entry;
    }

    private void ensurePostedPurchase(RawMaterialPurchase purchase) {
        if (purchase == null) {
            return;
        }
        if (purchase.getJournalEntry() == null || purchase.getJournalEntry().getId() == null) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(
                    "Only posted purchases can be corrected through purchase return");
        }
        String status = purchase.getStatus();
        if (!StringUtils.hasText(status)
                || "DRAFT".equalsIgnoreCase(status)
                || "VOID".equalsIgnoreCase(status)
                || "REVERSED".equalsIgnoreCase(status)) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidState(
                    "Only posted purchases can be corrected through purchase return");
        }
    }

    private void ensureLinkedCorrectionJournal(Company company,
                                               JournalEntryDto entryDto,
                                               JournalEntry sourceEntry,
                                               String purchaseInvoiceNumber) {
        if (entryDto == null || entryDto.id() == null || sourceEntry == null || sourceEntry.getId() == null) {
            return;
        }
        journalEntryRepository.findByCompanyAndId(company, entryDto.id())
                .ifPresent(entry -> ensureLinkedCorrectionJournal(entry, sourceEntry, purchaseInvoiceNumber));
    }

    private void ensureLinkedCorrectionJournal(JournalEntry entry,
                                               JournalEntry sourceEntry,
                                               String purchaseInvoiceNumber) {
        if (entry == null || entry.getId() == null || sourceEntry == null || sourceEntry.getId() == null) {
            return;
        }
        boolean changed = false;
        if (entry.getCorrectionType() != JournalCorrectionType.REVERSAL) {
            entry.setCorrectionType(JournalCorrectionType.REVERSAL);
            changed = true;
        }
        if (!"PURCHASE_RETURN".equalsIgnoreCase(entry.getCorrectionReason())) {
            entry.setCorrectionReason("PURCHASE_RETURN");
            changed = true;
        }
        if (!"PURCHASING_RETURN".equalsIgnoreCase(entry.getSourceModule())) {
            entry.setSourceModule("PURCHASING_RETURN");
            changed = true;
        }
        if (entry.getReversalOf() == null
                || !Objects.equals(entry.getReversalOf().getId(), sourceEntry.getId())) {
            entry.setReversalOf(sourceEntry);
            changed = true;
        }
        if (!Objects.equals(purchaseInvoiceNumber, entry.getSourceReference())) {
            entry.setSourceReference(purchaseInvoiceNumber);
            changed = true;
        }
        if (changed) {
            journalEntryRepository.save(entry);
        }
    }

    private JournalEntry validateReplayJournalBinding(Company company,
                                                      RawMaterialPurchase purchase,
                                                      String reference,
                                                      List<RawMaterialMovement> existingMovements,
                                                      JournalEntryDto entryDto) {
        Long replayJournalId = entryDto != null ? entryDto.id() : null;
        Long movementJournalId = resolveMovementReplayJournalId(reference, existingMovements);
        if (movementJournalId != null && replayJournalId != null && !Objects.equals(movementJournalId, replayJournalId)) {
            throwReplayBindingConflict(reference, purchase, movementJournalId, replayJournalId);
        }
        Long authoritativeJournalId = movementJournalId != null ? movementJournalId : replayJournalId;
        if (authoritativeJournalId == null) {
            return null;
        }
        JournalEntry replayJournal = journalEntryRepository.findByCompanyAndId(company, authoritativeJournalId)
                .orElseThrow(() -> replayBindingConflict(reference, purchase)
                        .withDetail("journalEntryId", authoritativeJournalId));
        assertReplayJournalMatchesPurchase(reference, purchase, replayJournal);
        return replayJournal;
    }

    private Long resolveMovementReplayJournalId(String reference,
                                                List<RawMaterialMovement> existingMovements) {
        List<Long> journalEntryIds = existingMovements.stream()
                .map(RawMaterialMovement::getJournalEntryId)
                .filter(Objects::nonNull)
                .distinct()
                .toList();
        if (journalEntryIds.size() > 1) {
            throw replayBindingConflict(reference, null)
                    .withDetail("journalEntryIds", journalEntryIds);
        }
        return journalEntryIds.isEmpty() ? null : journalEntryIds.getFirst();
    }

    private void assertReplayJournalMatchesPurchase(String reference,
                                                    RawMaterialPurchase purchase,
                                                    JournalEntry replayJournal) {
        if (replayJournal == null) {
            return;
        }
        Long sourceJournalId = purchase != null && purchase.getJournalEntry() != null
                ? purchase.getJournalEntry().getId()
                : null;
        boolean hasEstablishedCorrectionProvenance = replayJournal.getCorrectionType() != null
                || replayJournal.getReversalOf() != null
                || "PURCHASE_RETURN".equalsIgnoreCase(replayJournal.getCorrectionReason())
                || "PURCHASING_RETURN".equalsIgnoreCase(replayJournal.getSourceModule());
        if (replayJournal.getReversalOf() != null
                && replayJournal.getReversalOf().getId() != null
                && !Objects.equals(replayJournal.getReversalOf().getId(), sourceJournalId)) {
            throwReplayBindingConflict(reference, purchase, replayJournal.getId(), replayJournal.getId());
        }
        if (hasEstablishedCorrectionProvenance
                && StringUtils.hasText(replayJournal.getSourceReference())
                && !Objects.equals(replayJournal.getSourceReference(), purchase.getInvoiceNumber())) {
            throwReplayBindingConflict(reference, purchase, replayJournal.getId(), replayJournal.getId());
        }
    }

    private void throwReplayBindingConflict(String reference,
                                            RawMaterialPurchase purchase,
                                            Long existingJournalId,
                                            Long replayJournalId) {
        throw replayBindingConflict(reference, purchase)
                .withDetail("existingJournalEntryId", existingJournalId)
                .withDetail("replayedJournalEntryId", replayJournalId);
    }

    private ApplicationException replayBindingConflict(String reference, RawMaterialPurchase purchase) {
        ApplicationException exception = new ApplicationException(
                ErrorCode.CONCURRENCY_CONFLICT,
                "Purchase return reference already linked to a different historical return")
                .withDetail("reference", reference);
        if (purchase != null) {
            if (purchase.getId() != null) {
                exception.withDetail("purchaseId", purchase.getId());
            }
            if (StringUtils.hasText(purchase.getInvoiceNumber())) {
                exception.withDetail("purchaseInvoiceNumber", purchase.getInvoiceNumber());
            }
        }
        return exception;
    }

    private String resolvePostingReference(Company company, Supplier supplier, String providedReference) {
        if (!StringUtils.hasText(providedReference)) {
            return referenceNumberService.purchaseReturnReference(company, supplier);
        }
        String trimmed = providedReference.trim();
        if (isPreviewReference(trimmed)) {
            return resolvePreviewPostingReference(company, supplier, trimmed);
        }
        return trimmed;
    }

    private String resolvePreviewPostingReference(Company company, Supplier supplier, String previewReference) {
        Optional<JournalReferenceMapping> existing = journalReferenceMappingRepository
                .findByCompanyAndLegacyReferenceIgnoreCase(company, previewReference);
        if (existing.isPresent() && StringUtils.hasText(existing.get().getCanonicalReference())) {
            return existing.get().getCanonicalReference().trim();
        }

        String canonicalReference = referenceNumberService.purchaseReturnReference(company, supplier);
        if (company == null || company.getId() == null) {
            return canonicalReference;
        }

        journalReferenceMappingRepository.reserveReferenceMapping(
                company.getId(),
                previewReference,
                canonicalReference,
                JOURNAL_ENTRY_MAPPING_TYPE,
                CompanyTime.now(company));

        return journalReferenceMappingRepository.findByCompanyAndLegacyReferenceIgnoreCase(company, previewReference)
                .map(JournalReferenceMapping::getCanonicalReference)
                .filter(StringUtils::hasText)
                .map(String::trim)
                .orElse(canonicalReference);
    }

    private void finalizePreviewReferenceMapping(Company company,
                                                 String providedReference,
                                                 String canonicalReference,
                                                 Long journalEntryId) {
        if (company == null || !isPreviewReference(providedReference) || !StringUtils.hasText(canonicalReference)) {
            return;
        }
        journalReferenceMappingRepository.findByCompanyAndLegacyReferenceIgnoreCase(company, providedReference.trim())
                .filter(mapping -> canonicalReference.equalsIgnoreCase(mapping.getCanonicalReference()))
                .ifPresent(mapping -> {
                    boolean changed = false;
                    if (!JOURNAL_ENTRY_MAPPING_TYPE.equalsIgnoreCase(mapping.getEntityType())) {
                        mapping.setEntityType(JOURNAL_ENTRY_MAPPING_TYPE);
                        changed = true;
                    }
                    if (journalEntryId != null && !Objects.equals(mapping.getEntityId(), journalEntryId)) {
                        mapping.setEntityId(journalEntryId);
                        changed = true;
                    }
                    if (changed) {
                        journalReferenceMappingRepository.save(mapping);
                    }
                });
    }

    private boolean isPreviewReference(String reference) {
        return StringUtils.hasText(reference)
                && reference.regionMatches(true, 0,
                PURCHASE_RETURN_PREVIEW_PREFIX, 0,
                PURCHASE_RETURN_PREVIEW_PREFIX.length());
    }

    private void validateReturnReplay(RawMaterial material,
                                      BigDecimal quantity,
                                      BigDecimal unitCost,
                                      String reference,
                                      List<RawMaterialMovement> existingMovements) {
        List<Long> materialIds = existingMovements.stream()
                .map(movement -> movement.getRawMaterial().getId())
                .distinct()
                .toList();
        if (materialIds.size() != 1 || !materialIds.get(0).equals(material.getId())) {
            throwIdempotencyConflict(material, quantity, unitCost, reference);
        }
        BigDecimal existingQty = existingMovements.stream()
                .map(RawMaterialMovement::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (existingQty.compareTo(quantity) != 0) {
            throwIdempotencyConflict(material, quantity, unitCost, reference);
        }
        BigDecimal expectedCost = unitCost != null ? unitCost : BigDecimal.ZERO;
        boolean unitCostMismatch = existingMovements.stream()
                .map(RawMaterialMovement::getUnitCost)
                .filter(Objects::nonNull)
                .anyMatch(cost -> cost.compareTo(expectedCost) != 0);
        if (unitCostMismatch) {
            throwIdempotencyConflict(material, quantity, unitCost, reference);
        }
    }

    private void throwIdempotencyConflict(RawMaterial material,
                                          BigDecimal quantity,
                                          BigDecimal unitCost,
                                          String reference) {
        throw new ApplicationException(ErrorCode.CONCURRENCY_CONFLICT,
                "Purchase return reference already used with different payload")
                .withDetail("reference", reference)
                .withDetail("rawMaterialId", material.getId())
                .withDetail("quantity", quantity)
                .withDetail("unitCost", unitCost);
    }

    private List<RawMaterialMovement> issueReturnFromBatches(RawMaterial material,
                                                              BigDecimal quantity,
                                                              BigDecimal unitCost,
                                                              String reference,
                                                              Long journalEntryId) {
        List<RawMaterialBatch> batches = rawMaterialBatchRepository.findAvailableBatchesFIFO(material);
        BigDecimal remaining = quantity;
        List<RawMaterialMovement> movements = new ArrayList<>();

        for (RawMaterialBatch batch : batches) {
            if (remaining.compareTo(BigDecimal.ZERO) <= 0) {
                break;
            }
            BigDecimal available = batch.getQuantity() != null ? batch.getQuantity() : BigDecimal.ZERO;
            if (available.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }
            BigDecimal take = available.min(remaining);
            if (take.compareTo(BigDecimal.ZERO) <= 0) {
                continue;
            }

            int updated = rawMaterialBatchRepository.deductQuantityIfSufficient(batch.getId(), take);
            if (updated == 0) {
                throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                        "Concurrent modification detected or insufficient quantity for batch " + batch.getBatchCode());
            }

            RawMaterialMovement movement = new RawMaterialMovement();
            movement.setRawMaterial(material);
            movement.setRawMaterialBatch(batch);
            movement.setReferenceType(InventoryReference.PURCHASE_RETURN);
            movement.setReferenceId(reference);
            movement.setMovementType("RETURN");
            movement.setQuantity(take);
            movement.setUnitCost(unitCost);
            movement.setJournalEntryId(journalEntryId);
            movements.add(movement);

            remaining = remaining.subtract(take);
        }

        if (remaining.compareTo(BigDecimal.ZERO) > 0) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                    "Insufficient batch availability for " + material.getName());
        }
        return movements;
    }

    private BigDecimal computeReturnTax(RawMaterialPurchase purchase,
                                        RawMaterial material,
                                        BigDecimal returnQuantity) {
        if (purchase == null || material == null || returnQuantity == null) {
            return currency(BigDecimal.ZERO);
        }
        BigDecimal purchaseTax = MoneyUtils.zeroIfNull(purchase.getTaxAmount());
        if (purchaseTax.compareTo(BigDecimal.ZERO) <= 0) {
            return currency(BigDecimal.ZERO);
        }
        BigDecimal materialLineTotal = purchase.getLines().stream()
                .filter(line -> line.getRawMaterial() != null
                        && line.getRawMaterial().getId().equals(material.getId()))
                .map(RawMaterialPurchaseLine::getLineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal materialLineQty = purchase.getLines().stream()
                .filter(line -> line.getRawMaterial() != null
                        && line.getRawMaterial().getId().equals(material.getId()))
                .map(RawMaterialPurchaseLine::getQuantity)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (materialLineTotal.compareTo(BigDecimal.ZERO) <= 0 || materialLineQty.compareTo(BigDecimal.ZERO) <= 0) {
            return currency(BigDecimal.ZERO);
        }
        boolean hasCompleteLineTaxData = purchase.getLines().stream()
                .map(RawMaterialPurchaseLine::getTaxAmount)
                .allMatch(Objects::nonNull);
        if (hasCompleteLineTaxData) {
            BigDecimal materialLineTax = purchase.getLines().stream()
                    .filter(line -> line.getRawMaterial() != null
                            && line.getRawMaterial().getId().equals(material.getId()))
                    .map(RawMaterialPurchaseLine::getTaxAmount)
                    .filter(Objects::nonNull)
                    .reduce(BigDecimal.ZERO, BigDecimal::add);
            if (materialLineTax.compareTo(BigDecimal.ZERO) <= 0) {
                return currency(BigDecimal.ZERO);
            }
            BigDecimal taxPerUnit = materialLineTax.divide(materialLineQty, 6, RoundingMode.HALF_UP);
            return currency(taxPerUnit.multiply(returnQuantity));
        }

        BigDecimal inventoryTotal = purchase.getLines().stream()
                .map(RawMaterialPurchaseLine::getLineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        if (inventoryTotal.compareTo(BigDecimal.ZERO) <= 0) {
            return currency(BigDecimal.ZERO);
        }
        BigDecimal allocationRatio = materialLineTotal.divide(inventoryTotal, 6, RoundingMode.HALF_UP);
        BigDecimal allocatedTax = purchaseTax.multiply(allocationRatio);
        BigDecimal taxPerUnit = allocatedTax.divide(materialLineQty, 6, RoundingMode.HALF_UP);
        return currency(taxPerUnit.multiply(returnQuantity));
    }

    private BigDecimal positive(BigDecimal value, String field) {
        if (value == null || value.compareTo(BigDecimal.ZERO) <= 0) {
            throw com.bigbrightpaints.erp.core.validation.ValidationUtils.invalidInput(
                    "Value for " + field + " must be greater than zero");
        }
        return value;
    }

    private BigDecimal currency(BigDecimal value) {
        return MoneyUtils.roundCurrency(value);
    }

    private String returnMemo(RawMaterial material, Supplier supplier, String reason) {
        String prefix = StringUtils.hasText(reason) ? reason.trim() : "Purchase return";
        return prefix + " - " + material.getName() + " to " + supplier.getName();
    }
}
