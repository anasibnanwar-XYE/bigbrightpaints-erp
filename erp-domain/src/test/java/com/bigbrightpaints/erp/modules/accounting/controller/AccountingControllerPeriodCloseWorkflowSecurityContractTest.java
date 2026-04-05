package com.bigbrightpaints.erp.modules.accounting.controller;

import static org.assertj.core.api.Assertions.assertThat;

import java.lang.reflect.Method;

import org.junit.jupiter.api.Test;
import org.springframework.security.access.prepost.PreAuthorize;

class AccountingControllerPeriodCloseWorkflowSecurityContractTest {

  @Test
  void approveClose_requiresAdminAuthorityAnnotation() throws NoSuchMethodException {
    Method method =
        PeriodController.class.getMethod(
            "approvePeriodClose",
            Long.class,
            com.bigbrightpaints.erp.modules.accounting.dto.PeriodCloseRequestActionRequest.class);

    PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
    assertThat(preAuthorize).isNotNull();
    assertThat(preAuthorize.value()).isEqualTo("hasAuthority('ROLE_ADMIN')");
  }

  @Test
  void rejectClose_requiresAdminAuthorityAnnotation() throws NoSuchMethodException {
    Method method =
        PeriodController.class.getMethod(
            "rejectPeriodClose",
            Long.class,
            com.bigbrightpaints.erp.modules.accounting.dto.PeriodCloseRequestActionRequest.class);

    PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
    assertThat(preAuthorize).isNotNull();
    assertThat(preAuthorize.value()).isEqualTo("hasAuthority('ROLE_ADMIN')");
  }

  @Test
  void reopenPeriod_requiresSuperAdminAuthorityAnnotation() throws NoSuchMethodException {
    Method method =
        PeriodController.class.getMethod(
            "reopenPeriod",
            Long.class,
            com.bigbrightpaints.erp.modules.accounting.dto.AccountingPeriodReopenRequest.class);

    PreAuthorize preAuthorize = method.getAnnotation(PreAuthorize.class);
    assertThat(preAuthorize).isNotNull();
    assertThat(preAuthorize.value()).isEqualTo("hasAuthority('ROLE_SUPER_ADMIN')");
  }
}
