package com.bigbrightpaints.erp.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.math.RoundingMode;

import org.junit.jupiter.api.Test;

class MoneyUtilsTest {

  @Test
  void zeroIfNull_returnsZero() {
    assertThat(MoneyUtils.zeroIfNull(null)).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void zeroIfNull_returnsValue() {
    BigDecimal value = new BigDecimal("12.50");
    assertThat(MoneyUtils.zeroIfNull(value)).isEqualByComparingTo(value);
  }

  @Test
  void safeMultiply_leftNull_returnsZero() {
    assertThat(MoneyUtils.safeMultiply(null, new BigDecimal("5")))
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void safeMultiply_rightNull_returnsZero() {
    assertThat(MoneyUtils.safeMultiply(new BigDecimal("5"), null))
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void safeMultiply_multipliesValues() {
    assertThat(MoneyUtils.safeMultiply(new BigDecimal("2.5"), new BigDecimal("4")))
        .isEqualByComparingTo(new BigDecimal("10.0"));
  }

  @Test
  void safeAdd_nulls_returnsZero() {
    assertThat(MoneyUtils.safeAdd(null, null)).isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void safeAdd_leftNull_returnsRight() {
    assertThat(MoneyUtils.safeAdd(null, new BigDecimal("7.25")))
        .isEqualByComparingTo(new BigDecimal("7.25"));
  }

  @Test
  void safeDivide_nullDividend_returnsZero() {
    assertThat(MoneyUtils.safeDivide(null, new BigDecimal("3"), 2, RoundingMode.HALF_UP))
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void safeDivide_nullDivisor_returnsZero() {
    assertThat(MoneyUtils.safeDivide(new BigDecimal("3"), null, 2, RoundingMode.HALF_UP))
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void safeDivide_zeroDivisor_returnsZero() {
    assertThat(MoneyUtils.safeDivide(new BigDecimal("3"), BigDecimal.ZERO, 2, RoundingMode.HALF_UP))
        .isEqualByComparingTo(BigDecimal.ZERO);
  }

  @Test
  void safeDivide_scalesAndRounds() {
    assertThat(
            MoneyUtils.safeDivide(
                new BigDecimal("10"), new BigDecimal("3"), 2, RoundingMode.HALF_UP))
        .isEqualByComparingTo(new BigDecimal("3.33"));
  }

  @Test
  void withinTolerance_exactMatch_true() {
    assertThat(
            MoneyUtils.withinTolerance(
                new BigDecimal("10"), new BigDecimal("10"), new BigDecimal("0.01")))
        .isTrue();
  }

  @Test
  void withinTolerance_withinDelta_true() {
    assertThat(
            MoneyUtils.withinTolerance(
                new BigDecimal("10.00"), new BigDecimal("10.05"), new BigDecimal("0.10")))
        .isTrue();
  }

  @Test
  void withinTolerance_outsideDelta_false() {
    assertThat(
            MoneyUtils.withinTolerance(
                new BigDecimal("10.00"), new BigDecimal("10.20"), new BigDecimal("0.10")))
        .isFalse();
  }
}
