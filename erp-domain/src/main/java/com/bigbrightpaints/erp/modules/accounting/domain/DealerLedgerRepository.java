package com.bigbrightpaints.erp.modules.accounting.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import com.bigbrightpaints.erp.modules.accounting.dto.DealerBalanceView;
import com.bigbrightpaints.erp.modules.sales.domain.Dealer;
import java.util.Collection;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface DealerLedgerRepository extends JpaRepository<DealerLedgerEntry, Long> {

    List<DealerLedgerEntry> findByCompanyAndDealerOrderByEntryDateAsc(Company company, Dealer dealer);

    @Query("select new com.bigbrightpaints.erp.modules.accounting.dto.DealerBalanceView(e.dealer.id, coalesce(sum(e.debit - e.credit), 0)) " +
            "from DealerLedgerEntry e where e.company = :company and e.dealer.id in :dealerIds group by e.dealer.id")
    List<DealerBalanceView> aggregateBalances(@Param("company") Company company,
                                              @Param("dealerIds") Collection<Long> dealerIds);

    @Query("select new com.bigbrightpaints.erp.modules.accounting.dto.DealerBalanceView(e.dealer.id, coalesce(sum(e.debit - e.credit), 0)) " +
            "from DealerLedgerEntry e where e.company = :company and e.dealer = :dealer group by e.dealer.id")
    Optional<DealerBalanceView> aggregateBalance(@Param("company") Company company, @Param("dealer") Dealer dealer);
}
