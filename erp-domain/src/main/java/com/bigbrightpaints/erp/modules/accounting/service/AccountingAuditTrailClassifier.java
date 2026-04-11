package com.bigbrightpaints.erp.modules.accounting.service;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;
import java.util.stream.Collectors;

import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryStatus;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocation;
import com.bigbrightpaints.erp.modules.invoice.domain.Invoice;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchase;

final class AccountingAuditTrailClassifier {

  List<String> moduleReferencePrefixes(String module) {
    return switch (module) {
      case "SALES" -> List.of("INV", "CRN", "SR");
      case "PURCHASING" -> List.of("RMP", "DBN", "PUR", "GRN");
      case "SETTLEMENT" ->
          List.of("SET", "RCPT", "SUP-", "SUP-SET", "SUPPLIER-SETTLEMENT", "DEALER-SETTLEMENT");
      case "PAYROLL" -> List.of("PAY", "PRL", "SAL");
      case "INVENTORY" -> List.of("ADJ", "REVAL", "WIP", "PACK", "BULK");
      case "REVERSAL", "ADJUSTMENT" -> List.of("REV", "VOID");
      default -> List.of();
    };
  }

  String deriveTransactionType(
      JournalEntry entry,
      Invoice invoice,
      RawMaterialPurchase purchase,
      List<PartnerSettlementAllocation> allocations) {
    String reference =
        entry.getReferenceNumber() != null
            ? entry.getReferenceNumber().toUpperCase(Locale.ROOT)
            : "";
    if (entry.getReversalOf() != null) {
      return "REVERSAL_ENTRY";
    }
    if (entry.getReversalEntry() != null) {
      return "REVERSED_ORIGINAL";
    }
    if (invoice != null) {
      return "SALES_INVOICE";
    }
    if (purchase != null) {
      return "PURCHASE_INVOICE";
    }
    if (allocations != null && !allocations.isEmpty()) {
      Set<String> partnerTypes =
          allocations.stream()
              .map(
                  allocation ->
                      allocation.getPartnerType() != null
                          ? allocation.getPartnerType().name()
                          : "UNKNOWN")
              .collect(Collectors.toSet());
      if (partnerTypes.size() == 1) {
        return "SETTLEMENT_" + partnerTypes.iterator().next();
      }
      return "SETTLEMENT_MIXED";
    }
    if (reference.startsWith("SUP-")
        || reference.startsWith("SUPPLIER-SETTLEMENT")
        || reference.startsWith("SUP-SET")) {
      return "SETTLEMENT_SUPPLIER";
    }
    if (reference.startsWith("SET")
        || reference.startsWith("RCPT")
        || reference.startsWith("DEALER-SETTLEMENT")) {
      return "SETTLEMENT_DEALER";
    }
    if (reference.startsWith("PAY") || reference.contains("PAYROLL")) {
      return "PAYROLL_ENTRY";
    }
    if (reference.startsWith("ADJ")
        || reference.startsWith("REVAL")
        || reference.startsWith("WIP")) {
      return "INVENTORY_ADJUSTMENT";
    }
    if (entry.getDealer() != null && entry.getSupplier() == null) {
      return "DEALER_JOURNAL";
    }
    if (entry.getSupplier() != null && entry.getDealer() == null) {
      return "SUPPLIER_JOURNAL";
    }
    return "GENERAL_JOURNAL";
  }

  String deriveModule(String transactionType, String referenceNumber) {
    String normalizedType = transactionType != null ? transactionType.toUpperCase(Locale.ROOT) : "";
    if (normalizedType.contains("SETTLEMENT")) {
      return "SETTLEMENT";
    }
    if (normalizedType.contains("SALES") || normalizedType.contains("DEALER")) {
      return "SALES";
    }
    if (normalizedType.contains("PURCHASE") || normalizedType.contains("SUPPLIER")) {
      return "PURCHASING";
    }
    if (normalizedType.contains("PAYROLL")) {
      return "PAYROLL";
    }
    if (normalizedType.contains("INVENTORY")) {
      return "INVENTORY";
    }
    if (normalizedType.contains("REVERSAL")) {
      return "ADJUSTMENT";
    }
    String reference = referenceNumber != null ? referenceNumber.toUpperCase(Locale.ROOT) : "";
    if (reference.startsWith("INV") || reference.startsWith("CRN")) {
      return "SALES";
    }
    if (reference.startsWith("RMP") || reference.startsWith("DBN")) {
      return "PURCHASING";
    }
    if (reference.startsWith("SUP-")) {
      return "SETTLEMENT";
    }
    if (reference.startsWith("SET")
        || reference.startsWith("RCPT")
        || reference.contains("SETTLEMENT")) {
      return "SETTLEMENT";
    }
    return "ACCOUNTING";
  }

  ConsistencyResult assessConsistency(
      JournalEntry entry,
      List<PartnerSettlementAllocation> allocations,
      BigDecimal totalDebit,
      BigDecimal totalCredit) {
    List<String> notes = new ArrayList<>();
    String status = "OK";
    if (totalDebit.compareTo(totalCredit) != 0) {
      notes.add("Journal is not balanced: total debit and credit differ.");
      status = "ERROR";
    }
    if (entry.getStatus() == JournalEntryStatus.POSTED && entry.getPostedAt() == null) {
      notes.add("Entry is POSTED but postedAt is null.");
      if (!"ERROR".equals(status)) {
        status = "WARNING";
      }
    }
    if (entry.getStatus() == JournalEntryStatus.REVERSED && entry.getReversalEntry() == null) {
      notes.add("Entry status is REVERSED but reversal link is missing.");
      status = "ERROR";
    }
    if (entry.getStatus() == JournalEntryStatus.VOIDED && entry.getReversalEntry() == null) {
      notes.add("Entry status is VOIDED but void reversal link is missing.");
      status = "ERROR";
    }
    String ref =
        entry.getReferenceNumber() != null
            ? entry.getReferenceNumber().toUpperCase(Locale.ROOT)
            : "";
    boolean likelySettlement =
        ref.contains("SETTLEMENT")
            || ref.startsWith("SET")
            || ref.startsWith("RCPT")
            || ref.startsWith("SUP-");
    if (likelySettlement && (allocations == null || allocations.isEmpty())) {
      notes.add("Settlement-like reference has no settlement allocation rows.");
      if (!"ERROR".equals(status)) {
        status = "WARNING";
      }
    }
    return new ConsistencyResult(status, notes);
  }

  record ConsistencyResult(String status, List<String> notes) {}
}
