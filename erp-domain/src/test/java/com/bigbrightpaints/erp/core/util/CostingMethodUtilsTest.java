package com.bigbrightpaints.erp.core.util;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.Locale;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Test;

class CostingMethodUtilsTest {

  @Test
  void isWeightedAverage_acceptsCanonicalWeightedAverageNames() {
    assertThat(CostingMethodUtils.isWeightedAverage("WAC")).isTrue();
    assertThat(CostingMethodUtils.isWeightedAverage("weighted_average")).isTrue();
    assertThat(CostingMethodUtils.isWeightedAverage("  wAc  ")).isTrue();
  }

  @Test
  void isWeightedAverage_rejectsUnsupportedValues() {
    assertThat(CostingMethodUtils.isWeightedAverage(null)).isFalse();
    assertThat(CostingMethodUtils.isWeightedAverage("")).isFalse();
    assertThat(CostingMethodUtils.isWeightedAverage("   ")).isFalse();
    assertThat(CostingMethodUtils.isWeightedAverage("FIFO")).isFalse();
    assertThat(CostingMethodUtils.isWeightedAverage("LIFO")).isFalse();
  }

  @Test
  void selectWeightedAverageValue_usesWeightedSupplierOnceForWeightedMethods() {
    AtomicInteger weightedCalls = new AtomicInteger();
    AtomicInteger nonWeightedCalls = new AtomicInteger();

    String selected =
        CostingMethodUtils.selectWeightedAverageValue(
            " weighted_average ",
            () -> {
              weightedCalls.incrementAndGet();
              return "weighted";
            },
            () -> {
              nonWeightedCalls.incrementAndGet();
              return "non-weighted";
            });

    assertThat(selected).isEqualTo("weighted");
    assertThat(weightedCalls.get()).isEqualTo(1);
    assertThat(nonWeightedCalls.get()).isZero();
  }

  @Test
  void selectWeightedAverageValue_usesNonWeightedSupplierOnceForNonWeightedMethods() {
    AtomicInteger weightedCalls = new AtomicInteger();
    AtomicInteger nonWeightedCalls = new AtomicInteger();

    String selected =
        CostingMethodUtils.selectWeightedAverageValue(
            "FIFO",
            () -> {
              weightedCalls.incrementAndGet();
              return "weighted";
            },
            () -> {
              nonWeightedCalls.incrementAndGet();
              return "non-weighted";
            });

    assertThat(selected).isEqualTo("non-weighted");
    assertThat(weightedCalls.get()).isZero();
    assertThat(nonWeightedCalls.get()).isEqualTo(1);
  }

