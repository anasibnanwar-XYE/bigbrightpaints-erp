package com.bigbrightpaints.erp.core.auditaccess;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

class AuditFeedFilterTest {

  @Test
  void fetchLimit_growsWithRequestedWindowBeyondFiveThousandRows() {
    AuditFeedFilter filter = new AuditFeedFilter(null, null, null, null, null, null, null, null, 30, 200);

    assertThat(filter.fetchLimit()).isEqualTo(6200);
  }
}
