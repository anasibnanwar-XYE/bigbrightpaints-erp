package com.bigbrightpaints.erp.modules.company.dto;

import java.util.Set;

public record SuperAdminTenantSupportContextDto(
    Long companyId, String companyCode, Set<String> supportTags) {
  public SuperAdminTenantSupportContextDto(
      Long companyId, String companyCode, String supportNotes, Set<String> supportTags) {
    this(companyId, companyCode, supportTags);
  }
}
