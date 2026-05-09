package com.bigbrightpaints.erp.modules.accounting.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface JournalReferenceMappingRepository
    extends JpaRepository<JournalReferenceMapping, Long> {
  Optional<JournalReferenceMapping> findByCompanyAndReferenceKeyIgnoreCase(
      Company company, String referenceKey);

  Optional<JournalReferenceMapping> findByCompanyAndCanonicalReferenceIgnoreCase(
      Company company, String canonicalReference);

  List<JournalReferenceMapping> findAllByCompanyAndReferenceKeyIgnoreCase(
      Company company, String referenceKey);

  List<JournalReferenceMapping> findAllByCompanyAndCanonicalReferenceIgnoreCase(
      Company company, String canonicalReference);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO journal_reference_mappings (
              company_id,
              reference_key,
              canonical_reference,
              entity_type,
              created_at
          )
          VALUES (:companyId, :referenceKey, :canonicalReference, :entityType, :createdAt)
          ON CONFLICT (company_id, reference_key) DO NOTHING
          """,
      nativeQuery = true)
  int reserveManualReference(
      @Param("companyId") Long companyId,
      @Param("referenceKey") String referenceKey,
      @Param("canonicalReference") String canonicalReference,
      @Param("entityType") String entityType,
      @Param("createdAt") java.time.Instant createdAt);

  @Modifying
  @Query(
      value =
          """
          INSERT INTO journal_reference_mappings (
              company_id,
              reference_key,
              canonical_reference,
              entity_type,
              created_at
          )
          VALUES (:companyId, :referenceKey, :canonicalReference, :entityType, :createdAt)
          ON CONFLICT (company_id, reference_key) DO NOTHING
          """,
      nativeQuery = true)
  int reserveReferenceMapping(
      @Param("companyId") Long companyId,
      @Param("referenceKey") String referenceKey,
      @Param("canonicalReference") String canonicalReference,
      @Param("entityType") String entityType,
      @Param("createdAt") java.time.Instant createdAt);
}
