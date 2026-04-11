package com.bigbrightpaints.erp.modules.accounting.service;

import java.nio.charset.StandardCharsets;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.LockSupport;

import org.hibernate.AssertionFailure;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

import com.bigbrightpaints.erp.core.audit.IntegrationFailureMetadataSchema;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.idempotency.IdempotencyReservationService;
import com.bigbrightpaints.erp.core.idempotency.IdempotencyUtils;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalReferenceMapping;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalReferenceMappingRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocation;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerSettlementAllocationRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.PartnerType;
import com.bigbrightpaints.erp.modules.company.domain.Company;

@Service
class JournalReplayService {

  private static final long IDEMPOTENCY_WAIT_SLEEP_MILLIS = 50L;
  private static final long IDEMPOTENCY_WAIT_TIMEOUT_MILLIS = 8000L;
  private static final int IDEMPOTENCY_LOG_HASH_LENGTH = 12;

  private final JournalEntryRepository journalEntryRepository;
  private final JournalReferenceResolver journalReferenceResolver;
  private final JournalReferenceMappingRepository journalReferenceMappingRepository;
  private final PartnerSettlementAllocationRepository settlementAllocationRepository;
  private final IdempotencyReservationService idempotencyReservationService =
      new IdempotencyReservationService();

  JournalReplayService(
      JournalEntryRepository journalEntryRepository,
      JournalReferenceResolver journalReferenceResolver,
      JournalReferenceMappingRepository journalReferenceMappingRepository,
      PartnerSettlementAllocationRepository settlementAllocationRepository) {
    this.journalEntryRepository = journalEntryRepository;
    this.journalReferenceResolver = journalReferenceResolver;
    this.journalReferenceMappingRepository = journalReferenceMappingRepository;
    this.settlementAllocationRepository = settlementAllocationRepository;
  }

  String normalizeIdempotencyMappingKey(String idempotencyKey) {
    String key = idempotencyReservationService.normalizeKey(idempotencyKey);
    if (!StringUtils.hasText(key)) {
      return "";
    }
    return key.toLowerCase(Locale.ROOT);
  }

  String sanitizeIdempotencyLogValue(String idempotencyKey) {
    if (!StringUtils.hasText(idempotencyKey)) {
      return "<empty>";
    }
    return IdempotencyUtils.sha256Hex(idempotencyKey, IDEMPOTENCY_LOG_HASH_LENGTH);
  }

  String reservedManualReference(String idempotencyKey) {
    if (!StringUtils.hasText(idempotencyKey)) {
      return "RESERVED";
    }
    String hash =
        org.springframework.util.DigestUtils.md5DigestAsHex(
            idempotencyKey.getBytes(StandardCharsets.UTF_8));
    return "RESERVED-" + hash;
  }

  boolean isRetryableManualConcurrencyFailure(Throwable throwable) {
    Throwable current = throwable;
    while (current != null) {
      if (current instanceof DataIntegrityViolationException
          || current instanceof AssertionFailure) {
        return true;
      }
      current = current.getCause();
    }
    return false;
  }

  boolean isReservedReference(String reference) {
    if (!StringUtils.hasText(reference)) {
      return false;
    }
    return reference.trim().toUpperCase(Locale.ROOT).startsWith("RESERVED-");
  }

  Optional<JournalReferenceMapping> findLatestLegacyReferenceMapping(
      Company company, String idempotencyKey) {
    if (company == null || !StringUtils.hasText(idempotencyKey)) {
      return Optional.empty();
    }
    List<JournalReferenceMapping> mappings =
        journalReferenceMappingRepository.findAllByCompanyAndLegacyReferenceIgnoreCase(
            company, idempotencyKey);
    if (mappings == null || mappings.isEmpty()) {
      return Optional.empty();
    }
    Comparator<JournalReferenceMapping> ranking =
        Comparator.comparing((JournalReferenceMapping mapping) -> mapping.getEntityId() != null)
            .thenComparing(
                JournalReferenceMapping::getCreatedAt,
                Comparator.nullsLast(Comparator.naturalOrder()))
            .thenComparing(
                JournalReferenceMapping::getId, Comparator.nullsLast(Comparator.naturalOrder()));
    return mappings.stream().max(ranking);
  }

