package com.bigbrightpaints.erp.modules.sales.dto;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

class SalesTargetRequestTest {

  @Test
  void canonicalConstructor_exposesAllAccessors() {
    SalesTargetRequest request =
        new SalesTargetRequest(
            "Q1 Dealer Growth",
            LocalDate.of(2026, 1, 1),
            LocalDate.of(2026, 3, 31),
            new BigDecimal("150000.00"),
            new BigDecimal("25000.00"),
            "dealer.ops",
            "Annual target rollout");

    assertThat(request.name()).isEqualTo("Q1 Dealer Growth");
    assertThat(request.periodStart()).isEqualTo(LocalDate.of(2026, 1, 1));
    assertThat(request.periodEnd()).isEqualTo(LocalDate.of(2026, 3, 31));
    assertThat(request.targetAmount()).isEqualByComparingTo("150000.00");
    assertThat(request.achievedAmount()).isEqualByComparingTo("25000.00");
    assertThat(request.assignee()).isEqualTo("dealer.ops");
    assertThat(request.changeReason()).isEqualTo("Annual target rollout");
  }

  @Test
  void equalsAndHashCode_sameFieldValues_areEqual() {
    SalesTargetRequest one =
        new SalesTargetRequest(
            "Q2 Dealer Growth",
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 6, 30),
            new BigDecimal("200000.00"),
            new BigDecimal("50000.00"),
            "regional.manager",
            "Approved forecast");
    SalesTargetRequest two =
        new SalesTargetRequest(
            "Q2 Dealer Growth",
            LocalDate.of(2026, 4, 1),
            LocalDate.of(2026, 6, 30),
            new BigDecimal("200000.00"),
            new BigDecimal("50000.00"),
            "regional.manager",
            "Approved forecast");

    assertThat(one).isEqualTo(two);
    assertThat(one.hashCode()).isEqualTo(two.hashCode());
  }

  @Test
  void equalsAndHashCode_whenAnyComponentDiffers_areNotEqual() {
    SalesTargetRequest baseline =
        new SalesTargetRequest(
            "Q3 Dealer Growth",
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 9, 30),
            new BigDecimal("175000.00"),
            null,
            "south.zone",
            "Mid-year plan");
    SalesTargetRequest differentAssignee =
        new SalesTargetRequest(
            "Q3 Dealer Growth",
            LocalDate.of(2026, 7, 1),
            LocalDate.of(2026, 9, 30),
            new BigDecimal("175000.00"),
            null,
            "north.zone",
            "Mid-year plan");

    assertThat(baseline).isNotEqualTo(differentAssignee);
    assertThat(baseline.hashCode()).isNotEqualTo(differentAssignee.hashCode());
  }

  @Test
  void toString_includesNamedComponents() {
    SalesTargetRequest request =
        new SalesTargetRequest(
            "Q4 Dealer Growth",
            LocalDate.of(2026, 10, 1),
            LocalDate.of(2026, 12, 31),
            new BigDecimal("225000.00"),
            new BigDecimal("0.00"),
            "west.zone",
            "Year-end reset");

    assertThat(request.toString()).contains("name=Q4 Dealer Growth");
    assertThat(request.toString()).contains("assignee=west.zone");
  }
}
