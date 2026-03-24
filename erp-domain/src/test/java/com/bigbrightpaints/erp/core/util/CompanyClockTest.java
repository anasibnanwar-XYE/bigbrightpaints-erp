package com.bigbrightpaints.erp.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.modules.company.domain.Company;

class CompanyClockTest {

  @Test
  void today_respects_company_timezone_at_day_boundary() {
    Instant instant = Instant.parse("2026-01-26T23:30:00Z");
    CompanyClock clock = new CompanyClock(Clock.fixed(instant, ZoneOffset.UTC));

    Company ahead = new Company();
    ahead.setTimezone("Pacific/Kiritimati"); // UTC+14
    assertThat(clock.today(ahead)).isEqualTo(LocalDate.of(2026, 1, 27));

    Company behind = new Company();
    behind.setTimezone("America/Los_Angeles"); // UTC-8
    assertThat(clock.today(behind)).isEqualTo(LocalDate.of(2026, 1, 26));
  }

  @Test
  void dateForInstant_uses_company_timezone() {
    Instant instant = Instant.parse("2026-01-26T00:30:00Z");
    CompanyClock clock = new CompanyClock(Clock.fixed(instant, ZoneOffset.UTC));

    Company company = new Company();
    company.setTimezone("Pacific/Auckland"); // UTC+13 in summer

    assertThat(clock.dateForInstant(company, instant)).isEqualTo(LocalDate.of(2026, 1, 26));
  }
}