  boolean hasExistingIdempotencyMapping(Company company, String idempotencyKey) {
    if (company == null || !StringUtils.hasText(idempotencyKey)) {
      return false;
    }
    String key = normalizeIdempotencyMappingKey(idempotencyKey);
    return findLatestLegacyReferenceMapping(company, key).isPresent();
  }

  boolean hasExistingSettlementAllocations(Company company, String idempotencyKey) {
    if (company == null || !StringUtils.hasText(idempotencyKey)) {
      return false;
    }
    return !findAllocationsByIdempotencyKey(company, idempotencyKey).isEmpty();
  }

  IdempotencyReservation reserveReferenceMapping(
      Company company, String idempotencyKey, String canonicalReference, String entityType) {
    if (company == null
        || !StringUtils.hasText(idempotencyKey)
        || !StringUtils.hasText(canonicalReference)) {
      throw new ApplicationException(
          ErrorCode.VALIDATION_MISSING_REQUIRED_FIELD,
          "Idempotency key and reference number are required to reserve journal mapping");
    }
    String key = normalizeIdempotencyMappingKey(idempotencyKey);
    String canonical = canonicalReference.trim();
    Optional<JournalReferenceMapping> existing = findLatestLegacyReferenceMapping(company, key);
    if (existing.isPresent()) {
      JournalReferenceMapping mapping = existing.get();
      if (StringUtils.hasText(mapping.getCanonicalReference())
          && !mapping.getCanonicalReference().equalsIgnoreCase(canonical)) {
        throw new ApplicationException(
                ErrorCode.CONCURRENCY_CONFLICT,
                "Idempotency key already used for another reference")
            .withDetail(IntegrationFailureMetadataSchema.KEY_IDEMPOTENCY_KEY, key)
            .withDetail("referenceNumber", mapping.getCanonicalReference());
      }
      return new IdempotencyReservation(false, canonical);
    }
    int reserved =
        journalReferenceMappingRepository.reserveReferenceMapping(
            company.getId(), key, canonical, entityType, CompanyTime.now(company));
    if (reserved == 1) {
      return new IdempotencyReservation(true, canonical);
    }
    JournalReferenceMapping mapping =
        findLatestLegacyReferenceMapping(company, key)
            .orElseThrow(
                () ->
                    new ApplicationException(
                            ErrorCode.INTERNAL_CONCURRENCY_FAILURE,
                            "Idempotency key already reserved but mapping not found")
                        .withDetail(IntegrationFailureMetadataSchema.KEY_IDEMPOTENCY_KEY, key));
    if (StringUtils.hasText(mapping.getCanonicalReference())
        && !mapping.getCanonicalReference().equalsIgnoreCase(canonical)) {
      throw new ApplicationException(
              ErrorCode.CONCURRENCY_CONFLICT, "Idempotency key already used for another reference")
          .withDetail(IntegrationFailureMetadataSchema.KEY_IDEMPOTENCY_KEY, key)
          .withDetail("referenceNumber", mapping.getCanonicalReference());
    }
    return new IdempotencyReservation(false, canonical);
  }

