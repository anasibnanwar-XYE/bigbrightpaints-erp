package com.bigbrightpaints.erp.modules.factory.service;

import org.junit.jupiter.api.Test;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;

class PackagingSizeParserTest {

    @Test
    void parseSizeInLitersSupportsUnitAliases() {
        assertThat(PackagingSizeParser.parseSizeInLiters("500ML"))
                .isEqualByComparingTo(new BigDecimal("0.500000"));
        assertThat(PackagingSizeParser.parseSizeInLiters("1L"))
                .isEqualByComparingTo(new BigDecimal("1"));
        assertThat(PackagingSizeParser.parseSizeInLiters("1LTR"))
                .isEqualByComparingTo(new BigDecimal("1"));
        assertThat(PackagingSizeParser.parseSizeInLiters("1LITRE"))
                .isEqualByComparingTo(new BigDecimal("1"));
        assertThat(PackagingSizeParser.parseSizeInLiters("1LITER"))
                .isEqualByComparingTo(new BigDecimal("1"));
    }

    @Test
    void parseSizeInLitersAllowBareNumberSupportsLegacyPackingInput() {
        assertThat(PackagingSizeParser.parseSizeInLitersAllowBareNumber("1"))
                .isEqualByComparingTo(new BigDecimal("1"));
        assertThat(PackagingSizeParser.parseSizeInLitersAllowBareNumber("1L"))
                .isEqualByComparingTo(new BigDecimal("1"));
    }

    @Test
    void parseSizeInLitersRejectsInvalidValues() {
        assertThat(PackagingSizeParser.parseSizeInLiters("SIZE"))
                .isNull();
        assertThat(PackagingSizeParser.parseSizeInLiters("1"))
                .isNull();
        assertThat(PackagingSizeParser.parseSizeInLiters("abc1L"))
                .isNull();
    }
}
