package com.bigbrightpaints.erp.modules.accounting.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

import org.junit.jupiter.api.Test;

class SupplierPaymentServiceTest {

  @Test
  void supplierPaymentServiceUsesFocusedValidationCollaborator() {
    Set<Class<?>> fieldTypes =
        Arrays.stream(SupplierPaymentService.class.getDeclaredFields())
            .map(field -> field.getType())
            .collect(Collectors.toSet());
    assertThat(fieldTypes).contains(SettlementTotalsValidationService.class);
    assertThat(serviceFile("SettlementRequestResolutionService.java").toFile()).doesNotExist();
    assertThat(readService("SupplierPaymentService.java"))
        .doesNotContain("SettlementRequestResolutionService");
  }

  private Path serviceFile(String name) {
    return Path.of(
        "/home/realnigga/Desktop/Mission-control/erp-domain/src/main/java/com/bigbrightpaints/erp/modules/accounting/service/"
            + name);
  }

  private String readService(String name) {
    try {
      return Files.readString(serviceFile(name));
    } catch (IOException ex) {
      throw new AssertionError(ex);
    }
  }
}
