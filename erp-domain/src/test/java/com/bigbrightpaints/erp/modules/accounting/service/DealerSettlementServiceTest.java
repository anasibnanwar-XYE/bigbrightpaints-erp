package com.bigbrightpaints.erp.modules.accounting.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class DealerSettlementServiceTest {

  @Test
  void settlementWritersUseFocusedResolutionCollaborators() {
    Set<Class<?>> fieldTypes =
        Arrays.stream(DealerSettlementService.class.getDeclaredFields())
            .map(field -> field.getType())
            .collect(Collectors.toSet());
    assertThat(fieldTypes)
        .contains(
            SettlementAllocationResolutionService.class,
            SettlementTotalsValidationService.class,
            SettlementJournalLineDraftService.class);
  }
}
