package com.bigbrightpaints.erp.core.util;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;

import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.time.ZonedDateTime;

@Component
public class CompanyClock {

    private static final String DEFAULT_TIMEZONE = "UTC";

    public LocalDate today(Company company) {
        return LocalDate.now(zoneId(company));
    }

    public Instant now(Company company) {
        return ZonedDateTime.now(zoneId(company)).toInstant();
    }

    public ZoneId zoneId(Company company) {
        String timezone = company != null ? company.getTimezone() : null;
        return ZoneId.of(StringUtils.hasText(timezone) ? timezone : DEFAULT_TIMEZONE);
    }
}
