package com.bigbrightpaints.erp.modules.company.dto;

import jakarta.validation.constraints.NotNull;

public record TenantSeedMappingUpdateRequest(@NotNull Long accountId) {}
