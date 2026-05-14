package com.bigbrightpaints.erp.modules.company.domain;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.nullable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.time.LocalDate;

import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.core.util.CompanyClock;
import com.bigbrightpaints.erp.core.util.CompanyTime;

class CoATemplateTest {

  @Test
  void prePersist_setsCreatedAtFromCompanyTimeWhenMissing() {
    CompanyClock companyClock = mock(CompanyClock.class);
    Instant now = Instant.parse("2026-03-18T06:30:00Z");
    when(companyClock.now(nullable(Company.class))).thenReturn(now);
    when(companyClock.today(nullable(Company.class))).thenReturn(LocalDate.of(2026, 3, 18));
    new CompanyTime(companyClock);

    CoATemplate template = new CoATemplate();
    template.setCode("DEFAULT");
    template.setName("Default");
    template.setDescription("Default chart");
    template.setAccountCount(10);

    template.prePersist();

    assertThat(template.getCreatedAt()).isEqualTo(now);
    assertThat(template.getPublicId()).isNotNull();
  }
}
