package com.bigbrightpaints.erp.modules.admin.domain;

import java.util.Optional;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ChangelogEntryRepository extends JpaRepository<ChangelogEntry, Long> {

    Page<ChangelogEntry> findByDeletedFalseOrderByPublishedAtDescIdDesc(Pageable pageable);

    Optional<ChangelogEntry> findByIdAndDeletedFalse(Long id);

    Optional<ChangelogEntry> findFirstByHighlightedTrueAndDeletedFalseOrderByPublishedAtDescIdDesc();
}
