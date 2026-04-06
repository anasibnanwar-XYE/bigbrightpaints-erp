package com.bigbrightpaints.erp.modules.sales.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.math.BigDecimal;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Map;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.ResponseEntity;
import org.springframework.mock.web.MockMultipartFile;

import com.bigbrightpaints.erp.modules.sales.dto.DealerImportResponse;
import com.bigbrightpaints.erp.modules.sales.service.DealerImportService;
import com.bigbrightpaints.erp.modules.sales.service.DealerService;
import com.bigbrightpaints.erp.modules.sales.service.DunningService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

@ExtendWith(MockitoExtension.class)
@Tag("critical")
class DealerControllerTest {

  @Mock private DealerService dealerService;
  @Mock private DealerImportService dealerImportService;
  @Mock private DunningService dunningService;

  @Test
  void holdIfOverdue_routesThroughDunningService() {
    DealerController controller =
        new DealerController(dealerService, dealerImportService, dunningService);
    ResponseEntity<ApiResponse<Map<String, Object>>> response =
        controller.holdIfOverdue(42L, 30, new BigDecimal("5000.00"));

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    verify(dunningService).evaluateDealerHold(42L, 30, new BigDecimal("5000.00"));
  }

  @Test
  void importDealers_routesThroughDealerImportService() {
    DealerController controller =
        new DealerController(dealerService, dealerImportService, dunningService);
    MockMultipartFile file =
        new MockMultipartFile(
            "file",
            "dealers.csv",
            "text/csv",
            "name,email,creditLimit,region,paymentTerms\nDealer,dealer@example.com,1000,NORTH,NET_30\n"
                .getBytes(StandardCharsets.UTF_8));
    DealerImportResponse payload =
        new DealerImportResponse(
            1, 0, List.of(new DealerImportResponse.ImportError(0L, "placeholder")));
    when(dealerImportService.importDealers(file)).thenReturn(payload);

    ResponseEntity<ApiResponse<DealerImportResponse>> response = controller.importDealers(file);

    assertThat(response.getStatusCode().is2xxSuccessful()).isTrue();
    assertThat(response.getBody()).isNotNull();
    assertThat(response.getBody().data()).isEqualTo(payload);
    verify(dealerImportService).importDealers(file);
  }
}
