package com.bigbrightpaints.erp.modules.purchasing.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

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
}
