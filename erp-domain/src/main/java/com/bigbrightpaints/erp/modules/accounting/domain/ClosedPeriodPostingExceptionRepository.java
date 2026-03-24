package com.bigbrightpaints.erp.modules.accounting.domain;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface ClosedPeriodPostingExceptionRepository
    extends JpaRepository<ClosedPeriodPostingException, Long> {

  List<ClosedPeriodPostingException>
      findByCompanyAndDocumentTypeIgnoreCaseAndDocumentReferenceIgnoreCaseOrderByApprovedAtDescIdDesc(
          Company company, String documentType, String documentReference);
}
