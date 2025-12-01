package com.bigbrightpaints.erp.modules.accounting.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bigbrightpaints.erp.core.service.NumberSequenceService;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import java.time.YearMonth;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class ReferenceNumberServiceTest {

    @Mock
    private NumberSequenceService numberSequenceService;

    private ReferenceNumberService referenceNumberService;

    @BeforeEach
    void setup() {
        referenceNumberService = new ReferenceNumberService(numberSequenceService);
    }

    @Test
    void shouldFormatJournalReferenceWithPadding() {
        Company company = new Company();
        company.setCode("ACME");
        company.setTimezone("UTC");

        YearMonth currentPeriod = YearMonth.now(ZoneId.of("UTC"));
        String period = currentPeriod.format(DateTimeFormatter.ofPattern("yyyyMM"));
        String expectedKey = "JRN-%s-%s".formatted(company.getCode(), period);

        when(numberSequenceService.nextValue(company, expectedKey)).thenReturn(5L);

        String reference = referenceNumberService.nextJournalReference(company);

        assertEquals(expectedKey + "-0005", reference);
        verify(numberSequenceService).nextValue(company, expectedKey);
    }
}
