package com.bigbrightpaints.erp.modules.hr.domain;

import com.bigbrightpaints.erp.modules.accounting.domain.JournalEntry;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import jakarta.persistence.*;
import java.math.BigDecimal;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import com.bigbrightpaints.erp.core.domain.VersionedEntity;

@Entity
@Table(name = "payroll_runs")
public class PayrollRun extends VersionedEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "public_id", nullable = false)
    private UUID publicId;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id")
    private Company company;

    @Column(name = "run_date", nullable = false)
    private LocalDate runDate;

    @Column(nullable = false)
    private String status = "DRAFT";

    private String processedBy;

    private String notes;

    @Column(name = "total_amount", nullable = false)
    private BigDecimal totalAmount = BigDecimal.ZERO;

    @Column
    private String idempotencyKey;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "journal_entry_id")
    private JournalEntry journalEntry;

    @Column(name = "created_at", nullable = false)
    private Instant createdAt;

    @OneToMany(mappedBy = "payrollRun", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<com.bigbrightpaints.erp.modules.hr.domain.PayrollRunLine> lines = new java.util.ArrayList<>();

    @PrePersist
    public void prePersist() {
        if (publicId == null) {
            publicId = UUID.randomUUID();
        }
        if (createdAt == null) {
            createdAt = Instant.now();
        }
    }

    @PreUpdate
    public void preUpdate() {
        if (!"PAID".equals(status) || journalEntry == null) {
            return;
        }

        BigDecimal debitTotal = journalEntry.getLines().stream()
                .map(line -> line.getDebit() == null ? BigDecimal.ZERO : line.getDebit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);
        BigDecimal creditTotal = journalEntry.getLines().stream()
                .map(line -> line.getCredit() == null ? BigDecimal.ZERO : line.getCredit())
                .reduce(BigDecimal.ZERO, BigDecimal::add);

        if (debitTotal.compareTo(creditTotal) != 0) {
            throw new IllegalStateException("Payroll journal not balanced");
        }
        // Note: totalAmount stores gross wages, but journal debit may be less
        // when advances are deducted without a separate recovery account
        if (totalAmount.compareTo(debitTotal) < 0) {
            throw new IllegalStateException("Journal debit cannot exceed gross payroll total");
        }
    }

    public Long getId() { return id; }
    public UUID getPublicId() { return publicId; }
    public Company getCompany() { return company; }
    public void setCompany(Company company) { this.company = company; }
    public LocalDate getRunDate() { return runDate; }
    public void setRunDate(LocalDate runDate) { this.runDate = runDate; }
    public String getStatus() { return status; }
    public void setStatus(String status) { this.status = status; }
    public String getProcessedBy() { return processedBy; }
    public void setProcessedBy(String processedBy) { this.processedBy = processedBy; }
    public String getNotes() { return notes; }
    public void setNotes(String notes) { this.notes = notes; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public void setTotalAmount(BigDecimal totalAmount) { this.totalAmount = totalAmount; }

    public String getIdempotencyKey() { return idempotencyKey; }
    public void setIdempotencyKey(String idempotencyKey) { this.idempotencyKey = idempotencyKey; }
    public JournalEntry getJournalEntry() { return journalEntry; }
    public void setJournalEntry(JournalEntry journalEntry) { this.journalEntry = journalEntry; }
    public java.util.List<com.bigbrightpaints.erp.modules.hr.domain.PayrollRunLine> getLines() { return lines; }
    public void setLines(java.util.List<com.bigbrightpaints.erp.modules.hr.domain.PayrollRunLine> lines) { this.lines = lines; }
}
