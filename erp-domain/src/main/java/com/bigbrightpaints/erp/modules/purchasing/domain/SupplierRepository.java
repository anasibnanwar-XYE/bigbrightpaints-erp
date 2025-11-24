package com.bigbrightpaints.erp.modules.purchasing.domain;

import com.bigbrightpaints.erp.modules.accounting.domain.Account;
import com.bigbrightpaints.erp.modules.company.domain.Company;
import jakarta.persistence.LockModeType;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {

    List<Supplier> findByCompanyOrderByNameAsc(Company company);

    Optional<Supplier> findByCompanyAndId(Company company, Long id);

    Optional<Supplier> findByCompanyAndCodeIgnoreCase(Company company, String code);

    Optional<Supplier> findByCompanyAndPayableAccount(Company company, Account payableAccount);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("select s from Supplier s where s.company = :company and s.id = :id")
    Optional<Supplier> lockByCompanyAndId(@Param("company") Company company, @Param("id") Long id);
}
