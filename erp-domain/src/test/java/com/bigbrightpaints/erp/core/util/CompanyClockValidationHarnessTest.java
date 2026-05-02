package com.bigbrightpaints.erp.core.util;

import static org.assertj.core.api.Assertions.assertThat;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneOffset;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.core.validationharness.ValidationTimeControlService;
import com.bigbrightpaints.erp.modules.company.domain.Company;

@Tag("critical")
class CompanyClockValidationHarnessTest {

  @Test
  void validationTimeControlOverridesClockOnlyWhenServiceFreezesTime() {
    Instant systemInstant = Instant.parse("2026-04-28T01:00:00Z");
    Instant validationInstant = Instant.parse("2026-05-01T23:30:00Z");
    ValidationTimeControlService timeControl = new ValidationTimeControlService();
    CompanyClock companyClock =
        new CompanyClock(Clock.fixed(systemInstant, ZoneOffset.UTC), timeControl);
    Company kolkata = new Company();
    kolkata.setTimezone("Asia/Kolkata");

    assertThat(companyClock.now(kolkata)).isEqualTo(systemInstant);

    timeControl.freeze("m0-time-boundary", validationInstant);

    assertThat(companyClock.now(kolkata)).isEqualTo(validationInstant);
    assertThat(companyClock.today(kolkata)).isEqualTo(LocalDate.of(2026, 5, 2));
    assertThat(companyClock.dateForInstant(kolkata, systemInstant))
        .isEqualTo(LocalDate.of(2026, 5, 2));

    timeControl.clear("m0-time-boundary");
    assertThat(companyClock.now(kolkata)).isEqualTo(systemInstant);
  }
}
