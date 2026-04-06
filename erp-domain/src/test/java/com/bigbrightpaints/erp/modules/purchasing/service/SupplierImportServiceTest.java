package com.bigbrightpaints.erp.modules.purchasing.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.atomic.AtomicInteger;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.modules.purchasing.dto.SupplierImportResponse;
import com.bigbrightpaints.erp.modules.purchasing.dto.SupplierRequest;

@Tag("critical")
class SupplierImportServiceTest {

  private final SupplierService supplierService = mock(SupplierService.class);
  private final SupplierImportService service = new SupplierImportService(supplierService);

  @Test
  void importSuppliers_returnsRowLevelErrorForInvalidPaymentTerms() {
    MockMultipartFile file =
        csvFile(
            "name,email,creditLimit,paymentTerms\n"
                + "Supplier One,one@example.com,1000,NET_30\n"
                + "Supplier Two,two@example.com,1200,NET_15\n");

    SupplierImportResponse response = service.importSuppliers(file);

    assertThat(response.successCount()).isEqualTo(1);
    assertThat(response.failureCount()).isEqualTo(1);
    assertThat(response.errors()).hasSize(1);
    assertThat(response.errors().getFirst().rowNumber()).isEqualTo(2L);
    assertThat(response.errors().getFirst().message())
        .contains("paymentTerms must be one of NET_30, NET_60, NET_90");
    verify(supplierService, times(1)).createSupplier(any(SupplierRequest.class));
  }

  @Test
  void importSuppliers_capturesServiceValidationErrorsPerRow() {
    AtomicInteger invocation = new AtomicInteger();
    doAnswer(
            ignored -> {
              if (invocation.incrementAndGet() == 2) {
                throw new ApplicationException(
                    ErrorCode.VALIDATION_INVALID_INPUT, "Supplier code already exists");
              }
              return null;
            })
        .when(supplierService)
        .createSupplier(any(SupplierRequest.class));

    MockMultipartFile file =
        csvFile(
            "name,email,creditLimit,paymentTerms,code\n"
                + "Supplier One,one@example.com,1000,NET_30,SUP-001\n"
                + "Supplier Duplicate,two@example.com,1200,NET_30,SUP-001\n");

    SupplierImportResponse response = service.importSuppliers(file);

    assertThat(response.successCount()).isEqualTo(1);
    assertThat(response.failureCount()).isEqualTo(1);
    assertThat(response.errors()).hasSize(1);
    assertThat(response.errors().getFirst().rowNumber()).isEqualTo(2L);
    assertThat(response.errors().getFirst().message()).isEqualTo("Supplier code already exists");
  }

  @Test
  void importSuppliers_rejectsCsvWithoutRequiredHeaders() {
    MockMultipartFile file = csvFile("name,email,paymentTerms\nSupplier One,one@example.com,NET_30\n");

    assertThatThrownBy(() -> service.importSuppliers(file))
        .isInstanceOf(ApplicationException.class)
        .hasMessageContaining("CSV is missing required headers");
  }

  private MockMultipartFile csvFile(String payload) {
    return new MockMultipartFile(
        "file", "suppliers.csv", "text/csv", payload.getBytes(StandardCharsets.UTF_8));
  }
}
