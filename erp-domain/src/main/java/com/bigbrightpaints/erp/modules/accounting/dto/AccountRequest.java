package com.bigbrightpaints.erp.modules.accounting.dto;

import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public record AccountRequest(
    @NotBlank String code,
    @NotBlank String name,
    @NotNull AccountType type,
    Long parentId // Optional: parent account for hierarchy
    ) {
  // Convenience constructor for backward compatibility
  public AccountRequest(String code, String name, AccountType type) {
    this(code, name, type, null);
  }
}
