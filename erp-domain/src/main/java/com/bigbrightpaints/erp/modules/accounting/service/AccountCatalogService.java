package com.bigbrightpaints.erp.modules.accounting.service;

import java.util.List;

import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.bigbrightpaints.erp.core.exception.ApplicationException;
import com.bigbrightpaints.erp.core.exception.ErrorCode;
import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.accounting.domain.AccountRepository;
import com.bigbrightpaints.erp.modules.accounting.dto.AccountDto;
import com.bigbrightpaints.erp.modules.accounting.dto.AccountRequest;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.company.service.CompanyContextService;

@Service
class AccountCatalogService {

  private final CompanyContextService companyContextService;
  private final AccountRepository accountRepository;
  private final AccountingDtoMapperService accountingDtoMapperService;
  private final ApplicationEventPublisher eventPublisher;
  private final AccountingComplianceAuditService accountingComplianceAuditService;

  AccountCatalogService(
      CompanyContextService companyContextService,
      AccountRepository accountRepository,
      AccountingDtoMapperService accountingDtoMapperService,
      ApplicationEventPublisher eventPublisher,
      org.springframework.beans.factory.ObjectProvider<AccountingComplianceAuditService>
          accountingComplianceAuditServiceProvider) {
    this.companyContextService = companyContextService;
    this.accountRepository = accountRepository;
    this.accountingDtoMapperService = accountingDtoMapperService;
    this.eventPublisher = eventPublisher;
    this.accountingComplianceAuditService =
        accountingComplianceAuditServiceProvider.getIfAvailable();
  }

  List<AccountDto> listAccounts() {
    Company company = companyContextService.requireCurrentCompany();
    return accountRepository.findByCompanyOrderByCodeAsc(company).stream()
        .map(accountingDtoMapperService::toAccountDto)
        .toList();
  }

  @Transactional
  AccountDto createAccount(AccountRequest request) {
    Company company = companyContextService.requireCurrentCompany();
    Account account = new Account();
    account.setCompany(company);
    account.setCode(request.code());
    account.setName(request.name());
    account.setType(request.type());
    if (request.parentId() != null) {
      Account parent =
          accountRepository
              .findByCompanyAndId(company, request.parentId())
              .orElseThrow(
                  () ->
                      new ApplicationException(
                          ErrorCode.VALIDATION_INVALID_REFERENCE, "Parent account not found"));
      if (parent.getType() != request.type()) {
        throw new ApplicationException(
            ErrorCode.VALIDATION_INVALID_INPUT, "Child account must have same type as parent");
      }
      account.setParent(parent);
    }
    Account saved = accountRepository.save(account);
    if (company.getId() != null) {
      eventPublisher.publishEvent(
          new com.bigbrightpaints.erp.modules.accounting.event.AccountCacheInvalidatedEvent(
              company.getId()));
    }
    if (accountingComplianceAuditService != null) {
      accountingComplianceAuditService.recordAccountCreated(company, saved);
    }
    return accountingDtoMapperService.toAccountDto(saved);
  }
}
