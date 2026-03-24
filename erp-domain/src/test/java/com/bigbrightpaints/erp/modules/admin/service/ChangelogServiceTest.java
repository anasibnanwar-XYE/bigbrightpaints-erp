package com.bigbrightpaints.erp.modules.admin.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.List;
import java.util.Optional;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;

import com.bigbrightpaints.erp.core.audit.AuditService;
import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.modules.admin.domain.ChangelogEntry;
import com.bigbrightpaints.erp.modules.admin.domain.ChangelogEntryRepository;
import com.bigbrightpaints.erp.modules.admin.dto.ChangelogEntryRequest;
import com.bigbrightpaints.erp.shared.dto.PageResponse;

@ExtendWith(MockitoExtension.class)
class ChangelogServiceTest {

  @Mock private ChangelogEntryRepository changelogEntryRepository;
  @Mock private AuditService auditService;

  @Test
  void create_persistsEntryAndReturnsResponse() {
    ChangelogService service = new ChangelogService(changelogEntryRepository, auditService);
    when(changelogEntryRepository.save(any(ChangelogEntry.class)))
        .thenAnswer(
            invocation -> {
              ChangelogEntry entry = invocation.getArgument(0);
              org.springframework.test.util.ReflectionTestUtils.setField(entry, "id", 12L);
              return entry;
            });

    var response =
        service.create(
            new ChangelogEntryRequest(
                "1.2.0", "Inventory updates", "- Added a new stock audit report", true));

    assertThat(response.id()).isEqualTo(12L);
    assertThat(response.version()).isEqualTo("1.2.0");
    assertThat(response.title()).isEqualTo("Inventory updates");
    assertThat(response.body()).isEqualTo("- Added a new stock audit report");
    assertThat(response.isHighlighted()).isTrue();
    assertThat(response.createdBy()).isNotBlank();
    assertThat(response.publishedAt()).isNotNull();

    verify(changelogEntryRepository).save(any(ChangelogEntry.class));
    verify(auditService).logAuthSuccess(any(), any(), any(), any());
  }

  @Test
  void list_returnsNewestFirstPageResponse() {
    ChangelogService service = new ChangelogService(changelogEntryRepository, auditService);

    ChangelogEntry newest = entry(2L, "1.2.0", Instant.parse("2026-03-04T08:00:00Z"), true, false);
    ChangelogEntry older = entry(1L, "1.1.0", Instant.parse("2026-03-01T08:00:00Z"), false, false);
    Page<ChangelogEntry> page = new PageImpl<>(List.of(newest, older), PageRequest.of(0, 20), 2);
    when(changelogEntryRepository.findByDeletedFalseOrderByPublishedAtDescIdDesc(
            any(PageRequest.class)))
        .thenReturn(page);

    PageResponse<?> response = service.list(0, 20);

    assertThat(response.totalElements()).isEqualTo(2);
    assertThat(response.page()).isEqualTo(0);
    assertThat(response.size()).isEqualTo(20);
    assertThat(response.content()).hasSize(2);
  }

  @Test
  void latestHighlighted_returnsMostRecentHighlightedEntry() {
    ChangelogService service = new ChangelogService(changelogEntryRepository, auditService);
    ChangelogEntry highlighted =
        entry(5L, "2.0.0", Instant.parse("2026-03-04T08:00:00Z"), true, false);
    when(changelogEntryRepository
            .findFirstByHighlightedTrueAndDeletedFalseOrderByPublishedAtDescIdDesc())
        .thenReturn(Optional.of(highlighted));

    var response = service.latestHighlighted();

    assertThat(response.id()).isEqualTo(5L);
    assertThat(response.version()).isEqualTo("2.0.0");
    assertThat(response.isHighlighted()).isTrue();
  }

  @Test
  void latestHighlighted_throwsWhenNoneExists() {
    ChangelogService service = new ChangelogService(changelogEntryRepository, auditService);
    when(changelogEntryRepository
            .findFirstByHighlightedTrueAndDeletedFalseOrderByPublishedAtDescIdDesc())
        .thenReturn(Optional.empty());

    assertThatThrownBy(service::latestHighlighted)
        .isInstanceOf(ApplicationException.class)
        .satisfies(
            ex ->
                assertThat(((ApplicationException) ex).getErrorCode())
                    .isEqualTo(ErrorCode.BUSINESS_ENTITY_NOT_FOUND));
  }

  @Test
  void softDelete_marksEntryAsDeletedAndRecordsAudit() {
    ChangelogService service = new ChangelogService(changelogEntryRepository, auditService);
    ChangelogEntry entry = entry(9L, "1.3.0", Instant.parse("2026-03-03T08:00:00Z"), false, false);
    when(changelogEntryRepository.findByIdAndDeletedFalse(9L)).thenReturn(Optional.of(entry));

    service.softDelete(9L);

    assertThat(entry.isDeleted()).isTrue();
    assertThat(entry.getDeletedAt()).isNotNull();
    verify(auditService).logAuthSuccess(any(), any(), any(), any());
  }

  @Test
  void update_modifiesExistingEntry() {
    ChangelogService service = new ChangelogService(changelogEntryRepository, auditService);
    ChangelogEntry entry = entry(7L, "1.1.0", Instant.parse("2026-03-01T08:00:00Z"), false, false);
    when(changelogEntryRepository.findByIdAndDeletedFalse(7L)).thenReturn(Optional.of(entry));

    var response =
        service.update(
            7L,
            new ChangelogEntryRequest(
                "1.1.1", "Patched release", "- Fixed payroll reference issue", false));

    assertThat(response.id()).isEqualTo(7L);
    assertThat(response.version()).isEqualTo("1.1.1");
    assertThat(response.title()).isEqualTo("Patched release");
    assertThat(response.body()).isEqualTo("- Fixed payroll reference issue");
    assertThat(response.isHighlighted()).isFalse();
    verify(auditService).logAuthSuccess(any(), any(), any(), any());
  }

  private ChangelogEntry entry(
      Long id, String version, Instant publishedAt, boolean highlighted, boolean deleted) {
    ChangelogEntry entry = new ChangelogEntry();
    org.springframework.test.util.ReflectionTestUtils.setField(entry, "id", id);
    entry.setVersionLabel(version);
    entry.setTitle("Title " + version);
    entry.setBody("Body " + version);
    entry.setPublishedAt(publishedAt);
    entry.setCreatedBy("admin@bbp.com");
    entry.setHighlighted(highlighted);
    entry.setDeleted(deleted);
    entry.setUpdatedAt(publishedAt);
    return entry;
  }
}
