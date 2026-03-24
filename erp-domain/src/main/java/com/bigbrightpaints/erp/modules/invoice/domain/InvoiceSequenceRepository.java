package com.bigbrightpaints.erp.modules.invoice.domain;

import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;

import com.bigbrightpaints.erp.modules.company.domain.Company;

import jakarta.persistence.LockModeType;

public interface InvoiceSequenceRepository extends JpaRepository<InvoiceSequence, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<InvoiceSequence> findByCompanyAndFiscalYear(Company company, Integer fiscalYear);
}