  void linkReferenceMapping(
      Company company, String idempotencyKey, JournalEntry entry, String entityType) {
    if (company == null || !StringUtils.hasText(idempotencyKey) || entry == null) {
      return;
    }
    String key = normalizeIdempotencyMappingKey(idempotencyKey);
    Optional<JournalReferenceMapping> mappingCandidate =
        findLatestLegacyReferenceMapping(company, key);
    if (mappingCandidate.isEmpty()) {
      JournalReferenceMapping created = new JournalReferenceMapping();
      created.setCompany(company);
      created.setLegacyReference(key);
      created.setCanonicalReference(entry.getReferenceNumber());
      created.setEntityId(entry.getId());
      if (StringUtils.hasText(entityType)) {
        created.setEntityType(entityType);
      }
      journalReferenceMappingRepository.save(created);
      return;
    }
    JournalReferenceMapping mapping = mappingCandidate.get();
    boolean canonicalMismatch =
        StringUtils.hasText(mapping.getCanonicalReference())
            && !mapping.getCanonicalReference().equalsIgnoreCase(entry.getReferenceNumber());
    boolean canRepairUnlinkedMapping = mapping.getEntityId() == null;
    if (StringUtils.hasText(mapping.getCanonicalReference())
        && canonicalMismatch
        && !isReservedReference(mapping.getCanonicalReference())
        && !canRepairUnlinkedMapping) {
      throw new ApplicationException(
              ErrorCode.CONCURRENCY_CONFLICT,
              "Idempotency key maps to a different journal reference")
          .withDetail(IntegrationFailureMetadataSchema.KEY_IDEMPOTENCY_KEY, key)
          .withDetail("referenceNumber", mapping.getCanonicalReference());
    }
    mapping.setCanonicalReference(entry.getReferenceNumber());
    mapping.setEntityId(entry.getId());
    if (StringUtils.hasText(entityType)) {
      mapping.setEntityType(entityType);
    }
    journalReferenceMappingRepository.save(mapping);
  }

  JournalEntry findExistingEntry(Company company, String reference, String idempotencyKey) {
    if (company == null) {
      return null;
    }
    String normalizedReference = StringUtils.hasText(reference) ? reference.trim() : null;
    String normalizedIdempotencyKey =
        StringUtils.hasText(idempotencyKey) ? idempotencyKey.trim() : null;
    if (StringUtils.hasText(normalizedReference)) {
      Optional<JournalEntry> byReference =
          journalReferenceResolver.findExistingEntry(company, normalizedReference);
      if (byReference.isPresent()) {
        return byReference.get();
      }
    }
    if (StringUtils.hasText(normalizedIdempotencyKey)
        && (normalizedReference == null
            || !normalizedReference.equalsIgnoreCase(normalizedIdempotencyKey))) {
      Optional<JournalEntry> byKey =
          journalReferenceResolver.findExistingEntry(company, normalizedIdempotencyKey);
      return byKey.orElse(null);
    }
    return null;
  }

  JournalEntry awaitJournalEntry(Company company, String reference, String idempotencyKey) {
    JournalEntry existing = findAwaitedJournalEntry(company, reference, idempotencyKey);
    if (existing != null || company == null || !StringUtils.hasText(idempotencyKey)) {
      return existing;
    }
    long deadline =
        System.nanoTime() + TimeUnit.MILLISECONDS.toNanos(IDEMPOTENCY_WAIT_TIMEOUT_MILLIS);
    while (System.nanoTime() < deadline) {
      LockSupport.parkNanos(TimeUnit.MILLISECONDS.toNanos(IDEMPOTENCY_WAIT_SLEEP_MILLIS));
      if (Thread.currentThread().isInterrupted()) {
        break;
      }
      existing = findAwaitedJournalEntry(company, reference, idempotencyKey);
      if (existing != null) {
        return existing;
      }
    }
    return null;
  }

  List<PartnerSettlementAllocation> awaitAllocations(Company company, String idempotencyKey) {
    if (company == null || !StringUtils.hasText(idempotencyKey)) {
      return List.of();
    }
    List<PartnerSettlementAllocation> existing =
        findAllocationsByIdempotencyKey(company, idempotencyKey);
    if (!existing.isEmpty()) {
      return existing;
    }
    JournalEntry existingEntry = awaitJournalEntry(company, null, idempotencyKey);
    if (existingEntry == null) {
      return List.of();
    }
    List<PartnerSettlementAllocation> byEntry =
        settlementAllocationRepository.findByCompanyAndJournalEntryOrderByCreatedAtAsc(
            company, existingEntry);
    return byEntry != null ? byEntry : List.of();
  }

