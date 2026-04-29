package com.bigbrightpaints.erp.modules.company.domain;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import jakarta.persistence.LockModeType;

public interface SuperAdminBillingSubscriptionRepository
    extends JpaRepository<SuperAdminBillingSubscription, Long> {

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select s from SuperAdminBillingSubscription s where s.company.id = :companyId and"
          + " s.status in ('TRIAL', 'MANUAL', 'ACTIVE') order by s.createdAt desc, s.id desc")
  List<SuperAdminBillingSubscription> lockActiveByCompanyId(@Param("companyId") Long companyId);

  @Query(
      "select s from SuperAdminBillingSubscription s where s.company.id = :companyId order by"
          + " s.createdAt desc, s.id desc")
  List<SuperAdminBillingSubscription> findByCompanyIdOrderByCreatedAtDesc(
      @Param("companyId") Long companyId);

  Optional<SuperAdminBillingSubscription> findTopByCompanyIdOrderByCreatedAtDescIdDesc(
      Long companyId);

  @Query(
      "select s from SuperAdminBillingSubscription s where s.status in ('TRIAL', 'MANUAL',"
          + " 'ACTIVE', 'CANCELED', 'ARCHIVED')")
  List<SuperAdminBillingSubscription> findAllForMetrics();

  @Lock(LockModeType.PESSIMISTIC_WRITE)
  @Query(
      "select s from SuperAdminBillingSubscription s join fetch s.company where"
          + " s.pendingCommercialAction in ('CANCEL', 'ARCHIVE') order by"
          + " s.pendingCommercialEffectiveAt asc, s.id asc")
  List<SuperAdminBillingSubscription> lockPendingCommercialActions();
}
