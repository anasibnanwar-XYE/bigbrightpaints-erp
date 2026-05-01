package com.bigbrightpaints.erp.modules.company.service;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface TenantSupportControlPort {

  int recalculateActiveTenantTicketsForSupportTierChange(
      Company company, String oldSupportTier, String newSupportTier, Long planAuditEventId);

  long countOpenSupportTickets();

  long countOpenBugs();
}