  List<PartnerSettlementAllocation> findAllocationsByIdempotencyKey(
      Company company, String idempotencyKey) {
    if (company == null || !StringUtils.hasText(idempotencyKey)) {
      return List.of();
    }
    String key = idempotencyKey.trim();
    List<PartnerSettlementAllocation> matches =
        settlementAllocationRepository
            .findByCompanyAndIdempotencyKeyIgnoreCaseOrderByCreatedAtAscIdAsc(company, key);
    if (matches != null && !matches.isEmpty()) {
      return matches;
    }
    List<PartnerSettlementAllocation> exact =
        settlementAllocationRepository.findByCompanyAndIdempotencyKey(company, key);
    return exact != null ? exact : List.of();
  }

  JournalEntry resolveReplayJournalEntry(
      String idempotencyKey,
      JournalEntry mappingEntry,
      List<PartnerSettlementAllocation> allocations) {
    JournalEntry allocationEntry = null;
    if (allocations != null && !allocations.isEmpty()) {
      allocationEntry = allocations.getFirst().getJournalEntry();
    }
    if (mappingEntry != null
        && allocationEntry != null
        && mappingEntry.getId() != null
        && allocationEntry.getId() != null
        && !Objects.equals(mappingEntry.getId(), allocationEntry.getId())) {
      throw new ApplicationException(
              ErrorCode.CONCURRENCY_CONFLICT,
              "Idempotency mapping points to a different journal than settled allocations")
          .withDetail(IntegrationFailureMetadataSchema.KEY_IDEMPOTENCY_KEY, idempotencyKey)
          .withDetail("mappingJournalEntryId", mappingEntry.getId())
          .withDetail("allocationJournalEntryId", allocationEntry.getId());
    }
    return allocationEntry != null ? allocationEntry : mappingEntry;
  }

  JournalEntry resolveReplayJournalEntryFromExistingAllocations(
      Company company,
      String reference,
      String idempotencyKey,
      List<PartnerSettlementAllocation> allocations) {
    JournalEntry mappingEntry = findExistingEntry(company, reference, idempotencyKey);
    return resolveReplayJournalEntry(idempotencyKey, mappingEntry, allocations);
  }

  ApplicationException missingReservedPartnerAllocation(
      String subject, String idempotencyKey, PartnerType partnerType, Long partnerId) {
    ApplicationException exception =
        new ApplicationException(
                ErrorCode.INTERNAL_CONCURRENCY_FAILURE,
                subject + " idempotency key is reserved but allocation not found")
            .withDetail(IntegrationFailureMetadataSchema.KEY_IDEMPOTENCY_KEY, idempotencyKey);
    if (partnerType != null) {
      exception.withDetail(IntegrationFailureMetadataSchema.KEY_PARTNER_TYPE, partnerType.name());
    }
    if (partnerId != null) {
      exception.withDetail(IntegrationFailureMetadataSchema.KEY_PARTNER_ID, partnerId);
    }
    return exception;
  }

  private JournalEntry findAwaitedJournalEntry(
      Company company, String reference, String idempotencyKey) {
    JournalEntry existing = findExistingEntry(company, reference, idempotencyKey);
    if (existing != null || company == null || !StringUtils.hasText(idempotencyKey)) {
      return existing;
    }
    Optional<JournalReferenceMapping> mappingCandidate =
        findLatestLegacyReferenceMapping(company, normalizeIdempotencyMappingKey(idempotencyKey));
    if (mappingCandidate.isEmpty()) {
      return null;
    }
    JournalReferenceMapping mapping = mappingCandidate.get();
    if (mapping.getEntityId() != null) {
      Optional<JournalEntry> byId =
          journalEntryRepository.findByCompanyAndId(company, mapping.getEntityId());
      if (byId.isPresent()) {
        return byId.get();
      }
    }
    if (StringUtils.hasText(mapping.getCanonicalReference())
        && !isReservedReference(mapping.getCanonicalReference())) {
      Optional<JournalEntry> byCanonical =
          journalReferenceResolver.findExistingEntry(
              company, mapping.getCanonicalReference().trim());
      if (byCanonical.isPresent()) {
        return byCanonical.get();
      }
    }
    return null;
  }

  record IdempotencyReservation(boolean leader, String canonicalReference) {}
}
