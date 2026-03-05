package com.bigbrightpaints.erp.modules.factory.service;

import java.math.BigDecimal;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class BulkPackingServiceTest {

    @Test
    void parseSizeInLitersSupportsMlAndLtr() {
        assertThat(BulkPackingOrchestrator.parseSizeInLiters("500ML"))
                .isEqualByComparingTo(new BigDecimal("0.500000"));
        assertThat(BulkPackingOrchestrator.parseSizeInLiters("1LTR"))
                .isEqualByComparingTo(new BigDecimal("1"));
        assertThat(BulkPackingOrchestrator.parseSizeInLiters("0.5L"))
                .isEqualByComparingTo(new BigDecimal("0.5"));
    }

    @Test
    void parseSizeInLitersReturnsNullForInvalid() {
        assertThat(BulkPackingOrchestrator.parseSizeInLiters("SIZE"))
                .isNull();
    }
}
