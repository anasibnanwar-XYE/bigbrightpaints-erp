package com.bigbrightpaints.erp.test.support;

import java.time.LocalDate;
import java.time.ZoneId;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public final class TestDateUtils {

  private TestDateUtils() {}

  public static LocalDate safeDate(Company company) {
    String tz = company != null && company.getTimezone() != null ? company.getTimezone() : "UTC";
    LocalDate today = LocalDate.now(ZoneId.of(tz));
    return today.minusDays(2);
  }

  public static String safeDateIso(Company company) {
    return safeDate(company).toString();
  }
}
