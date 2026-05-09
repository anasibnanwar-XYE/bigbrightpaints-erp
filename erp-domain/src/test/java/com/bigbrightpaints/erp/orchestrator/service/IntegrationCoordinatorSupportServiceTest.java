package com.bigbrightpaints.erp.orchestrator.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;

import org.junit.jupiter.api.Tag;
import org.junit.jupiter.api.Test;

import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;

@Tag("critical")
class IntegrationCoordinatorSupportServiceTest {

  private final IntegrationCoordinatorSupportService service =
      new IntegrationCoordinatorSupportService(mock(CompanyRepository.class));

  @Test
  void correlationMemoAppendsSanitizedCorrelationFieldsWhenPresent() {
    String memo = service.correlationMemo("dispatch memo", " trace-200 ", " idem-200 ");

    assertThat(memo).contains("[trace=trace-200]").contains("[idem=idem-200]");
  }

  @Test
  void correlationMemoKeepsBaseMemoWhenCorrelationFieldsAreBlank() {
    String memo = service.correlationMemo("dispatch memo", "   ", null);

    assertThat(memo).isEqualTo("dispatch memo");
  }

  @Test
  void correlationSuffixUsesSafeLogRenderingForCorrelationFields() {
    String suffix = service.correlationSuffix(" trace-201 ", " idem-201 ");

    assertThat(suffix).contains("[trace=trace-201").contains("[idem=idem-201");
  }

  @Test
  void correlationSuffixIsEmptyWhenCorrelationFieldsAreBlank() {
    String suffix = service.correlationSuffix(null, "   ");

    assertThat(suffix).isEmpty();
  }
}
