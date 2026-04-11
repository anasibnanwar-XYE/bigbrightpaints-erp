package com.bigbrightpaints.erp.core.util;

import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.modules.company.domain.Company;

@Service
public class CompanyScopedLookupService {

  public <T> T require(
      Company company, Long id, CompanyScopedEntityFinder<T> finder, String entityLabel) {
    return finder
        .find(company, id)
        .orElseThrow(
            () ->
                new ApplicationException(
                        ErrorCode.VALIDATION_INVALID_REFERENCE, entityLabel + " not found")
                    .withDetail("entity", entityLabel)
                    .withDetail("id", id)
                    .withDetail("companyId", company != null ? company.getId() : null));
  }
}
