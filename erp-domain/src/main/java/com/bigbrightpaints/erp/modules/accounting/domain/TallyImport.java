package com.bigbrightpaints.erp.modules.accounting.domain;

import java.time.Instant;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;
import com.bigbrightpaints.erp.core.util.CompanyTime;
import com.bigbrightpaints.erp.modules.company.domain.Company;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.Table;
import jakarta.persistence.UniqueConstraint;

@Entity
@Table(
    name = "tally_imports",
    uniqueConstraints =
        @UniqueConstraint(
            name = "uq_tally_import_company_key",
            columnNames = {"company_id", "idempotency_key"}))
public class TallyImport extends VersionedEntity {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @ManyToOne(optional = false, fetch = FetchType.LAZY)
  @JoinColumn(name = "company_id")
  private Company company;

  @Column(name = "idempotency_key", nullable = false, length = 128)
  private String idempotencyKey;

  @Column(name = "idempotency_hash", length = 64)
  private String idempotencyHash;

  @Column(name = "reference_number", length = 128)
  private String referenceNumber;

  @Column(name = "file_hash", length = 64)
  private String fileHash;

  @Column(name = "file_name", length = 256)
  private String fileName;

  @Column(name = "journal_entry_id")
  private Long journalEntryId;

  @Column(name = "ledgers_processed", nullable = false)
  private int ledgersProcessed;

  @Column(name = "mapped_ledgers", nullable = false)
  private int mappedLedgers;

  @Column(name = "accounts_created", nullable = false)
  private int accountsCreated;

  @Column(name = "opening_voucher_entries_processed", nullable = false)
  private int openingVoucherEntriesProcessed;

  @Column(name = "opening_balance_rows_processed", nullable = false)
  private int openingBalanceRowsProcessed;

  @Column(name = "unmapped_groups_json", columnDefinition = "text")
  private String unmappedGroupsJson;

  @Column(name = "unmapped_items_json", columnDefinition = "text")
  private String unmappedItemsJson;

  @Column(name = "errors_json", columnDefinition = "text")
  private String errorsJson;

  @Column(name = "created_at", nullable = false)
  private Instant createdAt;

  @PrePersist
  public void prePersist() {
    if (createdAt == null) {
      createdAt = CompanyTime.now(company);
    }
  }

  public Long getId() {
    return id;
  }

  public Company getCompany() {
    return company;
  }

  public void setCompany(Company company) {
    this.company = company;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public void setIdempotencyKey(String idempotencyKey) {
    this.idempotencyKey = idempotencyKey;
  }

  public String getIdempotencyHash() {
    return idempotencyHash;
  }

  public void setIdempotencyHash(String idempotencyHash) {
    this.idempotencyHash = idempotencyHash;
  }

  public String getReferenceNumber() {
    return referenceNumber;
  }

  public void setReferenceNumber(String referenceNumber) {
    this.referenceNumber = referenceNumber;
  }

  public String getFileHash() {
    return fileHash;
  }

  public void setFileHash(String fileHash) {
    this.fileHash = fileHash;
  }

  public String getFileName() {
    return fileName;
  }

  public void setFileName(String fileName) {
    this.fileName = fileName;
  }

  public Long getJournalEntryId() {
    return journalEntryId;
  }

  public void setJournalEntryId(Long journalEntryId) {
    this.journalEntryId = journalEntryId;
  }

  public int getLedgersProcessed() {
    return ledgersProcessed;
  }

  public void setLedgersProcessed(int ledgersProcessed) {
    this.ledgersProcessed = ledgersProcessed;
  }

  public int getMappedLedgers() {
    return mappedLedgers;
  }

  public void setMappedLedgers(int mappedLedgers) {
    this.mappedLedgers = mappedLedgers;
  }

  public int getAccountsCreated() {
    return accountsCreated;
  }

  public void setAccountsCreated(int accountsCreated) {
    this.accountsCreated = accountsCreated;
  }

  public int getOpeningVoucherEntriesProcessed() {
    return openingVoucherEntriesProcessed;
  }

  public void setOpeningVoucherEntriesProcessed(int openingVoucherEntriesProcessed) {
    this.openingVoucherEntriesProcessed = openingVoucherEntriesProcessed;
  }

  public int getOpeningBalanceRowsProcessed() {
    return openingBalanceRowsProcessed;
  }

  public void setOpeningBalanceRowsProcessed(int openingBalanceRowsProcessed) {
    this.openingBalanceRowsProcessed = openingBalanceRowsProcessed;
  }

  public String getUnmappedGroupsJson() {
    return unmappedGroupsJson;
  }

  public void setUnmappedGroupsJson(String unmappedGroupsJson) {
    this.unmappedGroupsJson = unmappedGroupsJson;
  }

  public String getUnmappedItemsJson() {
    return unmappedItemsJson;
  }

  public void setUnmappedItemsJson(String unmappedItemsJson) {
    this.unmappedItemsJson = unmappedItemsJson;
  }

  public String getErrorsJson() {
    return errorsJson;
  }

  public void setErrorsJson(String errorsJson) {
    this.errorsJson = errorsJson;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }
}
