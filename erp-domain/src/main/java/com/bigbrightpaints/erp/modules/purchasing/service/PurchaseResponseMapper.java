package com.bigbrightpaints.erp.modules.purchasing.service;

import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.inventory.domain.RawMaterial;
import com.bigbrightpaints.erp.modules.purchasing.domain.GoodsReceipt;
import com.bigbrightpaints.erp.modules.purchasing.domain.GoodsReceiptLine;
import com.bigbrightpaints.erp.modules.purchasing.domain.PurchaseOrder;
import com.bigbrightpaints.erp.modules.purchasing.domain.PurchaseOrderLine;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchase;
import com.bigbrightpaints.erp.modules.purchasing.domain.RawMaterialPurchaseLine;
import com.bigbrightpaints.erp.modules.purchasing.domain.Supplier;
import com.bigbrightpaints.erp.modules.purchasing.dto.GoodsReceiptLineResponse;
import com.bigbrightpaints.erp.modules.purchasing.dto.GoodsReceiptResponse;
import com.bigbrightpaints.erp.modules.purchasing.dto.PurchaseOrderLineResponse;
import com.bigbrightpaints.erp.modules.purchasing.dto.PurchaseOrderResponse;
import com.bigbrightpaints.erp.modules.purchasing.dto.RawMaterialPurchaseLineResponse;
import com.bigbrightpaints.erp.modules.purchasing.dto.RawMaterialPurchaseResponse;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.List;
import java.util.Objects;

@Component
public class PurchaseResponseMapper {

    public RawMaterialPurchaseResponse toPurchaseResponse(RawMaterialPurchase purchase) {
        JournalEntry journalEntry = purchase.getJournalEntry();
        Supplier supplier = purchase.getSupplier();
        PurchaseOrder purchaseOrder = purchase.getPurchaseOrder();
        GoodsReceipt goodsReceipt = purchase.getGoodsReceipt();
        List<RawMaterialPurchaseLineResponse> lines = purchase.getLines().stream()
                .map(this::toPurchaseLineResponse)
                .toList();
        return new RawMaterialPurchaseResponse(
                purchase.getId(),
                purchase.getPublicId(),
                purchase.getInvoiceNumber(),
                purchase.getInvoiceDate(),
                purchase.getTotalAmount(),
                purchase.getTaxAmount(),
                purchase.getOutstandingAmount(),
                purchase.getStatus(),
                purchase.getMemo(),
                supplier != null ? supplier.getId() : null,
                supplier != null ? supplier.getCode() : null,
                supplier != null ? supplier.getName() : null,
                purchaseOrder != null ? purchaseOrder.getId() : null,
                purchaseOrder != null ? purchaseOrder.getOrderNumber() : null,
                goodsReceipt != null ? goodsReceipt.getId() : null,
                goodsReceipt != null ? goodsReceipt.getReceiptNumber() : null,
                journalEntry != null ? journalEntry.getId() : null,
                purchase.getCreatedAt(),
                lines
        );
    }

    public RawMaterialPurchaseLineResponse toPurchaseLineResponse(RawMaterialPurchaseLine line) {
        RawMaterial material = line.getRawMaterial();
        return new RawMaterialPurchaseLineResponse(
                material != null ? material.getId() : null,
                material != null ? material.getName() : null,
                line.getRawMaterialBatch() != null ? line.getRawMaterialBatch().getId() : null,
                line.getBatchCode(),
                line.getQuantity(),
                line.getUnit(),
                line.getCostPerUnit(),
                line.getLineTotal(),
                line.getTaxRate(),
                line.getTaxAmount(),
                line.getNotes(),
                line.getCgstAmount(),
                line.getSgstAmount(),
                line.getIgstAmount()
        );
    }

    public PurchaseOrderResponse toPurchaseOrderResponse(PurchaseOrder order) {
        Supplier supplier = order.getSupplier();
        List<PurchaseOrderLineResponse> lines = order.getLines().stream()
                .map(this::toPurchaseOrderLineResponse)
                .toList();
        BigDecimal totalAmount = lines.stream()
                .map(PurchaseOrderLineResponse::lineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new PurchaseOrderResponse(
                order.getId(),
                order.getPublicId(),
                order.getOrderNumber(),
                order.getOrderDate(),
                totalAmount,
                order.getStatus(),
                order.getMemo(),
                supplier != null ? supplier.getId() : null,
                supplier != null ? supplier.getCode() : null,
                supplier != null ? supplier.getName() : null,
                order.getCreatedAt(),
                lines
        );
    }

    public PurchaseOrderLineResponse toPurchaseOrderLineResponse(PurchaseOrderLine line) {
        RawMaterial material = line.getRawMaterial();
        return new PurchaseOrderLineResponse(
                material != null ? material.getId() : null,
                material != null ? material.getName() : null,
                line.getQuantity(),
                line.getUnit(),
                line.getCostPerUnit(),
                line.getLineTotal(),
                line.getNotes()
        );
    }

    public GoodsReceiptResponse toGoodsReceiptResponse(GoodsReceipt receipt) {
        Supplier supplier = receipt.getSupplier();
        PurchaseOrder purchaseOrder = receipt.getPurchaseOrder();
        List<GoodsReceiptLineResponse> lines = receipt.getLines().stream()
                .map(this::toGoodsReceiptLineResponse)
                .toList();
        BigDecimal totalAmount = lines.stream()
                .map(GoodsReceiptLineResponse::lineTotal)
                .filter(Objects::nonNull)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        return new GoodsReceiptResponse(
                receipt.getId(),
                receipt.getPublicId(),
                receipt.getReceiptNumber(),
                receipt.getReceiptDate(),
                totalAmount,
                receipt.getStatus(),
                receipt.getMemo(),
                supplier != null ? supplier.getId() : null,
                supplier != null ? supplier.getCode() : null,
                supplier != null ? supplier.getName() : null,
                purchaseOrder != null ? purchaseOrder.getId() : null,
                purchaseOrder != null ? purchaseOrder.getOrderNumber() : null,
                receipt.getCreatedAt(),
                lines
        );
    }

    public GoodsReceiptLineResponse toGoodsReceiptLineResponse(GoodsReceiptLine line) {
        RawMaterial material = line.getRawMaterial();
        return new GoodsReceiptLineResponse(
                material != null ? material.getId() : null,
                material != null ? material.getName() : null,
                line.getBatchCode(),
                line.getQuantity(),
                line.getUnit(),
                line.getCostPerUnit(),
                line.getLineTotal(),
                line.getNotes()
        );
    }
}
