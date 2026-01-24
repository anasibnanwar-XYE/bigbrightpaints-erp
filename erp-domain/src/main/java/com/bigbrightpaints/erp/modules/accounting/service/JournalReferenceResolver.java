package com.bigbrightpaints.erp.modules.accounting.service;

import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntryRepository;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalReferenceMapping;
import com.bigbrightpaints.erp.modules.accounting.domain.JournalReferenceMappingRepository;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class JournalReferenceResolver {

    private final JournalEntryRepository journalEntryRepository;
    private final JournalReferenceMappingRepository journalReferenceMappingRepository;

    public JournalReferenceResolver(JournalEntryRepository journalEntryRepository,
                                    JournalReferenceMappingRepository journalReferenceMappingRepository) {
        this.journalEntryRepository = journalEntryRepository;
        this.journalReferenceMappingRepository = journalReferenceMappingRepository;
    }

    public Optional<JournalEntry> findExistingEntry(Company company, String reference) {
        if (company == null || !StringUtils.hasText(reference)) {
            return Optional.empty();
        }
        String trimmed = reference.trim();
        Optional<JournalEntry> direct = journalEntryRepository.findByCompanyAndReferenceNumber(company, trimmed);
        if (direct.isPresent()) {
            return direct;
        }
        List<JournalReferenceMapping> legacyMappings = journalReferenceMappingRepository
                .findAllByCompanyAndLegacyReferenceIgnoreCase(company, trimmed);
        if (legacyMappings.size() > 1) {
            throw new IllegalStateException("Multiple journal reference mappings for legacy reference '" + trimmed + "'");
        }
        Optional<JournalReferenceMapping> legacy = legacyMappings.stream().findFirst();
        if (legacy.isPresent()) {
            String canonical = legacy.get().getCanonicalReference();
            if (StringUtils.hasText(canonical) && !canonical.equalsIgnoreCase(trimmed)) {
                Optional<JournalEntry> canonicalEntry = journalEntryRepository
                        .findByCompanyAndReferenceNumber(company, canonical);
                if (canonicalEntry.isPresent()) {
                    return canonicalEntry;
                }
            }
        }
        List<JournalReferenceMapping> canonicalMappings = journalReferenceMappingRepository
                .findAllByCompanyAndCanonicalReferenceIgnoreCase(company, trimmed);
        if (canonicalMappings.size() > 1) {
            throw new IllegalStateException("Multiple journal reference mappings for canonical reference '" + trimmed + "'");
        }
        Optional<JournalReferenceMapping> canonical = canonicalMappings.stream().findFirst();
        if (canonical.isPresent()) {
            String legacyRef = canonical.get().getLegacyReference();
            if (StringUtils.hasText(legacyRef) && !legacyRef.equalsIgnoreCase(trimmed)) {
                Optional<JournalEntry> legacyEntry = journalEntryRepository
                        .findByCompanyAndReferenceNumber(company, legacyRef);
                if (legacyEntry.isPresent()) {
                    return legacyEntry;
                }
            }
        }
        return Optional.empty();
    }

    public boolean exists(Company company, String reference) {
        return findExistingEntry(company, reference).isPresent();
    }
}
