package com.bigbrightpaints.erp.modules.accounting.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;

public interface AccountingPeriodTrialBalanceLineRepository
    extends JpaRepository<AccountingPeriodTrialBalanceLine, Long> {
  List<AccountingPeriodTrialBalanceLine> findBySnapshotOrderByAccountCodeAsc(
      AccountingPeriodSnapshot snapshot);

  Optional<AccountingPeriodTrialBalanceLine> findBySnapshotAndAccountId(
      AccountingPeriodSnapshot snapshot, Long accountId);

  void deleteBySnapshot(AccountingPeriodSnapshot snapshot);
}
