package com.bigbrightpaints.erp.modules.invoice.service;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.transaction.annotation.Transactional;
import org.thymeleaf.TemplateEngine;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;
import com.bigbrightpaints.erp.modules.company.service.TenantRealActionUsageService;

@ExtendWith(MockitoExtension.class)
class InvoicePdfServiceQuotaTest {

  @Mock private CompanyContextService companyContextService;
  @Mock private CompanyScopedInvoiceLookupService invoiceLookupService;
  @Mock private TemplateEngine templateEngine;
  @Mock private TenantRealActionUsageService realActionUsageService;

  @Test
  void renderInvoicePdfUsesWritableTransactionForDurableUsageRecording() throws Exception {
    Transactional transactional =
        InvoicePdfService.class
            .getMethod("renderInvoicePdf", Long.class)
            .getAnnotation(Transactional.class);

    org.assertj.core.api.Assertions.assertThat(transactional).isNotNull();
    org.assertj.core.api.Assertions.assertThat(transactional.readOnly()).isFalse();
  }

  @Test
  void renderInvoicePdfEnforcesPdfQuotaBeforeInvoiceLookupAndRenderSideEffects() {
    Company company = new Company();
    ReflectionTestUtils.setField(company, "id", 7L);
    company.setCode("ACME");
    InvoicePdfService service =
        new InvoicePdfService(
            companyContextService, invoiceLookupService, templateEngine, realActionUsageService);
    ApplicationException quotaExceeded =
        new ApplicationException(ErrorCode.BUSINESS_LIMIT_EXCEEDED, "PDF exports quota exhausted");
    when(companyContextService.requireCurrentCompany()).thenReturn(company);
    org.mockito.Mockito.doThrow(quotaExceeded)
        .when(realActionUsageService)
        .enforcePdfExportAllowed(company);

    assertThatThrownBy(() -> service.renderInvoicePdf(42L)).isSameAs(quotaExceeded);

    verify(invoiceLookupService, never())
        .requireInvoicePdf(org.mockito.Mockito.any(), org.mockito.Mockito.any());
  }
}
