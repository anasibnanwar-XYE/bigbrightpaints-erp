package com.bigbrightpaints.erp.modules.hr.domain;

import com.bigbrightpaints.erp.modules.company.domain.Company;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, Long> {
    List<LeaveRequest> findByCompanyOrderByCreatedAtDesc(Company company);
    Optional<LeaveRequest> findByCompanyAndId(Company company, Long id);

    @Query("select case when count(l) > 0 then true else false end from LeaveRequest l " +
            "where l.employee.id = :employeeId and l.startDate <= :endDate and l.endDate >= :startDate " +
            "and upper(l.status) not in ('REJECTED', 'CANCELLED')")
    boolean existsOverlappingByEmployeeIdAndDates(@Param("employeeId") Long employeeId,
                                                  @Param("startDate") LocalDate startDate,
                                                  @Param("endDate") LocalDate endDate);
}