  @Test
  void isWeightedAverage_isLocaleStable() {
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));
      assertThat(CostingMethodUtils.isWeightedAverage("weighted_average")).isTrue();
      assertThat(CostingMethodUtils.normalizeRawMaterialMethodOrDefault(" weighted_average "))
          .isEqualTo("WAC");
      assertThat(CostingMethodUtils.normalizeFinishedGoodMethodOrDefault(" weighted_average "))
          .isEqualTo("WAC");
      assertThat(CostingMethodUtils.canonicalizeFinishedGoodMethodForSync(" weighted_average "))
          .isEqualTo("WAC");
      assertThat(CostingMethodUtils.canonicalizeRawMaterialMethodForSync(" weighted_average "))
          .isEqualTo("WAC");
    } finally {
      Locale.setDefault(previous);
    }
  }

  @Test
  void normalizeRawMaterialMethodOrDefault_canonicalizesKnownValuesAndRejectsUnsupported() {
    assertThat(CostingMethodUtils.normalizeRawMaterialMethodOrDefault(null)).isEqualTo("FIFO");
    assertThat(CostingMethodUtils.normalizeRawMaterialMethodOrDefault(" weighted_average "))
        .isEqualTo("WAC");
    assertThat(CostingMethodUtils.normalizeRawMaterialMethodOrDefault("fifo")).isEqualTo("FIFO");

    assertThatThrownBy(() -> CostingMethodUtils.normalizeRawMaterialMethodOrDefault("LIFO"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported costing method");
    assertThatThrownBy(
            () -> CostingMethodUtils.normalizeRawMaterialMethodOrDefault("weighted-average"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported costing method");
  }

  @Test
  void normalizeFinishedGoodMethodOrDefault_supportsLifoAndRejectsUnsupported() {
    assertThat(CostingMethodUtils.normalizeFinishedGoodMethodOrDefault(null)).isEqualTo("FIFO");
    assertThat(CostingMethodUtils.normalizeFinishedGoodMethodOrDefault(" weighted_average "))
        .isEqualTo("WAC");
    assertThat(CostingMethodUtils.normalizeFinishedGoodMethodOrDefault(" lifo ")).isEqualTo("LIFO");

    assertThatThrownBy(() -> CostingMethodUtils.normalizeFinishedGoodMethodOrDefault("UNKNOWN"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported costing method");
  }

  @Test
  void canonicalizeFinishedGoodMethodForSync_canonicalizesKnownAndRejectsUnknown() {
    assertThat(CostingMethodUtils.canonicalizeFinishedGoodMethodForSync(" weighted_average "))
        .isEqualTo("WAC");
    assertThat(CostingMethodUtils.canonicalizeFinishedGoodMethodForSync(" lifo "))
        .isEqualTo("LIFO");
    assertThat(CostingMethodUtils.canonicalizeFinishedGoodMethodForSync(null)).isEqualTo("FIFO");

    assertThatThrownBy(
            () -> CostingMethodUtils.canonicalizeFinishedGoodMethodForSync("custom_method"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported costing method");
  }

  @Test
  void canonicalizeRawMaterialMethodForSync_canonicalizesKnownAndRejectsUnknown() {
    assertThat(CostingMethodUtils.canonicalizeRawMaterialMethodForSync(" weighted_average "))
        .isEqualTo("WAC");
    assertThat(CostingMethodUtils.canonicalizeRawMaterialMethodForSync(" fifo ")).isEqualTo("FIFO");
    assertThat(CostingMethodUtils.canonicalizeRawMaterialMethodForSync(null)).isEqualTo("FIFO");

    assertThatThrownBy(
            () -> CostingMethodUtils.canonicalizeRawMaterialMethodForSync("custom_method"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported costing method");
  }

  @Test
  void resolveFinishedGoodBatchSelectionMethod_canonicalizesSupportedAndRejectsUnknown() {
    assertThat(CostingMethodUtils.resolveFinishedGoodBatchSelectionMethod("weighted_average"))
        .isEqualTo(CostingMethodUtils.FinishedGoodBatchSelectionMethod.WAC);
    assertThat(CostingMethodUtils.resolveFinishedGoodBatchSelectionMethod("lifo"))
        .isEqualTo(CostingMethodUtils.FinishedGoodBatchSelectionMethod.LIFO);
    assertThat(CostingMethodUtils.resolveFinishedGoodBatchSelectionMethod("fifo"))
        .isEqualTo(CostingMethodUtils.FinishedGoodBatchSelectionMethod.FIFO);
    assertThat(CostingMethodUtils.resolveFinishedGoodBatchSelectionMethod(null))
        .isEqualTo(CostingMethodUtils.FinishedGoodBatchSelectionMethod.FIFO);

    assertThatThrownBy(
            () -> CostingMethodUtils.resolveFinishedGoodBatchSelectionMethod("custom_method"))
        .isInstanceOf(IllegalArgumentException.class)
        .hasMessageContaining("Unsupported costing method");
  }

  @Test
  void resolveFinishedGoodBatchSelectionMethod_isLocaleStable() {
    Locale previous = Locale.getDefault();
    try {
      Locale.setDefault(Locale.forLanguageTag("tr-TR"));
      assertThat(CostingMethodUtils.resolveFinishedGoodBatchSelectionMethod("weighted_average"))
          .isEqualTo(CostingMethodUtils.FinishedGoodBatchSelectionMethod.WAC);
      assertThat(CostingMethodUtils.resolveFinishedGoodBatchSelectionMethod("lifo"))
          .isEqualTo(CostingMethodUtils.FinishedGoodBatchSelectionMethod.LIFO);
    } finally {
      Locale.setDefault(previous);
    }
  }
}
