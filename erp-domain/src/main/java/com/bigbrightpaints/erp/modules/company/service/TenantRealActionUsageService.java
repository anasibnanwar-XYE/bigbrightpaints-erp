package com.bigbrightpaints.erp.modules.company.service;

import java.util.Map;

import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.core.validation.ValidationUtils;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.domain.CompanyRepository;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminTenantEntitlementsDto;
import com.bigbrightpaints.erp.modules.company.dto.SuperAdminUsageDtos;

@Service
public class TenantRealActionUsageService {

  private final TenantUsageRollupService usageRollupService;
  private final SuperAdminTenantEntitlementService entitlementService;
  private final CompanyRepository companyRepository;

  public TenantRealActionUsageService(
      TenantUsageRollupService usageRollupService,
      SuperAdminTenantEntitlementService entitlementService,
      CompanyRepository companyRepository) {
    this.usageRollupService = usageRollupService;
    this.entitlementService = entitlementService;
    this.companyRepository = companyRepository;
  }

  public void enforcePdfExportAllowed(Company company) {
    enforce(company, "PDF_EXPORTS", 1L, null, null);
  }

  public void recordPdfExport(Company company) {
    usageRollupService.recordPdfExport(company);
  }

  public void enforceBusinessEmailAllowed(Company company) {
    enforce(company, "EMAILS", 1L, null, "BUSINESS");
  }

  public void recordBusinessEmail(Company company) {
    usageRollupService.recordEmailSend(company);
  }

  public void enforceJobSubmissionAllowed(Company company) {
    enforce(company, "JOBS", 1L, null, null);
  }

  public void enforceJobSubmissionAllowed(String companyCode) {
    enforceJobSubmissionAllowed(resolveCompany(companyCode));
  }

  public void recordJobSubmission(Company company) {
    usageRollupService.recordJobSubmission(company);
  }

  public void recordJobSubmission(String companyCode) {
    recordJobSubmission(resolveCompany(companyCode));
  }

  public void enforceStorageWriteAllowed(Company company, long bytes) {
    if (bytes <= 0L) {
      return;
    }
    enforce(company, "STORAGE", bytes, bytes, null);
  }

  public void recordStorageWrite(Company company, long bytes) {
    usageRollupService.recordStorageWrite(company, bytes);
  }

  public void recordStorageDelete(Company company, long bytes) {
    usageRollupService.recordStorageDelete(company, bytes);
  }

  private Company resolveCompany(String companyCode) {
    if (companyCode == null || companyCode.isBlank()) {
      throw ValidationUtils.invalidInput("Company code is required for quota enforcement");
    }
    return companyRepository
        .findByCodeIgnoreCase(companyCode.trim())
        .orElseThrow(() -> ValidationUtils.invalidInput("Company not found for quota enforcement"));
  }

  private void enforce(
      Company company, String dimension, Long units, Long bytes, String emailCategory) {
    if (company == null || company.getId() == null) {
      throw ValidationUtils.invalidInput("Company is required for quota enforcement");
    }
    SuperAdminTenantEntitlementsDto entitlements =
        entitlementService.getEffectiveEntitlements(company.getId());
    if (entitlements == null || entitlements.limits() == null) {
      throw ValidationUtils.invalidState("Tenant entitlements are required for quota enforcement");
    }
    Map<String, SuperAdminTenantEntitlementsDto.LimitEntitlement> limits =
        entitlements.limits();
    SuperAdminUsageDtos.QuotaActionResult result =
        usageRollupService.enforceQuotaAction(
            company.getId(),
            new SuperAdminUsageDtos.QuotaActionRequest(
                dimension, units, bytes, emailCategory, true),
            limits);
    if (result.accepted()) {
      return;
    }
    throw new ApplicationException(ErrorCode.BUSINESS_LIMIT_EXCEEDED, result.message())
        .withDetail("reasonCode", result.reasonCode())
        .withDetail("dimension", result.dimension())
        .withDetail("usedBefore", result.usedBefore())
        .withDetail("requestedUnits", result.requestedUnits())
        .withDetail("limit", result.limit())
        .withDetail("stateBefore", result.stateBefore());
  }
}
