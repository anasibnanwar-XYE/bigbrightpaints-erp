package com.bigbrightpaints.erp.core.util;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;

import org.springframework.stereotype.Component;

import com.bigbrightpaints.erp.modules.company.domain.Company;

/**
 * Static access to CompanyClock for domain/entity lifecycle hooks.
 * Defaults to a UTC CompanyClock in non-Spring contexts.
 */
@Component
public class CompanyTime {

  private static volatile CompanyClock companyClock;
  private static final CompanyClock SYSTEM_CLOCK = new CompanyClock((Clock) null);

  public CompanyTime(CompanyClock companyClock) {
    CompanyTime.companyClock = companyClock;
  }

  public static Instant now(Company company) {
    Instant now = requireClock().now(company);
    return now != null ? now : SYSTEM_CLOCK.now(company);
  }

  public static Instant now() {
    Instant now = requireClock().now(null);
    return now != null ? now : SYSTEM_CLOCK.now(null);
  }

  public static LocalDate today(Company company) {
    LocalDate today = requireClock().today(company);
    return today != null ? today : SYSTEM_CLOCK.today(company);
  }

  public static LocalDate today() {
    LocalDate today = requireClock().today(null);
    return today != null ? today : SYSTEM_CLOCK.today(null);
  }

  private static CompanyClock requireClock() {
    if (companyClock == null) {
      synchronized (CompanyTime.class) {
        if (companyClock == null) {
          companyClock = defaultClock();
        }
      }
    }
    return companyClock;
  }

  private static CompanyClock defaultClock() {
    return SYSTEM_CLOCK;
  }
}
