package com.bigbrightpaints.erp.modules.accounting.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class SupplierSettlementServiceTest {

  private static final Path SERVICE_DIR =
      Path.of(
              System.getProperty("user.dir"),
              "src/main/java/com/bigbrightpaints/erp/modules/accounting/service")
          .toAbsolutePath()
          .normalize();

  @Test
  void supplierSettlementServiceUsesFocusedResolutionCollaborators() {
    Set<Class<?>> fieldTypes =
        Arrays.stream(SupplierSettlementService.class.getDeclaredFields())
            .map(field -> field.getType())
            .collect(Collectors.toSet());
    assertThat(fieldTypes)
        .contains(
            SettlementAllocationResolutionService.class,
            SettlementTotalsValidationService.class,
            SettlementJournalLineDraftService.class);
    assertThat(serviceFile("SettlementRequestResolutionService.java").toFile()).doesNotExist();
    assertThat(readService("SupplierSettlementService.java"))
        .doesNotContain("SettlementRequestResolutionService");
  }

  private Path serviceFile(String name) {
    return SERVICE_DIR.resolve(name);
  }

  private String readService(String name) {
    try {
      return Files.readString(serviceFile(name));
    } catch (IOException ex) {
      throw new AssertionError(ex);
    }
  }
}
