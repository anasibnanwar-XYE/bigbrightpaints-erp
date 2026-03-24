package com.bigbrightpaints.erp.modules.accounting.controller;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;

import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.modules.accounting.dto.AccountingPeriodDto;
import com.bigbrightpaints.erp.modules.accounting.dto.AccountingPeriodReopenRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.PeriodCloseRequestActionRequest;
import com.bigbrightpaints.erp.modules.accounting.dto.PeriodCloseRequestDto;
import com.bigbrightpaints.erp.modules.accounting.service.AccountingPeriodService;
import com.bigbrightpaints.erp.shared.dto.ApiResponse;

class AccountingControllerPeriodCloseWorkflowEndpointsTest {

  @Test
  void requestPeriodClose_delegatesToService() {
    AccountingPeriodService periodService = mock(AccountingPeriodService.class);
    AccountingController controller = controller(periodService);
    PeriodCloseRequestActionRequest request =
        new PeriodCloseRequestActionRequest("  prepare close  ", true);
    PeriodCloseRequestDto expected = periodCloseRequestDto(101L, "PENDING");
    when(periodService.requestPeriodClose(77L, request)).thenReturn(expected);

    ApiResponse<PeriodCloseRequestDto> body = controller.requestPeriodClose(77L, request).getBody();

    assertThat(body).isNotNull();
    assertThat(body.success()).isTrue();
    assertThat(body.message()).isEqualTo("Period close request submitted");
    assertThat(body.data()).isEqualTo(expected);
  }

  @Test
  void approvePeriodClose_delegatesToService() {
    AccountingPeriodService periodService = mock(AccountingPeriodService.class);
    AccountingController controller = controller(periodService);
    PeriodCloseRequestActionRequest request = new PeriodCloseRequestActionRequest("approved", true);
    AccountingPeriodDto expected = periodDto(77L, "CLOSED");
    when(periodService.approvePeriodClose(77L, request)).thenReturn(expected);

    ApiResponse<AccountingPeriodDto> body = controller.approvePeriodClose(77L, request).getBody();

    assertThat(body).isNotNull();
    assertThat(body.success()).isTrue();
    assertThat(body.message()).isEqualTo("Accounting period close approved");
    assertThat(body.data()).isEqualTo(expected);
  }

  @Test
  void rejectPeriodClose_delegatesToService() {
    AccountingPeriodService periodService = mock(AccountingPeriodService.class);
    AccountingController controller = controller(periodService);
    PeriodCloseRequestActionRequest request =
        new PeriodCloseRequestActionRequest("needs correction", null);
    PeriodCloseRequestDto expected = periodCloseRequestDto(102L, "REJECTED");
    when(periodService.rejectPeriodClose(77L, request)).thenReturn(expected);

    ApiResponse<PeriodCloseRequestDto> body = controller.rejectPeriodClose(77L, request).getBody();

    assertThat(body).isNotNull();
    assertThat(body.success()).isTrue();
    assertThat(body.message()).isEqualTo("Accounting period close rejected");
    assertThat(body.data()).isEqualTo(expected);
  }

  @Test
  void reopenPeriod_delegatesToService() {
    AccountingPeriodService periodService = mock(AccountingPeriodService.class);
    AccountingController controller = controller(periodService);
    AccountingPeriodReopenRequest request =
        new AccountingPeriodReopenRequest("historical correction");
    AccountingPeriodDto expected = periodDto(77L, "OPEN");
    when(periodService.reopenPeriod(77L, request)).thenReturn(expected);

    ApiResponse<AccountingPeriodDto> body = controller.reopenPeriod(77L, request).getBody();

    assertThat(body).isNotNull();
    assertThat(body.success()).isTrue();
    assertThat(body.message()).isEqualTo("Accounting period reopened");
    assertThat(body.data()).isEqualTo(expected);
  }

  private AccountingController controller(AccountingPeriodService periodService) {
    return new AccountingController(
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        periodService,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null,
        null);
  }

  private AccountingPeriodDto periodDto(Long id, String status) {
    return new AccountingPeriodDto(
        id,
        2026,
        2,
        LocalDate.of(2026, 2, 1),
        LocalDate.of(2026, 2, 28),
        "February 2026",
        status,
        false,
        null,
        null,
        false,
        null,
        null,
        status.equals("CLOSED") ? Instant.parse("2026-02-28T12:00:00Z") : null,
        status.equals("CLOSED") ? "checker.user" : null,
        status.equals("CLOSED") ? "approved" : null,
        status.equals("CLOSED") ? Instant.parse("2026-02-28T12:00:00Z") : null,
        status.equals("CLOSED") ? "checker.user" : null,
        status.equals("CLOSED") ? "approved" : null,
        status.equals("OPEN") ? Instant.parse("2026-03-01T08:00:00Z") : null,
        status.equals("OPEN") ? "super.admin" : null,
        status.equals("OPEN") ? "historical correction" : null,
        status.equals("CLOSED") ? 9901L : null,
        status.equals("CLOSED") ? "approved" : null,
        "WEIGHTED_AVERAGE");
  }

  private PeriodCloseRequestDto periodCloseRequestDto(Long id, String status) {
    return new PeriodCloseRequestDto(
        id,
        UUID.fromString("aaaaaaaa-aaaa-aaaa-aaaa-aaaaaaaaaaaa"),
        77L,
        "February 2026",
        "OPEN",
        status,
        true,
        "maker.user",
        "prepare close",
        Instant.parse("2026-02-28T10:00:00Z"),
        status.equals("PENDING") ? null : "checker.user",
        status.equals("PENDING") ? null : Instant.parse("2026-02-28T12:00:00Z"),
        status.equals("REJECTED") ? "needs correction" : null,
        status.equals("REJECTED") ? null : "approved");
  }
}
