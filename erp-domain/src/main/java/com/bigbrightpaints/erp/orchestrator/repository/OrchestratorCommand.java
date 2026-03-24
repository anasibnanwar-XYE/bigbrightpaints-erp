package com.bigbrightpaints.erp.orchestrator.repository;

import java.time.Instant;
import java.util.UUID;

import com.bigbrightpaints.erp.core.domain.VersionedEntity;
import com.bigbrightpaints.erp.core.util.CompanyTime;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;

@Entity
@Table(name = "orchestrator_commands")
public class OrchestratorCommand extends VersionedEntity {

  public enum Status {
    IN_PROGRESS,
    SUCCESS,
    FAILED
  }

  @Id
  @GeneratedValue(strategy = GenerationType.UUID)
  private UUID id;

  @Column(name = "company_id", nullable = false)
  private Long companyId;

  @Column(name = "command_name", nullable = false, length = 64)
  private String commandName;

  @Column(name = "idempotency_key", nullable = false, length = 255)
  private String idempotencyKey;

  @Column(name = "request_hash", nullable = false, length = 64)
  private String requestHash;

  @Column(name = "trace_id", nullable = false, length = 128)
  private String traceId;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false, length = 32)
  private Status status;

  @Column(nullable = false)
  private Instant createdAt;

  @Column(nullable = false)
  private Instant updatedAt;

  @Column private String lastError;

  protected OrchestratorCommand() {}

  public OrchestratorCommand(
      Long companyId,
      String commandName,
      String idempotencyKey,
      String requestHash,
      String traceId) {
    this.companyId = companyId;
    this.commandName = commandName;
    this.idempotencyKey = idempotencyKey;
    this.requestHash = requestHash;
    this.traceId = traceId;
    this.status = Status.IN_PROGRESS;
    this.createdAt = CompanyTime.now();
    this.updatedAt = this.createdAt;
  }

  public UUID getId() {
    return id;
  }

  public Long getCompanyId() {
    return companyId;
  }

  public String getCommandName() {
    return commandName;
  }

  public String getIdempotencyKey() {
    return idempotencyKey;
  }

  public String getRequestHash() {
    return requestHash;
  }

  public String getTraceId() {
    return traceId;
  }

  public Status getStatus() {
    return status;
  }

  public Instant getCreatedAt() {
    return createdAt;
  }

  public Instant getUpdatedAt() {
    return updatedAt;
  }

  public String getLastError() {
    return lastError;
  }

  public void markSuccess() {
    this.status = Status.SUCCESS;
    this.lastError = null;
    this.updatedAt = CompanyTime.now();
  }

  public void markFailed(String error) {
    this.status = Status.FAILED;
    this.lastError = error;
    this.updatedAt = CompanyTime.now();
  }

  public void markRetry() {
    this.status = Status.IN_PROGRESS;
    this.lastError = null;
    this.updatedAt = CompanyTime.now();
  }
}
