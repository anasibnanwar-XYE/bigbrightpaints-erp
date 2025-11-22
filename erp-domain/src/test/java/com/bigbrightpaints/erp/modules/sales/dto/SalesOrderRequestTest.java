package com.bigbrightpaints.erp.modules.sales.dto;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SalesOrderRequestTest {

    @Test
    void resolveIdempotencyKey_isDeterministicForSamePayload() {
        SalesOrderItemRequest item = new SalesOrderItemRequest(
                "SKU-1",
                "Desc",
                new BigDecimal("2"),
                new BigDecimal("10.00"),
                null
        );
        SalesOrderRequest req1 = new SalesOrderRequest(
                1L,
                new BigDecimal("20.00"),
                "INR",
                "Note",
                List.of(item),
                "NONE",
                null,
                false,
                null
        );
        SalesOrderRequest req2 = new SalesOrderRequest(
                1L,
                new BigDecimal("20.00"),
                "INR",
                "Note",
                List.of(item),
                "NONE",
                null,
                false,
                null
        );

        assertThat(req1.resolveIdempotencyKey()).isEqualTo(req2.resolveIdempotencyKey());
    }

    @Test
    void resolveIdempotencyKey_changesWhenItemsChange() {
        SalesOrderItemRequest item1 = new SalesOrderItemRequest(
                "SKU-1", "Desc", new BigDecimal("2"), new BigDecimal("10.00"), null);
        SalesOrderItemRequest item2 = new SalesOrderItemRequest(
                "SKU-1", "Desc", new BigDecimal("3"), new BigDecimal("10.00"), null);

        SalesOrderRequest req1 = new SalesOrderRequest(
                1L, new BigDecimal("20.00"), "INR", "Note", List.of(item1), "NONE", null, false, null);
        SalesOrderRequest req2 = new SalesOrderRequest(
                1L, new BigDecimal("30.00"), "INR", "Note", List.of(item2), "NONE", null, false, null);

        assertThat(req1.resolveIdempotencyKey()).isNotEqualTo(req2.resolveIdempotencyKey());
    }
}
