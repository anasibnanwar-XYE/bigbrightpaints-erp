package com.bigbrightpaints.erp.modules.accounting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.YearMonth;

import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.modules.accounting.dto.GstReconciliationDto;
import com.bigbrightpaints.erp.modules.accounting.dto.GstReturnDto;
import com.bigbrightpaints.erp.modules.accounting.service.JournalEntryService;
import com.bigbrightpaints.erp.modules.accounting.service.StatementService;
import com.bigbrightpaints.erp.modules.accounting.service.TaxService;
import com.bigbrightpaints.erp.modules.accounting.service.TemporalBalanceService;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.sales.service.SalesReturnService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

class StatementReportControllerTaxEndpointsTest {

  @Test
  void generateGstReturn_trimsAndParsesPeriod() {
    TaxService taxService = mock(TaxService.class);
    StatementReportController controller = controller(taxService);
    GstReturnDto expected = new GstReturnDto();
    expected.setPeriod(YearMonth.of(2026, 3));
    when(taxService.generateGstReturn(YearMonth.of(2026, 3))).thenReturn(expected);

    ApiResponse<GstReturnDto> body = controller.generateGstReturn(" 2026-03 ").getBody();

    assertThat(body).isNotNull();
    assertThat(body.data()).isSameAs(expected);
    verify(taxService).generateGstReturn(YearMonth.of(2026, 3));
  }

  @Test
  void getGstReconciliation_allowsMissingPeriod() {
    TaxService taxService = mock(TaxService.class);
    StatementReportController controller = controller(taxService);
    GstReconciliationDto expected = new GstReconciliationDto();
    when(taxService.generateGstReconciliation(null)).thenReturn(expected);

    ApiResponse<GstReconciliationDto> body = controller.getGstReconciliation(null).getBody();

    assertThat(body).isNotNull();
    assertThat(body.data()).isSameAs(expected);
    verify(taxService).generateGstReconciliation(null);
  }

  private StatementReportController controller(TaxService taxService) {
    return new StatementReportController(
        new StatementReportControllerSupport(
            taxService,
            mock(JournalEntryService.class),
            mock(SalesReturnService.class),
            mock(StatementService.class),
            mock(TemporalBalanceService.class),
            mock(CompanyContextService.class),
            mock(CompanyClock.class),
            mock(AuditService.class)));
  }
}
