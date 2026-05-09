package com.bigbrightpaints.erp.modules.sales.dto;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.math.BigDecimal;
import java.util.List;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;

@Tag("critical")
class SalesOrderRequestTest {

  private SalesOrderRequest requestWithIdempotency(
      String key, String currency, String productCode, BigDecimal quantity) {
    SalesOrderItemRequest item =
        new SalesOrderItemRequest(productCode, "Item", quantity, new BigDecimal("10"), null);
    return new SalesOrderRequest(
        1L,
        new BigDecimal("100"),
        currency,
        null,
        List.of(item),
        "NONE",
        BigDecimal.ZERO,
        false,
        key);
  }

  @Test
  void resolveIdempotencyKey_prefersExplicitKeyTrimmed() {
    SalesOrderRequest request =
        requestWithIdempotency("  KEY-123  ", "INR", "FG-1", new BigDecimal("2"));
    assertThat(request.resolveIdempotencyKey()).isEqualTo("KEY-123");
  }

  @Test
  void resolveIdempotencyKey_blankKey_failsClosed() {
    SalesOrderRequest request = requestWithIdempotency(" ", "INR", "FG-1", new BigDecimal("2"));

    assertThatThrownBy(request::resolveIdempotencyKey)
        .isInstanceOf(ApplicationException.class)
        .satisfies(
            ex ->
                assertThat(((ApplicationException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD));
  }

  @Test
  void constructor_defaultsBlankPaymentModeToCredit() {
    SalesOrderRequest request =
        requestWithIdempotency("key-default", "INR", "FG-1", BigDecimal.ONE);

    assertThat(request.normalizedPaymentMode()).isEqualTo("CREDIT");
  }

  @Test
  void constructor_keepsPaymentModeCanonicalToken() {
    SalesOrderItemRequest item =
        new SalesOrderItemRequest("FG-1", "Item", new BigDecimal("2"), new BigDecimal("10"), null);
    SalesOrderRequest request =
        new SalesOrderRequest(
            7L,
            new BigDecimal("100"),
            "INR",
            null,
            List.of(item),
            "NONE",
            BigDecimal.ZERO,
            false,
            null,
            "hybrid");

    assertThat(request.normalizedPaymentMode()).isEqualTo("HYBRID");
  }

  @Test
  void resolveIdempotencyKey_supportsFinishedGoodIdSelectorWhenProductCodeMissing() {
    SalesOrderItemRequest item =
        new SalesOrderItemRequest(
            77L, null, "Item", new BigDecimal("2"), new BigDecimal("10"), BigDecimal.ZERO);
    SalesOrderRequest request =
        new SalesOrderRequest(
            7L,
            new BigDecimal("100"),
            "INR",
            null,
            List.of(item),
            "NONE",
            BigDecimal.ZERO,
            false,
            null,
            "CREDIT",
            "CUSTOM_120");

    assertThat(request.usesFinishedGoodSelection()).isTrue();
  }
}
