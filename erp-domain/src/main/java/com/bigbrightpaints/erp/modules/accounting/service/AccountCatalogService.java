package com.bigbrightpaints.erp.modules.accounting.service;

import java.util.List;

import org.springframework.stereotype.Service;

import com.bigbrightpaints.erp.modules.accounting.dto.AccountDto;
import com.bigbrightpaints.erp.modules.accounting.dto.AccountRequest;

@Service
class AccountCatalogService {

  private final AccountingCoreSupport accountingCoreSupport;

  AccountCatalogService(AccountingCoreSupport accountingCoreSupport) {
    this.accountingCoreSupport = accountingCoreSupport;
  }

  List<AccountDto> listAccounts() {
    return accountingCoreSupport.listAccounts();
  }

  AccountDto createAccount(AccountRequest request) {
    return accountingCoreSupport.createAccount(request);
  }
}
