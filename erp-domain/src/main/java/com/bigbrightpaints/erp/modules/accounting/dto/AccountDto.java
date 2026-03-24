package com.bigbrightpaints.erp.modules.accounting.dto;

import java.math.BigDecimal;
import java.util.UUID;

import com.bigbrightpaints.erp.modules.accounting.domain.AccountType;

public record AccountDto(
    Long id, UUID publicId, String code, String name, AccountType type, BigDecimal balance) {}
