package com.bigbrightpaints.erp.modules.purchasing.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class RawMaterialPurchaseRequestJsonAliasTest {

    private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

    @Test
    void deserialization_acceptsInvoiceAndGoodsReceiptAliases() throws Exception {
        String json = """
                {
                  "supplierId": 42,
                  "invoiceNo": "SUP-INV-2026-001",
                  "invoiceDate": "2026-02-13",
                  "goodsReceiptID": 77,
                  "lines": [
                    {
                      "rawMaterialId": 11,
                      "quantity": 25.5,
                      "costPerUnit": 120.0
                    }
                  ]
                }
                """;

        RawMaterialPurchaseRequest request = objectMapper.readValue(json, RawMaterialPurchaseRequest.class);

        assertThat(request.supplierId()).isEqualTo(42L);
        assertThat(request.invoiceNumber()).isEqualTo("SUP-INV-2026-001");
        assertThat(request.goodsReceiptId()).isEqualTo(77L);
        assertThat(request.lines()).hasSize(1);
        assertThat(request.lines().getFirst().rawMaterialId()).isEqualTo(11L);
    }

    @Test
    void deserialization_rejectsConflictingInvoiceAliases() {
        String json = """
                {
                  "supplierId": 42,
                  "invoiceNumber": "SUP-INV-2026-001",
                  "invoiceNo": "SUP-INV-2026-XYZ",
                  "invoiceDate": "2026-02-13",
                  "goodsReceiptId": 77,
                  "lines": [
                    {
                      "rawMaterialId": 11,
                      "quantity": 25.5,
                      "costPerUnit": 120.0
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, RawMaterialPurchaseRequest.class))
                .hasMessageContaining("Conflicting values provided for invoiceNumber and invoiceNo");
    }

    @Test
    void deserialization_rejectsConflictingGoodsReceiptAliases() {
        String json = """
                {
                  "supplierId": 42,
                  "invoiceNumber": "SUP-INV-2026-001",
                  "invoiceDate": "2026-02-13",
                  "goodsReceiptId": 77,
                  "grnId": 88,
                  "lines": [
                    {
                      "rawMaterialId": 11,
                      "quantity": 25.5,
                      "costPerUnit": 120.0
                    }
                  ]
                }
                """;

        assertThatThrownBy(() -> objectMapper.readValue(json, RawMaterialPurchaseRequest.class))
                .hasMessageContaining("Conflicting values provided for goodsReceiptId and grnId");
    }
}
