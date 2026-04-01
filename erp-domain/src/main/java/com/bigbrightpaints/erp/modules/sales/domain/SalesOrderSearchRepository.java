package com.bigbrightpaints.erp.modules.sales.domain;

import java.time.Instant;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.bigbrightpaints.erp.modules.company.domain.Company;

public interface SalesOrderSearchRepository {

  Page<Long> searchIdsByCompany(
      Company company,
      String status,
      Dealer dealer,
      String orderNumber,
      Instant fromDate,
      Instant toDate,
      Pageable pageable);
}
