package com.bigbrightpaints.erp.modules.purchasing.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.jupiter.api.Test;

import com.fasterxml.jackson.databind.ObjectMapper;

class RawMaterialPurchaseRequestJsonAliasTest {

  private final ObjectMapper objectMapper = new ObjectMapper().findAndRegisterModules();

  @Test
  void deserialization_acceptsCanonicalFields() throws Exception {
    String json =
        """
        {
          "supplierId": 42,
          "invoiceNumber": "SUP-INV-2026-001",
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

    RawMaterialPurchaseRequest request =
        objectMapper.readValue(json, RawMaterialPurchaseRequest.class);

    assertThat(request.supplierId()).isEqualTo(42L);
    assertThat(request.invoiceNumber()).isEqualTo("SUP-INV-2026-001");
    assertThat(request.goodsReceiptId()).isEqualTo(77L);
    assertThat(request.lines()).hasSize(1);
    assertThat(request.lines().getFirst().rawMaterialId()).isEqualTo(11L);
  }

  @Test
  void deserialization_rejectsLegacyInvoiceAliases() {
    String json =
        """
        {
          "supplierId": 42,
          "invoiceNumber": "SUP-INV-2026-001",
          "invoiceNo": "SUP-INV-2026-001",
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
        .hasMessageContaining("Legacy field invoiceNo is not supported; use invoiceNumber");
  }

  @Test
  void deserialization_rejectsLegacyGoodsReceiptAliases() {
    String json =
        """
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
        .hasMessageContaining("Legacy field grnId is not supported; use goodsReceiptId");
  }
}
