package com.bigbrightpaints.erp.modules.accounting.service;

import java.time.LocalDate;
import java.util.function.Supplier;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.env.Environment;
import org.springframework.core.env.Profiles;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.modules.company.domain.Company;

@Service
class PeriodValidationService {

  private static final ThreadLocal<Boolean> SYSTEM_ENTRY_DATE_OVERRIDE =
      ThreadLocal.withInitial(() -> Boolean.FALSE);

  private final CompanyClock companyClock;
  private final Environment environment;

  @Value("${erp.benchmark.skip-date-validation:false}")
  private boolean skipDateValidation;

  PeriodValidationService(CompanyClock companyClock, Environment environment) {
    this.companyClock = companyClock;
    this.environment = environment;
  }

  void validateEntryDate(
      Company company, LocalDate entryDate, boolean overrideRequested, boolean overrideAuthorized) {
    if (entryDate == null) {
      throw new ApplicationException(ErrorCode.VALIDATION_INVALID_INPUT, "Entry date is required");
    }
    if (skipDateValidation && !isProductionProfileActive()) {
      return;
    }
    LocalDate today = currentDate(company);
    LocalDate oldestAllowed = today.minusDays(30);
    boolean future = entryDate.isAfter(today);
    boolean tooOld = entryDate.isBefore(oldestAllowed);
    if ((!overrideAuthorized) && (future || tooOld)) {
      if (overrideRequested && !overrideAuthorized) {
        String reason = future ? "the future" : "entries older than 30 days";
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT,
            "Administrator approval with a mandatory reason is required to post into " + reason);
      }
      if (future) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT, "Entry date cannot be in the future");
      }
      throw new ApplicationException(
          ErrorCode.VALIDATION_INVALID_INPUT,
          "Entry date cannot be more than 30 days old without an explicit admin exception");
    }
  }

  boolean hasEntryDateOverrideAuthority() {
    if (Boolean.TRUE.equals(SYSTEM_ENTRY_DATE_OVERRIDE.get())) {
      return true;
    }
    Authentication authentication = SecurityContextHolder.getContext().getAuthentication();
    if (authentication == null || authentication.getAuthorities() == null) {
      return false;
    }
    for (GrantedAuthority authority : authentication.getAuthorities()) {
      if ("ROLE_ADMIN".equals(authority.getAuthority())
          || "ROLE_SUPER_ADMIN".equals(authority.getAuthority())) {
        return true;
      }
    }
    return false;
  }

  <T> T runWithSystemEntryDateOverride(Supplier<T> action) {
    Boolean previous = SYSTEM_ENTRY_DATE_OVERRIDE.get();
    SYSTEM_ENTRY_DATE_OVERRIDE.set(Boolean.TRUE);
    try {
      return action.get();
    } finally {
      if (Boolean.TRUE.equals(previous)) {
        SYSTEM_ENTRY_DATE_OVERRIDE.set(Boolean.TRUE);
      } else {
        SYSTEM_ENTRY_DATE_OVERRIDE.remove();
      }
    }
  }

  LocalDate currentDate(Company company) {
    return companyClock.today(company);
  }

  private boolean isProductionProfileActive() {
    return environment != null && environment.acceptsProfiles(Profiles.of("prod"));
  }
}
