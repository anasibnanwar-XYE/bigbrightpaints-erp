package com.bigbrightpaints.erp.core.util;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Clock;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;

@Component
public class CompanyClock {

    private static final String DEFAULT_TIMEZONE = "UTC";

    private final Clock clock;

    /**
     * Override date for benchmark mode. When set, all calls to today() and now()
     * will return this date instead of the actual system date.
     * Format: yyyy-MM-dd (e.g., "2026-02-28")
     */
    @Value("${erp.benchmark.override-date:#{null}}")
    private String overrideDateString;

    @Autowired
    public CompanyClock(ObjectProvider<Clock> clockProvider) {
        this.clock = clockProvider.getIfAvailable(Clock::systemUTC);
    }

    CompanyClock(Clock clock) {
        this.clock = clock != null ? clock : Clock.systemUTC();
    }

    public LocalDate today(Company company) {
        if (StringUtils.hasText(overrideDateString)) {
            return LocalDate.parse(overrideDateString);
        }
        return LocalDate.now(clock.withZone(zoneId(company)));
    }

    public Instant now(Company company) {
        if (StringUtils.hasText(overrideDateString)) {
            LocalDate overrideDate = LocalDate.parse(overrideDateString);
            return overrideDate.atStartOfDay(zoneId(company)).toInstant();
        }
        return Instant.now(clock.withZone(zoneId(company)));
    }

    public LocalDate dateForInstant(Company company, Instant instant) {
        if (StringUtils.hasText(overrideDateString)) {
            return LocalDate.parse(overrideDateString);
        }
        if (instant == null) {
            return today(company);
        }
        return LocalDate.ofInstant(instant, zoneId(company));
    }

    public ZoneId zoneId(Company company) {
        String timezone = company != null ? company.getTimezone() : null;
        return ZoneId.of(StringUtils.hasText(timezone) ? timezone : DEFAULT_TIMEZONE);
    }
}
