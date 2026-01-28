package com.bigbrightpaints.erp.core.util;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDate;

/**
 * Static access to CompanyClock for domain/entity lifecycle hooks.
 * Defaults to a UTC CompanyClock in non-Spring contexts.
 */
@Component
public class CompanyTime {

    private static volatile CompanyClock companyClock;

    public CompanyTime(CompanyClock companyClock) {
        CompanyTime.companyClock = companyClock;
    }

    public static Instant now(Company company) {
        return requireClock().now(company);
    }

    public static Instant now() {
        return requireClock().now(null);
    }

    public static LocalDate today(Company company) {
        return requireClock().today(company);
    }

    public static LocalDate today() {
        return requireClock().today(null);
    }

    private static CompanyClock requireClock() {
        if (companyClock == null) {
            synchronized (CompanyTime.class) {
                if (companyClock == null) {
                    companyClock = new CompanyClock((java.time.Clock) null);
                }
            }
        }
        return companyClock;
    }
}
