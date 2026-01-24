package com.bigbrightpaints.erp.modules.accounting.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface JournalReferenceMappingRepository extends JpaRepository<JournalReferenceMapping, Long> {
    Optional<JournalReferenceMapping> findByCompanyAndLegacyReferenceIgnoreCase(Company company, String legacyReference);
    Optional<JournalReferenceMapping> findByCompanyAndCanonicalReferenceIgnoreCase(Company company, String canonicalReference);
    List<JournalReferenceMapping> findAllByCompanyAndLegacyReferenceIgnoreCase(Company company, String legacyReference);
    List<JournalReferenceMapping> findAllByCompanyAndCanonicalReferenceIgnoreCase(Company company, String canonicalReference);
}
